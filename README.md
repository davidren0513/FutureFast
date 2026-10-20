# Future Fast

由 **David Ren** 维护的 v2rayNG 定制分支，面向 Just My Socks（JMS）使用场景。

- 项目地址：https://github.com/davidren0513/v2rayNG-for-justmysocks
- 上游项目：[2dust/v2rayNG](https://github.com/2dust/v2rayNG)
- 原项目作者与贡献者：**2dust 及 v2rayNG contributors**
- 开源许可证：[GNU GPL v3](LICENSE)；保留上游署名、版权声明与第三方许可证。
- 本项目是独立社区定制版本，与 Just My Socks 及上游项目无官方隶属或背书关系。

## 当前状态

首个预览版本为 **Future Fast 0.1.0**。当前功能分支已加入液态玻璃首页、订阅定时更新设置、断开时真实延迟测速与节点优选、AI 独立节点和作者信息；以 GitHub Actions 构建结果为准。尚未完成真机和真实 JMS 订阅验证。

预览 APK 由 [Future Fast Preview APK](https://github.com/davidren0513/v2rayNG-for-justmysocks/actions/workflows/future-fast-preview.yml) 构建并以 Artifact 提供。预览版使用测试签名，应用 ID 为 tech.future.fast，可与原版并存。自动更新和 AI 分流默认关闭，需用户配置。上游 APK 与签名不代表本项目。

## 已确定的改造方向

| 页面／能力 | 目标 |
| --- | --- |
| 连接首页 | 液态玻璃风格；中央连接／断开按钮；下方显示当前或下次使用的节点 |
| 节点与订阅 | 保留现有能力，将低频入口放入高级设置；完整导入 JMS 参数 |
| 自动管理 | 分别配置订阅更新与测速间隔；仅在代理断开时测速并选择最快可用节点 |
| AI 分流 | ChatGPT、Claude、Gemini 等可独立指定节点，不受普通线路自动优选覆盖 |
| 关于 | 显示 David Ren、项目链接、上游署名、开源许可与免责声明 |

具体行为、验收条件和官方资料见 [JMS 适配与开发计划](docs/JMS_CUSTOMIZATION.md)。

## 作者信息与免责声明

定制设计与维护：**David Ren**。

本项目基于 v2rayNG 修改，原项目作者与贡献者为 2dust 及 v2rayNG contributors；各第三方组件归其原作者所有。应用“关于”页面将以较小但清晰可读的文字保留上游署名，并提供原项目与开源许可入口。

本应用无偿提供，仅供访问业务内容所需。请遵守您所在国家或地区的法律法规，不得将本应用用于任何非法用途。本项目提供客户端软件，不提供代理服务器、JMS 账号或 AI 服务账号。用户需自行配置有权使用的服务，并遵守服务条款。节点延迟、连通性与 AI 服务可用性取决于网络、服务提供商及第三方平台，不保证持续可用。软件按随附开源许可证提供，不作该许可证以外的保证；本说明不限制许可证授予的权利。

订阅链接、节点凭据和导出配置可能包含敏感信息，请勿提交到公开仓库、问题报告或截图中。

## 开发与维护

- 使用独立分支与 PR 维护定制修改，明确区分规划、已实现和已验证的行为。
- 上游更新先检查协议、核心和存储兼容性，再合入本分支。
- 发布时提供对应源码、版本变更、构建来源与本项目自己的签名说明。
- 上游开发资料保存在 [原始 README](docs/UPSTREAM_README.md)，仅作为上游参考，不代表本分支的发布或验证状态。

完整许可证见 [LICENSE](LICENSE)。
