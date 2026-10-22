# Installation

## Windows beta status

The `0.10.0-beta.1` candidate is prepared locally for owner review; it has not been published, distributed or approved by Plugin Hub. After approval, the owner will upload the ZIP/checksum to [GitHub Releases](https://github.com/PilotsRKwl/fromScratch/releases). There is no direct public beta asset link yet.

Prerequisite: **Windows x64 and the official Temurin 11 JDK**, installed from [Adoptium](https://adoptium.net/temurin/releases/?version=11). Choose Java 11 / Windows / x64 / JDK. The installer can find Java through JAVA_HOME, PATH or the vendor's normal Program Files directory. It does not install Java, require admin rights, or require Git/Gradle/IntelliJ. Internet is required when RuneLite obtains game/client resources and signs in.

1. Download the approved ZIP and its `.sha256` sidecar. Compare `Get-FileHash -Algorithm SHA256 <zip>` with the published value. A hash detects differences; it is not a publisher signature.
2. Extract **all** ZIP contents. Do not launch from inside the ZIP.
3. Run **Install.cmd**. It checks every packaged file, installs versioned program files and creates **From Scratch Beta** on your desktop. Paths with spaces are supported.
4. Close other RuneLite clients and open that shortcut. No password or token is requested by the installer.
5. In RuneLite, open **Jagex Accounts**, click **Add Jagex Account**, complete the official login window yourself, select your character, then **Play Now**. Follow the current [official developer login guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
6. Enable **From Scratch** in the plugin list and open its purple coin sidebar. Follow Before Starting, or resume the saved run for that character.

The packaged help/classpath, official panel presence and a logged-out graphical startup using an isolated empty profile are tested. **The real desktop shortcut, authentication and actual gameplay still require owner acceptance.** If the panel or supported login fails, stop and report it; do not use credential files, tokens, environment scraping or unofficial authentication helpers. The normal RuneLite launcher does not enable developer mode, so copying this JAR into a normal installation is not the beta installation path.

## Updates and uninstall

Export Progress before updating. Extract the newer approved ZIP and run its installer again. The owned desktop shortcut points to the new immutable program version; older versions remain. Repeating the same install reuses that version. Do not use an older client against a newer save without a verified compatible backup.

To uninstall, close the beta, open its version directory under `%LOCALAPPDATA%\FromScratchBeta\versions`, and run **Uninstall.cmd**. Confirm removal. It deletes unchanged owned program files and removes the shortcut only if it still points at that version. Other versions, modified/extra files, real progression, `.fsp` exports, RuneLite settings, credentials and other plugins are preserved. Uninstall is not Reset Progress.

Unsigned beta scripts may receive Windows security prompts. Review the source and origin; never disable antivirus or obtain credentials to make them run. The wrapper uses a process-only PowerShell execution policy and does not change the system policy.

## Maintainer packaging

Use Java 11 and the existing Gradle wrapper. Run from the repository root:

```powershell
.\gradlew.bat build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Build-Beta.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-Beta.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-StandardBuild.ps1
```

Run the full regression once for the release candidate; documentation-only changes do not require repeats. Packaging itself never runs tests/installs/launches the GUI or publishes.

Outputs: `build/distributions/from-scratch-beta-0.10.0-beta.1-windows.zip`, its SHA-256 sidecar, production JAR in `build/libs`, and staged files in `build/beta/stage`. The ZIP contains a thin production plugin, a separate beta bootstrap and **unmodified** official RuneLite/runtime dependency JARs—not the old shadow JAR with test fixtures. It excludes JUnit, source test fixtures, credentials, profiles and progression. `package.json`, `DEPENDENCIES.json` and `SOURCE.json` record file hashes, Maven coordinates, Java/RuneLite version, exact baseline revision, frozen gameplay tree and uncommitted release-build inputs. A dirty-worktree candidate is explicitly identified, not passed off as a future committed release.

The official `build=standard` check uses the current pinned tooling template verbatim, supplies the Hub's client version externally and compiles only `src/main`. Beta sources/scripts are outside the Hub plugin. Full hosted packaging/scanning/review belongs to Milestone 11.

Installer tests use isolated `build/beta-tests` roots and a fake desktop. The actual Java help smoke uses an empty sandbox user home and never signs in or reads real progression. Owner acceptance must use the actual shortcut and official login flow.
