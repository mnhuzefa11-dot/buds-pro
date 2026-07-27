# buds-pro

Buds Pro Android app project.

## Build APK

This repository is set up as a standard Android application module at `app/`.

To build a debug APK locally:

```bash
gradle assembleDebug --no-daemon --project-dir .
```

The APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

You can also use the GitHub Actions workflow named **Build Android APK** and download the `BudsPro-debug-apk` artifact after it finishes.
