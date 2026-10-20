# Future Fast 0.4.0 ARM64 测试包

本轮在 0.3.0 上完成以下调整，保留已确认的融合 FF 图标。

## 行为与界面

1. 连接页的圆形按钮增加外投影、双层边缘高光与表面反光；当前节点和自动管理卡片增强折射、内阴影和厚度。
2. 节点页测速时，原按钮变为“测试中…”并禁用；不新增状态文字行，不提供取消按钮。完成后恢复“测速”。后台探测超时仍由 Xray 控制。
3. 在已有分组（包括 Default）粘贴单个订阅 URL 时，下载节点并导入当前分组，保留分组 ID、名称及调度设置，不创建“import sub”。手动粘贴采用追加语义；订阅管理中的后续刷新维持原刷新语义。无效或存储失败的响应不覆盖原订阅元数据。
4. AI 指定节点缺失时生成默认代理规则；节点运行中故障时，通过 Xray Observatory 和带 fallbackTag 的独立 balancer 回到默认节点。每个服务仍有自己的选择器，恢复健康后可回到指定节点。已有默认策略组通过独立 loopback 入口回到原 balancer，不改成任意成员。健康观测默认每 10 秒进行，已有观测配置存在时沿用其间隔；故障切换需要等待观测结果，已失败的连接不会被透明重放。
5. 连接、节点、AI 分流的页面外层不滚动，节点多时仅列表内部滚动。较矮的屏幕使用紧凑布局，AI 说明移为可点击查看，保留全部服务选择入口。自动管理和设置等长页面仍可滚动。
6. 全局共享玻璃背景与主题；设置项、顶部栏、表单、列表卡片、弹窗、菜单统一玻璃材质。全部 Switch 替换为参考官方 LiquidToggle 的玻璃滑块，保留原有回调，提供统一的触摸、拖动、键盘与语义行为。设置列表改为逐项惰性加载，避免一次创建全部玻璃效果。

参考来源：[AndroidLiquidGlass 固定提交](https://github.com/Kyant0/AndroidLiquidGlass/tree/65ab177e90e5c1d8c62e70cf7755841982da65f6)、[Xray 路由与 fallbackTag](https://xtls.github.io/config/routing.html#balancerobject)、[连接观测](https://xtls.github.io/config/observatory.html)。授权见 THIRD_PARTY_NOTICES.md。

## 已运行验证

- 定向 `FutureFastRoutingTest`、`SubscriptionImportTargetTest`、`FutureFastPolicyTest` 通过。
- `:app:testPlaystoreDebugUnitTest`：113 项，0 失败、0 错误、0 跳过。
- `:app:compilePlaystoreDebugKotlin`、`:app:assemblePlaystoreDebug`、`:app:lintPlaystoreDebug`：通过；参数 `-PABI_FILTERS=arm64-v8a`。
- APK 签名与 `zipalign -c -P 16 4` 通过。ZIP 检查包含 ARM64 的 Xray JNI、两个 HEV 库；没有混入其他 ABI。
- Android 15 / API 35 ARM64：实际复制订阅 URL 到剪贴板、从节点页粘贴、Default 分组内显示、失败响应保留节点与原 URL、进程重启后保留均通过。使用本机 HTTP 服务经 adb reverse 返回合成订阅，不连接真实订阅服务。
- 真实 Xray SOCKS 入站连接本机三个模拟上游，确认三个 AI 独立出站；关闭 ChatGPT 上游后自动改走默认节点，恢复后自动回到独立节点；Gemini 不受影响。手动将 Gemini 改回默认并重连，其他服务仍保持原线路。删除 ChatGPT 指定节点后，重新应用配置仍走默认节点，没有生成阻断流量的行为。
- 清空模拟器数据后的三个空状态页面均为固定一屏；固定主页无外层滚动、长节点列表内部滚动、“测试中…”仅出现于禁用按钮且无取消、完成后恢复按钮、玻璃 AI 开关键盘 Enter/D-pad 中心键均通过。
- 1080×2400 与 1080×1920 紧凑布局（包括已连接时的重新应用按钮）、浅色/深色、设置/订阅/关于页面已采集实际截图。设置全量加载曾在模拟器触发无响应，改为惰性加载后重新验证，新的检查中无 ANR 和 FATAL EXCEPTION。

## 回归工具

仅在可丢弃的模拟器运行，设置 `ANDROID_SERIAL`，将 platform-tools 加入 PATH，从仓库根目录执行。测试会导入并修改模拟节点、订阅、AI 设置和测速 URL，不应连接存有个人数据的设备。

初始夹具可由既有 `tools/verify-future-fast-routing-ui.py` 建立（安装后先授予通知权限并将应用语言设置为简体中文）。之后依次运行：

- `python3 tools/verify-future-fast-import-group.py`
- `python3 tools/verify-future-fast-glass-ui.py`
- `python3 tools/verify-future-fast-failover.py`

输出在 `smoke-v4/`。模拟线路与测试域名不代表实际 AI 平台可用性；测试数据未写入 APK。

## Not run

- 真实订阅、AI 账户登录、外部 HTTPS 服务可用性：未提供实际订阅和凭据。
- 完整 TalkBack 朗读、每个高级页面的全部键盘/拖动路径、所有语言和超大字体、动态配色单双栏全矩阵、横屏极矮窗口：未完成对应辅助功能与布局矩阵。主要三页、核心开关和设置导航已作上述检查。
- Android 7–12 兼容分支、其他系统版本与物理手机性能/耗电、16 KB 设备运行：本轮设备为 API 35；低于 Android 13 使用兼容材质，无法执行 AGSL 实时折射。对齐检查不等于设备运行测试。
- 默认策略组的 loopback 回退实机场景、proxy-only/root 完整生命周期、长时间后台调度：默认策略组配置由 JVM 测试覆盖，运行验证使用 VPN 模式与普通节点；未完成以上设备场景。

## 交付

仅提供 `FutureFast_0.4.0_arm64-v8a.apk`。包名 `tech.future.fast`，使用与此前本地测试包相同的测试证书；SHA256SUMS 随包提供。编译产物和实际截图保存在 `deliverables/0.4.0/`。
