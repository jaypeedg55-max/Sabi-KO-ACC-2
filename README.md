# Sabi Ko v1.6 — Android AAC Board

New pastel button grid inspired by the supplied reference. Original **Sabi Ko** app name, original launcher icon and original `pictures/sabi-ko.webp` logo are preserved. Sentence builder, voice, quick words, categories, favorites, custom words/photos/recordings and parent settings are retained.

## APK via GitHub Actions
Upload the CONTENTS of this extracted folder to the **root** of your repository including the complete `app/` directory and `.github/workflows/android.yml`. Open **Actions → Build Android APK → Run workflow**. Download the artifact **Sabi-Ko-v1.6-debug-APK** and extract `app-debug.apk`.

## Important
This is the Android **source project**, not a precompiled APK. A signed debug APK is created by GitHub Actions. The application ID stays `ph.sabiko.app`; debug signing keys can differ between GitHub runs, so an install over an existing app may fail. Back up any custom data before considering uninstall, as uninstalling removes saved words, pictures and recordings.
