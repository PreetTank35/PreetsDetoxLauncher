# Preet's Detox Launcher

> **Less scrolling. More living.**  
> *"Your phone should be a tool you control, not a habit that controls you."*

**Preet's Detox Launcher** is a native Android home screen replacement engineered to break smartphone addiction, reduce dopamine-driven scrolling, and restore digital intentionality.

Built exclusively with **Kotlin**, **Jetpack Compose**, **Material 3**, and modern Android Architecture Components (Room, Coroutines, Flow, DataStore), it is designed as a genuine Android `HOME` launcher that can be installed as an APK and set as your device's default launcher.

---

## 🌟 Key Features

### 1. Minimalist AMOLED Home Screen
- **Pure Black Design:** Engineered with `#000000` AMOLED surfaces to minimize screen emission and battery drain.
- **Large Digital Clock & Date:** Real-time clock with configurable sizes (`Large`, `Medium`, `Compact`).
- **Live Screen-Time Progress:** Dynamic progress gauge comparing today's actual screen time against your daily goal.
- **Reliable Device Unlock Counter:** Tracks phone unlocks via Android system `UsageEvents`.
- **Custom Motivational Quotes:** Customizable reflection prompts (e.g. *"Be present in this moment"*).
- **Curated Favorites Dock:** Pin 1 to 5 intentional applications.
- **Intuitive Gestures:**
  - **Swipe Up:** Opens the full Application Drawer.
  - **Swipe Down:** Smoothly pulls down the Android notification shade.

### 2. Full Application Drawer & Fast Search
- **Instant Search:** Fuzzy search by application name or package ID with a single-tap clear button.
- **Categorization:** Tabs for *All*, *Favorites*, *Recents*, and custom user-defined categories.
- **Contextual Management:** Long-press any application to favorite, hide, rename, customize icons, or set detox limits.
- **Clean Empty States:** Clear feedback when search queries return zero results.

### 3. Digital Detox Engine (Balanced Restriction Philosophy)
Never implement aggressive locks that prevent access to emergency or essential functions. Preet's Detox Launcher offers 3 customizable restriction tiers:
- **Soft Block:** Mindful pause dialog explaining daily quota exhaustion and suggesting 5-minute stretching or breathing exercises, while giving users the conscious agency to continue.
- **Strict Block:** Full-screen blocking activity (`DetoxBlockActivity`) displaying time remaining until midnight reset, mindful reflection prompts, and an emergency override quota (max 3/day).
- **Scheduled Block:** Restricts selected distraction apps during specific recurring windows (e.g., 22:00 to 07:00 on weekdays).
- **Whitelisted Essentials:** Emergency calling, dialer, SMS, camera, and system settings can **never** be restricted.

### 4. Intentional App Opening & Countdown Delays
- **Mindful Questioning:** Before launching reflexively opened apps, prompts: *"Why are you opening this app?"* (Study, Work, Communication, Entertainment, Just browsing). Logs choices for weekly reflection.
- **App Opening Delay:** Optional 5, 10, or 15-second breathing countdown with visual circular progress ring and cancellation button.

### 5. Pomodoro Focus System
- **Presets & Custom Timers:** Standard 25m/5m, 50m/10m, and custom focus/break durations.
- **Persistent State:** Timer state persists if minimized or interrupted.
- **Ongoing Notifications:** Displays remaining time in the notification shade without battery-draining polling loops.
- **Focus History:** Daily focus minutes tracked in local Room database.

### 6. Detox Streaks & Local Daily Challenges
- **Streak Tracker:** Calculates consecutive days meeting screen-time goals with timezone-aware midnight boundaries.
- **Daily Challenges:** Encouraging daily goals (e.g., *"Keep screen time under 2 hours"*, *"Complete 2 Pomodoro sessions"*). No fake or gamified microtransactions.

### 7. Dual Icon Customization System
- **A. Icon Pack Integration:** Discovers installed ADW, Nova, and Launcher-compatible icon packs and maps them to installed apps.
- **B. Individual Image Customization:** Select photos or icons from device storage via Android's Photo Picker / Storage Access Framework (SAF). Crops, previews, and saves assets to app-private storage.
- **Application Renaming:** Custom display labels persist across launcher refreshes.

### 8. Grayscale Screen Mode
- **Launcher-Level Grayscale:** Toggles monochrome aesthetic inside the launcher to reduce visual stimulation.
- **System-Wide Guided Setup:** In-app walkthrough to enable Android OS-level grayscale in Accessibility / Bedtime settings.

### 9. 100% Offline Privacy Guarantee & SAF Backup
- **Zero Cloud / Zero Tracking:** No account, no email registration, zero telemetry or server uploads.
- **Versioned JSON Backup:** Export and import settings, rules, categories, icon mappings, and streaks via Storage Access Framework (`.json`).

---

## 📱 Architecture & Tech Stack

```
com.preet.detoxlauncher/
├── data/
│   ├── local/
│   │   ├── DetoxDatabase.kt          # Room Database (7 entities, 7 DAOs)
│   │   ├── dao/                      # AppDao, DetoxRuleDao, FocusDao, StreakDao...
│   │   └── entity/                   # AppItem, DetoxRule, FocusSession, Streak...
│   ├── model/                        # Domain models: AppInfo, DetoxRule, PomodoroState...
│   ├── preferences/                  # AndroidX DataStore UserPreferences
│   └── repository/                   # AppRepo, DetoxRepo, UsageRepo, FocusRepo, BackupRepo...
├── service/
│   ├── DetoxAccessibilityService.kt  # On-device foreground window observer (zero keylogging)
│   └── DetoxForegroundMonitor.kt    # Background alarms & periodic quota evaluations
├── receiver/
│   ├── BootReceiver.kt               # Re-registers alarms on device restart
│   └── AppChangeReceiver.kt          # Dynamic package add/remove detection
└── ui/
    ├── MainActivity.kt               # Launcher EntryPoint & Compose NavHost
    ├── block/                        # Fullscreen Strict Block Interceptor
    ├── components/                   # ClockWidget, UsageCard, SearchBar, Dialogs...
    ├── customicon/                   # Photo Picker, Crop, Icon Packs
    ├── dashboard/                    # Insights, Weekly Charts, Streaks
    ├── detox/                        # Boundary configuration per application
    ├── drawer/                       # App Drawer, Search, Categories, Context Menu
    ├── focus/                        # Pomodoro Timer & Focus Sessions
    ├── home/                         # Minimalist Home Screen & Gestures
    ├── onboarding/                   # 12-Step Guided Onboarding Flow
    ├── settings/                     # Appearance, Grayscale, Permissions, Backup
    └── theme/                        # Pure AMOLED Color Palette, Typography, Shapes
```

- **Language:** Kotlin 1.9.24
- **UI Toolkit:** Jetpack Compose + Material 3 (BOM 2024.04.01)
- **Minimum SDK:** Android 10 (API 29)
- **Target SDK:** Android 14 (API 34)

---

## 🛠️ Build & Installation

### Option 1: Automated Cloud Build (GitHub Actions)
This repository includes a pre-configured CI/CD workflow (`.github/workflows/build.yml`).
1. Push this repository to GitHub.
2. Navigate to **Actions** > **Build Android APK**.
3. Download the compiled `PreetsDetoxLauncher-Debug-APK` directly from the workflow artifacts.

### Option 2: Android Studio / Desktop Gradle
1. Clone or copy the project to your computer.
2. Open in Android Studio (Ladybug / Koala / Jellyfish).
3. Connect your Android device via USB debugging or start an emulator.
4. Run:
   ```bash
   ./gradlew assembleDebug
   ```
5. Install the generated APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## ⚙️ Initial Setup & Permissions Guide

1. **Set Default Home App:**
   - Open Android **Settings** > **Apps** > **Default Apps** > **Home App**.
   - Select **Preet's Detox Launcher**.
2. **Grant Usage Access (Essential for Screen Time):**
   - Open Android **Settings** > **Security & Privacy** > **Special App Access** > **Usage Access**.
   - Enable access for **Preet's Detox Launcher**.
3. **Optional Accessibility Service (For Strict App Interception):**
   - Open Android **Settings** > **Accessibility** > **Preet's Detox Launcher**.
   - Toggle **On**. (Zero keystrokes or private data is ever inspected).

---

## 🔒 Honest Transparency & Limitations

- **Ordinary Launcher Boundaries:** Preet's Detox Launcher adheres to standard Android sandboxing. It does **not** employ hidden device-owner exploits, anti-uninstall tricks, or root workarounds. Users can change their default launcher or uninstall anytime via Android Settings.
- **Accurate Screen Time:** Unlock counts and application usage depend directly on Android `UsageStatsManager`. If permission is withheld, the launcher cleanly displays placeholder guidance rather than fabricating false metrics.

---

## 📄 License
Open source under the [Apache License 2.0](LICENSE).  
*Created with mindfulness for digital wellbeing.*
