# 鸿蒙原生代理调研

目标：HarmonyOS 6 / API 20，ARM64，优先完成基础代理上网。记录日期 2026-10-21。

## 实现路线

ArkUI → VpnExtensionAbility → TUN fd → HEV SOCKS5 转换 → 本地 Shadowsocks Rust 核心 → 自备远端节点。

VPN API 只提供虚拟网卡，不包含代理协议。Android 的 AAR、VpnService 和 Compose 界面不能直接重打包成鸿蒙原生应用，因此本目录采用独立 Stage 工程。

API 20 路径使用 `blockedApplications` 排除本应用，避免核心出站被重新送入 VPN。`protectProcessNet` 是 API 22 接口，不用于此原型。首次启动 VPN 时 Want 参数的 API 版本限制，也通过先保存配置、扩展启动后读配置规避。

Rust 官方提供 OHOS 目标平台，优先试验可原生编译的 Shadowsocks，避免先解决 Go/Xray 的鸿蒙运行时移植。协议收窄意味着现有 VMess/VLESS 节点暂时不可使用。

## 一手来源

- [OpenHarmony VPN API](https://github.com/openharmony/docs/blob/master/en/application-dev/reference/apis-network-kit/js-apis-net-vpnExtension.md)
- [OpenHarmony 6.0 Release SDK 下载及版本记录](https://github.com/openharmony/docs/blob/master/zh-cn/release-notes/OpenHarmony-v6.0-release.md)
- [Rust 官方 OpenHarmony 平台说明](https://doc.rust-lang.org/rustc/platform-support/openharmony.html)
- [Shadowsocks Rust 源码](https://github.com/shadowsocks/shadowsocks-rust)
- [HEV SOCKS5 Tunnel 源码](https://github.com/heiher/hev-socks5-tunnel)
- [AndroidLiquidGlass 原项目](https://github.com/Kyant0/AndroidLiquidGlass)，Android 图形实现不能作为 ArkUI 原生控件直接复用。

作为可行性参考阅读了 shadowsocks/shadowsocks-ohos 及 wgc0303/Xray-HarmonyOS；未将其应用代码复制进本目录。

## 本机工具与构建边界

公开 M1 SDK 下载文件为 `L2-SDK-MAC-M1-PUBLIC.tar.gz`，SHA-256：

`d4b0c942cbc8dfc7f28f49c0f33648f332d43c3a51399f2463655c2ef1f2c659`

解包得到 API 20、6.0.0.47、Beta1 标签组件。HEV 已生成 ARM64 ELF。Hvigor 的实际失败原因是 SDK 许可尚未接受，并缺 Previewer 组件；未绕过许可检查。许可原文另存于本机 `deliverables/harmony-prototype/OpenHarmony-SDK-LICENSE.txt` 供开发者确认。

下一道有效性门槛依次是完整原生链接、ArkTS 编译、HAP 打包、正式 HarmonyOS SDK 适配、开发者签名和真机 VPN 上网测试。当前尚未越过这些门槛，不应据源码推断手机可用。
