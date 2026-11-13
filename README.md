# IGI 2 Mobile

A native Android launcher and companion for **Project I.G.I. 2: Covert Strike**.

The app detects and starts the community native Android port of the game, walks
you through importing your own PC game folder, keeps all 19 mission briefings
available offline, and is ready to play playigi.com's browser port full screen
as soon as that port ships.

This is an unofficial fan project. It bundles **no game data** and is not
affiliated with Innerloop Studios, Codemasters, playigi.com, OpenIGI or the
native port's author.

---

## Current status

There is no official Android build of I.G.I. 2, and no official browser port
yet. The situation today:

| Way to play | Status | What this app does |
|---|---|---|
| Community native port (`rayishere0/IGI-2-Covert-Strike-Android`, package `com.projectigi2.app`) | **Playable.** ARM64 only, requires your own `pc/` folder | Detects it, launches it, links to its releases |
| playigi.com browser port | **Not published.** The site says "coming soon" | Probes for it, opens it in a full-screen player when live |
| This app alone | Launcher / briefing reader | No game engine and no game assets |

The browser port probe checks a short list of candidate URLs and only reports
**LIVE** when the response really is the sequel's page. Until then the app is
honest about the status instead of showing a dead PLAY button. If the port
appears at a different URL, paste it into **Settings → Browser port URL** and
the app will use it without an update.

---

## Features

- **One PLAY button** that resolves to the best available option: native port
  first, then the browser port, then the setup guide.
- **Native port launcher** with package detection, version display, app-info
  shortcut and release-page link. The other app is never bundled or copied.
- **All 19 mission briefings** from all three campaigns, stored as plain Kotlin
  data so they work offline.
- **Full-screen browser player** for playigi.com: landscape, immersive, screen
  kept on, zoom disabled, camera permission for co-op QR pairing, and back
  sends Escape to the page before leaving.
- **Setup guide** covering the native port, the expected game-folder layout and
  the folder picker inside the port.
- **Settings** for port preference (Auto / Native / Browser), a custom browser
  URL, touch controls, Wi-Fi-only preference and cache clearing.
- **First-run disclaimer** and an About screen with credits and legal notes.

No analytics, no ads, no accounts, no background services.

---

## Requirements

**Phone**

- Android 8.0 (API 26) or newer
- ARM64 device for the native port (the launcher itself runs on any ABI)
- Your own legally owned copy of the PC game for the native port
- Internet only for the browser port or for fetching the native port

**Build machine**

- JDK 17
- Android SDK Platform 34 with Build-Tools 34.0.0
- Gradle 8.9 via the included wrapper

> The checked-in build uses `compileSdk = 34` / `targetSdk = 34` because that is
> the platform installed on the development machine. If you have Platform 35,
> you can bump both values in `app/build.gradle.kts`; nothing in the app needs
> API 35.

---

## Playing with the native port

1. Install the community port from
   [its releases page](https://github.com/rayishere0/IGI-2-Covert-Strike-Android/releases/latest).
2. Copy the `pc` folder from your I.G.I. 2 installation to the phone, for
   example to `/sdcard/Download/IGI 2/pc`.
3. Open the port, choose **SELECT GAME FOLDER**, and pick that `pc` folder.
4. The port verifies the structure and enables **PLAY**.
5. Open IGI 2 Mobile and press **PLAY I.G.I. 2** — it launches the port.

Expected folder contents include `igi2.exe`, `config.qvm`, `common/`,
`missions/`, `menusystem/` and `language/`. This launcher never reads, copies or
modifies those files; it only starts the other app.

---

## Building

```bash
# Point Gradle at your SDK (local.properties is git-ignored).
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew :app:assembleDebug     # debug APK
./gradlew :app:assembleRelease   # unsigned release APK
```

Output:

- `app/build/outputs/apk/debug/app-debug.apk` (`com.igi2.mobile.debug`)
- `app/build/outputs/apk/release/app-release.apk` (`com.igi2.mobile`)

The debug APK is about 9 MB. Install with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release signing

Release signing is optional and read from `keystore.properties` at the project
root:

```properties
storeFile=keystore/igi2-release.jks
storePassword=change-me
keyAlias=igi2
keyPassword=change-me
```

```bash
keytool -genkeypair -v -keystore keystore/igi2-release.jks -storetype PKCS12 \
  -alias igi2 -keyalg RSA -keysize 4096 -validity 10000
```

`keystore.properties`, `keystore/` and `*.jks` are git-ignored. Back the key up:
losing it means you cannot update the app under the same package name.

---

## How it works

```
MainActivity (Compose)
  Home / Missions / Setup / Settings / About
        │
        ▼
LauncherRepository (StateFlow + SharedPreferences)
        │
        ├─ NativePort ─ package lookup, launch, release page, app info
        ├─ WebPort    ─ candidate probe, touch flag, URL normalisation
        └─ Missions   ─ 19 briefings, 3 campaigns
        │
        ▼
WebPortActivity (full-screen landscape WebView)
```

- **Play resolution** is in `LauncherRepository.playTarget()`. `AUTO` prefers
  the native port, then a live browser port, then setup. The user can force a
  choice in Settings.
- **Native detection** uses `PackageManager` with `<queries>` entries in the
  manifest so Android 11+ package visibility does not hide the port.
- **Browser probe** runs on `Dispatchers.IO` with OkHttp, follows redirects and
  checks both the HTTP status and page markers so the I.G.I. 1 page cannot be
  mistaken for the sequel.
- **The player** is a plain `WebView` activity, not Compose, to keep the game
  surface simple and full screen. Cookies and DOM storage are enabled because
  the port uses both.
- **State** is a single `StateFlow<AppState>`. Preferences survive restarts;
  port status is re-checked when the activity resumes.

### Adding the real browser port URL

When playigi.com publishes the sequel's build, either:

1. Add the URL to `WebPort.CANDIDATES` in
   `app/src/main/java/com/igi2/mobile/data/Ports.kt`, or
2. Paste it into **Settings → Browser port URL** and press **CHECK**.

A hand-entered URL is trusted after a successful HTTP response; the built-in
candidates must also contain a sequel marker.

---

## Project layout

```
app/src/main/java/com/igi2/mobile/
├── MainActivity.kt              Compose host, navigation, play resolution
├── Igi2Application.kt           Application entry point
├── data/
│   ├── LauncherRepository.kt    AppState, preferences, play target
│   ├── Ports.kt                 Native port detection and browser probe
│   ├── Missions.kt              19 mission briefings
│   └── Net.kt                   OkHttp client, user agent, connectivity
├── ui/
│   ├── Screens.kt               Home, missions, setup, settings, about
│   ├── Components.kt            Terminal-style shared components
│   └── Theme.kt                 Near-black / amber / cyan theme
└── web/
    └── WebPortActivity.kt       Immersive full-screen browser player
```

---

## Privacy

The app collects nothing and sends nothing except the browser-port probe
(a normal HTTPS request to playigi.com or the URL you entered) and whatever the
browser port itself does while you play it. Preferences stay in the app's
private storage.

---

## Credits

- **Original game:** Project I.G.I. 2: Covert Strike, Innerloop Studios,
  published by Codemasters (2003). All game content remains with its rights
  holders.
- **Mission briefings:** [playigi.com](https://playigi.com), quoted as published.
- **Native Android port:** [rayishere0/IGI-2-Covert-Strike-Android](https://github.com/rayishere0/IGI-2-Covert-Strike-Android).
- **Engine research:** the [OpenIGI](https://openigi.com) project.

---

## Legal

This project is an unofficial companion. It ships no game assets, no game
engine and no APK of the native port. It does not grant you any rights to the
game. You must own a legitimate copy of Project I.G.I. 2: Covert Strike to use
the native port, and you must respect the licences of every third-party project
involved.

Do not redistribute game data or the native port's APK with this app.
