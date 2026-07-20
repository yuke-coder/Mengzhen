# 梦枕纯原生 App 后端逻辑开发方案

## 〇、代码开发底层指令

以最低复杂度解决问题根因。

在每一处实现中：
1. 先定位问题的根本原因，再动手写代码，不要绕着症状打补丁
2. 选择能解决问题的最简单方案，不引入多余的抽象层、接口、模式
3. 能用 10 行代码解决的事不写 100 行
4. 不提前设计不需要的扩展点，需要时再加
5. 复制 > 继承 > 接口 > 抽象基类，优先选左边
6. 一个类只做一件事，一个方法只做一件事
7. 命名说清楚意图，注释只写"为什么"不写"是什么"
8. 不写没用的 try-catch、不写没用的 null 检查、不写没用的日志
9. 死代码立即删，不注释掉留着
10. 写完能跑就是最终版，不回头"优化"没出问题的代码

## 一、总体架构

1. **纯原生 App** - Kotlin + Jetpack Compose，对标喜马拉雅
2. **Web 端** - 保留不动，部署在 `https://mengzhen-chi.vercel.app`
3. **Capacitor 壳** - 已彻底清除（Android 侧 + Web 端零残留）
4. **现有 Java Service/Receiver** - 全部推翻，用 Kotlin 重写
5. **喜马拉雅反编译代码** - 作为参考路标，读懂逻辑后用 Kotlin 重新实现

## 二、核心原则

1. **后端逻辑全部抄喜马拉雅** - 不自实现，以喜马拉雅反编译代码为模板
2. **不抄的部分** - 广告逻辑、语音助手（依赖喜马拉雅自身后端服务）
3. **依赖处理三层策略**：
   - 核心逻辑（播放/保活/定时/耳机）-> 1:1 还原喜马拉雅策略，用干净 Kotlin 写
   - 喜马拉雅基础设施（日志、统计、崩溃上报、热修复）-> 砍掉，换 `android.util.Log`
   - 业务依赖（配置、网络请求）-> 自己写简单替代，接 Web API / Supabase
4. **UI 暂不讨论** - 本方案只覆盖后端逻辑

## 三、技术选型

1. **播放器引擎** - Media3 / ExoPlayer（喜马拉雅也用 ExoPlayer）
2. **定时调度** - AlarmManager.setExactAndAllowWhileIdle()（最可靠，喜马拉雅同款）
3. **网络层** - OkHttp（build.gradle 已加依赖）
4. **数据层** - 走 Web API（`https://mengzhen-chi.vercel.app/api/*`），不直连 Supabase
5. **音频上传** - 原生 App 调 Web API 拿签名 URL，直传 Supabase Storage（不走 Vercel，无大小限制）
6. **图片加载** - Coil Compose（build.gradle 已加依赖）

## 四、五大后端模块（开发顺序）

### 模块 1：AudioPlaybackService（播放器 Service）

**抄喜马拉雅的：**
- 播放控制（播放/暂停/停止/上一首/下一首/循环模式）
- 定时停止（播放 N 分钟后自动停）
- 断点续播（记住位置，本地 + Supabase 云端同步）
- 缓冲策略（网络不好时处理）
- 错误重试（播放失败后自动重试）
- 离线播放（缓存 + 下载到手机本地）

**自己实现的：**
- 淡入淡出（喜马拉雅没有此功能，梦枕独有）

**音频来源：**
- Supabase Storage 公开 URL，ExoPlayer 流式播放
- 下载的本地文件，离线播放

**喜马拉雅参考文件：**
- `D:\喜马拉雅\ximalaya-reverse\decompiled\jadx_full\sources\com\ximalaya\ting\android\exoplayer\i.java`（ExoPlayer 封装，含 WakeLock/WifiLock）
- `D:\喜马拉雅\ximalaya-reverse\decompiled\jadx_full\sources\com\ximalaya\ting\android\player\XMediaPlayer.java`
- `D:\喜马拉雅\ximalaya-reverse\decompiled\jadx_full\sources\com\ximalaya\ting\android\opensdk\player\service\` 目录

### 模块 2：SustainedListenService（息屏保活）

**抄喜马拉雅的：**
1. **前台 Service** - startForeground()，带通知栏，系统不轻易杀
2. **WakeLock** - 锁屏后持有 CPU 唤醒锁，播放器继续跑
3. **WifiLock** - 锁屏后保持 WiFi 连接，不断流
4. **Doze 模式监听** - 监听系统省电模式切换，记录状态
5. **电池优化白名单** - 引导用户加入白名单
6. **双 Service 保活** - 主 Service + 辅助 Service 互相拉起
7. **厂商适配** - 华为等厂商的特殊保活策略

**喜马拉雅参考文件：**
- `D:\喜马拉雅\ximalaya-reverse\decompiled\jadx_full\sources\com\ximalaya\ting\android\host\XiMaLaYaService.java`
- `D:\喜马拉雅\ximalaya-reverse\decompiled\jadx_full\sources\com\ximalaya\ting\android\host\AssistService.java`
- `D:\喜马拉雅\ximalaya-reverse\decompiled\jadx_full\sources\com\ximalaya\ting\android\opensdk\player\service\DozeReceiver.java`
- `D:\喜马拉雅\ximalaya-reverse\decompiled\jadx_full\sources\com\ximalaya\ting\android\opensdk\model\power\DozeModel.java`
- `D:\喜马拉雅\ximalaya-reverse\decompiled\jadx_full\sources\com\ximalaya\ting\android\main\h\permissionSetting\permission\BaseBatteryOptimizationPermission.java`
- `D:\喜马拉雅\ximalaya-reverse\decompiled\jadx_full\sources\com\ximalaya\ting\android\main\h\permissionSetting\device\` 目录（厂商适配）

### 模块 3：MediaSession + 通知栏

**抄喜马拉雅的：**
1. 通知栏显示当前播放音频名称、封面
2. 播放控制按钮（播放/暂停、上一首、下一首）
3. 进度条（显示播放进度，可拖动跳转）
4. 定时停止按钮（通知栏上直接设定）

**技术方案：** Media3 自带 MediaSession 支持，直接用

### 模块 4：AlarmScheduler（定时播放）

**抄喜马拉雅的：**
- AlarmManager.setExactAndAllowWhileIdle() 定时触发
- 到点自动启动播放 Service
- 支持多个定时任务
- 开机自启动恢复定时任务

**喜马拉雅参考文件：**
- `D:\喜马拉雅\ximalaya-reverse\decompiled\jadx_full\sources\com\ximalaya\ting\android\host\manager\alarm\e.java`
- `D:\喜马拉雅\ximalaya-reverse\decompiled\java_src\timer_core_classes.txt`（定时播放分析报告）

### 模块 5：WireControlReceiver（耳机控制）

**抄喜马拉雅的：**
1. 耳机拔出自动暂停
2. 耳机插入自动恢复
3. 线控单击 - 播放/暂停
4. 线控双击 - 下一首
5. 线控三击 - 上一首
6. 长按 - 不响应（喜马拉雅的语音助手不抄）

## 五、数据层

### API 域名
`https://mengzhen-chi.vercel.app/api/*`

### 用户体系
- 共用 Web 端 `users` 表（Supabase PostgreSQL）
- 用户名 + 密码，bcrypt 加密
- Web 端注册的账号原生 App 可登录，反过来也一样
- 不用 Supabase Auth SDK，沿用 Web 端自写的认证逻辑

### 音频管理
- 原生 App 全功能：选择文件、上传、管理音频库
- 上传流程：调 `/api/audio/upload-ticket` 拿签名 URL -> 直传 Supabase Storage -> 调 `/api/audio/upload-complete` 入库
- 大文件支持 TUS 断点续传
- 重复文件拦截：文件名 + 文件大小都匹配，直接不允许上传

### 断点续播存储
- 本地：DataStore / SharedPreferences
- 云端：Supabase（通过 Web API 同步）
- 换手机后云端恢复进度，但音频文件需重新下载

### 离线播放
- 缓存：播过的自动缓存到本地
- 下载：用户主动下载到手机
- 播放优先读本地文件，不联网也能播

## 六、APK 分发

1. APK 文件放 Supabase Storage（新建 `apk` bucket）
2. 安装按钮的 `NEXT_PUBLIC_APK_DOWNLOAD_URL` 指向 Supabase 公开 URL
3. 新版本覆盖上传，URL 不变

## 七、不抄的部分

1. **广告逻辑** - 广点通 SDK、广告播放器、广告音量渐变
2. **语音助手** - 依赖喜马拉雅自身 ASR/NLP 云服务
3. **喜马拉雅基础设施** - 日志、统计、崩溃上报、热修复
4. **React Native** - 喜马拉雅的 RN 层不相关

## 八、开发工具和路径

- **JDK 21**: `C:\Users\kmzho\tools\jdk\jdk-21.0.2` 或 `D:\开发工具\jdk21`
- **Android SDK**: `D:\开发工具\android-sdk`
- **jadx 工具**: `D:\喜马拉雅\jadx\bin\jadx.bat`（保留）
- **喜马拉雅反编译源码**: `D:\喜马拉雅\ximalaya-reverse\decompiled\jadx_full\sources\`
- **Smali 字节码**: `D:\喜马拉雅\ximalaya-reverse\decompiled\smali_all\`
- **DEX 字符串池**: `D:\喜马拉雅\ximalaya-reverse\decompiled\dex_all_strings.txt`
- **定时分析报告**: `D:\喜马拉雅\ximalaya-reverse\decompiled\java_src\timer_core_classes.txt`
- **梦枕项目路径**: `C:\Users\kmzho\lobsterai\project\`
- **Android 源码**: `android\app\src\main\java\com\mengzhen\app\`

## 九、Compose 依赖（已配置）

- compose-bom 2024.12.01
- navigation-compose 2.9.0
- okhttp 4.12.0
- coil-compose 2.7.0
- media3 1.5.1
- datastore-preferences 1.1.1

## 十、现有代码处理

**全部删除，Kotlin 重写：**
- `audio/AudioPlaybackService.java`
- `audio/SustainedListenService.java`
- `receiver/AlarmReceiver.java`
- `receiver/BootReceiver.java`
- `receiver/ScreenStatusReceiver.java`
- `receiver/WireControlReceiver.java`
- `scheduler/AlarmScheduler.java`
- `scheduler/TaskInfo.java`

**保留参考但最终也会重写：**
- `data/api/SupabaseClient.kt`
- `data/model/Models.kt`
- `data/store/TaskStore.kt`
- `ui/` 下的 Compose 代码

**已删除的 Capacitor 文件（零残留）：**
- capacitor.config.ts、capacitor.settings.gradle、capacitor.build.gradle
- plugins/ 目录、capacitor-cordova-android-plugins/ 目录
- MainActivity.java（BridgeActivity）
- assets/capacitor.* 文件
- src/lib/native-scheduler.ts、src/lib/background-optimization.ts
- package.json 中 @capacitor/* 依赖
