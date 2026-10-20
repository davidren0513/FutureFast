# FutureFast 0.1.1 实际截图

这些图片由 Android 35 x86_64 模拟器安装实际 APK 后，通过 adb screencap 获取，没有使用设计稿替代运行结果。

- 源码：`b8b1da71c1c3a537303a03bdd28155f51106e05a`
- 构建：[39401539475](https://github.com/davidren0513/FutureFast/actions/runs/39401539475)
- 环境：Pixel 6 模拟器，简体中文，空节点与默认关闭的自动管理 / AI 分流设置。
- `home.png`：连接首页。
- `automation.png`：触摸首页自动管理卡片后进入的页面。
- `ai.png`：触摸底部 AI 分流入口后进入的页面。
- `home-dark.png`：系统切换为深色后的首页。

完整 UIAutomator XML 和 crash buffer 保存在该次构建的 `FutureFast-emulator-smoke` artifact。截图验证外观，不代替 TalkBack、键盘、真实订阅或网络连接验证；详见 [VALIDATION.md](../VALIDATION.md)。
