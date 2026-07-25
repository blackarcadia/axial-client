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
- Build DMG with bundled runtime: `./gradlew clean jpackageMac -PappVersion=1.0.0`
- Generate macOS uninstaller script: `./gradlew macUninstaller` (creates `build/jpackage/uninstall-axiallauncher.command`; run it to remove `/Applications/AxialLauncher.app` and its logs)

### Notes
- Authentication is offline by default (access token `0`, UUID derived from player name). Use a legitimate account if required by replacing the auth tokens in `LaunchRequest`.
- Natives and libraries are resolved for your current OS/architecture at runtime.
- Asset and library downloads are cached; re-runs skip existing files.
