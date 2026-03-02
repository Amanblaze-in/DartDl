<div align="center">

<img width="144" src="fastlane/metadata/android/en-US/images/icon.png" alt="DartDL logo">

# DartDL

<p>
    <b>A fast, modern video downloader app for Android, powered by yt-dlp.</b>
    <br/>
    <a href="https://github.com/Amanblaze-in/DartDl">View the repository on GitHub</a>
</p>

</div>

---

## 🚀 Overview

**DartDL** is a powerful video/audio downloading application designed with a clean, modern interface using Material You principles. It allows you to download videos from hundreds of supported platforms directly to your Android device with advanced format selection and metadata extraction.

> **Note on Play Store Compliance:** Due to Google Play strict policies, downloading from YouTube is explicitly **disabled** in the official Play Store release of DartDL to prevent account suspension.

## ✨ Features

- **Blazing Fast Downloads:** Powered by the robust `yt-dlp` backend.
- **Material You Design:** A beautiful, responsive UI that adapts to your device's theme colors.
- **Background Downloading:** Uses efficient Android services to download in the background without keeping the app open.
- **Subtitles & Metadata:** Automatically embeds thumbnails, metadata, and subtitles into downloaded files alongside the video.
- **Custom Formats:** Choose exactly the audio or video quality you want.
- **PlayList Support:** Download entire playlists with a single click.

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

## 📜 License

DartDL is a free software project (licensed under the GNU General Public License v3.0). It is modified from the original open-source [Seal](https://github.com/JunkFood02/Seal) project to comply with Google Play Store policies and to introduce new branding. 

* `yt-dlp` is used as a backend dependency for extraction.
* The DartDL Icon and Brand Name are custom assets and should not be reused without permission.
