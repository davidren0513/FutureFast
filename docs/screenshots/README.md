# Future Fast 0.4.3 实际截图

2026-10-21 使用实际 ARM64 APK 在 Android 15 / API 35 模拟器运行，通过 `adb screencap` 获取。截图没有用设计稿替代，没有添加演示节点、虚构延迟或连接状态，也没有裁掉系统界面。

- 对应源码：[0b7afabf](https://github.com/davidren0513/FutureFast/commit/0b7afabfbfcd396f8e2b1a9a8c1771524ecc4f24)，与本地测试提交 `b40bacbb` 的文件树相同。
- 安装包：`FutureFast_0.4.3_arm64-v8a.apk`，SHA-256 为 `ce014e82e3447341bbc178b72be961f9172f9beb0ca87925dd5d7972a6ea785b`。
- 下载：[v0.4.3 Release](https://github.com/davidren0513/FutureFast/releases/tag/v0.4.3)。
- 环境：API 35 ARM64，可丢弃模拟器，720 × 1600 像素，简体中文；清空测试应用数据后同意首次使用条款，没有导入订阅或真实节点。
- 配置：自动优选、订阅自动更新与 AI 分流均为首次安装的默认关闭状态；未建立 VPN 连接。

| 文件 | 画面 |
| --- | --- |
| `home.png` | 浅色连接首页 |
| `nodes.png` | Default 分组、空节点列表及导入入口 |
| `ai.png` | ChatGPT、Claude、Gemini 独立线路入口 |
| `automation.png` | 订阅自动更新、自动优选及间隔设置 |
| `settings.png` | 订阅管理入口和统一玻璃开关 |
| `home-dark.png` | 系统深色模式的连接首页 |

首页、节点及 AI 分流为固定一屏布局，节点列表内部滚动；设置与自动管理页面可以滚动。静态图片不能展示完整的水珠拖动和涟漪动画。

截图验证画面，不代替真实网络连接、TalkBack、键盘、后台调度或物理手机性能测试。当前版本的验证范围见 [验证索引](../VALIDATION.md) 和 [0.4.3 记录](../RELEASE_0.4.3.md)。
