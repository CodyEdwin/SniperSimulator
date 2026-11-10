# DAGGER RIDGE

A first-person 3D long-range sniper simulation for desktop and Android, written in Java 11 with libGDX 1.14.2 and built with Gradle. Every shot is integrated through a full external-ballistics model: G7 drag, Mach-dependent drag rise, station atmosphere and humidity, wind shear and gusts, Coriolis, gyroscopic spin drift, Magnus, aerodynamic jump and terrain collision. Seven service sniper rifles, from the M110 to the Barrett M107, each with its own trajectory, recoil and optic. Ammunition is unlimited.

There is no imported art or audio. Terrain, targets, rifle, reticles, sky and sound effects are generated procedurally at runtime, so the repository stays small and every visual can be tuned from code.

## Requirements

- JDK 11 or newer for the desktop client and tests (bytecode target is Java 11)
- JDK 17 or newer to build Android or a self-contained desktop image (AGP and jpackage)
- A GPU with OpenGL 3.2+ (desktop LWJGL3 backend) or OpenGL ES 2.0+ (Android)
- Linux, Windows or macOS; Android builds additionally need the Android SDK (compile SDK 36)

The Gradle wrapper is included, so a system Gradle installation is not required. `run.sh` locates a suitable JDK and the Android SDK and exports `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT` and `PATH`.

## Build and run

The helper script sets up the toolchain and picks the right Gradle task:

```bash
./run.sh              # desktop client
./run.sh build        # core + desktop + debug APK
./run.sh test         # JUnit physics tests
./run.sh android      # debug APK
./run.sh install      # install APK on a connected adb device
./run.sh package      # self-contained desktop app image zip
./run.sh fatjar       # single executable DaggerRidge-1.0.6.jar
./run.sh smoke        # headless render/ballistics smoke test
```

Pass extra Gradle arguments after the command, for example `./run.sh client --args="--width=1280 --height=720"`.

Equivalent raw Gradle commands:

```bash
./gradlew :lwjgl3:run
./gradlew :core:test
./gradlew :android:assembleDebug
./gradlew :lwjgl3:desktopZip
./gradlew :lwjgl3:shadowJar
```

For the Android build, install the SDK command-line tools and set `ANDROID_HOME` (or leave the SDK in a standard location such as `~/Android/Sdk`):

```bash
sdkmanager --install "platform-tools" "platforms;android-36" "build-tools;35.0.0"
./run.sh android
```

`run.sh` writes an ignored `local.properties` with the detected SDK path. The APK is written to `android/build/outputs/apk/debug/android-debug.apk`.

The launcher accepts `--width=`, `--height=` and `--smoke-test`. The smoke test disables audio and MSAA, aims at the 200 m silhouette, fires a shot, renders 90 frames and logs `SMOKE_TEST_OK` with the OpenGL error code and hit result.

## Controls

| Input | Action |
| --- | --- |
| `Enter` | Leave the briefing / start shooting |
| Mouse | Aim |
| Right mouse (hold) | Look through the riflescope |
| Left mouse | Fire |
| Mouse wheel | Adjust magnification while scoped |
| `↑` / `↓` | Elevation turret (+/- 0.1 mil per click) |
| `←` / `→` | Windage turret (+/- 0.1 mil per click) |
| `[` / `]` | Holdover -/+ 1 mil (reticle marks, no turret dialing) |
| `Page Up` / `Page Down` | Holdover +/ -5 mil |
| `\` | Clear holdover |
| `Z` | Cycle zero distance (100-500 m, 100 m steps) |
| `R` | Note: ammunition is unlimited |
| `1`-`7` | Select weapon |
| `Shift` (hold) | Hold breath (steadies the reticle, then runs out) |
| `Tab` (hold) | DOPE table for the current weapon and conditions |
| `L` | Laser rangefinder |
| `V` | Bullet camera: follow a round in flight, or arm it for the next shot |
| `F5` | Reroll wind and recompute the zero |
| `H` | Help overlay |
| `Esc` | Pause / release the mouse |

## What is simulated

### External ballistics

`BallisticsEngine` integrates a point-mass projectile with fixed 1 ms classical RK4 steps:

- **G7 drag curve** interpolated by Mach number, converted to an actual drag coefficient through the bullet's sectional density and G7 BC.
- **Transonic drag rise** from the G7 curve and a stability penalty below a Miller stability factor of 1.0.
- **Station atmosphere**: pressure, temperature and relative humidity produce density, speed of sound, density altitude and pressure altitude. The default range is a high-desert station at 1,850 m.
- **3D wind**: meteorological "from" bearing, direction wobble, gust harmonics, and a logarithmic-style shear profile measured in metres above the terrain under the bullet. The wind drags against airspeed, not ground speed, so headwinds and tailwinds change time of flight as well as point of impact.
- **Coriolis**: full `-2 Ω × v` acceleration using the station latitude; in the northern hemisphere it pushes right and slightly down.
- **Gyroscopic spin drift**: right-hand twist drift proportional to the Miller stability factor and time of flight.
- **Magnus force** from crosswind acting on the spinning projectile.
- **Aerodynamic jump** from the vertical wind component.
- **Terrain collision** against the same analytic heightfield used for rendering.

`Trajectory` stores the path at 1 kHz so the visual tracer, hit tests, bullet camera and DOPE interpolation all read the same samples.

### Zeroing, DOPE and ranging

- `ZeroSolver` uses a secant iteration to find the barrel elevation that puts the bullet on the line of sight at the selected zero distance, accounting for sight height and aerodynamic jump/drop.
- `BallisticTable` builds the data-of-prior-engagement table: drop, drift, time of flight, retained velocity, energy, Mach and the elevation/windage corrections in 0.1 mil clicks at every 100 m.
- The laser rangefinder first intersects the aim ray with target bounding boxes, then marches the ray against the terrain heightfield.
- Scope turrets are in 0.1 mil clicks. The scope view shows really useful data only when those numbers are accurate, so they come from the same solver that fires the bullet.
- **Holdover aiming**: `[`/`]` and `Page Up`/`Page Down` add a positive ballistic holdover in mils without touching the elevation turret. The shot direction is raised by that angle, exactly like placing a Christmas-tree mark below the target; the selected mark is highlighted amber in the scope. This is independent of the turret setting and is cleared by `\`, a zero change or a weapon change. Use it for fast holds from the DOPE card.

### Targets and scoring

Eleven steel and silhouette targets from 200 m to 2,000 m, including two movers. Hits are resolved by segment/AABB intersection against the target's animated pose, then scored by ring: bullseye, inner, outer, or edge. The HUD keeps score, hits, first-round accuracy, best ring, longest hit and best retained energy. Misses show the nearest target's horizontal and vertical error in MOA.

### Presentation

- Procedural heightfield with a firing ridge, valley, far slope and side spurs; chunked meshes keep every index buffer under the 16-bit limit.
- Procedural sky dome with a sun, haze and drifting cloud bands.
- Procedural rifle view model with receiver, barrel, suppressor, optic, bolt, magazine, cheek rest and bipod.
- Animated wind flags, foliage, rocks and sandbags.
- Reticles drawn in code: duplex, mil-dot and Christmas-tree holdover patterns.
- Ballistic HUD with scope data, environmental panel, wind call, breath bar, ammo, DOPE, hit markers, impact toasts, bullet-camera telemetry and a compass.
- Procedurally synthesized audio for each chambering, steel hits, dirt impacts, bolt cycling, reloads, empty clicks, turret clicks and wind. WAVs are generated into `assets/generated-audio/` on first launch (that folder is git-ignored).

The UI uses **JetBrains Mono Nerd Font** (`assets/fonts/JetBrainsMonoNerdFont-Regular.ttf` and `-Bold.ttf`), bundled under the SIL Open Font License in `assets/fonts/OFL.txt`.

## Weapons

| # | Rifle | Cartridge | Bullet | MV | Optic | Action | Mag |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | M110 SASS | 7.62×51 | 175 gr M118LR | 784 m/s | Leupold 3.5–10× | Semi | 20 |
| 2 | M24 SWS | 7.62×51 | 175 gr M118LR | 790 m/s | Leupold 10× fixed | Bolt | 5 |
| 3 | L96A1 | 7.62×51 | 168 gr SMK | 850 m/s | S&B 3–12× (6×) | Bolt | 10 |
| 4 | L115A3 | .338 Lapua | 250 gr B408 | 936 m/s | S&B 5–25× | Bolt | 5 |
| 5 | CheyTac M200 | .408 CheyTac | 419 gr | 838 m/s | Nightforce 7–35× | Bolt | 7 |
| 6 | McMillan TAC-50 | .50 BMG | 750 gr A-MAX | 823 m/s | S&B 5–25× | Bolt | 5 |
| 7 | Barrett M107 | .50 BMG | 661 gr M33 | 853 m/s | Leupold 4.5–14× | Semi | 10 |

Ammunition is unlimited. Magazine size, bolt or semi-auto cycle, recoil, damage and penetration are still per rifle. Specs and sources live in `WeaponCatalog`.

## Desktop packaging

`./run.sh package` builds a self-contained native app image with `jpackage` for the current OS, including a private Java runtime, and zips it:

```
lwjgl3/build/distributions/DaggerRidge-1.0.6-linux-x64.zip
lwjgl3/build/distributions/DaggerRidge-1.0.6-windows-x64.zip
lwjgl3/build/distributions/DaggerRidge-1.0.6-macos-arm64.zip
```

Extract the archive and run `DaggerRidge/bin/DaggerRidge` (`.exe` on Windows, `.app` launcher on macOS). No installed Java is required. `./run.sh fatjar` builds `lwjgl3/build/libs/DaggerRidge-1.0.6.jar`, a cross-platform fat jar that needs Java 11+ and runs with `java -jar DaggerRidge-1.0.6.jar`. The optional `./gradlew :lwjgl3:desktopInstaller` task builds a `.deb`, `.msi` or `.dmg` using the host platform's jpackage tooling. The GitHub Actions workflow builds the app-image zip on Linux, Windows and macOS runners and the debug APK on Linux.

## Project layout

```
settings.gradle
build.gradle                 shared Java 11 / JUnit configuration, Android plugin
gradle.properties
run.sh                       toolchain detection and build/run entry point
core/
  src/main/java/com/snipersim/
    SniperGame.java          application listener and screen hand-off
    screens/GameScreen.java  loop, input, zeroing, shots, scoring, bullet cam
    ballistics/              atmosphere, drag, wind, engine, trajectory,
                             zero solver, DOPE table, terrain interface
    weapons/                 weapon and bullet specs, reticles, catalog
    world/                   terrain, targets, flags, shots, world assembly
    render/                  scene, scope shader, HUD, rifle, effects, models
    audio/ProceduralAudio.java
    util/                    ballistic units and value noise
  src/test/java/com/snipersim/ballistics/
lwjgl3/
  src/main/java/com/snipersim/lwjgl3/Lwjgl3Launcher.java
android/
  src/main/AndroidManifest.xml
  src/main/java/com/snipersim/android/AndroidLauncher.java
assets/
  fonts/                     JetBrains Mono Nerd Font (OFL)
.github/workflows/build.yml  desktop matrix + Android APK CI
```

## Tests

```bash
./gradlew :core:test
```

Fourteen JUnit 5 tests cover the atmospheric model, drag/drop behaviour, wind drift sign, Coriolis and spin drift direction, terrain impacts, energy retention, zero solving, DOPE monotonicity, every chambering's DOPE table and rimfire-versus-magnum drop.

## Limitations

This is a simulation game, not a validated ballistic solver. The projectile is a point mass with empirical gyroscopic terms rather than a full rigid-body 6-DOF model; the wind field is deterministic and horizontally uniform; target movement is scripted; and scope parallax, mirage refraction and barrel harmonics are approximated or omitted. The numbers are physically motivated and internally consistent, which is what makes the shooting feel right.

The Android target currently uses the same keyboard/mouse control scheme as the desktop build; it is best suited to a tablet or Chromebook with a pointing device. On-screen touch controls are not implemented yet.
