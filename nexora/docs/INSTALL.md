# Install NEXORA on your phone

Two ways. Pick one.

## A. Direct download from GitHub (no account needed)

The release is public, so you can grab the APK straight from the browser.

**Open this on your phone:**

https://github.com/abhinavnoxus/Project1/releases/tag/nexora-v1.0.0

Under **Assets**, tap:

| File | Size | Use this when |
|---|---|---|
| `app-release.apk` | 1.49 MB | **Recommended.** R8-minified and resource-shrunk. |
| `app-debug.apk` | 18.16 MB | Only if the release build misbehaves — it has larger binaries and more logging. |
| `SHA256SUMS.txt` | — | Optional integrity check. |

Direct links:

```
https://github.com/abhinavnoxus/Project1/releases/download/nexora-v1.0.0/app-release.apk
https://github.com/abhinavnoxus/Project1/releases/download/nexora-v1.0.0/app-debug.apk
```

On a phone, tap the `.apk` and let Chrome or Files download it. It lands in
`Downloads/`. Tapping the download notification opens the installer directly.

### If Android blocks the install

You will see **"For your security, your phone is not allowed to install unknown apps
from this source."** This is normal for any APK that did not come from Play.

1. Tap **Settings** in that dialog.
2. Enable **Allow from this source** (typically marked *Chrome* or *Files*).
3. Press back and tap **Install**.

You do **not** need USB debugging for this route.

### If Play Protect warns you

Play Protect flags every self-signed APK. Tap **More details → Install anyway**. The APK is
signed with a debug key, not a Play signing key — see "Signed with a debug key" below.

## B. From a computer over USB

Requires `adb` (Android platform-tools) and USB debugging enabled on the phone.

```bash
curl -L -o app-release.apk \
  https://github.com/abhinavnoxus/Project1/releases/download/nexora-v1.0.0/app-release.apk

adb install -r app-release.apk
```

Or build it yourself from source, then `adb install -r` the output:

```bash
git clone https://github.com/abhinavnoxus/Project1.git
cd Project1/nexora
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Building requires JDK 17 and the Android SDK with platform 35 and build-tools 35+. The debug
build uses application ID `com.nexora.app.debug`, so it installs **alongside** the release
build rather than replacing it.

## Verify the download (optional)

```bash
sha256sum app-release.apk
# expect: b2fdd46473b2d32dca3db8ce3814ada7c07140315a21c5478ff7dbbb6c658ec9
```

Compare against `SHA256SUMS.txt` on the release page. On Windows use
`certutil -hashfile app-release.apk SHA256`.

## First launch — what to grant

NEXORA asks for three things, and degrades gracefully if you refuse:

| Permission | Why | If you decline |
|---|---|---|
| **Notifications** (Android 13+) | Alarms and reminders cannot ring without it | Alarms fire silently — the Alarm Health Centre flags this |
| **Alarms & reminders** / exact alarms (Android 12+) | Fires at exactly the minute you set | Alarm is inexact and may be a few minutes late |
| **Calendar (read-only)** | Shows your agenda and detects conflicts | Everything else still works; agenda stays empty |

Open **Alarms → Alarm Health Centre** at any time to see exactly which of these are missing.
That screen reads live system state — it will not show a green tick for a permission you
have not granted.

## Signed with a debug key

The APK is signed with the Android **debug** keystore, not a production key. Consequences:

- It installs normally and runs normally.
- It is **not** Play-Store distributable, and it will not receive Play updates.
- Reinstalling a build signed by a *different* key requires uninstalling first
  (`adb uninstall com.nexora.app`), which erases app data.

To produce a properly signed build, supply your own keystore per `README.md` §1 — no
credentials are committed to the repository.

## Uninstall

Normal app removal, or:

```bash
adb uninstall com.nexora.app          # release build
adb uninstall com.nexora.app.debug    # debug build
```

## Read this before relying on it as your alarm clock

The alarm engine's arithmetic and state machine are covered by 79 passing unit tests, and
the APK's permissions and components were verified with `aapt2`. But the app has **never
been run on a physical phone or emulator** — no device was available in the build
environment. Aggressive battery management on Realme UI in particular is untested.

**Use it alongside your existing alarm for the first week.** Details in
`docs/LIMITATIONS.md`.
