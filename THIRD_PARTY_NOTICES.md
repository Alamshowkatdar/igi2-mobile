# Third-party notices

IGI 2 Mobile embeds the OpenIGI-based I.G.I. 2 Android engine by rayishere0 and
runs it on top of several open-source runtime components. This file is a
summary, not a replacement for the licences shipped by those projects.

## Embedded engine

- **Project:** [rayishere0/IGI-2-Covert-Strike-Android](https://github.com/rayishere0/IGI-2-Covert-Strike-Android)
- **Licence:** GPL-3.0-only (declared by the project's build metadata)
- **Contents used:** game activity, JNI bridge, managed engine assemblies,
  engine UI assets and CoreCLR hosting code.
- **Also based on:** the [OpenIGI](https://openigi.com) project.

The engine is merged into this app's single APK. Its Java package is kept as
`com.projectigi2.app` so the JNI symbols in `libprojectigi.so` continue to
resolve. The corresponding engine source is available at the project URL
above. This app is distributed under GPL-3.0-only as a whole.

## Runtime and libraries

| Component | Licence |
|---|---|
| .NET / CoreCLR runtime, CoreFX native libraries | MIT |
| OpenAL Soft (`libopenal.so`) | LGPL-2.0-or-later |
| Silk.NET (Vulkan, OpenAL bindings) | MIT |
| SkiaSharp | MIT |
| StbTrueTypeSharp | MIT / public domain |
| BCnEncoder.Net | MIT |
| AndroidX, Jetpack Compose, Material 3 | Apache-2.0 |
| OkHttp | Apache-2.0 |
| Kotlin, kotlinx.coroutines | Apache-2.0 |

The native libraries are redistributed in their original form. Source and
licence texts for each component are available from the respective upstream
projects.

## Mission briefings

The briefing text is quoted from [playigi.com](https://playigi.com) for
reference. It contains no game files. All mission names, objectives and game
content remain the property of their rights holders.

## Original game

**Project I.G.I. 2: Covert Strike** (2003), Innerloop Studios, published by
Codemasters. This project is unofficial and is not endorsed by or affiliated
with the rights holders.
