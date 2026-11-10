# Build & Compilation Instructions

This document provides clear, actionable instructions for building **Preet's Detox Launcher** into a real, installable Android APK (`app-debug.apk` and `app-release.apk`).

---

## 1. System Requirements

- **Java Development Kit (JDK):** OpenJDK 17 or higher
- **Android SDK:** Platform 34 (Android 14) and Build Tools 34.0.0
- **Build System:** Gradle 8.4+ (invoked via `./gradlew`)
- **Android Target:** minSdk 29 (Android 10), targetSdk 34 (Android 14)

---

## 2. Recommended Build Options

### Option A: Automated GitHub Actions CI/CD (Zero Setup Required)
This repository includes a turnkey GitHub Actions workflow at [`.github/workflows/build.yml`](.github/workflows/build.yml).

1. Push or mirror this repository to GitHub:
   ```bash
   git init
   git add .
   git commit -m "Initial commit of Preet's Detox Launcher"
   git remote add origin <your-github-repo-url>
   git push -u origin main
   ```
2. In your GitHub repository, open the **Actions** tab.
3. The **Build Android APK** workflow will execute automatically on Ubuntu runners equipped with OpenJDK 17 and Android SDK 34.
4. Once completed (approx. 2-3 minutes), download the compiled artifacts:
   - `PreetsDetoxLauncher-Debug-APK` (`app-debug.apk`)
   - `PreetsDetoxLauncher-Release-APK` (`app-release.apk`)

---

### Option B: Android Studio (Desktop)
1. Launch Android Studio (Version Ladybug 2024.2+, Koala, or Hedgehog).
2. Select **File** > **Open** and choose the `PreetsDetoxLauncher` project root folder.
3. Allow Gradle to synchronize dependencies.
4. Select build variant **debug** or **release**.
5. Click **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**, or run directly on a connected device/emulator via the **Run** button (Shift + F10).
6. Output APK location:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

### Option C: Desktop Command Line (Linux / macOS / Windows)
Open a terminal in the project directory:

```bash
# Ensure execution permissions on the wrapper
chmod +x gradlew

# Run automated unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Optional: Assemble Release APK
./gradlew assembleRelease
```

The compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

To install directly to a connected phone:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### Option D: Building on Android Device via Termux (PRoot Distro)
On standard Android devices, Android sandbox restrictions prevent executing standalone binary compilers (`fork/exec` permissions). To compile locally on your Android device:

1. Install PRoot Distro in Termux:
   ```bash
   pkg install proot-distro
   proot-distro install ubuntu
   proot-distro login ubuntu
   ```
2. Inside the PRoot Ubuntu container, install OpenJDK 17:
   ```bash
   apt update && apt install -y openjdk-17-jdk git wget unzip
   ```
3. Run the Gradle build:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 3. Verifying the APK

After building, inspect the generated APK file:
```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```
Ensure the file size is non-zero (typically 12MB - 18MB depending on ProGuard shrinking).

Install onto your Android device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Upon installation, press your device's **Home** button to select **Preet's Detox Launcher** as your default home experience.
