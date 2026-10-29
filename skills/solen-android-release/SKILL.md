---
name: solen-android-release
description: Build, version-bump and sign a release .aab for a SolenLabs Android WebView wrapper app (Drava, Fovu, Bonva, Sammy...). Use when Cameron asks to build a new version, make an .aab, bump the version, or fix a Gradle/signing/JDK error.
---

# SolenLabs Android release (.aab)

The apps are thin WebView wrappers (one `MainActivity`) that load the live PWA. The web app ships via `solen-site-deploy`; a new .aab is only needed for native changes (billing, permissions, file chooser, icons, SDK targets).

## Where builds happen
- **Signed release builds only happen on Cameron's Windows PC**, because the keystore lives there. Cloud sessions can't download Google's Maven artifacts (dl.google.com is blocked), so in the cloud you can only write Java and syntax-check it with `javac` (ignore missing-symbol errors).
- Hand Cameron ONE paste-able prompt for his local Claude: `cd C:\Users\camer\<Project>` then `claude` (add `--chrome` if Play Console work follows).
- Get files to his PC via a **public raw GitHub URL** (e.g. a non-`main` branch of a public `<app>-site` repo) that the local Claude downloads with `Invoke-WebRequest`. Files "sent" in the cloud chat do NOT land in his Downloads.

## Prompt template for the local Claude
```
1. Download <files> from <raw URLs> into app/src/main/java/<package path>/. Diff against the current file first and tell me what the old one had that the new one lacks.
2. In app/build.gradle.kts set versionCode = <N> and versionName = "<X.Y>".
3. Build: $env:JAVA_HOME = "C:\Users\camer\.jdks\jbr-17.0.14"; .\gradlew.bat bundleRelease
4. If you get "Duplicate class kotlin..." fix ONLY with implementation(platform("org.jetbrains.kotlin:kotlin-bom:<version from error>")).
5. Give me the full path to the .aab.
```

## Hard rules
- **JDK 17** (`C:\Users\camer\.jdks\jbr-17.0.14`). Android Studio's bundled JDK 25 breaks Gradle 8.14. In Android Studio: Settings → Build Tools → Gradle → Gradle JDK = jbr-17. Leave Gradle user home at `C:\Users\camer\.gradle`.
- **Never accept Android Studio's AGP Upgrade Assistant.** If he already did, diff the version files, rebuild, and revert if it fails.
- **Dependencies:** `androidx.appcompat:appcompat:1.6.1`, plus `com.android.billingclient:billing:8.x` only if the app sells anything. Nothing else.
- **versionCode** must be higher than anything ever uploaded on ANY track. Check Play Console first; handoff docs go stale (Drava's said 7 when it was 10).
- **Keystore passwords** live in `C:\Users\camer\.gradle\gradle.properties`, never in `build.gradle.kts` and never in Drive docs. If signing fails with "password incorrect", test candidates with `keytool -list` (quote passwords containing `!`).
- compileSdk/targetSdk **36** (Google blocks updates below the current target API — check Play Console Policy status each year), minSdk 24, Java 11. Guard API-30+ calls such as `setDecorFitsSystemWindows` with `Build.VERSION.SDK_INT >= R`.
- **WebView must-haves:** `WebViewClient` keeping the app's own domain in-app and opening everything else externally; `WebChromeClient.onShowFileChooser` (otherwise `<input type=file>` silently does nothing); `onCreateWindow` for `target=_blank`/`window.open`; no `window.location.hash=` in the PWA (it reloads the WebView, so use `history.pushState`).
- Android Studio terminal "Unhandled exception … Dispatchers.UI / read lock" popups are IDE bugs. Clear them and use plain PowerShell.

## Reference implementation
`solenlabsllc-ui/claude` → `drava/android/` (`MainActivity.java`, `BillingBridge.java`).
