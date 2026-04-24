<div align="center">
<img width="144" src="fastlane/metadata/android/en-US/images/icon.png" alt="DartDL logo">

# DartDL

<p>
    <b>A fast, modern video downloader app for Android, powered by yt-dlp.</b>
</p>

[![GitHub License](https://img.shields.io/github/license/JunkFood02/Seal?style=flat-square)](LICENSE)
[![Play Store](https://img.shields.io/badge/Google_Play-DartDL-green?style=flat-square&logo=google-play)](https://play.google.com/store/apps/details?id=com.dartdl.app)
[![Weblate](https://hosted.weblate.org/widgets/seal/-/seal/svg-badge.svg)](https://hosted.weblate.org/engage/seal/)

[![Telegram](https://img.shields.io/badge/Telegram-Channel-blue.svg?style=flat-square&logo=telegram)](https://t.me/Amanblaze)

</div>

---

## 🚀 Overview

**DartDL** is a powerful video/audio downloading application designed with a clean, modern interface using Material You principles. It is based on the amazing [Seal](https://github.com/JunkFood02/Seal) project and powered by the robust `yt-dlp` backend.

DartDL allows you to download videos from hundreds of supported platforms directly to your Android device with advanced format selection and metadata extraction.

> **Note on Play Store Compliance:** Due to Google Play strict policies, downloading from YouTube is explicitly **disabled** in the official Play Store release of DartDL to prevent account suspension.

## ✨ Features

- **Blazing Fast Downloads:** Powered by the robust `yt-dlp` backend.
- **Material You Design:** A beautiful, responsive UI that adapts to your device's theme colors.
- **Background Downloading:** Uses efficient Android services to download in the background without keeping the app open.
- **Subtitles & Metadata:** Automatically embeds thumbnails, metadata, and subtitles into downloaded files alongside the video.
- **Custom Formats:** Choose exactly the audio or video quality you want.
- **Playlist Support:** Download entire playlists with a single click.
- **Aria2 Integration:** Support for aria2c as an external downloader for even faster speeds.
- **SponsorBlock Support:** Automatically remove or mark sponsor segments in videos.
- **Advanced Networking:** Built-in proxy support and cookie management for accessing restricted content.
- **Custom Commands:** Execute complex yt-dlp commands directly with templates.

## 📂 Project Structure 📱

```text
📂 ./
┣ 📂 app/                        📱 Main Android application module
┣ 📂 buildSrc/                   🧰 Custom Gradle build constants
┣ 📂 color/                      🖌️ Secondary module for color processing/dynamic theming
┣ 📂 gradle/                     📦 Gradle wrapper and version catalog
┣ 📂 fastlane/                   🏎️ Fastlane configuration for automated deployment
┣ 📂 logo_assets/                🎨 Branding and design assets
```

## 🛠️ Build Instructions

To build DartDL from source, you will need **JDK 21** and Android Studio.

### Standard Build (Everything Enabled)
```bash
./gradlew assembleGithubRelease
```

### Google Play Store Build (YouTube Disabled)
```bash
./gradlew bundlePlayStoreRelease
```
*This generates a compliant `.aab` file ready for Play Console upload.*

## ❤️ Credits

DartDL is a fork of the incredible **[Seal](https://github.com/JunkFood02/Seal)** project by **[JunkFood02](https://github.com/JunkFood02)**. We are grateful for their amazing work and for making it open source.

Special thanks to:
- **[yt-dlp](https://github.com/yt-dlp/yt-dlp)** for the downloader core.
- **[youtubedl-android](https://github.com/yausername/youtubedl-android)** for the Android wrapper.

## 📜 License

DartDL is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for more details.

---
<div align="center">
Made with ❤️ by the DartDL Team
</div>

