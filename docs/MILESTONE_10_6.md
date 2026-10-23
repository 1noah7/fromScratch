# Milestone 10.6: Windows installer

**Status: native setup EXE built; 14 isolated native packaging checks passed. Actual wizard appearance, normal client launch and authentication await owner acceptance.** No gameplay change, save migration, commit, push, tag, publication or distribution is authorized. v0.10.5 artifacts remain intact.

## Implementation

- `scripts/Build-NativeInstaller.ps1` reuses the existing beta stage through the optional version/stage-only arguments in `scripts/Build-Beta.ps1` and `gradle/beta.gradle`. Production plugin and `src/beta` bootstrap code are unchanged. Release label: **v0.10.6-friends-installer**; internal package version: **0.10.6-beta.1**.
- `beta/windows-native/Launcher.cs` is a small BSD-2-Clause Windows GUI PE wrapper, compiled with the Microsoft .NET Framework compiler supplied by Windows. It uses Windows 10/11's built-in .NET Framework 4.x. No .NET SDK or third-party launcher binary is required by players. `jpackage` was considered, but a wrapper is still needed for pre-JVM file verification, bounded local diagnostics and friendly missing-runtime failures. The existing Java 11 bootstrap/classpath remains unchanged.
- The launcher selects only the installed private `runtime/bin/javaw.exe`, with assertions, quoted `lib/*` and `com.fromscratch.beta.BetaLauncher`. It never searches PATH/JAVA_HOME. Java option-injection variables are removed only from the child process; no system/user environment is modified. Normal startup creates no console. Native file paths are based on the EXE's folder, not the caller's working directory.
- File checks use bounded manifests, path containment, no reparse points, exact hashes and rejection of unlisted JARs. Failures show a Windows error dialog and private local logs. Logs are bounded to roughly 2 MB per launch and five retained launcher logs. `--verify-only` is a maintainer mode that uses an isolated home/log folder beneath the disposable app copy, runs the packaged bootstrap's help only and never signs in or starts the game.
- `beta/windows-native/FromScratch.iss` defines a conventional Inno Setup wizard, stable application ID, per-user path, native desktop/Start Menu shortcuts, Installed apps registration and uninstaller. It requires no admin rights. Setup refuses unrelated same-name shortcuts and protected/unexpected installation paths. It does not forcibly close RuneLite. Owned version payloads are immutable; a new root selector points at the new version after an update. Inno retains its owned-file uninstall history; no recursive user-data deletion is configured.
- The public wizard has no test options. An explicit marker-guarded `/TestRoot` mode redirects the app directory, shortcuts and uninstall registration together for isolated automated tests. It never uses the real desktop, progression or production uninstall identity.
- The original `icon.png` is converted mechanically to a PNG-backed ICO at **16, 32, 48, 64 and 256 pixels** using nearest-neighbor pixel-art scaling. `scripts/Prepare-WindowsIcon.ps1` reproduces it. The same asset is embedded in launcher and setup/uninstaller and referenced by shortcuts. No new brand artwork is introduced.

## Pinned runtime and tools

`beta/windows-native/toolchain.json` pins the official Eclipse Temurin **Windows x64 JRE 11.0.32.1+1** archive. SHA-256:

```text
f8c7da672f5dba36b6f870608820b6b598cfae91296929f1b8f21ef2f1e8a0dd
```

The downloaded archive matched both the Adoptium release API and its official checksum sidecar before execution/extraction. All runtime legal files, release metadata and notices are preserved. Native notices link the exact corresponding source archive. No Java installer runs on the player's computer.

Developer compiler: official **Inno Setup 6.7.3**, [download](https://jrsoftware.org/isdl.php), SHA-256 `9c73c3bae7ed48d44112a0f48e66742c00090bdb5bef71d9d3c056c66e97b732`. The downloaded developer installer has a valid **Pyrsys B.V.** Authenticode signature. Following explicit owner approval, it was installed without elevation under `build/native-installer/tools/inno`. The build script never installs developer tools automatically. The installer source verifies the real compiler version through ISPP because the official compiler's Windows PE version fields report `0.0.0.0`. Existing installed compilers may be passed with `-InnoCompiler`.

Developer prerequisites: Windows x64, the existing Java 11/Gradle build environment, the Windows .NET Framework compiler and approved Inno Setup 6.7.3. Build:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Build-NativeInstaller.ps1 -InnoCompiler "path\to\ISCC.exe"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-NativeInstaller.ps1
```

`-ImageOnly` prepares/tests the launcher and runtime without the installer compiler. The build verifies pinned downloads, stages allowlisted artifacts, compiles the branded GUI EXE, builds per-file manifests and source/tool hashes, compiles setup and writes a SHA-256 sidecar. No installer is published or installed by the build command. The generated metadata discloses uncommitted packaging inputs rather than inventing a future commit. The workflow is reproducible from the recorded inputs; compiler timestamps and unsigned Inno output do not promise byte-identical EXEs across machines.

Output: `build/distributions/v0.10.6-friends-installer/FromScratch-Beta-Setup.exe`, its `.sha256`, `installer-build.json` and `installer-files.json`. Setup SHA-256: `f7a590b75578f6205717b58dd09902cf3b62a90a6edba09bb603365b78d9d95d`. Source revision: `7f4ea8d0b4584062e6b38c274f781b619559ef66`, with uncommitted packaging changes disclosed and hashed in the payload. The compiler itself is not installed on the player's computer. The old v0.10.5 ZIP/checksum are not modified.

## Authentication and privacy

The [current official RuneLite guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) describes the developer-mode Jagex Accounts panel, user-operated account login and character selection. The pinned client contains that panel. There is no token extraction, credential export, insecure credential flag, custom login or authentication-internals change. This verifies a supported route exists; actual authentication must still be demonstrated by the owner before friend distribution.

The ordinary user's RuneLite home and local From Scratch data remain unchanged. Program files: `%LOCALAPPDATA%\Programs\FromScratchBeta`; progression: `%USERPROFILE%\.runelite\plugin-data\from_scratch`; launcher logs: `%LOCALAPPDATA%\FromScratchBeta\logs`. Uninstall preserves progression, exports, credentials, RuneLite settings, other plugins, unrelated Java and unknown files. Logs may contain private local/client context and are not uploaded automatically.

The new setup and launcher are **unsigned**. `AppPublisher` is the project's declared author, not an authenticated Windows publisher. SmartScreen/Defender remain enabled. Code signing can be reviewed separately before broad distribution.

## Investigation of the old double-click problem

The v0.10.5 CMD wrapper already uses `%~dp0` and an absolute PowerShell path, so merely starting in Downloads is not established as the failure. It requires system Java discovered through JAVA_HOME/PATH/vendor folders, and a preopened terminal can inherit a different environment. No original manual failure trace was provided, so a single exact prior exception is **unconfirmed**. M10.6 removes that dependency rather than claiming an unobserved diagnosis. The installer and EXE use absolute application-relative paths and a private runtime.

## Validation and acceptance

No full gameplay suite was run. **Fourteen native packaging checks passed**: script syntax; all payload hashes/source/privacy checks; five ICO sizes; GUI PE/embedded icon; pinned runtime/legal files; actual wrapper/help startup with invalid JAVA_HOME, no PATH Java, hostile inherited Java options and spaced paths; tampered JAR; missing runtime; unexpected JAR rejection; setup checksum and matching embedded icon; actual isolated installation/upgrade with every installed file hash verified; native shortcut targets/icons and Installed apps registration; reinstallation/private-runtime startup; actual Windows uninstallation with preserved settings/progression/backups/unrelated files and changed shortcuts. The setup log confirms **User privileges: None**, **Administrative install mode: No** and **HKEY_CURRENT_USER** registration. The predecessor-version installer was a disposable fixture using this candidate's payload, not a modification of the v0.10.5 release. **Ten existing script-beta tests also passed** for the reused stage. Incremental Java/beta compilation succeeded; no gameplay code changed.

The image contains **398 files**. Exact image paths, per-file hashes and build metadata are under `build/native-installer/latest-build.json` and `build/distributions/v0.10.6-friends-installer/installer-files.json`. Results are in `build/native-installer/test-results.json`; the test fixture retains its setup log. Tests use marker-guarded workspace folders and a separate uninstall identity, never real user profiles. Neither actual graphical launch nor login has been tested.

The prior `v0.10.5-friends-beta` ZIP is unchanged, SHA-256 `427fe1050484557f59b3ffdf2eeffb43380e3cc1dac798e0aa25dbf94f1365d9`. `src/main` and `src/beta` remain unchanged. Restricted execution initially prevented a Windows framework assembly from initializing its cryptographic provider; normal Windows verification outside that sandbox worked. A real JSON list-type bug found during development was corrected before acceptance testing. No security setting was disabled.

The owner approved and the agent executed only the verified Inno Setup developer installer and the resulting isolated installer tests. No additional software or administrator privileges were needed. A test initially expected a literal uninstall registry key; Inno shortens long AppIds with a hash. The test now validates the exact isolated installation path instead, and both the initial fixture and successful fixture were uninstalled. Generated tools, runtimes, executables and logs remain ignored under `build/`.

Owner tests:

1. Download/copy setup into Downloads on a Windows 10/11 x64 PC with no configured Java. Double-click from Explorer. Inspect welcome text, icon and absence of admin/terminal requirements.
2. Finish installation, use both desktop and Start Menu entries, and confirm the branded EXE opens without a command window. Check Windows Installed apps.
3. Complete the official Jagex login yourself, enable From Scratch, resume or start a disposable challenge and confirm the accepted gameplay. Do not share tokens or use credential workarounds.
4. Close the app and reinstall/update. Confirm progress remains. Export privately before any real update.
5. On a disposable installation, rename a required runtime/library file, open normally and check the friendly dialog/log; restore it afterward.
6. Uninstall through Windows Settings. Confirm owned program files/shortcuts disappear while saves, exports, credentials, settings, other plugins and unrelated files remain.
7. Review unsigned publisher warnings honestly. Do not disable security protections. Return one acceptance or correction list; no friend distribution or publication occurs automatically.
