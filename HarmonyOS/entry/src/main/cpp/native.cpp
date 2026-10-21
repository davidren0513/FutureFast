#include <napi/native_api.h>
#include <atomic>
#include <chrono>
#include <mutex>
#include <string>
#include <thread>
#include <unistd.h>

extern "C" {
int ff_ss_start(const char *json);
int ff_ss_stop();
int ff_ss_running();
int ff_hev_ready();
int hev_socks5_tunnel_main_from_str(const unsigned char *, unsigned int, int);
void hev_socks5_tunnel_quit();
}

namespace {
std::mutex operation;
std::thread tunnel;
std::atomic<bool> tunnelLive{false};

void StopLocked() {
    if (tunnel.joinable()) {
        if (tunnelLive.load()) hev_socks5_tunnel_quit();
        tunnel.join();
    }
    ff_ss_stop();
}

int StartLocked(int sourceFd, const std::string &json, const std::string &yaml) {
    if (sourceFd < 0 || json.empty() || yaml.empty()) return -1;
    if (tunnel.joinable() && tunnelLive.load()) return -3;
    StopLocked();
    int result = ff_ss_start(json.c_str());
    if (result != 0) return result;
    int fd = dup(sourceFd);
    if (fd < 0) { ff_ss_stop(); return -6; }
    // The system owns sourceFd. HEV borrows the duplicate; this thread owns
    // and closes that duplicate exactly once, after HEV has finished using it.
    tunnelLive.store(true);
    try {
        tunnel = std::thread([fd, yaml] {
            hev_socks5_tunnel_main_from_str(
                reinterpret_cast<const unsigned char *>(yaml.data()), yaml.size(), fd);
            tunnelLive.store(false);
            close(fd);
        });
    } catch (...) { tunnelLive.store(false); close(fd); ff_ss_stop(); return -5; }
    for (int i = 0; i < 250; ++i) {
        if (!tunnelLive.load()) { StopLocked(); return -7; }
        if (ff_hev_ready() != 0) return 0;
        std::this_thread::sleep_for(std::chrono::milliseconds(20));
    }
    StopLocked();
    return -8;
}

struct Work {
    napi_async_work work{};
    napi_deferred deferred{};
    bool start{false};
    int fd{-1};
    int result{-1};
    std::string json;
    std::string yaml;
};

bool ReadString(napi_env env, napi_value value, std::string &out) {
    size_t size = 0;
    if (napi_get_value_string_utf8(env, value, nullptr, 0, &size) != napi_ok || size > 65536) return false;
    out.resize(size + 1);
    if (napi_get_value_string_utf8(env, value, out.data(), out.size(), &size) != napi_ok) return false;
    out.resize(size);
    return out.find('\0') == std::string::npos;
}

napi_value Queue(napi_env env, napi_callback_info info, bool start) {
    auto *data = new Work();
    data->start = start;
    if (start) {
        size_t count = 3;
        napi_value args[3]{};
        napi_get_cb_info(env, info, &count, args, nullptr, nullptr);
        if (count != 3 || napi_get_value_int32(env, args[0], &data->fd) != napi_ok ||
            !ReadString(env, args[1], data->json) || !ReadString(env, args[2], data->yaml)) {
            delete data;
            napi_throw_type_error(env, nullptr, "Invalid native start arguments");
            return nullptr;
        }
    }
    napi_value promise{}, label{};
    napi_create_promise(env, &data->deferred, &promise);
    napi_create_string_utf8(env, "FutureFastSession", NAPI_AUTO_LENGTH, &label);
    auto execute = [](napi_env, void *raw) {
        auto *data = static_cast<Work *>(raw);
        std::lock_guard<std::mutex> guard(operation);
        data->result = data->start ? StartLocked(data->fd, data->json, data->yaml) : (StopLocked(), 0);
        // Release credential buffers once startup is complete.
        data->json.clear(); data->yaml.clear();
    };
    auto complete = [](napi_env env, napi_status status, void *raw) {
        auto *data = static_cast<Work *>(raw);
        napi_value result{};
        napi_create_int32(env, status == napi_ok ? data->result : -5, &result);
        napi_resolve_deferred(env, data->deferred, result);
        napi_delete_async_work(env, data->work);
        delete data;
    };
    if (napi_create_async_work(env, nullptr, label, execute, complete, data, &data->work) != napi_ok ||
        napi_queue_async_work(env, data->work) != napi_ok) {
        napi_value error{};
        napi_create_int32(env, -5, &error);
        napi_resolve_deferred(env, data->deferred, error);
        if (data->work) napi_delete_async_work(env, data->work);
        delete data;
    }
    return promise;
}

napi_value Start(napi_env env, napi_callback_info info) { return Queue(env, info, true); }
napi_value Stop(napi_env env, napi_callback_info info) { return Queue(env, info, false); }
napi_value Running(napi_env env, napi_callback_info) {
    napi_value result{};
    napi_get_boolean(env, tunnelLive.load() && ff_ss_running() != 0, &result);
    return result;
}
napi_value Init(napi_env env, napi_value exports) {
    napi_property_descriptor props[] = {
        {"start", nullptr, Start, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"stop", nullptr, Stop, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"isRunning", nullptr, Running, nullptr, nullptr, nullptr, napi_default, nullptr}
    };
    napi_define_properties(env, exports, 3, props);
    return exports;
}
napi_module module = {1, 0, nullptr, Init, "futurefast", nullptr, {0}};
__attribute__((constructor)) void Register() { napi_module_register(&module); }
}
