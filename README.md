# TapLock

A minimal Android home-screen widget that locks your phone in one tap — with a smooth shackle animation and screen-dim effect.

## Why TapLock exists

On **HyperOS 3 with Android 16** (the only setup verified so far), the built-in **lock screen shortcut** is no longer available from the system widget picker or quick settings — you can't drop a native "lock phone" tile on your home screen the way you used to. **Android 15 may still offer this on some devices**; TapLock is mainly for setups like HyperOS 3 / Android 16 where the system option is gone.

TapLock fills that gap: a single home-screen widget that locks your device like the power button, with fingerprint/face unlock still working normally on the next wake.

## Features

- **One-tap lock** from a home screen widget
- **Center-screen animation** — shackle closes, screen dims to black, then locks
- **Almost no RAM or CPU when idle** — no background polling, no wake locks, no always-on service work
- **Tiny burst only when used** — a short overlay (~710ms) on tap, then the app exits; the accessibility service stays registered but passive until the next lock
- **No internet**, no analytics, no background drain
- **No screen reading** — accessibility service only performs `GLOBAL_ACTION_LOCK_SCREEN`
- **Minimal permissions** — vibration only (for tap feedback)

## Requirements

| | |
|---|---|
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 15 (API 35) |
| Setup | Enable TapLock in Accessibility Settings (one time) |

> **Tested on:** HyperOS 3, Android 16. Other devices and ROMs may work but are not verified yet.

## Setup

1. Install the APK and open **TapLock** from the launcher.
2. Tap **Open Accessibility Settings** and enable **TapLock**.
3. Tap **Close** — the app exits and won't clutter Recents.
4. Long-press home screen → **Widgets** → add **TapLock**.
5. Tap the widget to lock.

## How it works

```
Widget tap
    ↓
LockReceiver → LockAnimationActivity (center overlay)
    ↓
Shackle animation + dim to black (~710ms)
    ↓
LockAccessibilityService → GLOBAL_ACTION_LOCK_SCREEN
```

The accessibility service is scoped to the minimum needed:

- `canRetrieveWindowContent="false"` — cannot read screen content
- `canPerformGestures="false"` — cannot inject touches
- Only calls the system lock action
- **Idle cost:** registered with the system but does no work until you tap the widget — no timers, no network, no screen scraping

## Resource usage

| State | RAM / CPU |
|---|---|
| **Idle** (widget on home screen, service enabled) | Effectively negligible — static widget + dormant accessibility binding |
| **On tap** | Brief spike only: vibration, ~710ms animation overlay, one lock action, then activity finishes |
| **Background** | Nothing runs in the background; no periodic jobs or listeners |

The app is designed to stay out of the way until you actually lock the phone.

## Build

Requirements: JDK 17+, Android SDK 35.

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK (R8 + shrink)
./gradlew testDebugUnitTest  # unit tests
./gradlew installDebug       # install via ADB
```

APK output: `app/build/outputs/apk/`

## Uninstall

1. **Settings → Accessibility** → disable **TapLock**
2. Remove the widget from your home screen
3. Uninstall the app normally

## Privacy

- No `INTERNET` permission
- No data collection or storage
- Backup disabled (`allowBackup="false"`)
- Debug logging only in debug builds

## Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/taplock/app/
│   ├── MainActivity.kt              # One-time setup
│   ├── LockAnimationActivity.kt     # Center-screen animation
│   ├── LockAccessibilityService.kt  # Screen lock action
│   ├── LockWidgetProvider.kt        # Home screen widget
│   └── LockReceiver.kt              # Widget tap handler
└── res/
    ├── drawable/                    # Lock icon frames
    ├── layout/
    └── xml/                         # Widget + accessibility config
```

## License

MIT — use freely for personal projects. No warranty.
