# Android 录音应用开发计划

## 项目概述

开发一个简单的 Android 录音应用，使用 AudioRecord API 进行录音和播放，目标平台是 Android 9（API 28）。项目利用 GitHub Actions 进行 CI/CD，在提交 tag 时自动触发构建并发布 Release APK。

## 技术方案

### 开发环境

| 工具 | 版本 |
|------|------|
| Android Studio | Hedgehog (2023.1.1) |
| Gradle | 8.5 |
| Kotlin | 1.9.20 |
| JDK | 17 |
| 目标 SDK | 34 |
| 最低 SDK | 28 (Android 9) |

### 项目架构

```
audiorecord-demo/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/audiorecord/
│   │   │   ├── MainActivity.kt      # 主界面 - UI交互、权限请求
│   │   │   ├── AudioRecorder.kt    # 录音管理 - AudioRecord API封装
│   │   │   └── AudioPlayer.kt      # 播放管理 - AudioTrack API封装
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml    # 主界面布局
│   │   │   ├── layout/item_recording.xml   # 录音列表项布局
│   │   │   ├── drawable/*.xml              # 按钮选择器、焦点样式
│   │   │   └── values/                    # 字符串、颜色资源
│   │   └── AndroidManifest.xml            # 权限声明
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── .github/workflows/release.yml          # GitHub Actions CI/CD
```

### 核心功能模块

#### 1. AudioRecorder 类

录音功能封装，使用 Android AudioRecord API：

```kotlin
// 配置参数
val sampleRate = 44100      // 采样率
val channelConfig = CHANNEL_IN_MONO  // 单声道
val audioFormat = ENCODING_PCM_16BIT  // 16位

// WAV 文件头（44字节）
// 包含：RIFF头、fmt块、data块
```

**功能点**：
- 开始/停止录音
- PCM 数据写入临时文件
- 转换为 WAV 格式（添加 44 字节头）
- 录音时长回调

#### 2. AudioPlayer 类

播放功能封装，使用 Android AudioTrack API：

**功能点**：
- 播放 WAV 文件
- 暂停/恢复播放
- 停止播放
- 播放进度回调
- 播放完成回调

#### 3. MainActivity

主界面交互逻辑：

**功能点**：
- 请求 RECORD_AUDIO 权限
- 录音按钮（开始/停止）
- 播放/暂停按钮
- 停止按钮
- 录音时长显示
- 录音历史列表
- 选择录音播放

### UI 设计

#### 界面布局

```
┌─────────────────────────────────┐
│        Audio Record Demo        │  ← 标题
│          Ready to record        │  ← 状态
│           00:00:00             │  ← 时长
│     recording_xxx.wav          │  ← 文件名
├─────────────────────────────────┤
│      [  Start Recording  ]      │  ← 录音按钮（红色）
├─────────────────────────────────┤
│    [  Play  ]    [  Stop  ]    │  ← 播放控制
├─────────────────────────────────┤
│       Recording History         │  ← 历史标题
│  ┌─────────────────────────┐   │
│  │ ▶ recording_001.wav    │   │  ← 录音列表项
│  │   00:01:23 | 01/26 14:30│   │
│  └─────────────────────────┘   │
│  ┌─────────────────────────┐   │
│  │ ▶ recording_002.wav    │   │
│  │   00:00:45 | 01/26 15:00│   │
│  └─────────────────────────┘   │
└─────────────────────────────────┘
```

#### 焦点样式

- **按钮焦点**：4dp 蓝色边框高亮
- **列表项焦点**：蓝色边框 + 浅蓝背景
- **选中项**：蓝色边框 + 播放图标指示

### GitHub Actions CI/CD

#### 工作流程

```yaml
触发条件: 推送 tag (v*)

步骤:
1. Checkout 代码
2. 设置 JDK 17
3. 设置 Gradle（禁用缓存避免服务错误）
4. 构建 Debug APK（自动签名）
5. 重命名 APK 文件
6. 创建 GitHub Release
7. 上传 APK 到 Release
```

#### 使用方式

```bash
# 创建新版本 tag
git tag v1.0.0

# 推送 tag 触发构建
git push origin v1.0.0

# 自动生成 APK 并发布到 Releases 页面
```

### 权限配置

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

### 文件存储

录音文件保存位置：
```
/Android/data/com.example.audiorecord/files/Music/Recordings/
```

文件命名格式：`recording_yyyyMMdd_HHmmss.wav`

## 开发里程碑

### Phase 1: 基础功能
- [x] 项目初始化
- [x] AudioRecorder 类实现
- [x] AudioPlayer 类实现
- [x] MainActivity UI
- [x] 权限请求
- [x] GitHub Actions 配置

### Phase 2: UI 优化
- [x] 焦点样式改进
- [x] 录音历史列表
- [x] 选择播放功能
- [x] 视觉效果优化

### Phase 3: 功能完善
- [ ] 录音删除功能
- [ ] 录音重命名
- [ ] 录音分享
- [ ] 后台录音服务

## 测试计划

### 功能测试

| 测试项 | 预期结果 |
|--------|---------|
| 首次启动请求权限 | 弹出麦克风权限请求 |
| 点击录音按钮 | 开始录音，按钮变为"停止录音" |
| 再次点击录音按钮 | 停止录音，保存 WAV 文件 |
| 点击播放按钮 | 播放最后一段录音 |
| 点击历史列表项 | 播放选中的录音 |
| 播放中点击暂停 | 暂停播放 |
| 暂停中点击播放 | 继续播放 |

### 兼容性测试

| Android 版本 | 测试状态 |
|-------------|---------|
| Android 9 (API 28) | 待测试 |
| Android 10 (API 29) | 待测试 |
| Android 11 (API 30) | 待测试 |
| Android 12 (API 31) | 待测试 |
| Android 13 (API 33) | 待测试 |
| Android 14 (API 34) | 待测试 |

## 已知问题

1. ~~GitHub Actions 缓存服务错误~~ - 已通过禁用缓存解决
2. ~~APK 未签名无法安装~~ - 已改为构建 Debug APK
3. ~~焦点样式不明显~~ - 已优化焦点样式

## 参考资料

- [Android AudioRecord API](https://developer.android.com/reference/android/media/AudioRecord)
- [Android AudioTrack API](https://developer.android.com/reference/android/media/AudioTrack)
- [WAV 文件格式](https://en.wikipedia.org/wiki/WAV)
- [GitHub Actions for Android](https://github.com/actions/setup-java)