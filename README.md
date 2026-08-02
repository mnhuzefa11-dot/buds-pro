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

The app uses a three-tab bottom navigation bar.

- **Library** — every imported file as a card. Covers fill the card and crop
  to fit, with the title on a gradient overlay, a file-type badge, a favourite
  heart and a progress bar. Search, filter chips (favourites, recent, by type),
  sorting, a grid/list toggle and pull-to-refresh are all here, plus the `+`
  Import button.
- **Collections** — group items into named collections such as "Biology" or
  "Math Games". Each tile shows a 2x2 mosaic of the first four covers inside
  it. Tap to open, long press to rename or delete.
- **Settings** — theme, default view, storage usage, cache, backup and About.

The **Study** screen (image annotation) is still available from the book icon
in the Library top bar.

## Image viewing and zoom

Both the image viewer and the Study screen use a shared zoomable canvas:

- **Pinch** with two fingers to zoom (up to 6x, 8x in Study).
- **Double tap** to jump to 3x, double tap again to fit.
- **Drag** to pan; panning is clamped so the image never flies off screen.
- **Single tap** hides/shows the top bar in the viewer.

In Study, notes are placed deliberately: tap **Add note**, then tap the exact
spot on the image. Markers are anchored to the image, so they stay on the
right spot while you zoom and pan, and tapping one opens it for reading or
deleting. The eye icon hides all markers so you can see the image cleanly.

Supported file types: **HTML**, **PDF**, **JSON**, **images**.

## Long press any item

Long pressing a card (with haptic feedback) opens a context menu:

Open/Play · Add or Change Cover · Rename · Add to Collection ·
Mark as Favorite / Unfavorite · Share file · View Info · Delete

"View Info" shows file size, date added, type, last opened, progress and
total time spent. "Delete" always asks for confirmation.

## Themes

Four themes are selectable in Settings, all built around the purple accent
`#A855F7`: **Dark** (default), **Light**, **AMOLED Black** and **Purple**.
Your choice is saved with DataStore and applied on next launch.

## Backup and restore

Settings can export your whole library — content files, cover images,
collections and progress — into a single `.zip` you can store anywhere.
Importing a backup **merges** it into your library: entries sharing an id are
replaced by the backup's copy, everything else is left alone.

## Where your files live

Everything you import is copied into the app's private storage under
`filesDir/games/`, with covers in `filesDir/covers/`. Nothing is uploaded and
nothing needs network access, so the whole library works offline.

HTML games are served through `WebViewAssetLoader` over a real local origin
(`appassets.androidplatform.net`). That is what makes `localStorage` a genuine
OS-backed file, so game saves survive app restarts and updates.

## Data and upgrades

The Room database is at version 4. Upgrading from any earlier version migrates
in place — imported files, covers, favourites and progress are all preserved.
Because the package name is unchanged, installing a new APK is an update over
the existing app rather than a fresh install.
