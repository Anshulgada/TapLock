# TapLock

A minimal Android home-screen widget that locks your phone in one tap — with a smooth shackle animation and screen-dim effect.

## Why TapLock exists

Recent Android versions (including Android 14 and 15) removed or restricted the built-in **lock screen shortcut** from the system widget picker and quick settings on many devices. OEM skins like HyperOS/MIUI followed suit — you can no longer drop a native "lock phone" tile on your home screen the way you used to.

TapLock fills that gap: a single home-screen widget that locks your device like the power button, with fingerprint/face unlock still working normally on the next wake.

## Features

- **One-tap lock** from a home screen widget
- **Center-screen animation** — shackle closes, screen dims to black, then locks
- **No internet**, no analytics, no background drain
- **No screen reading** — accessibility service only performs `GLOBAL_ACTION_LOCK_SCREEN`
- **Minimal permissions** — vibration only (for tap feedback)

## Requirements

| | |
|---|---|
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 15 (API 35) |
| Setup | Enable TapLock in Accessibility Settings (one time) |

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
Shackle animation + dim to black (~780ms)
    ↓
LockAccessibilityService → GLOBAL_ACTION_LOCK_SCREEN
```

The accessibility service is scoped to the minimum needed:

- `canRetrieveWindowContent="false"` — cannot read screen content
- `canPerformGestures="false"` — cannot inject touches
- Only calls the system lock action

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
