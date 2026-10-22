# Validation plan

Gameplay is frozen at owner-accepted Milestone 9.2. Current beta and documentation evidence is in [MILESTONE_10.md](../MILESTONE_10.md); older test results and milestone checklists remain in the [archive](../history/TEST_PLAN_PRE_M10.md).

## Current verified baseline

GitHub Actions [run 39868207087](https://github.com/PilotsRKwl/fromScratch/actions/runs/39868207087) at `2bb5dbd4cb7973a05b9f449be70e5adcc0101d06` passed both `windows-latest` and `ubuntu-latest`. The former stale UI assertions and font-sensitive Ubuntu assertion were corrected before this task. No frozen production source has changed since that passing run.

Documentation, screenshot and isolated preview-tool changes do not justify another full gameplay regression. Compile the preview source and execute its explicit capture task; run documentation/asset checks and the isolated beta integration script. Do not interact with RuneScape through automation or touch actual progression during tests.

## Maintainer checks

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Prepare-UiPreviews.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-Documentation.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Build-Beta.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\Test-Beta.ps1
git diff --check
```

Preview rendering uses unchanged production Swing components, explicit sample data, an isolated empty user home and official cached item sprites. It does not open the game. Visually inspect PNGs for clipping, missing text and accurate captions. Native bank overlays and gameplay require owner captures.

Beta tests use disposable paths with spaces and a fake desktop. They verify manifest/hash allowlists, script parsing, prerequisites/classpath, actual shortcut creation in the isolated directory, repeated install, update, safe uninstall, tamper rejection and an isolated launcher help smoke. They do not authenticate or validate live gameplay. Never substitute real profile paths in failure-injection tests.

`scripts/Test-StandardBuild.ps1` checks the official pinned standard template with production source only. Reuse the documented valid result when production source and standard-build inputs are unchanged. This is not Plugin Hub approval or hosted scanning.

## If production changes are later authorized

Use Java 11 and incremental `compileJava compileTestJava`. Discover and filter only affected test classes during iteration. Preserve coverage for signed deltas, immutable baselines, complete-bank observation, atomic/replayed buybacks, contents offsets, GE lifecycle/partial fills, crash recovery, character isolation, native-tab intersections and re-entrant shutdown. Run the full regression once at a changed release checkpoint, rather than after every edit. Do not weaken tests or disable either CI platform.

## Owner acceptance

Follow [BETA_TESTING.md](../BETA_TESTING.md) after the [authoritative installation guide](../INSTALLATION.md). Check the actual desktop shortcut, supported Jagex login, before-start full-bank snapshot, active Virtual Bank/native tabs, gray/bright quantities, completed GE history, buybacks and restart persistence. Use disposable progression for destructive restore/reset experiments; an ordinary uninstall must preserve real saves/settings/backups.

The owner alone operates the game. A compile, unit fixture, help response or logged-out startup is not live acceptance. Launch/authentication and actual gameplay remain pending until explicitly confirmed.
