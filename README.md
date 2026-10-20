<div align="center">

# FutureFast

**让连接更简单。**

由 **David Ren** 维护的免费 Android 客户端，面向 Just My Socks 使用场景。<br>
液态玻璃界面 · 一键连接 · 自动管理节点 · AI 独立分流

[下载预览版](#下载与安装) · [实际界面](#实际界面) · [功能说明](#功能说明) · [验证记录](docs/VALIDATION.md)

[![Android build](https://github.com/davidren0513/FutureFast/actions/workflows/future-fast-preview.yml/badge.svg?branch=master)](https://github.com/davidren0513/FutureFast/actions/workflows/future-fast-preview.yml)
![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3DDC84)
![Preview 0.1.1](https://img.shields.io/badge/Preview-0.1.1-2878FF)
[![License GPL v3](https://img.shields.io/badge/License-GPLv3-blue)](LICENSE)

</div>

## 实际界面

<p align="center">
  <img src="docs/screenshots/home.png" width="250" alt="FutureFast 0.1.1 实际连接首页">
  <img src="docs/screenshots/automation.png" width="250" alt="FutureFast 0.1.1 实际自动管理页面">
  <img src="docs/screenshots/ai.png" width="250" alt="FutureFast 0.1.1 实际 AI 分流页面">
</p>

<details>
<summary>查看深色首页</summary>
<p align="center"><img src="docs/screenshots/home-dark.png" width="300" alt="FutureFast 0.1.1 实际深色首页"></p>
</details>

<p align="center"><sub>0.1.1 预览版 · Android 35 模拟器实际截图 · 简体中文 · 尚未导入节点</sub></p>

0.1.1 按最初三张设计图重做了流动背景、玻璃圆形主按钮、彩色边缘高光、卡片图标和悬浮导航。Android 13 及以上使用实时背景采样、模糊与透镜折射；较旧系统保留渐变与高光降级效果。

首页把连接操作放在中央：点击大按钮连接或断开，下方显示当前或下次使用的节点。节点管理和 AI 分流从底部进入，自动管理从首页卡片进入。

截图来自[成功构建与启动检查](https://github.com/davidren0513/FutureFast/actions/runs/39401539475)，没有使用虚构节点或演示延迟。截图来源与环境见[截图说明](docs/screenshots/README.md)。

## 下载与安装

当前提供 **FutureFast 0.1.1 预览版**，尚未发布正式稳定版。

| 下载入口 | 用途 |
| --- | --- |
| [ARM64 手机预览包](https://github.com/davidren0513/FutureFast/actions/runs/39401539475/artifacts/12255236498) | 大多数现代 Android 手机；ZIP 内包含 APK |
| [最新构建列表](https://github.com/davidren0513/FutureFast/actions/workflows/future-fast-preview.yml) | 选择成功的构建，在 Artifacts 中下载 `FutureFast-0.1.1-arm64` |
| [完整构建与其他架构](https://github.com/davidren0513/FutureFast/actions/runs/39401539475) | `FutureFast-0.1.1-preview` 包含各架构 APK 和 SHA256SUMS |

GitHub Artifacts 下载需要登录 GitHub，当前保存 14 天。固定下载链接过期后，请从最新成功构建获取。

**旧预览版升级提示：** 早期构建的测试签名没有成功缓存，不能保证不同构建可以覆盖安装。如果系统提示签名冲突，请先在旧应用导出并妥善保存配置，确认可恢复后再卸载旧版、安装新版并导入；不要直接卸载唯一保存配置的旧应用。正式发布签名仍待配置。

1. 下载并解压 ZIP，在 Android 手机上安装适合设备架构的 APK。
2. 打开 Future Fast，粘贴你自己的订阅地址，或扫描节点二维码。
3. 在“节点”中确认导入结果，回到首页点击连接，按系统提示授予 VPN 权限。
4. 按需开启自动管理，或为 AI 服务指定节点。

最低支持 **Android 7.0**。预览包使用测试签名；Play Store 构建变体的应用 ID 为 `tech.future.fast`，可与原版 v2rayNG 并存。这里的构建变体名称不代表已上架 Google Play。本应用不附带订阅、服务器或 AI 账户。

## 功能说明

| 功能 | 0.1.1 预览版行为 |
| --- | --- |
| 液态玻璃首页 | 简化连接操作；Android 13 及以上使用模糊与折射，较旧系统使用半透明效果 |
| 一键连接 / 断开 | 中央大按钮控制连接，下方显示节点 |
| 订阅自动更新 | 可设置更新间隔；成功导入后才记录更新时间 |
| 断开时节点优选 | 按设置间隔测试真实出站延迟，为下次连接选择最快的有效节点；活动连接期间暂停 |
| AI 独立分流 | ChatGPT、Claude、Gemini 可分别指定节点，或跟随默认节点；配置变更需重新连接 |
| 保留高级功能 | 原节点管理、路由与高级设置仍可访问，首版保留这些页面的原有布局 |
| 导入保护 | 检测 HTML 错误页和不兼容的 Clash YAML 响应，避免把这些内容当作普通节点订阅导入 |

订阅更新和自动优选默认关闭。间隔支持 **15–10080 分钟**，后台实际执行时间受 Android 调度、省电策略和网络状态影响。单个节点的测速超时由现有 Xray 核心控制，当前未提供自定义硬超时。

AI 分流按已配置的域名规则工作。指定节点丢失时，对匹配流量采用阻断策略；订阅刷新后的节点匹配仅在结果明确时恢复。节点延迟低不代表某个 AI 平台一定可用。

## Just My Socks 使用说明

- 使用适用于本客户端的普通节点订阅；专用 Mihomo / Clash.Meta YAML 订阅不适用。
- 节点地址或协议变化时，优先刷新订阅，避免长期依赖手动填写的旧地址。
- AI 服务可以单独指定你确认可用的节点，普通流量继续使用默认节点。
- 本项目与 Just My Socks 没有官方合作或隶属关系。

官方资料来源及设计背景见 [JMS 适配与开发计划](docs/JMS_CUSTOMIZATION.md)。该文档保留初期需求与规划，当前实现和验证状态以本页及验证记录为准。

## 预览版状态

[2026-10-20 构建](https://github.com/davidren0513/FutureFast/actions/runs/39401539475)已通过单元测试、Kotlin 编译、Play Store / F-Droid 两种变体打包与 Lint、原生库打包检查，以及 Android 模拟器安装与启动检查。

真实 JMS 订阅、AI 平台访问、后台长时间运行及完整 VPN / 仅代理 / root 场景仍需设备验证。当前液态玻璃改造集中在连接、自动管理和 AI 分流页面。详细测试命令、对应提交和 **Not run** 项目见[验证记录](docs/VALIDATION.md)。

## 作者与开源致谢

**定制与维护：David Ren**<br>
GitHub：[davidren0513/FutureFast](https://github.com/davidren0513/FutureFast)

<sub>基于 [2dust/v2rayNG](https://github.com/2dust/v2rayNG) 修改。原项目作者与贡献者：2dust 及 v2rayNG contributors。感谢 [Xray-core](https://github.com/XTLS/Xray-core)、[AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite)、[HEV Socks5 Tunnel](https://github.com/heiher/hev-socks5-tunnel) 与 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) / Backdrop。原有版权和各依赖许可证继续保留。</sub>

项目遵循 [GNU GPL v3](LICENSE)。[上游原始 README](docs/UPSTREAM_README.md) 保留为参考，其中上游的发布地址、签名和社区信息不代表 FutureFast。

## 免责声明

**本应用无偿提供，仅供访问业务内容所需。请遵守您所在国家或地区的法律法规，不得将本应用用于任何非法用途。**

本项目提供客户端软件，不提供代理服务器、JMS 账号或 AI 服务账号。请自行配置有权使用的服务，并遵守相关服务条款。软件按随附开源许可证提供；本说明不限制许可证授予的权利。

请勿将订阅链接、节点密码、私钥或含有这些信息的日志和截图提交到公开仓库。

## 开发与维护

Android 工程位于 `V2rayNG/`。首次构建需要递归初始化子模块，并按[预览构建工作流](.github/workflows/future-fast-preview.yml)准备 HEV 原生库与匹配源码版本的 Xray AAR。

```sh
git clone --recursive https://github.com/davidren0513/FutureFast.git
cd FutureFast
```

JDK、SDK、NDK 和依赖版本以仓库中的工作流及 Gradle 配置为准。变更通过分支和 PR 记录，说明实现范围、测试结果和未验证项目。
