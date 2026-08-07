# 梦枕项目指令

## 项目纪要

- 梦枕是 Web + Android 双端睡眠音频应用：支持音频导入、后台播放、定时启播/关闭、轻唤醒渐强/渐弱、任务管理、B 站缓存导入和账号体系。
- Web 端位于 `src/`，Next.js API 位于 `src/app/api/`；数据模型与 Supabase 初始化位于 `storage/`、`supabase/`。
- Android 工程位于 `android/`，原生播放、调度、反馈流程和迁移的喜马拉雅/QQ 音乐/哔哩哔哩资源均以可编译源码和资源形式纳入 `android/app/`。
- 生产站点与原生 WebView 地址统一为 `https://driftcue.com`；GitHub `main` 推送后由 Vercel 自动部署。

## 目录约定

- `src/`：Web 页面、组件、客户端状态和 API 路由。
- `android/`：Android 源码、资源、测试和构建配置；`android/app/build/` 只允许生成，不提交。
- `public/`：Web 运行时静态资源；不得被部署忽略规则误排除。
- `storage/`、`supabase/`：数据库模型、迁移和初始化脚本。
- `docs/`：项目与功能文档；任务行原型集中在 `docs/task-row/`。
- `tools/diagnostics/`：一次性诊断和设备解析脚本。
- `research/`：反编译、设备验收、日志和研究归档；`xm_rev/`：原始反编译工作区。两者均不参与 Web 部署。
- `artifacts/`：本地产物归档；不得作为运行时代码依赖。

## 工作边界

- Always seek the cleanest solution that resolves the root cause.
- 优先迁移已有源码和资源，业务适配保持最小；能删除的空回调、伪功能、隐藏补丁和重复实现必须清理。
- 不创建 `.patch`、overlay、generated 覆盖层或临时替代实现；研究资料只做可回滚移动，不擅自删除。
- 不提交 `.env.local`、密钥、APK 构建产物、反编译大目录和设备截图；提交前检查 `git status` 与部署忽略范围。
- UI 继续遵守无全局边框设计；不要为了视觉问题破坏既有播放、任务、反馈和登录能力。

## 验证与发布

- Web：`pnpm exec tsc --noEmit`，随后按需执行 `pnpm build`。
- Android：使用 JDK 21，执行 `android\gradlew.bat :app:compileDebugKotlin`；批量修改完成后再编译，不逐个微改反复编译。
- 发布：提交并推送 `GitHub/main`，由 Vercel 自动部署；不要用本地手动部署绕过 Git 历史。
- 域名绑定记录：Vercel 项目为 `yuke-s-projects/mengzhen`；DNSPod 需将 `@` 与 `www` 的 A 记录指向 `76.76.21.21`。

## 当前注意事项

- 助眠入口统一复用现有 `/app-sleep` 路由；头像菜单只保留一个入口，不得重复添加。
- 反馈体系已包含类型选择、记录列表、详情、状态、图片和追问链路；修改时必须同时检查 Web API、Web 页面和 Android 导航。
- Turnstile 仅由服务端验证；不要把 secret 写进客户端或为原生端静默绕过验证。
- 设备验收前先确认域名解析和生产部署状态，再安装 APK，避免原生 WebView 指向未解析地址。
