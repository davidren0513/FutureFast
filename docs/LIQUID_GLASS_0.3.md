# Future Fast 0.3.0 ARM64 测试包

## 本轮改动

- 使用 Kyant0/AndroidLiquidGlass 官方 catalog 的 LiquidBottomTabs、LiquidButton 和拖动动画实现，固定来源提交 `65ab177e90e5c1d8c62e70cf7755841982da65f6`，保留 Apache 2.0 授权与归属。导航通过背景和图标文字双层采样，实现按压放大、色散折射、拖动形变和松手吸附。修正宿主页重组时动画状态被重新初始化的问题。
- 首页移除 AI 独立线路捷径、粘贴订阅、扫码和维护者信息。节点列表内直接提供编辑、分享、删除、导入、搜索与批量工具，不再跳转“节点与高级管理”。订阅管理与关于入口放在设置。
- 保留 ChatGPT、Claude、Gemini 各自的节点 GUID 和跟随默认节点选项；保持原有失效阻断和重连生效规则。
- 采用用户确认的白底蓝青融合 FF 图标，更新各密度启动图标、自适应前景、主题单色图标和通知图标。原稿位于 `design/icon/ff-approved.png`，资源打包脚本 `tools/package-ff-icons.py` 仅执行缩放和系统单色轮廓转换。
- 仅交付 ARM64 APK。包名 `tech.future.fast`，版本 `0.3.0`；使用与此前本地测试包相同的测试证书。

## 构建验证

- `:app:testPlaystoreDebugUnitTest`：108 项，0 失败、0 错误、0 跳过。
- `:app:compilePlaystoreDebugKotlin`、`:app:assemblePlaystoreDebug`、`:app:lintPlaystoreDebug`：通过，构建参数 `-PABI_FILTERS=arm64-v8a`。
- `LiquidTabPositionTest` 验证吸附索引、边界、非有限值、非法数量；现有 `FutureFastPolicyTest` 验证独立节点策略。
- Android 15 / API 35 ARM64 模拟器中以系统动画倍率 1 录制实际导航拖动；见交付目录中的 MP4。

## 模拟器功能检查

- Android 15 ARM64：批量导入、默认节点选择、ChatGPT/Claude/Gemini 分别指定节点、强制停止后保存、Gemini 重置为默认节点均通过。
- 键盘 Tab / Enter 和 D-pad 中心键操作 AI 开关通过；旋转重建后页面与三个服务的最终设置保持。
- 经真实 Xray SOCKS 入站发送本地合成请求：ChatGPT/OpenAI → TEST-US，Claude/Anthropic → TEST-JP，Gemini/API → TEST-SG；Gemini 改为跟随默认并重连后 → TEST-JP，其他两项不变。共 9 次请求通过，不连接外部 AI 服务。
- 首页/节点重复入口移除、点击节点页后水珠位置同步、节点直接操作菜单与编辑页、设置中的订阅管理与关于页、深色渲染检查通过；崩溃缓冲区无 FATAL EXCEPTION。
- APK 签名验证、16 KB zipalign 检查通过；APK 中 native 库仅包含 arm64-v8a。

## Not run

- 真实代理订阅、AI 平台账户登录与外网 HTTPS 可用性：未提供真实订阅或账号；模拟节点不能证明平台可用性。
- Android 7–12 降级路径、物理设备性能/耗电、16 KB 页面设备运行、大字体、多语言、动态配色单双栏全矩阵、完整 TalkBack 朗读和全部控件键盘/D-pad 遍历：没有完成对应设备与辅助功能矩阵。Android 13 以下显示兼容材质，不支持 AGSL 实时折射。
- 长时间后台自动更新与测速、proxy-only/root 模式完整生命周期：本轮未进行长时间测试；核心未修改。
