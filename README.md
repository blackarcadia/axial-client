## Axial Minecraft Launcher (Java)

Offline-friendly console launcher that installs and starts Minecraft Java 1.21.11 using Mojang’s official metadata endpoints.

### Prerequisites
- Java 21+ on PATH (uses the running JDK)
- Network access for first install
- Disk space for game assets (~1.5 GB)

### Run
```bash
./gradlew run
```
The launcher installs to `~/.minecraft` and starts the game immediately. Override defaults by editing `Main.java` or constructing a `LaunchRequest` with a custom game directory or player name.

### macOS packages
- Build fat jar: `./gradlew clean fatJar --no-build-cache`
- Build macOS app with bundled runtime: `./gradlew clean jpackageMac -PappVersion=1.0.0`
- Generate macOS uninstaller script: `./gradlew macUninstaller` (creates `build/jpackage/uninstall-axiallauncher.command`; run it to remove `/Applications/AxialLauncher.app` and its logs)

### Windows packages
- Build Windows app with bundled runtime on Windows: `gradlew.bat clean jpackageWindows -PappVersion=1.0.0`
- Release builds publish `AxialLauncher-windows.zip`; extract it, then open `AxialLauncher/AxialLauncher.exe`

### Release downloads
- macOS: download `AxialLauncher.app.zip`
- Windows: download `AxialLauncher-windows.zip`

### Notes
- Authentication is offline by default (access token `0`, UUID derived from player name). Use a legitimate account if required by replacing the auth tokens in `LaunchRequest`.
- Natives and libraries are resolved for your current OS/architecture at runtime.
- Asset and library downloads are cached; re-runs skip existing files.
