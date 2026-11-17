# 隐私友好2048 · 浩科青瓷版

[English](README.en.md) · [下载与软件介绍](https://www.haoke.org/software/b8597da489231e77) · [对应源码发行版](https://github.com/zh1414319052-lgtm/privacy-friendly-2048-haoke/releases/tag/v1.4.2-haoke.3)

<img src="art/app-icon/app-logo.png" width="96" alt="隐私友好2048 青瓷图标" />

**Android 正式版 1.4.2-haoke.3（108）**。基于 [SECUSO Privacy Friendly 2048](https://github.com/SecUSo/privacy-friendly-2048) 1.4.2 的浩科维护版本，保留上游 Git 历史、作者署名及 GPL-3.0-or-later 许可证。

## 玩法与界面

- 经典 4×4–7×7 棋盘，以及速玩、残局、障碍、斐波那契、每日挑战五种模式。保留撤销、续局和成绩统计。
- 青瓷 2.5D 原生界面、适配 Android 图标遮罩的新图标、七种游戏音效；Noto Sans SC 按真实字重渲染。
- 系统首选中文时使用简体中文，英文及其他语言使用英文。游戏离线运行，无广告、追踪和 INTERNET 权限；外部介绍链接仅由用户主动打开。
- Android 5.0 / API 21 起；目标与编译 SDK 34。正式包名 `org.secuso.privacyfriendly2048.haoke`，调试包追加 `.qa`，与上游独立安装，三者不自动共享存档。

**本次修复：** 经典棋盘在布局回调内初始化方块，可能导致方块为零尺寸、数字错位；现在延后至当前布局结束后初始化。回归测试覆盖 4×4–7×7 和 Activity 重建。

## 下载与版本对应

安装包从[浩科软件详情页](https://www.haoke.org/software/b8597da489231e77)获取；GitHub Release 提供对应源码，不另行分发 APK。网站下载入口由维护者管理。

| 项目 | 值 |
| --- | --- |
| 版本 / versionCode | `1.4.2-haoke.3` / `108` |
| APK 字节数 | `22491775` |
| APK SHA-256 | `86849eb040b435fdffef1a524e9851d2ca3e0f41a9317650c8182f4c17d5754a` |
| 正式签名证书 SHA-256 | `572cf1d85c695372bbab8b6036bb83259384bc0ed5379e840ed854561a2f4ba9` |

详情见 [公开发行记录](docs/SOURCE_RELEASE.json)。签名私钥、密码、网站凭据、设备标识和本机操作记录不属于公开源码。

## 获取源码和构建

推荐递归克隆，使用固定版本的 Backup API 子模块：

```sh
git clone --recurse-submodules https://github.com/zh1414319052-lgtm/privacy-friendly-2048-haoke.git
cd privacy-friendly-2048-haoke
git checkout v1.4.2-haoke.3
git submodule update --init --recursive
```

构建需要 JDK 17、Android SDK 34 和 Gradle Wrapper 8.5。在 `local.properties` 配置本机 `sdk.dir`，或使用 Android SDK 环境变量。Google Maven 与 Maven Central 依赖通过 Gradle 获取。Windows 使用 `gradlew.bat`，其他系统使用 `./gradlew`：

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

正式签名由进程环境提供 `PF2048_KEYSTORE`、`PF2048_KEY_ALIAS`、`PF2048_STORE_PASS`、`PF2048_KEY_PASS` 后构建：

```sh
./gradlew --no-daemon :app:assembleRelease :app:testReleaseUnitTest :app:lintRelease
```

新维护者用自己的密钥构建，不能覆盖安装现有正式签名版本。现有维护者升级须沿用既有证书。源码不保证不同工具链或签名所得 APK 逐字节相同。Windows 若 Android 工具不支持中文路径，可从 ASCII 路径检出构建。

GitHub 自动生成的源码 ZIP 不包含子模块内容；请使用递归克隆，或 Release 的 `pf2048-1.4.2-haoke.3-corresponding-source.zip`（已包含固定子模块源码）。Backup API 固定提交为 `8687a8dbd170866707dc41bcb879375400ef697a`，上游 [v1.3.0](https://github.com/SecUSo/privacy-friendly-backup-api/tree/8687a8dbd170866707dc41bcb879375400ef697a)。

## 可编辑资源

- `art/ui/direct-design`：432×960 逻辑单位的可编辑设计、布局生成器和静态/动效原型；`art/ui/materials`：独立陶瓷材质。早期 `art/ui/preview` 与 `art/ui/reference` 为设计过程材料，以 direct-design 和运行资源为当前实现依据。
- `tools/export_ceramic_android.mjs`：将已提交的设计文字测量及材质接入运行资源，Node.js 原生模块，无额外 npm 包。
- `art/app-icon` 与 `tools/export_launcher.py`：图标原件及 Android 密度、自适应和单色层导出，需 Python 3 与 Pillow。
- `art/sound`：七种 WAV 音效及合成脚本；`tools/subset_font.py` 使用 fontTools 生成字体子集。
- `app/src/main/assets/ceramic/NotoSansSC-VF.ttf` 及同目录 `NotoSansSC-OFL.txt`：可商用字体与 SIL OFL 1.1 全文。构建 APK 直接使用已提交资源，无需重新导出美术。

导出工具使用仓库自带字体。更改设计后须重新核对字体字重、基线和真机布局；原型图片不能证明实际游戏行为或逐像素完全一致。

## 已有验证与边界

2026-11-17 正式构建、lint 与 **15 项 release 单元测试通过**。Android 16 的一加真机以同签升级到 versionCode 108，已安装 APK 的哈希与交付文件一致。实际验证经典棋盘显示、滑动、撤销，以及五种挑战的入口/操作，残局第 1 关完成和进入下一关。前一版本 107 已验证五模式撤销、续局与冷启动持久化；这些完整流程未在 108 全部重跑。

本次源码公开只整理说明、可移植导出工具和发布材料，未改游戏实现或重打包 APK。仓库保留上游 CI 配置，不能把其存在当成本次 GitHub Actions 已通过。无客户端在线更新通道。

## 许可与归属

本项目采用 [GPL-3.0-or-later](LICENSE)，保留 [上游许可说明](LICENSE.md)。原始开发者：Saskia Jacob、Julian Wadephul；SECUSO / Privacy Friendly Apps。原始应用图标：Tatjana Albrandt。浩科维护修改与新青瓷美术、音效随本源码按 GPL-3.0-or-later 分发；Noto Sans SC 保留 SIL OFL 1.1，原型 XState 保留随附 MIT 许可。

原导航图标的 CC BY 2.5、Material Icons 的 Apache-2.0 及 SECUSO 标志归属不因本次修改改变。Backup API 保留其子模块内许可证。上游官网：[SECUSO PFA](https://secuso.org/pfa)。
