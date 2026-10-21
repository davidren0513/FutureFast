# Future Fast · HarmonyOS 原型

面向 HarmonyOS 6 手机的原生 ARM64 探索版本。当前目标是先实现基础代理上网。

**状态：源码原型，尚未完成 ArkTS 编译、HAP 打包和鸿蒙真机验证，不能作为已可用安装包交付。** 本机加密转发测试通过不代表手机 VPN 已可用。Android 版本不受此目录影响。

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
- `cargo test --locked --offline --manifest-path HarmonyOS/native/shadowsocks/Cargo.toml`：1 项端到端测试通过，覆盖真实本地 SOCKS5 → Shadowsocks AES-256-GCM → TCP 目标端往返、无效配置拒绝、端口占用失败、重复启动拒绝、断开释放端口及重新启动。
- HEV 已用公开 SDK 的 ARM64 Clang 编译为 AArch64 ELF 共享库。生成物不纳入 Git。

**Not run**：Rust 核心的 OHOS 交叉编译、N-API 链接、`hvigor assembleHap`，目前构建器要求确认 OpenHarmony SDK 使用协议并补齐组件。许可接受应由实际使用 SDK 的开发者确认，脚本不会自动接受协议。

**Not run**：HarmonyOS 6 真机上的 VPN 授权、首次启动/重连/撤销授权、后台运行、TCP/UDP/DNS/IPv6 上网、网络切换，以及 ArkUI 渲染和触控。尚无已连接、已签名授权的鸿蒙测试设备。

## 构建准备

1. 安装 DevEco Studio 和匹配 HarmonyOS 6 的 SDK，阅读并接受 SDK 许可。
2. 安装支持 `aarch64-unknown-linux-ohos` 的 Rust 工具链、Node 和 Hvigor。研究环境用了 Rust 1.99.1、Hvigor/插件 6.0.0。
3. `git submodule update --init --recursive`，保持 HEV 为仓库 gitlink 指定版本。
4. 配置 `OHOS_NDK_HOME` 指向 SDK 的 `native` 目录、`OHOS_SDK_HOME` 指向 SDK 根目录。将 `sdk.dir` 写入不提交的 `local.properties`。
5. 在工程本地安装与 SDK 匹配的 `@ohos/hvigor`、`@ohos/hvigor-ohos-plugin`，通过 IDE/ohpm 安装 `entry/oh-package.json5` 的本地 N-API 类型依赖。

```sh
bash HarmonyOS/scripts/build-hev.sh
bash HarmonyOS/scripts/build-ss.sh
cd HarmonyOS
node node_modules/@ohos/hvigor/bin/hvigor.js --mode module \
  -p product=default -p module=entry@default --no-daemon assembleHap
```

当前 `build-profile.json5` 使用公开 OpenHarmony API 20，作为移植编译路径；它不等于已完成华为 HarmonyOS 发行配置。研究下载的 OpenHarmony 6.0 Release 包内组件实际标为 `6.0.0.47 / Beta1`。上机前应在 DevEco 中选用正式 HarmonyOS SDK，并验证 API、VPN 能力与签名配置。不要将实验性公共 SDK 的构建结果直接称作正式鸿蒙发行版。

手机安装还需要华为开发者签名、包含测试设备的 Profile，以及设备侧安装授权。仓库不包含证书、私钥、Profile，也不会生成“通用已签名安装包”。

## 依赖与研究

见 [调研记录](docs/RESEARCH.md) 和 [第三方说明](licenses/NOTICE.md)。Cargo.lock 固定 Rust 依赖；HEV 构建脚本核对 gitlink，并仅在构建副本中增加只读就绪状态导出，不改 Android 的原始依赖。
