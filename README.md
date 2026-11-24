# Focus

An Android home screen with two things on it: **Phone** and **Messages**.

There is no app drawer, search, widgets, wallpaper, settings screen, or long-press menu, and none of those are hidden somewhere either. The launcher never lists installed apps. Android's package-visibility rules let it see only the apps that handle dialing or SMS (see `<queries>` in the manifest), so every other app is out of its reach.

- **Phone** opens your default dialer (recents and contacts), or a dial pad if there is none.
- **Messages** opens your default SMS app.

Back does nothing, and the screen is plain black.

## Build and install

You need JDK 17+ and the Android SDK (`ANDROID_HOME`, or `sdk.dir` in `local.properties`).

```sh
./gradlew installDebug          # with a phone connected over adb
# or
./gradlew assembleDebug         # APK at app/build/outputs/apk/debug/
```

Open Focus once from your current launcher. Android then asks whether to make it your home app. You can also set it later under **Settings → Apps → Default apps → Home app**.

To go back, choose your old launcher on that same Settings screen. Focus has no way to reach Settings, so open it from the notification shade's gear icon.

## CI

`.github/workflows/android.yml` runs on every push and pull request to `main`. It has three jobs:

- **Lint & Build** runs lint and builds the debug APK. The APK is saved as the `app-debug` artifact.
- **Emulator Smoke Test** runs `.github/scripts/smoke-test.sh` on an API 34 emulator. It installs the APK and sets it as the home app. Then it checks that Home lands on it, that Back stays there, and that Phone and Messages open the default dialer and SMS app. Screenshots are saved as the `smoke-test` artifact.
- **Build Release APK** runs on pushes to `main` and saves the `app-release` artifact. The APK is signed only when these repository secrets exist: `KEYSTORE_BASE64` (a base64-encoded PKCS12 keystore), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`.

Each CI run signs the debug APK with a new throwaway key, so a newer debug build won't install over an older one without uninstalling first. For an APK you can keep updating in place, use the signed release build.

## What a launcher can't remove

A home app only controls the home screen. These parts belong to the system and stay:

- the notification shade and quick settings, including the gear icon that opens Settings
- notifications from other apps
- the recents/overview screen
- the assistant gesture

To truly lock a phone to two apps, use lock task mode with this app as device owner, or uninstall and disable the other apps.
