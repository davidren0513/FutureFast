# Future Fast · HarmonyOS 原型

面向 HarmonyOS 6 手机的原生 ARM64 探索版本。当前目标是先实现基础代理上网。

**状态：已完成原生链接、ArkTS 编译和 ARM64 未签名 HAP 打包；尚未完成鸿蒙真机验证。** 当前产物是公开 OpenHarmony SDK 的实验构建，不能直接当作已签名、可安装的 HarmonyOS 手机版本。Android 版本不受此目录影响。

## 当前范围

- ArkUI 原生界面、FF 图标、玻璃质感背景和连接按钮。
- 单节点手动配置，以及 SIP002 / 旧格式 `ss://` 导入。
- Shadowsocks：`aes-256-gcm`、`aes-128-gcm`、`chacha20-ietf-poly1305`。
- VpnExtensionAbility 接管流量，HEV 转换 TUN 到本地 SOCKS5，Rust 核心加密转发。
- 首次使用条款确认、配置持久化、连接心跳、超时处理和断开清理。

暂未实现 Android 版本的全部协议、订阅、AI 独立节点、完整液态玻璃折射与水珠拖动。含插件的 Shadowsocks 链接会被拒绝，不会静默丢弃插件配置。本原型不提供节点或订阅服务，需自备有效 Shadowsocks 节点；服务端需要支持 UDP 才能验证当前 DNS 路径。

VPN 启动仅表示本地隧道和核心就绪，不表示远端节点可用。界面据此显示隧道状态。应用自身通过 API 20 的 `blockedApplications` 排除在 VPN 外，避免代理传输回环；不能用应用自身的网络请求证明 VPN 接管成功。

## 已做验证

- `node --experimental-strip-types --test HarmonyOS/tests/*.test.ts`：9 项通过。
- `cargo test --locked --offline --manifest-path HarmonyOS/native/shadowsocks/Cargo.toml`：1 项端到端测试通过，覆盖真实本地 SOCKS5 → Shadowsocks 三种 AEAD 加密方式 → TCP / UDP 目标端往返、无效配置拒绝、端口占用失败、重复启动拒绝、断开释放端口及重新启动。
- HEV 已用公开 SDK 的 ARM64 Clang 编译为 AArch64 ELF 共享库。生成物不纳入 Git。

- Rust 核心 OHOS 交叉编译、N-API 链接及 `hvigor assembleHap` 已通过。
- `scripts/verify-hap.py` 核对 HAP 的 ArkTS 字节码、VPN 扩展、三份 AArch64 共享库和许可文件通过；未携带构建机 RPATH。
- 已在用户明确同意后登记 SDK 协议并安装官方 Previewer 组件。源码脚本不会自动接受 SDK 协议。

默认产物：`entry/build/default/outputs/default/entry-default-unsigned.hap`。本次交付副本保存在仓库本机 `deliverables/harmony-prototype/` 下。

**Not run**：HarmonyOS 6 真机上的 VPN 授权、首次启动/重连/撤销授权、后台运行、TCP/UDP/DNS/IPv6 上网、网络切换，以及 ArkUI 渲染和触控。尚无已连接、已签名授权的鸿蒙测试设备。

## 构建准备

1. 安装 DevEco Studio 和匹配 HarmonyOS 6 的 SDK，阅读并接受 SDK 许可。
2. 安装支持 `aarch64-unknown-linux-ohos` 的 Rust 工具链、Node 和 Hvigor。研究环境用了 Rust 1.99.1、Hvigor/插件 6.0.0。
3. `git submodule update --init --recursive`，保持 HEV 为仓库 gitlink 指定版本。
4. 配置 `OHOS_NDK_HOME` 指向 SDK 的 `native` 目录、`OHOS_SDK_HOME` 指向 SDK 根目录。将 `sdk.dir` 写入不提交的 `local.properties`。
5. 在工程本地安装匹配工具，例如 `npm install --prefix HarmonyOS --no-save --package-lock=false --registry https://repo.harmonyos.com/npm/ @ohos/hvigor@6.0.0 @ohos/hvigor-ohos-plugin@6.0.0`；通过 IDE/ohpm 安装 `entry/oh-package.json5` 的本地 N-API 类型依赖。

```sh
bash HarmonyOS/scripts/build-hev.sh
bash HarmonyOS/scripts/build-ss.sh
python3 HarmonyOS/scripts/collect-licenses.py
cd HarmonyOS
node node_modules/@ohos/hvigor/bin/hvigor.js --mode module \
  -p product=default -p module=entry@default --no-daemon assembleHap
python3 scripts/verify-hap.py entry/build/default/outputs/default/entry-default-unsigned.hap
```

当前 `build-profile.json5` 使用公开 OpenHarmony API 20，作为移植编译路径；它不等于已完成华为 HarmonyOS 发行配置。研究下载的 OpenHarmony 6.0 Release 包内组件实际标为 `6.0.0.47 / Beta1`。本配置的设备类型为公开 SDK 的 `default`，上机前应在 DevEco 中选用正式 HarmonyOS SDK，调整为目标手机的设备类型，并验证 API、VPN 能力与签名配置。不要将实验性公共 SDK 的构建结果直接称作正式鸿蒙发行版。

手机安装还需要华为开发者签名、包含测试设备的 Profile，以及设备侧安装授权。仓库不包含证书、私钥、Profile，也不会生成“通用已签名安装包”。

## 依赖与研究

见 [调研记录](docs/RESEARCH.md) 和 [第三方说明](licenses/NOTICE.md)。Cargo.lock 固定 Rust 依赖；HEV 构建脚本核对 gitlink，并仅在构建副本中增加只读就绪状态导出，不改 Android 的原始依赖。
