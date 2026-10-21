# Milestone 1 review record

## Status

Foundation implementation and automated verification are ready for user review. **Manual RuneLite acceptance is pending; the milestone is not accepted yet.** No later milestone was implemented. No commit, push, publication, PR, or gameplay interaction was performed.

## Architecture decisions

- Separate lifecycle/config, validated immutable lifecycle model, and EDT-confined sidebar classes.
- Queue UI work without blocking RuneLite; register one navigation button and remove it on disable.
- Always disclose placeholder values; no start action before snapshot/persistence exist.
- Preserve Java 11, Gradle Wrapper, standard Hub build mode, and template dependencies.
- Use internal name `from_scratch` for future Filepath storage; config group remains `fromscratch`.
- Document pure multi-entry transaction accounting, pooled historical basis, per-character recoverable storage, and ambiguity handling before implementing them.

See [ARCHITECTURE.md](../ARCHITECTURE.md), [ACCOUNTING.md](../ACCOUNTING.md), and [compatibility evidence](COMPATIBILITY.md) for the full plan and future decisions.

## Changed-file inventory

Modified existing files:

- `.gitignore`, `AGENTS.md`, `README.md`
- `build.gradle`, `settings.gradle`, `runelite-plugin.properties`
- `src/test/resources/logback-test.xml`

Replaced the three template Java files with renamed implementations/launcher; the old `com/example` files appear as deletions until Git detects the replacements.

New production files:

- `src/main/java/com/fromscratch/FromScratchPlugin.java`
- `src/main/java/com/fromscratch/FromScratchConfig.java`
- `src/main/java/com/fromscratch/model/RunState.java`
- `src/main/java/com/fromscratch/model/RunStatus.java`
- `src/main/java/com/fromscratch/ui/FromScratchPanel.java`
- `src/main/java/com/fromscratch/ui/FromScratchSidebar.java`
- `icon.png`, `src/main/resources/com/fromscratch/icon.png`

New test/development files:

- `src/test/java/com/fromscratch/FromScratchPluginTest.java` (launcher)
- `src/test/java/com/fromscratch/FoundationResourcesTest.java`
- `src/test/java/com/fromscratch/model/RunStateTest.java`
- `src/test/java/com/fromscratch/ui/FromScratchSidebarTest.java`

New administration files:

- `ACCOUNTING.md`, `ARCHITECTURE.md`, `TEST_PLAN.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `LICENSE`, `NOTICE`
- `docs/ARTWORK.md`, `docs/COMPATIBILITY.md`, `docs/PUBLICATION.md`, this review record
- `.github/workflows/build.yml`
- `.github/ISSUE_TEMPLATE/bug_report.md`, `.github/ISSUE_TEMPLATE/feature_request.md`

CLAUDE.md still delegates to AGENTS.md. The Gradle Wrapper files are unchanged.

## Verification

- Requested Gradle clean build passed on Java 11 with default RuneLite resolution 1.13.3.
- Extra compatibility clean build against retrieved Hub target 1.13.2 passed.
- Each build: 27 tests, zero failures/errors/skips. Coverage: 16 lifecycle-header tests, 6 headless sidebar tests, 5 metadata/resource/bytecode/config tests.
- PNG exports: Hub 48x48 / 2,700 bytes; sidebar 24x24 / 842 bytes. Genuine PNG signatures, alpha, visible content, and dimensions checked. Exported Hub icon visually inspected.
- No leftover template implementation/metadata references or prohibited API matches in production source. Diff whitespace check passed. Build/private-data paths are ignored.
- CI configuration is prepared for Windows/Linux with Java 11; hosted execution is unverified.

The sandbox's initial default Gradle cache path was unwritable. Builds used the existing writable user cache through GRADLE_USER_HOME with approved tool escalation. This does not change repository configuration.

## Unresolved items

- User-run launch, sidebar readability/scaling, login-state behavior, toggles, and settings acceptance.
- Hosted CI run and privacy-safe screenshots.
- Existing template launcher generic-varargs compiler warning and Gradle 8.10 deprecation summary; builds pass, but upgrading to Gradle 9 is outside M1.
- Later milestone decisions: variant/copy allocation, exact outfit bundle mappings, observable GE behavior, elapsed time vs logged-in play time, and full storage format/migrations.

## Launch and acceptance

From the repository root:

```powershell
.\gradlew.bat run
```

The agent can launch this command on request, but must never control the game. Follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) for login. Enable From Scratch; open the purple coin sidebar. Confirm title, placeholder warning, six zero metrics, help-toggle behavior, disable cleanup, rapid reenable without duplicates, logged-out/logged-in preview, restart, and UI scaling/scroll readability. See [TEST_PLAN.md](../TEST_PLAN.md) for exact cases. A clean JVM launch alone does not pass acceptance.

## Git review state

Branch `master`; all changes are unstaged. Seven tracked files modified, three template Java files deleted, and 26 new files including this report. No build artifacts or temporary compatibility scripts are intended for version control. Nothing committed or pushed.
