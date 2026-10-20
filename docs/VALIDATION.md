# FutureFast 0.1.0 验证记录

## 已完成的构建检查

日期：2026-10-20。以下结果只对应提交 `424d55b5a9ce80ae3b595dad17ca8d6bc73e3290`。

[GitHub Actions / 39394813378](https://github.com/davidren0513/FutureFast/actions/runs/39394813378) 已成功完成：

- `:app:testPlaystoreDebugUnitTest --tests com.v2ray.ang.dto.FutureFastPolicyTest`
- `:app:testPlaystoreDebugUnitTest`
- `:app:compilePlaystoreDebugKotlin`
- `:app:assemblePlaystoreDebug`
- `:app:assembleFdroidDebug`
- 各 APK / ABI 中的 `libgojni.so`、`libhev-socks5-tunnel.so`、`libhevsockstun.so` 打包检查及 SHA256SUMS 生成。
- Android 35 x86_64 模拟器安装、启动、进程存活和 crash buffer 检查。实际首页截图已保存到 [screenshots/home.png](screenshots/home.png)。
- 上游 instruction 文件边界和补丁空白检查。

此代码通过 PR #2 合入主分支。后续 README 和截图文档更新不改变 Android 代码；文档检查不能替代新代码提交的构建或设备验证。最新主分支结果请查看 [Actions](https://github.com/davidren0513/FutureFast/actions/workflows/future-fast-preview.yml)。

## Not run

| 尚未完成的检查 | 原因 |
| --- | --- |
| 真实 JMS 订阅、协议握手、测速优选与 ChatGPT / Claude / Gemini 端到端访问 | 没有用于验证的真实服务配置 |
| VPN / 仅代理 / root 的重复启动、失败清理、停止竞争、网络切换与恢复 | 本次模拟器仅执行无节点的启动检查 |
| 自动更新 / 优选 / AI 设置关闭重开与重启持久化、订阅替换并发、系统休眠调度 | 尚未完成设备专项验证 |
| TalkBack、键盘与 D-pad、表单 IME 遍历、大字体、暗色和旧系统降级 | 尚未完成设备专项验证 |

## 当前边界

- 预览包使用测试签名，尚未配置正式发布签名。
- 单节点的硬超时由现有 Xray 核心控制。
- 节点和高级设置页面保留原布局。
- AI 规则覆盖列出的服务域名；共享登录域名和平台访问条件仍可能影响可用性。
- 非中文新增文案当前主要为英文。
