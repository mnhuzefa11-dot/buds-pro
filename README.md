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

## Get the APK without installing anything (easiest)

1. Open the repo on GitHub and click the **Actions** tab.
2. In the left sidebar click **Build Android APK**.
3. Click the **Run workflow** button on the right, pick the branch you want, and confirm.
4. Wait ~3-4 minutes for the green check.
5. Click the finished run, scroll to the bottom to **Artifacts**, and download **BudsPro-debug-apk**.
6. Unzip it — inside is `app-debug.apk`.

### Installing on your phone

Copy `app-debug.apk` to the phone and tap it. Android will ask permission to
"install unknown apps" the first time — allow it for your browser or file
manager, then tap Install.

Because the package name is unchanged (`com.budspro.app`), this installs as an
**update over the existing app**, so everything you already imported stays put.

## App interface

- **Library** — every imported file as a card (title, type, size, progress,
  favourite, delete) plus the `+` Import button.
- **Saves** — only the items you have hearted.
- **Recent** — items you have opened, most recent first.
- **Settings** — library/saved/recent counts, storage used, supported types.

Supported file types: **HTML**, **PDF**, **JSON**.
