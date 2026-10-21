//! First-party C ABI for the MIT-licensed shadowsocks-service crate.
//! Called on the N-API worker queue, never on the ArkUI thread. No configuration
//! or error text is logged: errors may contain server credentials.
use shadowsocks_service::{config::{Config, ConfigType}, local::Server};
use std::{ffi::{c_char, CStr}, sync::{atomic::{AtomicBool, Ordering}, Mutex}, time::Duration};
use tokio::runtime::Runtime;

static RUNTIME: Mutex<Option<Runtime>> = Mutex::new(None);
static RUNNING: AtomicBool = AtomicBool::new(false);

/// # Safety
/// `json` must be null or point to a valid, NUL-terminated UTF-8 string for this call.
#[no_mangle]
pub unsafe extern "C" fn ff_ss_start(json: *const c_char) -> i32 {
    if json.is_null() { return -1; }
    let Ok(text) = CStr::from_ptr(json).to_str() else { return -1; };
    if text.len() > 65536 { return -1; }
    let Ok(mut slot) = RUNTIME.lock() else { return -5; };
    if slot.is_some() { return -3; }
    let Ok(config) = Config::load_from_str(text, ConfigType::Local) else { return -2; };
    // Parsing alone accepts empty configurations. Validate before entering the
    // upstream constructor, whose preconditions include a nonempty local list.
    if config.check_integrity().is_err() || config.server.is_empty() {
        return -2;
    }
    let Ok(runtime) = tokio::runtime::Builder::new_multi_thread()
        .worker_threads(2).enable_all().build() else { return -5; };
    // Unlike spawning run_local and immediately reporting success, this waits
    // until the local TCP/UDP listeners are actually bound.
    let Ok(server) = runtime.block_on(Server::new(config)) else { return -4; };
    RUNNING.store(true, Ordering::Release);
    runtime.spawn(async move {
        let _ = server.run().await;
        RUNNING.store(false, Ordering::Release);
    });
    *slot = Some(runtime);
    0
}

#[no_mangle]
pub extern "C" fn ff_ss_stop() -> i32 {
    let Ok(mut slot) = RUNTIME.lock() else { return -5; };
    RUNNING.store(false, Ordering::Release);
    // Keep the lock until tasks finish so an old task cannot clear a new
    // session's RUNNING flag. The caller is a native worker, not the UI thread.
    if let Some(runtime) = slot.take() { runtime.shutdown_timeout(Duration::from_secs(2)); }
    0
}

#[no_mangle]
pub extern "C" fn ff_ss_running() -> i32 { RUNNING.load(Ordering::Acquire) as i32 }

#[cfg(test)]
mod tests {
    use super::*;
    use std::{ffi::CString, io::{Read, Write}, net::{TcpListener, TcpStream, UdpSocket}, thread};

    #[test]
    fn encrypted_proxy_transfers_bytes_and_stops_without_leaking_listener() {
        assert_eq!(unsafe { ff_ss_start(std::ptr::null()) }, -1);
        assert_eq!(unsafe { ff_ss_start(CString::new("{}").unwrap().as_ptr()) }, -2);
        assert_eq!(unsafe { ff_ss_start(CString::new("{bad json").unwrap().as_ptr()) }, -2);
        assert_eq!(ff_ss_running(), 0);
        for method in ["aes-256-gcm", "aes-128-gcm", "chacha20-ietf-poly1305"] {
            let destination = TcpListener::bind("127.0.0.1:0").unwrap();
            let destination_port = destination.local_addr().unwrap().port();
            let echo = thread::spawn(move || {
                let (mut stream, _) = destination.accept().unwrap();
                stream.set_read_timeout(Some(Duration::from_secs(5))).unwrap();
                let mut input = [0; 20]; stream.read_exact(&mut input).unwrap();
                assert_eq!(&input, b"futurefast-encrypted");
                stream.write_all(b"proxy-ok").unwrap();
            });
            let reserve = TcpListener::bind("127.0.0.1:0").unwrap();
            let server_port = reserve.local_addr().unwrap().port(); drop(reserve);
            let remote = Runtime::new().unwrap();
            let config = Config::load_from_str(&format!(r#"{{"server":"127.0.0.1","server_port":{server_port},"password":"local-test-only","method":"{method}","mode":"tcp_and_udp"}}"#), ConfigType::Server).unwrap();
            remote.spawn(shadowsocks_service::server::run(config));
            let mut listening = false;
            for _ in 0..100 {
                if TcpStream::connect(("127.0.0.1", server_port)).is_ok() { listening = true; break; }
                thread::sleep(Duration::from_millis(10));
            }
            assert!(listening, "loopback fixture must bind before testing");
            let reserve = TcpListener::bind("127.0.0.1:0").unwrap();
            let local_port = reserve.local_addr().unwrap().port(); drop(reserve);
            let text = CString::new(format!(r#"{{"server":"127.0.0.1","server_port":{server_port},"password":"local-test-only","method":"{method}","local_address":"127.0.0.1","local_port":{local_port},"mode":"tcp_and_udp"}}"#)).unwrap();
            let occupied = TcpListener::bind(("127.0.0.1", local_port)).unwrap();
            assert_eq!(unsafe { ff_ss_start(text.as_ptr()) }, -4);
            assert_eq!(ff_ss_running(), 0);
            drop(occupied);
            assert_eq!(unsafe { ff_ss_start(text.as_ptr()) }, 0);
            assert_eq!(unsafe { ff_ss_start(text.as_ptr()) }, -3);
            assert_eq!(ff_ss_running(), 1);
            let mut client = TcpStream::connect(("127.0.0.1", local_port)).unwrap();
            client.set_read_timeout(Some(Duration::from_secs(5))).unwrap();
            client.write_all(&[5, 1, 0]).unwrap();
            let mut greeting = [0; 2]; client.read_exact(&mut greeting).unwrap(); assert_eq!(greeting, [5, 0]);
            let [hi, lo] = destination_port.to_be_bytes();
            client.write_all(&[5, 1, 0, 1, 127, 0, 0, 1, hi, lo]).unwrap();
            let mut reply = [0; 10]; client.read_exact(&mut reply).unwrap(); assert_eq!(reply[1], 0);
            client.write_all(b"futurefast-encrypted").unwrap();
            let mut answer = [0; 8]; client.read_exact(&mut answer).unwrap(); assert_eq!(&answer, b"proxy-ok");
            echo.join().unwrap(); drop(client);
            // Exercise the UDP path used by tunneled DNS, independently of HEV.
            let destination_udp = UdpSocket::bind("127.0.0.1:0").unwrap();
            destination_udp.set_read_timeout(Some(Duration::from_secs(5))).unwrap();
            let udp_port = destination_udp.local_addr().unwrap().port();
            let udp_echo = thread::spawn(move || {
                let mut data = [0; 64];
                let (len, peer) = destination_udp.recv_from(&mut data).unwrap();
                assert_eq!(&data[..len], b"test-udp");
                destination_udp.send_to(b"udp-ok", peer).unwrap();
            });
            let mut association = TcpStream::connect(("127.0.0.1", local_port)).unwrap();
            association.set_read_timeout(Some(Duration::from_secs(5))).unwrap();
            association.write_all(&[5, 1, 0]).unwrap();
            association.read_exact(&mut greeting).unwrap(); assert_eq!(greeting, [5, 0]);
            association.write_all(&[5, 3, 0, 1, 0, 0, 0, 0, 0, 0]).unwrap();
            association.read_exact(&mut reply).unwrap();
            assert_eq!(&reply[..4], &[5, 0, 0, 1]);
            let relay_port = u16::from_be_bytes([reply[8], reply[9]]);
            let udp_client = UdpSocket::bind("127.0.0.1:0").unwrap();
            udp_client.set_read_timeout(Some(Duration::from_secs(5))).unwrap();
            let [hi, lo] = udp_port.to_be_bytes();
            let mut packet = vec![0, 0, 0, 1, 127, 0, 0, 1, hi, lo];
            packet.extend_from_slice(b"test-udp");
            udp_client.send_to(&packet, ("127.0.0.1", relay_port)).unwrap();
            let mut response = [0; 128];
            let (len, _) = udp_client.recv_from(&mut response).unwrap();
            assert_eq!(&response[..4], &[0, 0, 0, 1]);
            assert_eq!(&response[10..len], b"udp-ok");
            udp_echo.join().unwrap(); drop(association);
            assert_eq!(ff_ss_stop(), 0); assert_eq!(ff_ss_stop(), 0); assert_eq!(ff_ss_running(), 0);
            let rebound = TcpListener::bind(("127.0.0.1", local_port)).unwrap(); drop(rebound);
            assert_eq!(unsafe { ff_ss_start(text.as_ptr()) }, 0);
            assert_eq!(ff_ss_running(), 1);
            assert_eq!(ff_ss_stop(), 0);
            remote.shutdown_timeout(Duration::from_secs(2));
        }
    }
}
