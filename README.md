# Sabi Ko — Android v1.3

Compact layout: 20px word labels, 44px minimum utility controls, tighter spacing and header; same board order.

Kasama na ang 6 custom action pictures (Kain, Inom, Ligo, Tulog, Laro, Yakap), branded launcher icon, at native recording fix. Makikita ang anim sa Gawain.

UPDATE: I-upload ang buong app folder at project configuration sa parehong repository. Kung main.yml ang dati mong workflow, palitan iyon gamit ang .github/workflows/android.yml sa package at iwasan ang dalawang duplicate workflows. I-install bilang update; HUWAG mag-uninstall kung may importanteng custom buttons. Kung signing mismatch, itigil muna at i-report ang error.


Android project para sa Tagalog communication board. Kasama sa app ang mga buttons at larawan; walang server na kailangang patakbuhin. Android 8.0 pataas.

## Paano gumawa ng APK sa GitHub

1. I-extract ang ZIP. Gumawa ng bagong repository, halimbawa `sabi-ko-android`.
2. I-upload ang LAHAT ng laman sa root ng repository: `app`, `.github`, `settings.gradle`, `build.gradle`, `gradle.properties`, at README. Huwag i-upload ang ZIP mismo. Sa Windows, tiyaking kasama ang `.github` folder.
3. Gamitin ang `main` branch. Sa GitHub, buksan ang Actions → Build Android APK. Kung walang automatic run, piliin ang Run workflow. Kung disabled ang workflows, enable GitHub Actions para sa repository.
4. Hintaying maging green ang build. Sa pinakailalim ng successful run, i-download ang artifact na `Sabi-Ko-Android-APK`.
5. I-extract ang artifact ZIP at kopyahin ang `app-debug.apk` sa phone.
6. Buksan ang APK sa phone. Kung hinihingi ng Android, payagan ang Install unknown apps para lamang sa app na ginagamit mong pambukas ng file. Pagkatapos, piliin ang Install.

Walang computer na kailangang naka-on kapag ginagamit na ang installed app. GitHub Actions ang gumagawa ng APK; kailangan ng internet para sa upload, build at download.

## Offline at boses

- Naka-bundle ang app interface at symbols. Nasa app data ng phone ang sariling buttons, larawan at recordings.
- Ang built-in Android text-to-speech ay gumagamit ng Filipino voice kapag available. Ang ilang voice ay nangangailangan ng downloaded voice data o internet. Subukan sa airplane mode. Kung walang Filipino voice, gumamit ng sariling recordings.
- Para sa recording, piliin ang Para sa magulang → I-record ang boses at payagan ang microphone.
- Hindi inililipat ng APK ang sariling recordings mula sa naunang website. Magkahiwalay ang website storage at Android app storage.
- Huwag i-clear ang app data o mag-uninstall kung kailangan pang itago ang sariling buttons at recordings. Wala pang backup/export feature.

## Build status at testing

Source project ito, hindi pa compiled APK. JavaScript syntax at package structure lang ang na-check sa preparation environment. Kailangan pang patakbuhin ang GitHub build at subukan sa Android phone ang native text-to-speech, photo picker, microphone permission, recording playback, at persistence pagkatapos isara/buksan ang app.

Ang workflow ay gumagawa ng debug APK para sa personal testing. Bawat fresh runner ay maaaring gumamit ng ibang debug signing key; bago ang regular updates, gumamit ng permanenteng signing key para maiwasan ang uninstall na mawawalan ng app data. Hindi pa ito Play Store release.

## Local build (optional)

JDK 17, Android SDK 35 at Gradle 8.9 ang kailangan. Patakbuhin ang `gradle assembleDebug` sa project root. Nasa `app/build/outputs/apk/debug/app-debug.apk` ang output.

## Technical references

- Android local web content: https://developer.android.com/develop/ui/views/layout/webapps/load-local-content
- WebView microphone permission: https://developer.android.com/reference/android/webkit/PermissionRequest
- Android speech: https://developer.android.com/reference/android/speech/tts/TextToSpeech

Iangkop ang vocabulary sa bata; maaaring ipa-review ang layout sa speech-language professional na may AAC experience.
