# Future Fast 验证记录

## 当前版本：0.4.3

- [0.4.3：圆角涟漪恢复、构建和模拟器检查](RELEASE_0.4.3.md)
- [0.4.2：开关触控范围、即时状态更新及持久化](RELEASE_0.4.2.md)
- [0.4.1：首次使用条款和关于页](RELEASE_0.4.1.md)
- [0.4：统一玻璃控件和 AI 默认线路回退](LIQUID_GLASS_0.4.md)
- [当前实际截图来源](screenshots/README.md)

0.4.3 本地验证共 121 项 JVM 测试通过，Kotlin 编译、ARM64 Playstore debug 打包、Lint、APK 签名及对齐检查通过。各场景和未运行项目以对应版本记录为准。下方保留早期版本记录，不代表当前界面的功能边界。

---

## 历史版本：FutureFast 0.1.1

日期：2026-10-20。结果对应提交 `b8b1da71c1c3a537303a03bdd28155f51106e05a`。

[GitHub Actions / 39401539475](https://github.com/davidren0513/FutureFast/actions/runs/39401539475)

## 已完成的检查

- `:app:testPlaystoreDebugUnitTest --tests com.v2ray.ang.dto.FutureFastPolicyTest`
- `:app:testPlaystoreDebugUnitTest`
- `:app:compilePlaystoreDebugKotlin`
- `:app:assemblePlaystoreDebug` 和 `:app:assembleFdroidDebug`
- `:app:lintPlaystoreDebug` 和 `:app:lintFdroidDebug`
- 每个 APK 内各 ABI 的 Xray / HEV 原生库打包与 SHA256SUMS。
- 上游 instruction 文件边界和 `git diff --check`。
- Android 35 x86_64：安装、启动、简体中文首页、触摸进入自动管理和 AI 分流、返回首页、切换暗色、进程存活与 crash buffer 检查。实际截图及 UIAutomator XML 见本次构建的 `FutureFast-emulator-smoke`。

截图只验证所显示的空节点状态和外观，不代表真实网络连接、无障碍或后台调度已经验证。后续文档与截图提交不改变 APK 源码；它们的文档检查不替代 Android 构建。

## Not run

| 检查或场景 | 原因 |
| --- | --- |
| 真实 JMS 订阅、握手、测速优选及 ChatGPT / Claude / Gemini 端到端访问 | 没有可用于测试的真实服务配置 |
| VPN / 仅代理 / root 生命周期、失败恢复、网络切换和停止竞争 | 本次设备检查仅覆盖无节点界面 |
| 加载、错误、已有节点与连接中的各呈现状态 | 本次截图使用全新无节点安装 |
| TalkBack spoken label / state / focus order / activation；键盘 Enter / Space 和 D-pad 激活 | 尚未完成专项设备检查；XML 不能替代 TalkBack |
| 大字体、表单 IME 首末控件遍历、动态颜色与双列模式 | 尚未完成专项布局与交互检查 |
| Android 7–12 玻璃降级路径 | 本次模拟器为 Android 35，不能验证旧系统 |
| 自动设置重开与重启持久化、休眠调度、订阅替换并发 | 尚未完成专项设备检查 |

## 当前边界

- 这是测试签名预览包，尚未配置正式发布签名。早期缓存未保存实际签名文件，不同构建不保证可覆盖安装；更新前请先在旧版导出配置并确认可恢复。
- 连接、自动管理、AI 分流按原设计重做；节点管理和高级设置仍保留原布局。
- 设计图中的“自定义域名”“其他 AI”“单节点硬超时”等扩展控件尚未实现。单节点硬超时仍由 Xray 核心控制。
- Android 13+ 使用 Backdrop 实时透镜、模糊与高光；旧系统使用渐变、高光和半透明降级路径。
- 非中文新增文案目前主要为英文。
