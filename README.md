# IGI 2 Mobile

**Project I.G.I. 2: Covert Strike on Android, in one app.**

IGI 2 Mobile is a native Kotlin/Compose launcher with the OpenIGI-based engine
embedded in the same APK. You install one app, point it at your own copy of the
PC game files, and play. No second APK, no separate download, no browser.

This is an unofficial fan project. It ships **no game data** and is not
affiliated with Innerloop Studios, Codemasters, playigi.com, OpenIGI or the
engine's author.

---

## Status

| Piece | Status |
|---|---|
| Launcher + mission briefings | Included |
| Vulkan renderer + .NET engine | Embedded in this APK (ARM64) |
| Game data | **Not included** — you must supply your own `pc/` folder |
| playigi.com browser port | Still "coming soon"; kept as an optional fallback |

The engine is the OpenIGI-based native port by
[rayishere0](https://github.com/rayishere0/IGI-2-Covert-Strike-Android), merged
into this app under **GPL-3.0**. Its Java package and JNI entry points are kept
unchanged so the native library continues to resolve them.

---

## Features

- **One app, one icon** — the launcher opens the embedded game activity
  directly. Returning players skip the setup screen once a game folder is
  verified.
- **Native Vulkan engine** — the port's .NET 10/CoreCLR engine, Vulkan 1.1
  renderer, OpenAL Soft audio and touch HUD, running inside this package.
- **In-app game folder import** — the engine copies your `pc/` folder into
  app-private storage and verifies the structure before enabling PLAY.
- **All 19 mission briefings** — all three campaigns, readable offline.
- **Browser fallback** — playigi.com's port can still be opened full screen
  when it ships, or by pasting its URL in Settings.
- **No analytics, ads, accounts or background services.**

---

## Requirements

- **ARM64 Android phone** (the engine and CoreCLR runtime are `arm64-v8a` only)
- Android 8.0 (API 26) or newer
- Vulkan-capable GPU, OpenGL ES 3.0
- Your own legitimate copy of **Project I.G.I. 2: Covert Strike** for PC
- About 1 GB free storage for the app plus the copied game files

The launcher UI itself is Compose and runs on other ABIs, but the built-in
engine requires ARM64.

---

## Install

Download `IGI-2-Mobile-2.0.0.apk` from the
[latest release](https://github.com/Alamshowkatdar/igi2-mobile/releases/latest)
and sideload it:

```bash
adb install IGI-2-Mobile-2.0.0.apk
```

Or install a debug build from source with
`./gradlew :app:assembleDebug`.

---

## First run

1. Copy the `pc` folder from your I.G.I. 2 installation to the phone, for
   example `/sdcard/Download/IGI 2/pc`. It must contain `igi2.exe`,
   `config.qvm`, `common/`, `missions/`, `menusystem/` and `language/`.
2. Open **IGI 2 Mobile** and press **PLAY I.G.I. 2**.
3. In the game screen, tap **SELECT GAME FOLDER** and pick that `pc` folder.
4. The engine verifies the files, copies them into app-private storage and
   enables **PLAY**. Later launches go straight in once setup is complete.
5. Pick Vulkan or WebGPU as the renderer if you want to experiment; Vulkan is
   the default.

The original PC installation is never modified. Uninstalling the app removes
the copied game files from app storage.

---

## Build

Requirements:

- JDK 17
- Android SDK Platform 34 + Build-Tools 34.0.0
- Android NDK `26.1.10909125`
- CMake `3.22.1` (installed through the SDK)
- Gradle 8.9 via the included wrapper

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Output:

- `app/build/outputs/apk/debug/app-debug.apk` (`com.igi2.mobile.debug`)
- `app/build/outputs/apk/release/app-release.apk` (`com.igi2.mobile`) — ~65 MB

The release build runs `lintVital` by default. On machines with a preview
`android-37.0` platform, AGP's lint crashes parsing that SDK metadata; skip it
with:

```bash
./gradlew :app:assembleRelease -x lintVitalRelease -x lintVitalAnalyzeRelease
```

### Release signing

`app/build.gradle.kts` reads signing details from `keystore.properties`:

```properties
storeFile=keystore/igi2-release.jks
storePassword=change-me
keyAlias=igi2
keyPassword=change-me
```

Create your own key if you do not have the project key:

```bash
keytool -genkeypair -v -keystore keystore/igi2-release.jks -storetype PKCS12 \
  -alias igi2 -keyalg RSA -keysize 4096 -validity 10000
```

`keystore.properties`, `keystore/` and `*.jks` are git-ignored. Back the key up.

---

## Architecture

```
com.igi2.mobile
├── MainActivity.kt            Compose launcher, navigation, play resolution
├── data/
│   ├── LauncherRepository.kt  StateFlow, preferences, play target
│   ├── Ports.kt               Embedded engine handle + browser-port probe
│   ├── Missions.kt            19 mission briefings
│   └── Net.kt                 OkHttp client and connectivity
├── ui/                        Home, missions, setup, settings, about
├── web/WebPortActivity.kt     Optional full-screen browser fallback
└── com.projectigi2.app/       Embedded engine activity (Java, unchanged package)
    ├── MainActivity.java      Splash, folder picker, SurfaceView, input, HUD
    └── NativeEngine.java      JNI bridge into libprojectigi.so

app/src/main/cpp/              JNI bridge + CMake build for libprojectigi.so
app/src/main/jniLibs/          CoreCLR, OpenAL, WebGPU natives (arm64-v8a)
app/src/main/assets/managed/   Engine assemblies (no game data)
app/src/main/assets/public/    Engine UI assets (no game data)
```

The Compose launcher starts the engine activity with `auto_play=true`. The
engine waits for its managed assets to extract, finds the verified game folder
and the ready `SurfaceView`, then starts the engine loop without an extra PLAY
tap. The engine's Java package is deliberately `com.projectigi2.app`: changing
it would break the JNI symbols compiled into `libprojectigi.so`.

The launcher never reads or modifies the game files; the engine activity owns
the folder picker and the copy into `getFilesDir()/Project IGI 2`.

---

## Licence and credits

This project is distributed under **GPL-3.0-only**, because it embeds the
OpenIGI-based engine, which declares the same licence. See [LICENSE](LICENSE).

- **Original game:** Project I.G.I. 2: Covert Strike, Innerloop Studios,
  published by Codemasters (2003). All game content remains with its rights
  holders.
- **Embedded engine:** [rayishere0/IGI-2-Covert-Strike-Android](https://github.com/rayishere0/IGI-2-Covert-Strike-Android),
  GPL-3.0, based on the [OpenIGI](https://openigi.com) project.
- **Mission briefings:** [playigi.com](https://playigi.com), quoted as published.
- **Runtime components:** .NET/CoreCLR (MIT), OpenAL Soft (LGPL), Vulkan,
  Silk.NET and SkiaSharp (MIT). See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

You must own a legitimate copy of the game. Do not redistribute game data.
