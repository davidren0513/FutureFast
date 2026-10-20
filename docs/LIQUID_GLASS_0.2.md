# Future Fast 0.2.0 测试包

2026-10-20，在 FutureFast `eaffdde0` 基础上继续改造。Android 工程和既有 v2rayNG / Xray 架构保留；本轮没有从桌面 v2rayN 迁移协议核心。

## 改造内容

- 连接页采用确认后的液态玻璃风格：标题、连接状态、圆形主按钮、当前节点、AI 状态入口、自动管理和悬浮导航。
- 节点页展示真实订阅分组、节点、选中状态和测得的延迟；未测速不显示虚构数值。支持导入、扫码、测速、自动优选和订阅管理。完整高级节点工具保留独立入口。
- AI 分流继续分别为 ChatGPT、Claude、Gemini 保存节点 GUID，或跟随默认节点。节点失效后的阻断策略、配置生效需重新连接的行为保持不变。
- 继续使用 Kyant Backdrop 的实时背景采样、模糊、透镜与高光，保留旧 Android 的降级路径和深色主题。
- 版本为 `0.2.0`，应用 ID 为 `tech.future.fast`，Play Store 构建的 versionCode 为 `4000003`，最低 Android 7.0。

## 已运行的验证

- `:app:testPlaystoreDebugUnitTest --tests com.v2ray.ang.dto.FutureFastPolicyTest`：7 项通过。
- `:app:testPlaystoreDebugUnitTest`：106 项通过，0 失败、0 错误、0 跳过。
- `:app:compilePlaystoreDebugKotlin`：通过。
- `:app:assemblePlaystoreDebug`、`:app:assembleFdroidDebug`：通过，包含全部四种声明的 ABI 与通用包。
- `:app:lintPlaystoreDebug`、`:app:lintFdroidDebug`：通过，0 Error / Fatal，仍有非阻断警告；未通过压制警告改变结果。首次下载 Lint 工具发生 TLS 中断，重试成功。
- 四种 ABI 的 APK 均包含 Xray JNI、HEV JNI 和 root 模式可执行库；资产包含 geosite 数据。ARM64 APK 已通过签名验证和 `zipalign -c -P 16 -v 4`。
- 独立 Android 15 / API 35 ARM64 模拟器，1080 × 2400：安装、启动、空节点页、实际导入后的节点页、默认节点选择、三个 AI 的独立选择、切回默认节点、强制停止后重新打开、浅色/深色、键盘 Tab / Enter、D-pad 中心键、旋转重建均完成检查。崩溃缓冲区没有 FATAL EXCEPTION。

### 实际 Xray 出站探测

通过应用公开的批量分享导入路径创建三个测试节点，指向模拟器的 loopback 端口。使用 `adb reverse` 连接本机三个模拟 SOCKS 代理，再通过 Xray 的真实 SOCKS 入站发送测试请求。服务端只返回合成的 HTTP 响应，没有连接外部 AI 服务。

| 请求域名 | 实际接收请求的测试代理 |
| --- | --- |
| chatgpt.com、api.openai.com | TEST-US |
| claude.ai、api.anthropic.com | TEST-JP |
| gemini.google.com、generativelanguage.googleapis.com | TEST-SG |
| Gemini 改为跟随默认节点，重新连接后 | TEST-JP |
| 同次重连后的 ChatGPT、Claude | 分别保持 TEST-US、TEST-JP |

测试节点仅存在于模拟器数据中，没有写入 APK 默认配置。截图中的 TEST 标签是测试数据，不代表真实服务器位置、延迟或 AI 平台可用性。

相关脚本位于 `tools/capture-future-fast.py`、`tools/verify-future-fast-routing-ui.py`、`tools/verify-future-fast-outbounds.py`、`tools/verify-future-fast-focus.py`。在专用、可丢弃的模拟器中按此顺序执行；设置 `ANDROID_SERIAL`，并将 Android SDK 的 platform-tools 加入 PATH。截图与 XML 输出到 `smoke/`。采集使用与 CI 一样的关闭系统动画设置。

## Not run

- 真实订阅、真实 AI 账户与 HTTPS 服务可用性：没有用户凭据或实际代理订阅；本轮验证的是本地配置、保存行为和真实核心出站选择。
- Android 7–12 的视觉降级、16 KB 页面大小设备运行、物理手机性能与耗电：没有运行对应设备；旧系统分支未修改。zipalign 检查不等同于 16 KB 设备运行测试。
- 完整 TalkBack 朗读、所有控件的键盘/D-pad 遍历、大字体、全部语言的截图、动态配色组合、后台长时间调度、proxy-only / root 模式完整生命周期：没有完成这些矩阵；本轮键盘/D-pad 操作检查覆盖 AI 总开关，触摸检查覆盖主要页面与节点选择。

## 本地交付

`deliverables/` 中包含 ARM64 APK、四架构通用 APK、SHA256SUMS、测试说明、实际截图与出站记录。APK 使用本机 Android 测试证书签名，不是正式发布证书；证书不同会阻止覆盖旧版，安装处理前应先导出并确认配置可恢复。
