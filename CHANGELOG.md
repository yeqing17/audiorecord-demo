# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- None

### Changed
- None

### Fixed
- None

---

## [1.0.8] - 2026-02-26

### Added
- 录音历史列表功能，显示所有录音文件
- 点击列表项直接播放对应录音
- 当前播放录音显示播放图标指示
- 列表项选中状态高亮

### Changed
- 重新设计 UI 界面，使用卡片式布局
- 优化按钮焦点样式，添加 4dp 蓝色边框
- 优化列表项焦点样式，添加蓝色边框和浅蓝背景
- 增大字体和间距，改善可读性
- 录音时长使用等宽字体显示
- 添加 CardView 依赖

### Fixed
- 修复选择录音无法播放的问题
- 修复焦点样式不明显的问题

## [1.0.7] - 2026-02-26

### Fixed
- 修复 `RecordingAdapter.getItemId()` 类型转换错误（Int to Long）

## [1.0.6] - 2026-02-26

### Changed
- 禁用 Gradle 缓存以避免 GitHub Actions 缓存服务错误

## [1.0.5] - 2026-02-26

### Added
- 添加录音历史列表 UI
- 添加焦点指示器样式

### Changed
- 改进按钮背景选择器
- 优化录音列表项布局

## [1.0.4] - 2026-02-26

### Changed
- 改为构建 Debug APK（自动使用 debug 签名）

### Fixed
- 修复 APK 未签名无法安装的问题

## [1.0.3] - 2026-02-26

### Added
- 添加 `permissions: contents: write` 配置
- 添加 APK 文件查找和重命名逻辑

### Changed
- 使用 Gradle 8.5 替代动态版本

### Fixed
- 修复 GitHub Release 创建失败（403 权限错误）
- 修复 Gradle 版本与 Kotlin 插件不兼容问题

## [1.0.2] - 2026-02-26

### Changed
- 使用 `gradle` 命令替代 `./gradlew`（避免 wrapper jar 缺失）

## [1.0.1] - 2026-02-26

### Added
- 初始 GitHub Actions workflow 配置
- Gradle wrapper 配置

### Fixed
- 修复 Gradle wrapper jar 缺失问题

## [1.0.0] - 2026-02-26

### Added
- 初始版本发布
- AudioRecorder 类：使用 AudioRecord API 录制 PCM 音频
- AudioPlayer 类：使用 AudioTrack API 播放 WAV 音频
- MainActivity：录音/播放控制界面
- 录音保存为 WAV 格式（44100Hz, 16-bit, Mono）
- 播放、暂停、停止功能
- 录音时长实时显示
- GitHub Actions CI/CD 配置
- 自动构建并发布 APK 到 GitHub Releases

---

[Unreleased]: https://github.com/yeqing17/audiorecord-demo/compare/v1.0.8...HEAD
[1.0.8]: https://github.com/yeqing17/audiorecord-demo/compare/v1.0.7...v1.0.8
[1.0.7]: https://github.com/yeqing17/audiorecord-demo/compare/v1.0.6...v1.0.7
[1.0.6]: https://github.com/yeqing17/audiorecord-demo/compare/v1.0.5...v1.0.6
[1.0.5]: https://github.com/yeqing17/audiorecord-demo/compare/v1.0.4...v1.0.5
[1.0.4]: https://github.com/yeqing17/audiorecord-demo/compare/v1.0.3...v1.0.4
[1.0.3]: https://github.com/yeqing17/audiorecord-demo/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/yeqing17/audiorecord-demo/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/yeqing17/audiorecord-demo/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/yeqing17/audiorecord-demo/releases/tag/v1.0.0