# Audio Record Demo

一个简单的 Android 录音应用，使用 AudioRecord API 进行录音和播放。

## 功能特性

- 🎤 **录音功能**：使用 AudioRecord API 录制 PCM 音频，保存为 WAV 格式
- 🔊 **播放功能**：支持播放、暂停、停止录音
- 📝 **历史记录**：自动保存所有录音，支持从列表中选择播放
- 🎨 **现代 UI**：Material Design 风格界面，焦点状态清晰可见
- 📱 **Android 9+**：目标 API 28+，兼容现代 Android 设备

## 截图

| 录音界面 | 历史记录 |
|---------|---------|
| 录音按钮、时长显示、播放控制 | 录音列表、选择播放 |

## 技术规格

| 项目 | 规格 |
|------|------|
| 采样率 | 44100 Hz |
| 声道 | 单声道 (MONO) |
| 格式 | PCM 16-bit |
| 输出格式 | WAV |

## 环境要求

- Android 9 (API 28) 或更高版本
- 麦克风权限

## 下载安装

从 [Releases](https://github.com/yeqing17/audiorecord-demo/releases) 页面下载最新版本的 APK。

1. 下载 APK 文件
2. 在 Android 设备上启用"安装未知来源应用"
3. 安装 APK

## 开发环境

- Android Studio Hedgehog | 2023.1.1
- Gradle 8.5
- Kotlin 1.9.20
- JDK 17
- Android SDK 34

## 项目结构

```
audiorecord-demo/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/audiorecord/
│   │   │   ├── MainActivity.kt      # 主界面
│   │   │   ├── AudioRecorder.kt    # 录音管理类
│   │   │   └── AudioPlayer.kt      # 播放管理类
│   │   ├── res/
│   │   │   ├── layout/             # 布局文件
│   │   │   ├── drawable/           # 图形资源
│   │   │   └── values/             # 字符串、颜色等
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── .github/workflows/release.yml  # CI/CD 配置
```

## 构建项目

```bash
# 克隆项目
git clone https://github.com/yeqing17/audiorecord-demo.git
cd audiorecord-demo

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

## CI/CD

项目使用 GitHub Actions 自动构建和发布：

1. 推送 tag（如 `v1.0.0`）触发构建
2. 自动编译生成 Debug APK
3. 创建 GitHub Release 并上传 APK

```bash
# 创建并推送新版本
git tag v1.0.x
git push origin v1.0.x
```

## 权限说明

| 权限 | 用途 |
|------|------|
| `RECORD_AUDIO` | 录制音频 |
| `WRITE_EXTERNAL_STORAGE` | 保存录音文件（Android 9 及以下） |

## License

MIT License