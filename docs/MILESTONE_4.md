> The user accepted M4, including bank-only accounting, persistence and polished UI. The [performance/dialog report](MILESTONE_4_POLISH.md) and [UI polish report](MILESTONE_4_UI_POLISH.md) record its accepted baseline. Current development is [M5](MILESTONE_5.md); earlier pending-acceptance text below is historical.

# Milestone 4 — Bank-only tracking

By **Pilots r kwl**. **Automated validation passed; user-run manual acceptance pending.** M1–M3 remain accepted. M5 has not begun. No commit, push, tag, publication, game interaction, real-save inspection or private progression reset was performed.

The [earlier M4 report](history/MILESTONE_4_MULTI_CONTAINER.md) and [correction pass](MILESTONE_4_CORRECTIONS.md) are historical. Their multi-container acquisition rules, Update Progress wizard and session gates are superseded by this bank-only contract.

## Architectural change

The production path is now:

`BANK events / mounted bank → complete exact-ID bank aggregate → quiet tick stability → signed bank delta → background journal + checkpoint + current publication → committed bank projection → accepted M3 overlay`

Inventory/equipment observation, acquisition/source attribution, multi-container correlation, consumption/drop/Elsewhere tracking and normal-session review gates are removed from live wiring. Twenty-six retired source files were moved into test fixtures so the original 417 regressions remain executable without shipping that path in the production JAR. Historical immutable models/codecs/journal verification remain only to decode and archive existing saves.

New setup relies on manual banking plus player attestation, complete bank data and confirmed Start. It never inspects or operates inventory/equipment. Active sessions need only an open bank. There is no reconciliation wizard for ordinary play or returning from mobile.

## Signed accounting

`new B(i) = committed B(i) + (new physical P(i) - committed physical P(i))`

Initialize P to the complete bank and B to zero. Missing records mean zero. Physical counts are nonnegative; signed progress may be negative, including absent items. Checked long arithmetic rejects an entire invalid/overflowing operation. Anchored positive progress cannot exceed physical quantity. These are bank progress balances, not earned ownership or lifetime acquisition statistics.

Legacy axe withdrawal is 0→-1; return is -1→0. Deposit two new logs is 0→2; withdrawal is 2→1. Burning outside bank has no observation/accounting path, so reopening leaves 1. Mixed stacks follow the same net equation. Manual corrections change an explicit B-P offset, which later movements preserve. Initial legacy data never changes.

## API and observation evidence

The actual resolved RuneLite **1.13.3** BankPlugin source was inspected locally: InterfaceID.BANKMAIN, InventoryID.BANK, WidgetLoaded/WidgetClosed, ItemContainerChanged and Client.getItemContainer(BANK). Public tagged sources are linked in [COMPATIBILITY.md](COMPATIBILITY.md).

Widget/quantity events invalidate candidates; GameTick retries complete non-null bank API data under a mounted, visible normal bank without a PIN. Equal aggregate quantities on separated polls after a full quiet tick become authoritative. No event on reopening is required. Null container/array, null slots, malformed quantities, PIN or hidden/unmounted bank is unavailable rather than empty. A complete zero-length array is valid. Placeholders, fillers, BLANKOBJECT and empty slots contribute no physical quantity. Exact variants remain distinct.

Bank tabs/search/scroll/layout are not accounting sources. Close-before-settle defers comparison until reopening. The public API has no extra server completeness flag; stable full-container observations are the supported boundary, not proof that future packets cannot arrive. Later events invalidate/retry normally. Updates usually follow a few quiet ticks; actions within one settling interval may collapse into one net audit batch.

## Storage, recovery and migration

All files remain under the established local-only RuneLite Filepath character directory. A single background worker uses file locking/CAS; forced immutable bank journals/checkpoints precede atomic current replacement (or recoverable forced-copy fallback). Only durable acknowledgment publishes a new bank cursor/balance to renderer/UI. Failures retain the prior cursor, surface an error and require reload; orphan recovery files are never replayed. An acknowledgment lost after successful publication is recovered from current. Character/lifecycle callbacks are guarded, including pending saves and stale reset clicks after character switching.

Format **5** stores run/start/revision identity, immutable initial bank, authoritative physical bank anchor, sparse signed progress, recent audits and persisted first host-session comparison summary. Journal batches contain UUID, character/run/session, timestamp, kind, signed physical/progress deltas, previous revision/sequence and result-checkpoint hash. Load verifies exact published checkpoint bytes and the referenced chain; normal saves verify the current head without rescanning history. UI recent history is bounded to 30 batches; complete journal segments remain on disk. JSON documents are bounded to 4 MiB; oversized input/output fails safely rather than truncating/resetting.

Before an old format-1–4 current is changed, its envelope and original-format journals are verified. Every existing JSON save/checkpoint/ledger is copied to a unique migration archive, forced, byte-hash compared, and recorded in a forced/verified manifest. Original files also remain. No actual user files were migrated during development.

Preserve original run ID/start, original three-container snapshot/full envelope and archived transaction history. Import the old committed signed bank display: old BANK allocation minus old outstanding deficit. Personal/equipment/Elsewhere quantities remain archived; they are not bank gains. A one-time **Review Bank Anchor** requires a current complete bank and explicit approval. Keep valid balances without replaying pre-migration bank changes. Excess positives must be physically banked first or explicitly approved as listed reductions; reductions are auditable and never silent. Subsequent sessions do not repeat the walkthrough. Corrupt/incompatible/missing/failed data blocks defaults rather than replacing progression with zero.

Reset remains secondary, confirmed, archive-before-publish and does not start a replacement run. Back up/restore the entire character directory, including both journal generations and all migration archives, with the client closed. A single checkpoint alone is insufficient for recovery.

## Sidebar and corrections

The active dashboard shows status/start/duration, current positive Bank Items Unlocked and Recent Bank Changes. Bank Net Worth/GP are explicitly M5 placeholders; original wealth/quantities and unsupported lifetime value/acquisition claims are hidden. Accepted purple styling, approximately 48% gray tint, full-slot geometry, partial-row intersection clipping, quantity placement and drag/input behavior are preserved.

**Played Elsewhere?** instructs players to bank their items and open the bank on this PC. The first complete comparison in each host login/plugin session applies net changes automatically and persists an On return summary. It remains available after applying changes and is not replaced by subsequent ordinary bank movements. A no-change comparison is labeled accordingly. Items earned/consumed entirely outside bank cannot be reconstructed; there is no cloud merge.

**Account Tools → Bank Corrections** is searchable even with no automatic differences. Known current/baseline/history/progress IDs include absent negative items. Exact IDs distinguish identical names/variants. Inspect physical and signed counts, enter desired B or signed adjustment, review preview and explicitly confirm Save Correction. Positive B cannot exceed physical P. The bank must currently be stable/open to save. Revision/current-bank validation rejects stale editors. Cancel is non-mutating. Corrections append history and survive later deltas. **Bank History** is last/collapsed by default and separates recent automatic and manual records.

## Files

- Added production `bank/`: BankActions, BankAudit, BankController, BankCorrections, BankEngine, BankItems, BankObservation, BankObserver, BankPanel, BankRun, BankSidebar, BankSource, BankView, RuneLiteBankSource.
- Added persistence: BankRepository, BankRunCodec, BankStorageFiles. Retained historical RunRecordCodec/ItemJournalCodec/ItemJournalStore and immutable model/tracking records for migration only.
- Updated plugin wiring/config and overlay state/value source. The accepted painter geometry/tint was not changed by bank-only rearchitecture; signed rendering from the prior correction remains.
- Added four current-contract suites and BankFixtures. Adapted historical overlay assertions through LegacyOverlayFixture. Moved retired setup/observation/controller/review code to src/test; no retired classes are in the production JAR.
- Updated README, ARCHITECTURE, ACCOUNTING, TEST_PLAN, AGENTS, CHANGELOG, compatibility and privacy ignores; archived superseded design docs. Added src/test/HISTORICAL_FIXTURES.md and a [full working-tree inventory](MILESTONE_4_FILES.md).
- Build adds existing Lombok to test-only compile/annotation-processing for historical fixtures. Java 11, Wrapper, normal dependency conventions, CI, license, metadata and icons are retained. No production dependency/service was added.

## Automated validation

Final `.\gradlew.bat clean build`: **BUILD SUCCESSFUL**, seven tasks executed, Java 11 target, resolved RuneLite 1.13.3.

**500 passed; 0 failures; 0 errors; 0 skipped; 32 suites.** Original 417 cases retained; 83 current-contract regressions added:

| Suite | Tests |
| --- | ---: |
| BankOnlyPipelineTest | 38 |
| BankContractTest | 14 |
| BankPanelTest | 7 |
| BankPersistenceTest | 24 |

Pipeline tests use actual API Item[] aggregates/quiet event sequences, live controller, real Filepath persistence and actual VirtualBankOverlay drawing, not only reducers. Coverage includes normal signed deltas/no personal gates, incomplete/empty/PIN/reopen/search/reorder, corrections/cancel/staleness, offline return summary, durable acknowledgment/failure/retry, migration anchor, identity/save/reset races and signed overlay behavior. Migration tests cover verified formats 1/2/3/4, original byte preservation, bad old chains, archive corruption, interrupted publication and no default-zero recovery. Headless Swing covers setup, search, current dashboard, history and one navigation registration.

The production JAR was inspected: **zero retired SetupController/ItemTrackingSession/ItemAccountingEngine/FromScratchPanel/RuneLiteSnapshotObserver/ItemReview/personal-reader classes**. No real game input/account was used. Existing nonfatal warnings: unchecked development launcher operation and Gradle APIs deprecated for Gradle 9. Current Wrapper remains Gradle 8.10. `git diff --check` passes.

## Exact manual acceptance

1. Launch with `.\gradlew.bat run`, or ask the agent to launch it. Follow [RuneLite's Using Jagex Accounts instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) to log in to the development client.
2. Keep your existing run. If upgrading format 1–4, open only the bank and use **Review Bank Anchor** once. Check preserved start date and valid saved balances; cancel if you want to bank copies instead of accepting a listed excess reduction. Do not reset merely to test migration.
3. In an anchored run, start with a zero-progress legacy axe: withdraw one → -1; return → 0. If no slot exists, find its negative count in Bank Corrections. Never open inventory/equipment tabs as an observation prerequisite or enter a review editor.
4. Use a previously absent logs ID: deposit two → 2; withdraw one → 1; close bank; manually burn that withdrawn log; reopen → still 1. Reopen repeatedly: no duplicated change. Allow a few quiet ticks for a bank update to save.
5. Test mixed legacy/new stacks; rapid withdraw/deposit; tabs/search/reordering; genuinely empty bank; PIN/delayed load. No temporary unavailable data should create mass losses.
6. Open Account Tools → Bank Corrections with no differences. Search Wooden shield, inspect counts, preview/set an allowed signed balance and confirm. Check purple counter after save. Withdraw/redeposit: correction offset remains. Try Cancel, excessive positive input and a bank change while the editor is open; no unintended/stale write. Check negative absent items remain searchable.
7. Restart/disable/re-enable and open bank alone: balances persist, no recurring migration/review gate. After playing elsewhere with the host client closed, deposit desired items and open bank here: automatic net changes and retained On return summary. Ordinary later changes should not erase it.
8. Recheck accepted M3 visuals: first/middle/top/bottom partial rows, fast scrolling, tabs/search/empty results, resize/scaling, placeholders/fillers, tooltips/right-click/drag. Negative/zero dim, positive normal; original quantities remain readable. Nothing paints outside the bank viewport or after closure/disable.
9. Verify concise dashboard, no legacy/lifetime claims, bottom collapsed Bank History and separate automatic/manual history. Full checklist and safe recovery testing are in [TEST_PLAN.md](../TEST_PLAN.md).

Only the user can confirm live-client timing, UI usability, visuals and performance. A build or clean JVM launch is not acceptance.

## Known limitations and Plugin Hub considerations

Bank-only progress intentionally cannot establish acquisition provenance, outside-bank transformations/consumption, lifetime value, gross offline transactions, GP spending or GE behavior. Exact bank ID conversions can appear as separate signed changes. Special storage is unsupported. Initial attestation is honor-system. Small observation/IO delays and net-batched rapid actions are intentional. Recent UI history is bounded; older immutable history remains local. Oversized/corrupt/newer data stops safely for recovery.

Current public review/rejected-feature guidance was checked. The path is supported observation/accounting/rendering/local storage; no synthetic input, menu/action interception, packets, game item/widget/sprite mutation, reflection/native code, cloud or player-data HTTP. The draw-only bank effect and Filepath storage still need eventual Plugin Hub review; approval is not guaranteed. See [Plugin Hub Review](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review) and [Rejected Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features). No submission was made.

## Git state

Branch `master`, HEAD `5b7c49f` (user-accepted M3). Status: 24 modified tracked paths, 16 tracked main-source removals/moves, 79 new/untracked paths, zero staged changes. All M4 changes remain unstaged/uncommitted, including earlier M4 work and the bank-only replacement. Deleted main-source paths in status are deliberate moves to test-only historical fixtures, not lost source or progression data. Full file status is in the working-tree inventory. No commit, push, tag, publication, private-save reset or M5 work occurred. Stop here for manual acceptance.
