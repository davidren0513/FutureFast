# Future Fast 0.4.3 ARM64

恢复开关的点击涟漪。此前将矩形反馈问题误解为应当移除涟漪，本版改为保留 Material 点击涟漪，同时按玻璃卡片的圆角裁剪；独立开关按胶囊轮廓裁剪。卡片和内部装饰滑块共享按压状态，每张卡片只绘制一次涟漪。

保留 0.4.2 的整张卡片触控、间隔按钮独立操作、保存后立即发布状态及 140 毫秒滑块动画。本版不修改设置保存、代理配置或 AI 分流逻辑。

## 验证

- `:app:testPlaystoreDebugUnitTest`：121 项通过，0 失败、0 错误、0 跳过。
- `:app:compilePlaystoreDebugKotlin`、`:app:assemblePlaystoreDebug`、`:app:lintPlaystoreDebug`：通过，`-PABI_FILTERS=arm64-v8a`。
- API 35 ARM64 模拟器：AI 卡片边缘按压和自动优选卡片边缘按压分别只切换一次；测速间隔弹窗操作不切换父级开关。
- 实际查看 AI 卡片及设置行的按住截图，涟漪沿圆角裁剪，玻璃滑块继续显示按压形变。交付目录含 `ripple-demo.mp4`。
- 订阅列表独立开关：胶囊形涟漪截图检查通过，按压松开后只切换一次。
- APK 签名、16 KB zipalign 对齐检查通过，native 库仅包含 arm64-v8a。交互检查无崩溃或 ANR。

## Not run

- 物理手机及全部受支持 Android 版本：本轮使用 API 35 ARM64 模拟器。
- 键盘与 D-pad 操作及重启持久化：0.4.2 已检查，本轮仅修改绘制反馈，未重复运行。
- 完整 TalkBack、所有语言及深浅色/动态配色/单双列的全部组合：本次仅修改绘制反馈，未重新执行全部矩阵。
