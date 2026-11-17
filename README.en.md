# Privacy Friendly 2048 · Haoke Ceramic Edition

[简体中文](README.md) · [Download and product page](https://www.haoke.org/software/b8597da489231e77) · [Corresponding source release](https://github.com/zh1414319052-lgtm/privacy-friendly-2048-haoke/releases/tag/v1.4.2-haoke.3)

**Android 1.4.2-haoke.3 (versionCode 108)**, maintained from [SECUSO Privacy Friendly 2048](https://github.com/SecUSo/privacy-friendly-2048) 1.4.2. Upstream history, attribution and GPL-3.0-or-later licensing are preserved.

## Features and this release

Classic 4×4–7×7 boards plus five challenge modes: Sprint, Puzzles, Obstacles, Fibonacci and Daily. Undo, resume and statistics are retained. The native ceramic 2.5D interface shares editable design assets, a mask-aware Android launcher icon and seven sound effects. Noto Sans SC is rendered at explicit weights.

Chinese-first system locales use Simplified Chinese; English and other primary languages use English. Gameplay is offline, without ads, tracking or the INTERNET permission. External product links open only after user action. Requires Android 5.0 / API 21; compile and target SDK are 34.

This release fixes zero-sized classic tiles and misplaced numbers caused by creating tiles inside a layout callback. Initialization is deferred until that layout pass completes. Regression coverage includes all classic board sizes and Activity recreation.

## Artifact identity and download

Use the [Haoke product page](https://www.haoke.org/software/b8597da489231e77) for installers. GitHub provides source; no APK is attached to this source release.

- Package: `org.secuso.privacyfriendly2048.haoke`; debug adds `.qa`. Upstream, formal and debug installations do not automatically share saves.
- APK: `22491775` bytes; SHA-256 `86849eb040b435fdffef1a524e9851d2ca3e0f41a9317650c8182f4c17d5754a`.
- Signing certificate SHA-256: `572cf1d85c695372bbab8b6036bb83259384bc0ed5379e840ed854561a2f4ba9`.
- Public provenance and verification: [docs/SOURCE_RELEASE.json](docs/SOURCE_RELEASE.json).

## Build

```sh
git clone --recurse-submodules https://github.com/zh1414319052-lgtm/privacy-friendly-2048-haoke.git
cd privacy-friendly-2048-haoke
git checkout v1.4.2-haoke.3
git submodule update --init --recursive
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Use JDK 17, Android SDK 34 and Gradle Wrapper 8.5; set the SDK path in local.properties or your Android SDK environment. Use `gradlew.bat` on Windows. Dependencies resolve from Google Maven and Maven Central. Backup API is pinned to `8687a8dbd170866707dc41bcb879375400ef697a` (v1.3.0) in `.gitmodules` and the Git tree.

GitHub's automatic ZIP omits submodule content. Recursively clone, or download the additional `pf2048-1.4.2-haoke.3-corresponding-source.zip` release asset, which includes the pinned submodule sources.

For release signing, supply `PF2048_KEYSTORE`, `PF2048_KEY_ALIAS`, `PF2048_STORE_PASS`, `PF2048_KEY_PASS` in the process environment, then run `./gradlew --no-daemon :app:assembleRelease :app:testReleaseUnitTest :app:lintRelease`. Existing releases require the same signing identity for in-place upgrades. Private keys, passwords, service credentials and device identifiers are excluded. Independently signed builds are not byte-identical to the published APK.

Editable designs and render inputs live in `art/ui/direct-design` and `art/ui/materials`; icon originals are in `art/app-icon`, and sound sources in `art/sound`. Android export uses Node.js built-ins (`tools/export_ceramic_android.mjs`); image exporters need Python 3 and Pillow; font subsetting needs fontTools. The full licensed Noto Sans SC font is bundled. Android builds use committed generated assets and do not require re-running the art tools.

## Verification and limits

On 2026-11-17, release build, lint and **15 release unit tests passed**. A physical OnePlus running Android 16 upgraded with the same certificate to code 108; its installed APK hash matched the delivery. Checks covered classic layout, moves and undo, interaction with all five challenge modes, and completion of puzzle 1 followed by the next level. Full challenge undo/resume and cold-start persistence were verified on code 107, not fully repeated on 108.

This source publication updates documentation, portable asset-export helpers and release materials only; it does not change gameplay or rebuild the APK. Existing upstream CI definitions are retained, not evidence of a successful current GitHub Actions run. No online client-update channel is enabled.

## Attribution and licenses

[GPL-3.0-or-later](LICENSE), with [upstream notices](LICENSE.md). Original developers: Saskia Jacob and Julian Wadephul, SECUSO / Privacy Friendly Apps. Original icon: Tatjana Albrandt. Haoke maintenance changes, new ceramic artwork and sounds are distributed with this source under GPL-3.0-or-later. Noto Sans SC retains SIL OFL 1.1; the prototype's XState library retains its included MIT license. Upstream navigation icons retain CC BY 2.5, Material Icons Apache-2.0, and SECUSO marks their original attribution. Backup API retains its own included license. [Upstream project](https://secuso.org/pfa).
