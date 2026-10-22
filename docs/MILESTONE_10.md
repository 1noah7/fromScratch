# Milestone 10 — consolidated owner review

**Status: PARTIAL — local candidate prepared; manual acceptance and hosted CI evidence pending.** Gameplay is frozen at accepted M9.2. No production-source/save-format changes, publication, friend distribution or Milestone 11 submission occurred.

## GitHub and frozen baseline

- Canonical public repository: **[PilotsRKwl/fromScratch](https://github.com/PilotsRKwl/fromScratch)**. Public API, local origin and live remote master agree. Both origin URLs already use the new owner; no remote correction was needed. Git author configuration still has the legacy username; it was not changed and no credentials were inspected.
- Branch `master`, local/remote revision **`a2436132c9fb72cc25d015c35309628398486280`**, subject “Milestones 8-9.2: Virtual bank, GE hardening, and quantity styling”. `src/main` tree **`830cb55d79ef41711e5a478318b916598e0ebb09`** remains unchanged. M10 release inputs are deliberately uncommitted; provenance says so.
- Local and remote `v0.7.1`, `v0.8.0`, `v0.9.0`, `v0.9.1`, `v0.9.2` resolve to **`6015727cfe768cd74f77a9c96cec027981a0adfe`**, subject “Milestone 7: Fix banked rune pouch buybacks and refine UI”. They do **not** identify their named later implementations. `v0.6.0`/`v0.7.0` also share a commit, as do `v0.3.0`/`v0.4.0`. All tags/history were preserved. Use the combined master revision to identify the frozen baseline.
- Public [master CI run 39848151577](https://github.com/PilotsRKwl/fromScratch/actions/runs/39848151577) is **failed on both Windows and Ubuntu** at “Compile and run headless tests”; the validation job passed. Public job-log download returns **403**. No authenticated GitHub browser/connector was available, and account/token stores were not accessed. The underlying hosted error is **not confirmed**. Informational Ubuntu migration notices are not diagnosed as the cause.
- Locally, three unchanged older UI tests still expected pre-M9 gain/fee presentation, and the version test expected the former development version. Only those assertions were corrected, preserving their coverage and accepted behavior. The three baseline failures may explain CI, but that is not a claim about unavailable hosted logs. Both CI platforms, Java 11, caching, failure artifacts and commit/tag deduplication remain enabled unchanged.

## Windows beta candidate

Proposed release: **From Scratch — Windows Beta 0.10.0-beta.1**. Local artifacts:

| Artifact | Location |
| --- | --- |
| ZIP | `build/distributions/from-scratch-beta-0.10.0-beta.1-windows.zip` |
| SHA-256 | Same path plus `.sha256` |
| Production plugin | `build/libs/fromScratch-0.10.0-beta.1.jar` |
| Staged distribution | `build/beta/stage/` |
| Extracted review copy | `build/m10-review/final ZIP with spaces/` |
| Local review index / README render | `build/m10-review/index.html` / `README-preview.html` |
| Standard build evidence | `build/standard-check/evidence.json` |

Final ZIP: **29,354,890 bytes, 64 files**. SHA-256:

```text
af4afd4e1edf22ad0db703cec4968fda9d2e0f1edac7f5f5e3e717e8b1dd14e5
```

This identifies this local candidate only. Regenerate provenance/checksum if any packaged file changes, or after an approved source commit. No remote asset URL has been invented. Build directories and ZIPs remain ignored by Git.

The ZIP contains a thin production JAR, separate small bootstrap, **49 unmodified runtime dependency JARs**, scripts, notices, license and hash/provenance manifests. It excludes tests/JUnit, private profiles, authentication files and progression. All ten RuneLite-group artifacts matched official repository checksums (SHA-256 where published; older artifacts publish SHA-1). All packaged files have SHA-256 records. These are integrity checks, not publisher signatures. Embedded upstream notices stay intact; coordinates and a local JAR/POM notice inventory support redistribution review. Upstream licenses continue to apply; the From Scratch license does not relicense dependencies.

`Install.cmd` verifies the manifest/Java 11, installs under `%LOCALAPPDATA%\FromScratchBeta\versions`, and creates **From Scratch Beta.lnk**. No admin rights, Git, Gradle or IntelliJ are required by testers. Update-by-reinstallation atomically repoints only the owned shortcut; prior versions remain. Uninstall removes only unchanged owned program files/that version's shortcut. It refuses linked paths and preserves modified/unrelated files, normal RuneLite, credentials, backups and real progression. Per-process PowerShell execution policy does not alter machine policy. The beta does not replace the normal RuneLite client installation.

The bootstrap uses `ExternalPluginManager.loadBuiltin` and the unmodified RuneLite **1.13.3** developer-mode client with assertions. It does not use a credential helper, custom login, launcher-token/environment capture or insecure credential writing. The current official [Jagex Accounts instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) provide a user-initiated panel login. The panel is present in the resolved artifact. Java 11 is a separate official-vendor prerequisite; no runtime executable is downloaded by the installer.

**Verified:** packaged help outside IntelliJ; logged-out graphical client startup with an isolated empty user home; a real client window appeared and initialization logged about **7,966 ms**. The disposable process was stopped without login/game input. This does **not** verify the real desktop shortcut against the owner's normal profile, authentication, From Scratch in-game operation, or friend machines. Those remain manual checks.

One local PowerShell `Start-Process` smoke failed on duplicate `Path`/`PATH` keys inherited from the tool environment. The beta launcher now uses a small direct `ProcessStartInfo` launch with inherited environment; that method successfully started the isolated client. This is installer-only code, outside the Hub plugin. No gameplay workaround was introduced.

## Homepage and marketing

README now has the requested title/tagline, clear challenge pitch, near-top installation, feature showcase, six-stat explanation, honest limitations, privacy and technical links. It uses original `docs/media/from-scratch-hero.svg` / `.png` (1280×640, 29,607-byte PNG), generated by `scripts/Prepare-Marketing.ps1`. The banner was visually inspected and is explicitly **title artwork, not gameplay**. No Steam assets/affiliation or Plugin Hub approval is implied.

GitHub's public Markdown API rendered a local HTML preview; relative images/links are rooted at this checkout. No public page was changed. Exact GitHub theme/narrow-screen appearance still needs owner inspection. Real screenshots included: **zero**. [SCREENSHOTS.md](SCREENSHOTS.md) specifies seven genuine privacy-safe captures; README has no broken screenshot placeholders.

Recommended About: “A fresh-start OSRS challenge on your existing character. Rebuild a virtual bank, reclaim legacy gear, and track your comeback with RuneLite.” Topics/social preview and release-body recommendations are in [REPOSITORY_METADATA.md](REPOSITORY_METADATA.md). No remote settings/visibility were changed.

## Reviewer package and boundaries

[PLUGIN_HUB_REVIEW.md](PLUGIN_HUB_REVIEW.md) maps all 18 requested systems to verified source paths, including public native-bank callbacks, scoped quantity-display flags and restored title, cached Examine/chat behavior, finalized GE derivation/estimated tax, atomic storage buybacks, host-only playtime, Filepath persistence/export/import and public bulk prices. [FEATURES.md](FEATURES.md), [DATA_AND_PRIVACY.md](DATA_AND_PRIVACY.md), [INSTALLATION.md](INSTALLATION.md) and [BETA_TESTING.md](BETA_TESTING.md) separate player and internal behavior.

Production-source inspection found no gameplay automation, synthetic clicks/keys, withdrawal interception, packet manipulation, server ownership changes, credentials, cloud backend, reflection/native-memory/custom-loader/process calls. PriceService uses RuneLite's existing bulk public feed off rendering/client/EDT/storage HTTP paths, without player holdings in the request. Normal RuneLite networking/authentication belongs to the client. Saves/exports are private and unencrypted. M10 introduces no persistence migration.

Review-sensitive behavior is disclosed rather than hidden: native item quantity **display mode** is temporarily changed/restored before gray redraw; dynamic Bank Tags/native script filtering and bank title presentation are scoped public client APIs; local game messages append information. No native quantity/sprite/click region is changed. Existing anti-reentrancy, membership-cache, client-thread and full-container accounting safeguards are untouched. Human Plugin Hub approval is still required.

`build=standard` was verified using the official tooling template **unchanged** at `034eaffd5bce7bb678d35287b547874f3dba6b3b`, template SHA-256 `62af0c5e207ffb0285773ecb6346ebc8f29d676691ffd919dde0cb26ab73a3a5`, and Hub client version **1.13.3** supplied externally. Local Java 11 production compilation/resources/JAR passed. This is not full hosted packaging/scanning or review. No future-commit manifest or submission PR exists. Main plugin metadata/icon/license remain appropriate; beta sources never enter the Hub build.

## Validation

| Check | Result / precise scope |
| --- | --- |
| Frozen gameplay | PASS: no `src/main` diff; exact tree above. No real progression touched. |
| Production/test/beta incremental compilation | PASS, Java 11; class-file target 55 covered by resource tests. |
| One full regression | **937 executed, 933 passed, 4 stale assertions failed**. No repeat full run. |
| Follow-up | **22/22 passed** across FoundationResourcesTest, BankPanelTest, FinalCorrectionsUiTest and GeDashboardTest after assertion-only fixes. Combined full/follow-up evidence covers all 937; original full command was not green. |
| Final extracted ZIP | **10/10** installer scenarios passed, including fake-desktop COM shortcuts, spaces, repeat/update/uninstall, preservation, malformed paths and tampered/unexpected JARs. File allowlist, exact hashes and ZIP checksum checked. |
| Packaged startup | Help + isolated logged-out graphical initialization passed; authentication/gameplay pending. No game UI automation. |
| Standard template | PASS local Windows production compile/resources/JAR; no beta/test code included. |
| Unix wrapper | Executable bit `100755`, Git Bash syntax check passed; this is **not** Linux execution. |
| Hosted Windows / Ubuntu | Both failed; raw error unavailable via public API (403); approved rerun/log inspection pending. |
| README / links | GitHub Markdown render obtained; local link/file checks and artwork inspection performed. Final owner visual review and real captures pending. |

No dependency/CI checks were disabled and no routine clean build was used. Test fixes retain checks for the new peak label, two million-GP bank values with unknown peak, leading estimated-fee tilde, and current beta metadata. Documentation-only edits do not trigger another full regression.

## Changed-file inventory

- Root: `.gitignore`, `README.md`, `ARCHITECTURE.md`, `ACCOUNTING.md`, `TEST_PLAN.md`, `CHANGELOG.md`, `build.gradle`, `runelite-plugin.properties`.
- Existing docs: `docs/ARTWORK.md`, `docs/PUBLICATION.md`; issue template `.github/ISSUE_TEMPLATE/bug_report.md`.
- New docs: `docs/FEATURES.md`, `docs/DATA_AND_PRIVACY.md`, `docs/INSTALLATION.md`, `docs/BETA_TESTING.md`, `docs/PLUGIN_HUB_REVIEW.md`, `docs/SCREENSHOTS.md`, `docs/RELEASE_DRAFT.md`, `docs/REPOSITORY_METADATA.md`, this report.
- Art: `docs/media/from-scratch-hero.svg`, `docs/media/from-scratch-hero.png`.
- Distribution: `gradle/beta.gradle`, `src/beta/java/com/fromscratch/beta/BetaLauncher.java`; `beta/windows/Common.ps1`, `Install.cmd`, `Install.ps1`, `Launch.cmd`, `Launch.ps1`, `Uninstall.cmd`, `Uninstall.ps1`, `README.txt`, `THIRD_PARTY_NOTICES.txt`.
- Tools: `scripts/Build-Beta.ps1`, `Test-Beta.ps1`, `Test-StandardBuild.ps1`, `Prepare-Marketing.ps1`.
- Assertion-only tests: `src/test/java/com/fromscratch/FoundationResourcesTest.java`, `bank/BankPanelTest.java`, `bank/FinalCorrectionsUiTest.java`, `bank/GeDashboardTest.java` (the last three under the same test package).

Build artifacts/evidence remain local ignored files. Review the complete working/index diff before any commit. No real saves, exports, backups, credentials or binaries are included in Git changes apart from the original promotional PNG.

## One owner review checklist

1. Open `build/m10-review/index.html` and README's IntelliJ Markdown preview. Inspect branding, wording, narrow sizing and links. Review the seven missing real captures before public marketing.
2. Inspect the actual ZIP/sidecar and extracted files. Compare SHA-256. Install on a disposable Windows user/profile first if desired; test spaces, the real **From Scratch Beta** shortcut, update and uninstall. Existing same-character progress must remain intact; export privately before real testing.
3. Follow RuneLite's official Jagex Accounts panel login yourself. Never send tokens, credential files or passwords. If it fails, report a redacted error; do not substitute insecure credential writing. Only the owner tests gameplay.
4. Enable/resume From Scratch, verify Full Bank before Start, complete initial snapshot, Virtual Bank/tabs, gray/bright counters, ordinary deposit/withdraw, native Examine and restart. Use safe trades for full/partial-cancel GE history; inspect buyback quotes/cancel before any optional real payment. Do not reset/import real progression for testing.
5. Read the reviewer/privacy/installation docs and release draft. Review scoped widget/filter APIs, host-playtime/local-save limitations and upstream distribution notices.
6. In GitHub, download the latest failed **validation-ubuntu-latest** and **validation-windows-latest** artifacts or open step logs. Supply only the first relevant Gradle failure/redacted test result. After approved commit/push, both hosted platforms need a green run; no automatic push occurs here.
7. Inspect the staged/working diff and confirm no unrelated/private files. Give **one consolidated acceptance or correction list**. Commit, tag, public release/upload, friend distribution and Milestone 11 each remain outside this execution's approval.

No gameplay design decision remains. Missing authentic screenshots, real shortcut/login/gameplay acceptance and authenticated CI failure evidence require owner participation. The candidate is ready for that review, not public launch.
