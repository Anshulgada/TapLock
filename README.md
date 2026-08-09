# TapLock

A minimal Android home-screen widget that locks your phone in one tap — with a smooth shackle animation and screen-dim effect.

Ghost mode: lock without keeping accessibility enabled for UPI apps.

## Why TapLock exists

On some **Android 15 and 16** builds, the built-in **lock screen shortcut** is no longer available from the system widget picker or quick settings. TapLock adds a home-screen widget that locks your device like the power button, with fingerprint/face unlock still working normally on the next wake.

## Features

- **One-tap lock** from a home screen widget
- **Ghost mode** — accessibility is only enabled for ~700 ms while locking, so UPI/banking apps don't see TapLock in your accessibility list
- **Center-screen animation** — shackle closes over whatever app you're in, screen dims to black, then locks (no forced jump to the home screen)
- **Biometric unlock preserved** — uses the system accessibility lock action (not Device Admin, which forces PIN)
- **In-app home screen** — setup status, quick actions, widget help dialog, and expandable FAQ
- **Developer options helpers** — turn dev options and USB debugging on/off from the app after setup
- **Almost no RAM or CPU when idle** — no background polling, no wake locks
- **No internet**, no analytics, no background drain
- **No screen reading** — accessibility service only performs `GLOBAL_ACTION_LOCK_SCREEN`

## Requirements

|            |                                                                          |
| ---------- | ------------------------------------------------------------------------ |
| Version    | See `gradle.properties` (`tapLockVersionName` / `tapLockVersionCode`)    |
| Min SDK    | Android 9 (API 28) — won't install below this                            |
| Target SDK | Android 15 (API 35) — built against current platform APIs                |
| Setup      | One-time ghost mode via ADB (recommended), or classic accessibility mode |

TapLock installs on **Android 9 through Android 16**. Behavior differs slightly by version — see [Platform notes](#platform-notes).

| Android version   | Ghost mode                                                | Classic mode |
| ----------------- | --------------------------------------------------------- | ------------ |
| 9–12 (API 28–31)  | Yes — both ADB commands                                   | Yes          |
| 13 (API 33)       | Yes — sideloaded apps may need ECM allow (second command) | Yes          |
| 14–16 (API 34–36) | Yes — **both** ADB commands required                      | Yes          |

## Setup

### Ghost mode (recommended)

Ghost mode keeps TapLock invisible to UPI apps. You need a PC or wireless debugging **once**.

1. Build and install the APK from your PC (USB or wireless debugging on):

```bash
./gradlew deployTapLockDebug
```

Or step by step:

```bash
./gradlew installTapLockDebug
./gradlew grantTapLockPermissions
```

For release builds:

```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/TapLock-<version>.apk
./gradlew grantTapLockPermissions
```

Or use `./gradlew installTapLockRelease` then `./gradlew grantTapLockPermissions`.

**Wireless debugging (Android 11+):** pair in Developer options, then `adb connect <ip>:<port>` before install/grant.

2. Open **TapLock** from the launcher.
3. If you installed manually (not via `deployTapLockDebug`), run **both** grant commands:

```bash
./gradlew grantTapLockPermissions
```

Or directly:

```bash
adb shell pm grant com.taplock.app android.permission.WRITE_SECURE_SETTINGS
adb shell cmd appops set com.taplock.app ACCESS_RESTRICTED_SETTINGS allow
```

4. Re-open TapLock — you should see **Ghost mode is active** in the status card.
5. Tap **Turn off developer options** so banking apps stop blocking. If you need ADB again later, tap **Turn on developer options** — TapLock also turns USB debugging back on, or shows **Turn on USB debugging** if needed.
6. Tap **Done**, then add the **TapLock** widget to your home screen (use **How to add the widget** in the app for steps).

**After reinstall:** `./gradlew deployTapLockDebug` (or re-run `grantTapLockPermissions` if you only reinstalled the APK), then turn dev options off again from the app.

**No PC?** Use wireless debugging once, then `./gradlew deployTapLockDebug` from a local shell.

**Grant fails?** On some OEM ROMs, enable **USB debugging (Security settings)** in Developer options, reboot, then retry `pm grant`. The app must declare `WRITE_SECURE_SETTINGS` in its manifest (TapLock does).

### Classic mode (fallback)

If you cannot run ADB, tap **Use classic mode instead** in TapLock and enable it in **Settings → Accessibility**. This works immediately but **some UPI and banking apps may refuse to run** while TapLock stays enabled.

### Add the widget

- Long-press home screen → **Widgets** → **TapLock** → drag to home screen
- On some phones (Samsung, Xiaomi, OnePlus, etc.), long-press the TapLock app icon and choose **Add widgets**
- In the app, tap **How to add the widget** for a step-by-step dialog with vendor-specific tips

The widget icon matches the in-app branding: white squircle background with a purple padlock.

## Platform notes

### Android 14 and above

Android 14 added **Enhanced Confirmation Mode (ECM)** for sideloaded apps. Running only the first `adb` command is not enough — the accessibility write can be silently stripped and the service never binds. **Both commands in [Setup](#ghost-mode-recommended) are required on Android 14+.**

On Android 13, the second command is recommended for sideloaded installs but may not always be needed.

If ADB is not an option, use [classic mode](#classic-mode-fallback) instead.

### Some OEM skins

On a few manufacturer ROMs, `pm grant` fails until an extra **USB debugging (Security settings)** toggle is enabled in Developer options. If the first setup command throws a SecurityException, look for that option, reboot, and retry.

## How it works

```
Widget tap
    ↓
LockReceiver → LockAnimationActivity (transparent center overlay on current screen)
    ↓
Ghost mode: SecureSettingsGate.arm() at t=0
    ↓
Shackle animation + dim to black (~520 ms lock, ~935 ms overlay)
    ↓
LockAccessibilityService → GLOBAL_ACTION_LOCK_SCREEN
    ↓
Ghost mode: disableSelf() + SecureSettingsGate.disarm()
```

**Animation timing:** the dim layer ramps over 480 ms, then the lock fires once the overlay is fully opaque (520 ms). This prevents the system lock screen from flashing through during the animation. The overlay stays up until the lock completes, then dismisses — you are not sent to the home screen first.

**Why not Device Admin?** `DevicePolicyManager.lockNow()` sets `STRONG_AUTH_REQUIRED_AFTER_DPM_LOCK_NOW`, which disables fingerprint/face unlock until you enter PIN. TapLock uses the accessibility lock action, which does not set that flag.

**Why ghost mode?** Indian UPI apps (GPay, PhonePe, Paytm, bank apps) detect any enabled accessibility service at app launch. Ghost mode flash-enables the service only during the lock animation, then removes it from the enabled-services list.

The accessibility service is scoped to the minimum needed:

- `canRetrieveWindowContent="false"` — cannot read screen content
- `canPerformGestures="false"` — cannot inject touches
- Only calls the system lock action
- **Idle cost:** nothing runs until you tap the widget

## Resource usage

| State                                               | RAM / CPU                                                                                              |
| --------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| **Idle** (widget on home screen, ghost mode active) | Effectively negligible — static widget only                                                            |
| **On tap**                                          | Brief spike: vibration, ~935 ms animation overlay, flash accessibility enable/disable, one lock action |
| **Background**                                      | Watchdog scrubs leftover accessibility entry on boot/update only                                       |

## Versioning

App version is defined once in `gradle.properties`:

```properties
tapLockVersionCode=...
tapLockVersionName=...
```

Build, APK naming (`TapLock-<version>.apk`), and the in-app version badge all read from those values.

Useful tasks:

```bash
./gradlew printVersionName              # current version string
./gradlew printDebugApkPath             # full path to the debug APK
./gradlew printReleaseApkPath           # full path to the release APK
./gradlew installTapLockRelease         # build + adb install release APK
```

## Build & test

Requirements: JDK 17+, Android SDK 35.

```bash
./gradlew assembleDebug               # debug APK → app/build/outputs/apk/debug/TapLock-<version>.apk
./gradlew assembleRelease             # release APK → app/build/outputs/apk/release/TapLock-<version>.apk
./gradlew testDebugUnitTest           # unit tests (Robolectric, incl. API 34 ECM cases)
./gradlew connectedDebugAndroidTest   # on-device smoke tests (device required)
./gradlew deployTapLockDebug          # build + adb install debug + grant permissions
./gradlew installTapLockRelease       # build + adb install release (grant separately)
```

On Windows, use `.\gradlew.bat` instead of `./gradlew`.

APK output: `app/build/outputs/apk/{debug,release}/TapLock-<version>.apk` — version from `gradle.properties`.

### Test coverage

Unit tests (Robolectric) cover:

- Ghost-mode gate arm/disarm and enabled-services list manipulation
- Lock receiver routing (setup vs animation, no home-screen launch)
- Lock animation overlay (dim snap, no home intent)
- Accessibility service lock readiness
- MainActivity setup states, dev-options/USB-debug toggles, help expand
- Widget and app icon resources
- Watchdog scrub on boot/update

On-device smoke tests verify manifest declarations (version, accessibility service, widget provider, permissions, launch modes).

## Uninstall

1. Remove the widget from your home screen
2. If using classic mode: **Settings → Accessibility** → disable **TapLock**
3. Uninstall the app normally

## Privacy

- No `INTERNET` permission
- No data collection or storage
- Backup disabled (`allowBackup="false"`)
- Debug logging only in debug builds

## Project structure

```
app/src/main/
├── AndroidManifest.xml              # WRITE_SECURE_SETTINGS, components
├── java/com/taplock/app/
│   ├── MainActivity.kt              # Home screen, setup, help, quick actions
│   ├── LockAnimationActivity.kt     # Center-screen animation overlay
│   ├── LockAccessibilityService.kt  # Screen lock action
│   ├── SecureSettingsGate.kt        # Ghost mode arm/disarm + dev options
│   ├── AccessibilityServicesList.kt # Enabled-services list helpers
│   ├── WatchdogReceiver.kt          # Boot/update scrub
│   ├── LockWidgetProvider.kt        # Home screen widget
│   └── LockReceiver.kt              # Widget tap handler
└── res/
    ├── drawable/                    # ic_app_icon, ic_lock frames, UI backgrounds
    ├── layout/                      # activity_main, dialog_widget_help, view_app_icon_hero
    └── xml/                         # Widget + accessibility config
```

## License

MIT — use freely for personal projects. No warranty.
