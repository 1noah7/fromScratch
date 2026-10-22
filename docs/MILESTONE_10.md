# Milestone 10: final beta review

**Status: local release candidate prepared; owner installation/login acceptance pending.** Gameplay remains the accepted Milestone 9.2 baseline. No commit, push, tag, upload, distribution or Plugin Hub submission is performed by this task. Milestone 11 remains separate.

## Baseline and CI

- Canonical repository: [PilotsRKwl/fromScratch](https://github.com/PilotsRKwl/fromScratch), branch `master`.
- Source revision: `2bb5dbd4cb7973a05b9f449be70e5adcc0101d06`.
- Frozen `src/main` tree: `830cb55d79ef41711e5a478318b916598e0ebb09`.
- Hosted [run 39868207087](https://github.com/PilotsRKwl/fromScratch/actions/runs/39868207087): Windows **PASS**, Ubuntu **PASS**, verified from the public GitHub API. No full gameplay suite was rerun for documentation or preview tooling.
- Earlier Windows assertion and Ubuntu font-sensitive test failures, full prior build evidence and original candidate details remain in the [preparation archive](history/MILESTONE_10_PREPARATION.md). Old milestone tag inconsistencies remain unchanged; tags were not rewritten.

## Documentation and presentation

The approved README prose is preserved. New screenshots show actual production Swing components with clearly disclosed **sample data**, not a player's progression. The [documentation index](README.md) separates player help, current technical guidance and historical records. Root agent files remain discoverable; license/attribution are unchanged. [Installation](INSTALLATION.md) is the single authoritative beta guide.

The [screenshot guide](SCREENSHOTS.md) records capture provenance and the remaining owner gameplay shots. [Reviewer documentation](PLUGIN_HUB_REVIEW.md) exposes the precise Bank Tags/rendering APIs and local IO boundaries. Technical documents have moved into `docs/technical`; original accumulated versions and earlier milestone reports are preserved under `docs/history` with an explicit superseded-status warning.

## Candidate and safety

Version: **0.10.0-beta.1**, unpublished Windows x64 owner-review candidate. Build with `scripts/Build-Beta.ps1`; verify with `scripts/Test-Beta.ps1`.

Local ZIP: `build/distributions/from-scratch-beta-0.10.0-beta.1-windows.zip`. Its sibling `.sha256` file records the exact checksum. `package.json`, `SOURCE.json` and `DEPENDENCIES.json` record the real source revision, dirty release worktree, unchanged gameplay tree, pinned unmodified RuneLite 1.13.3 artifacts and per-file hashes. This is a review candidate, not a published release.

The installer requires official Java 11, verifies the allowlist, installs versioned files without admin rights and creates the owned desktop shortcut. Updates retain previous versions. Uninstall deletes only unchanged owned program files and an owned matching shortcut, preserving extra/modified files, other versions, progression, backups, RuneLite configuration, authentication and other plugins.

The launcher uses the separate beta bootstrap and RuneLite's supported developer entry point. The current [official Jagex Accounts guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) describes the in-client account panel and user-operated login. No token extraction, password collection, insecure credential export or custom authentication is included. Actual shortcut/login/gameplay remains manual; a help smoke is not evidence of authenticated play.

The previous official `build=standard` compile for unchanged production source remains valid: Java 11, RuneLite 1.13.3, official template revision `034eaffd5bce7bb678d35287b547874f3dba6b3b`. Preview and beta source sets do not enter the production JAR or Hub source build.

## Final checks and owner review

No real player saves or authentication files were used by these checks.

| Check | Result |
| --- | --- |
| Java 11 incremental production/beta build and isolated preview compilation | PASS; unchanged production/beta tasks reused their valid outputs. |
| Standalone Swing rendering | PASS; nine new PNGs from actual components, including an overview crop and storage-category view. Images visually inspected. The item-history Details column was resized in the sample dialog to avoid clipped text. |
| Windows installer integration | **10/10 PASS**: parsing, hashes/allowlist, production/resource/preview isolation, official authentication panel, traversal rejection, paths with spaces, real COM shortcut on fake desktop, repeat install, help launch, tamper/classpath rejection, versioned update and preserving uninstall. |
| Actual candidate ZIP | **64 files PASS**, 63 manifest-listed payload files plus the manifest; only expected directories, no duplicate paths. Every payload length/SHA-256 and the ZIP sidecar verified. |
| Documentation | **52 Markdown files, 244 local references and 10 total PNG assets PASS**, including local heading anchors and approved README style. Public Releases, issue and prerequisite links return HTTP 200; the repository still has zero published releases. GitHub's Markdown API rendered the README successfully. |
| Hosted regression evidence | Windows and Ubuntu PASS at the source revision above. No full suite rerun for this preparation. New uncommitted tooling/documentation has not been pushed for hosted validation. |

Candidate size: **29,354,938 bytes**. SHA-256:

```text
af8121c131cd8b5483d7824830b4ecba64b292ce3cba80a662f3f15004e85c06
```

Local GitHub-rendered preview: `build/m10-final-review/README-preview.html`. Evidence JSON and the sanitized rendered body are in the same ignored directory. This is a local viewing aid, not a published page. Final GitHub/mobile layout and actual shortcut/login/gameplay still need owner acceptance.

## File organization and preservation

- Root `ARCHITECTURE.md`, `ACCOUNTING.md` and `TEST_PLAN.md` are replaced by current concise guides under `docs/technical/`. Full original versions are preserved as `docs/history/*_PRE_M10.md`.
- Eighteen milestone reports moved from `docs/` to `docs/history/`: `MILESTONE_1`, `MILESTONE_2`, `MILESTONE_3`, `MILESTONE_4`, `MILESTONE_4_CORRECTIONS`, `MILESTONE_4_FILES`, `MILESTONE_4_POLISH`, `MILESTONE_4_UI_POLISH`, `MILESTONE_5`, `MILESTONE_5_FILES`, `MILESTONE_5_FINAL_CORRECTIONS`, `MILESTONE_5_POLISH`, `MILESTONE_6`, `MILESTONE_7`, `MILESTONE_8`, `MILESTONE_9`, `MILESTONE_9_1` and `MILESTONE_9_2` (all `.md`). `POST_MILESTONE_7.md` and historical `COMPATIBILITY.md` moved alongside them. No technical history was discarded.
- Prior M10 preparation and agent scope are preserved as `MILESTONE_10_PREPARATION.md` and `AGENTS_PRE_M10.md`. Root AGENTS is current; root CLAUDE remains its discovery pointer. Historical links/anchors were repaired, without changing gameplay evidence. LICENSE and attribution remain untouched; no content was deleted outright.
- Current root changes: README screenshot/link additions only, AGENTS current scope/paths, CONTRIBUTING focused workflow and CHANGELOG accepted-baseline status. Current doc changes: ARTWORK, BETA_TESTING, MILESTONE_10, PLUGIN_HUB_REVIEW, PUBLICATION, RELEASE_DRAFT, REPOSITORY_METADATA and SCREENSHOTS. New current indexes: `docs/README.md`, `docs/history/README.md`.
- New owner tooling: `gradle/previews.gradle`, `scripts/Prepare-UiPreviews.ps1`, `scripts/Test-Documentation.ps1` and `src/preview/java/com/fromscratch/preview/InterfacePreviews.java`. `build.gradle` loads the isolated preview task; `gradle/beta.gradle` records that build input, and `scripts/Test-Beta.ps1` explicitly rejects preview classes in the plugin JAR.
- New images: `dashboard.png`, `dashboard-overview.png`, `legacy-buybacks.png`, `legacy-buybacks-utility.png`, `ge-purchases.png`, `ge-sales.png`, `bank-history.png`, `item-history.png`, `setup.png`, all under `docs/media` and labeled sample UI. Native bank/gameplay images remain owner captures.
- `src/main`, `src/beta`, `beta/windows`, CI workflow, license, actual progression and credentials are unchanged. No save migration, gameplay change, commit, staging, push, tag, release or distribution was performed.

## One consolidated owner review

1. Preview [README](../README.md) in IntelliJ's Markdown preview. Check image size/captions and links; sample UI is disclosed beside each new image.
2. Inspect PNGs in `docs/media` and capture the real bank/gameplay shots in [SCREENSHOTS.md](SCREENSHOTS.md) when convenient.
3. Verify the ZIP's checksum, extract it, install and open the actual **From Scratch Beta** shortcut. Complete the official login yourself; do not share credentials or logs containing them.
4. Follow [beta testing](BETA_TESTING.md), including restart and settings. Export privately before updates. Check uninstall safety on a disposable installation.
5. Review `git diff`, untracked deliverables and the [technical map](PLUGIN_HUB_REVIEW.md). Approve or provide one consolidated correction list. No publication or Milestone 11 action is implied.
