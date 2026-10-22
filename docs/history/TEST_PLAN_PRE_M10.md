# Test plan

## Milestone 10 frozen release checkpoint

Gameplay through M9.2 is owner-accepted; M10 changes no production source or save format. One full local Java 11 run exercised **937 cases**: 933 passed, four failed on stale assertions (old development version, former Total Value Gained label/count, trailing estimated-fee tilde). Update only those assertions to the accepted M9 behavior/current beta metadata; the four affected classes then passed **22/22**. Do not repeat the entire suite for these test/documentation-only corrections. This is combined evidence across the full run and focused follow-up, not a claim that the original full command was green.

Windows installer/ZIP tests cover ten isolated scenarios, real COM shortcuts under a fake desktop, traversal/tamper/unexpected-JAR rejection, versioned update and preservation on uninstall. A separate packaged help smoke and logged-out graphical startup use empty sandbox user homes; real shortcut/authentication/gameplay remain owner tests. Official standard-template Java 11 compilation/resources/JAR passed. Hosted Windows at `8e252f6` now passed all 937 tests; verified Ubuntu artifacts show one layout-test failure. The font-aware compact sizing/no-clipping correction and three font fixtures plus short-copy minimum passed in 11 targeted cases; hosted Ubuntu confirmation awaits approval to push. Exact evidence, candidate and manual checklist: [MILESTONE_10.md](MILESTONE_10_PREPARATION.md#current-polish-and-ci-repair).

## Milestone 9.1 focused validation

63 distinct targeted cases passed: VirtualBankModeTest (22), production-plugin EventBus registration (1), GePipelineTest (30), GeHistoryViewTest (8), finalized pagination/immutable history (1), and GePerformanceTest (1). The pagination case was rerun alone after adding a cross-page duplicate-terminal assertion. Production/test incremental compilation passed; no clean build or full suite. Native-tab callbacks, real controller/Filepath bank deltas, restart/slot reuse, partial cancellation, no financial replay, 41 finalized offers across two pages, and storage-worker yielding are covered. The 1,200-item/128-update worker fixture retains bounded internal state while hiding its unfinished offer.

Live native widgets, banking interactions, tabs/All, Full Bank, placeholders, scrolling, restart/freeze absence and native search exit still require the player. Test full buy/sell offers, unfilled cancellations and partial cancellations; verify one executed row, quantities/GP, leading estimated-tax marker and unchanged accounting after viewing/restarting. Exact steps and limitations: [Milestone 9.1](MILESTONE_9_1.md). Reserve the full regression run for the release checkpoint.

M1–M8 are accepted; M9 awaits manual acceptance. The old 417 tests remain executable, with retired controllers/readers/review code moved into test-only historical fixtures. Their old source-attribution/session-gate assertions are compatibility evidence, not the new live contract. See [fixture policy](../../src/test/HISTORICAL_FIXTURES.md) and [historical plan](TEST_PLAN_MULTI_CONTAINER.md).

M9 restores native filtering with cached membership/deferred re-entrant lifecycle tests and focused banked/native-inspection Looting Bag evidence checks; live stability remains required. Correction tests cover re-entrant detach, hidden controls, GE slot-clear presentation versus durable evidence and centered numeric cells, new-run zero/leading-tilde expenses, monotonic peak/recovery/export, verified bag contents including mixed coins/runes, unknown/stale rejection, atomic purchase failure, variant/earned status, catalog card selection, exact signed Examine counts and unchanged compact overlays, hidden developer controls, full local export/import and retained purchase/pouch/history/playtime evidence, wrong-character/corrupt/traversal/incomplete/stale/canceled/interrupted backups, acknowledged-only restored overlays, explicit previous-head recovery, playtime checkpoint/restore barrier, and partial GE cancellation across restart. Use temporary Filepath roots only. Compile and run affected filters during iteration, then run the full existing regression once at the release checkpoint. Actual Ubuntu root-cause evidence and hosted validation remain required; see [M9](MILESTONE_9.md).

For M8 iteration, compile with `./gradlew compileJava compileTestJava` and run only affected filters listed in [M8](MILESTONE_8.md). The current 24 feature tests cover native Examine FIFO/unknown/expiry/core-price ordering and recursion, escaped names/live segmented color, signed GE/HA totals, catalog and shared/set/upgrade purchase evidence; default-off dynamic BankTag lifecycle and durable controller-to-overlay membership; read-only item history; host playtime/restart/CAS/recovery; and unchanged dashboard order/two columns. This presentation/configuration correction runs only NativeExamineTest, VirtualBankModeTest and Milestone8PanelTest (15 cases). Shared accounting/persistence and playtime are unchanged, so their broader suites are not repeated. Real chat ordering, Bank Tags interactions, mixed stacks and lag remain user-only acceptance tests. Full regression is reserved for the M8 release checkpoint.

Run the full suite with Java 11 and the existing Wrapper:

```powershell
.\gradlew.bat clean build
```

No test needs a game account, credentials, private progression, external service or game UI automation. Tests use Filepath roots under JUnit TemporaryFolder. The production plugin JAR excludes historical fixtures. The development shadow/launcher classpath includes test fixtures but registers only the current plugin.

Accepted M4 baseline: **544 passed, 0 failures/errors/skipped, 37 suites**. M5 foundation: 611 passed. Final M5 UI/quantity polish clean build: **638 passed, 0 failures/errors/skipped, 43 suites**, adding 27 targeted tests. All applicable earlier cases remain; obsolete UI/tooltip/item-label expectations match the authorized contract. Current timings/manual steps are in [the polish report](MILESTONE_5_POLISH.md). Historical tests remain compatibility evidence rather than the current live UX contract.

## Current bank-only acceptance coverage

| Suite | Coverage |
| --- | --- |
| BankOnlyPipelineTest | Actual full Item[] aggregation + quiet observer ticks → live BankController → Filepath journal/checkpoint → committed BankOverlayState → actual VirtualBankOverlay/Java2D. Setup/attestation, empty bank, legacy -1/0, deposit 1/2 and withdrawal, outside-bank burn/reopen, mixed stacks, repeated/reordered events, tabs/search, unavailable/partial/PIN, delayed data/already-open bank, absent negative item, direct correction/cancel/stale/current physical check, restart/offline net changes/persisted summary, commit acknowledgment/failure/retry, identity, variants/placeholders/fillers, duplicate setup/reset, one-time migration and stale anchor |
| BankContractTest | Immutable baseline/maps, duplicated unstackables and large stacks, null vs empty, invalid quantities, signed corrections/offset, physical upper bound, checked long edge/overflow, stable session summary, positive-type metric |
| BankPersistenceTest | Format 5 round trip, isolation, missing/corrupt/newer state, journal integrity, CAS/idempotency, interrupted publication/recovery, complete reset archive, verified formats 1/2/3/4 migration, exact original/current/ledger bytes, preserved run/start/baseline/balances, confirmed excess reduction, no replay after anchor, corrupt old ledger/archive and required fields, old reset tombstone |
| BankPanelTest | Actual headless Swing setup attestation, no personal waiting labels, active dashboard hides legacy/lifetime/setup claims, permanent history tiles and bottom typed-reset area, corrections with no differences, name/ID search/sort, played-elsewhere copy, navigation idempotency |
| Existing M3 overlay/painter/state tests | Bottom-right labels, signed compact formatting, ordinary quantity/graphics preservation, top/bottom partial-row intersection and clipping, tabs/search/empty/search scroll/resize, placeholders/fillers, normal-bank-only/disabled/closed behavior, drag-bound handling, one overlay registration, configuration/color changes |
| Retained M1–M3/legacy fixtures | Identity/lifecycle, resource/license/metadata, frozen three-container baselines/emptiness behavior and old codecs/recovery/ownership migrations; these mechanisms are no longer live setup prerequisites |

## Required user-only live acceptance

1. Launch the development client using `.\gradlew.bat run`; use RuneLite's [Jagex Accounts instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Agents must not interact with RuneScape.
2. **Existing run:** retain your data. Expect one Finish Bank Upgrade/Review Bank Anchor, with previous run date preserved. Open bank only. Confirm valid saved balances. If an excess positive balance is listed, cancel and bank missing copies or explicitly approve its reduction. Re-enable/relog after anchoring: no repeat walkthrough.
3. **New run only when desired:** manually bank inventory and equipment, attest, open bank, Start and confirm. Empty inventory/equipment do not matter; no personal tabs are required. Never reset an existing run merely to test migration.
4. In an anchored run with a zero-balance legacy axe, withdraw it: -1 if its bank slot/placeholder exists; otherwise find -1 in Bank Corrections. Return it: 0. Repeat without opening personal tabs or a review tool.
5. For a previously absent logs ID, deposit 2: 2. Withdraw 1: 1. Close bank, burn the withdrawn log manually, reopen bank: still 1. Confirm no duplicate history from reopening.
6. Test mixed stacks and rapid batches. Allow the next game-tick boundary and durable save after changes; if closed sooner, reopen for net comparison. Search/switch tabs/reorder: no economic changes.
7. Corrections: Played Elsewhere? → Bank Corrections, search Wooden shield with no pending changes; set an allowed count/negative balance, inspect preview, confirm. Purple number updates after save. Withdraw/redeposit: correction offset persists. Cancel: no change. Change bank while the editor is open: stale save must reject; reopen it. Positive counts above physical must reject. Negative absent items stay searchable.
8. Close/restart/disable/re-enable and open bank alone. Saved balances persist. Simulate another-client progress manually with RuneLite closed, then open bank here: automatic net update, retained On return summary, no wizard. Later bank actions do not replace that summary.
9. Overlay regression: first/middle rows, top/bottom partial clipping, fast scrolling, searches/tabs/empty results, resize/UI scaling, placeholders/fillers, dragging/right click/tooltips. Nonpositive gray tint/purple label and original physical quantities remain readable. No paint on tabs/search/other interfaces, after closure, or disabled.
10. Use the permanent Bank History tiles; check automatic vs correction histories, 25-row pagination and page-local filters. Reset Progress is the absolute bottom danger area and requires typed RESET. Check Bank Items Unlocked counts currently positive IDs, financial fields are explicitly deferred and original account totals/lifetime claims/setup instructions are absent when active.
11. With a disposable synthetic/test copy only, check storage failures preserve the previous anchor/history and display an actionable error. Never corrupt the user's authoritative live save for testing.

A successful build/launcher start does not prove live bank data timing, layout, FPS, item definitions, migration UX or interactions. Manual acceptance remains the final gate. No M5 or publication is authorized.

## Current M4 polish acceptance

Follow [MILESTONE_4_POLISH.md](MILESTONE_4_POLISH.md) for native-versus-purple timing comparison, exact diagnostic phase fields, two-column tile/icon validation, all dialog/keyboard/DPI checks and live configurable shading tests. The older collapsed-history/debug-feed assertions are deliberately updated to the new requested UX; bank accounting remains unchanged.

| Added suite | Tests | Evidence |
| --- | ---: | --- |
| BankPolishTest | 15 | Equal two-column cards, placeholders, component identity, history navigation/correction placement, RESET gate, sprite fallback/cache/failure, wrapping/resize/minimum bounds, one-boundary cached observations/null retry, monotonic sample phases, offscreen layout/text fit |
| LockedTintSettingsTest | 6 | #333333/50% default and green text, custom live independent settings, clamping, transparent/positive behavior, full-bounds partial clipping, signed green pixels and black text shadow over coin colors |
| BankPerformanceTest | 3 | 1,200-ID aggregation, 256 forced commits, early/late history costs, real worker/client separation, 100 FIFO transitions, read-only manual/automatic pagination through 1,240 changes |
| BankUiBenchmarkTest | 1 | Same 1,200-ID/100-refresh workload before and after, wall/EDT CPU diagnostic output |

The latest clean build passed **544 tests**. Synthetic timings are not a claim that native OSRS lag is fixed. No agent operated the game. Manually compare enabled/disabled native quantities and virtual values; temporarily enable sampled DEBUG timings if needed. Test start/reset cancellation without resetting the authoritative accepted run. All local schema/history/legacy snapshot safeguards remain.

## UI polish pass

BankUiPolishPassTest adds 19 cases: new config defaults and custom override preservation; exact verified item IDs; centered/cached/fallback sprites; transparent purple navigation artwork; two equal Played Elsewhere columns and right-tile navigation; solid red reset normal/hover/pressed painting; visible unchecked/checked checkbox pixels, distinct hover/focus, synthetic label click and keyboard Space; default-unchecked/start attestation and missing-bank gates; unchanged setup observation and 225-pixel layout. LockedTintSettingsTest adds the twentieth new case for green signed text over gold/yellow fixtures. Existing M3 clipping, full controller/persistence/overlay and performance regressions remain passing.

Follow [MILESTONE_4_UI_POLISH.md](MILESTONE_4_UI_POLISH.md) for exact manual steps, saved-setting preservation, live icon/contrast/scaling checks and safe dialog cancellation. Offscreen setup/dashboard PNGs under build/reports are synthetic test artifacts, not live acceptance. There is no accounting/schema/scheduling change in this pass.

## M5 regression and acceptance

| Suite | Evidence |
| --- | --- |
| OsrsQuantityFormatterTest | Every signed integer from 0–9,999, all requested examples, suffix boundaries/no rounding, integer millions for billions, both long bounds, negative-label right alignment |
| WealthModelTest | Positive/negative/zero market values; unknown versus verified zero; stale quotes; fixed historical prices and unknown gains; high-water conservation through cycles, legacy returns and corrections; exact coins once; explicit expense validation; overflow and offsetting large values; note price identities, cache/read/fetch/expiry/failure behavior; event duplicate rejection; exact/compact money |
| WealthPipelineTest | Real observer→controller→Filepath journal/checkpoint→overlay→dashboard; automatic gains/cycles/legacy returns/manual correction; price refresh with no save; rapid changes/no HTTP; financial restart/offline comparison; durable acknowledgment/failure/retry; explicit expense/stale intent; character switch; two-column monetary/purpose UI and narrow priced-subtotal layout |
| FinancialPersistenceTest | Fixed quote/state reload, genuine synthetic format-5 migration of checkpoints/journals, exact-byte archives and unchanged run/start/P/B/initial/history, no migration replay, interrupted migration/save and retry, damaged old journal/archive, missing financial payload, idempotent event save, forged totals rejected, explicit expense amount/reason |
| WealthPerformanceTest | 1,200 priced types, 1,296 durable valuation events, 96 subsequent forced financial commits, cached current-value CPU versus worker IO timing; dedicated cancellable pricing scheduler thread |
| Retained M1–M4 suites | Client/storage/EDT boundaries, 1,200-ID/256-commit benchmark and 100 FIFO changes, 1,240-row read-only history, migration/recovery/isolation, setup/RESET safeguards, custom colors/tint, tabs/search/partial clipping and one overlay registration |

Tests use fake bulk price providers and temporary Filepath roots. They do not require RuneScape credentials, actual saves, network or game input. Cached price refresh is not a bank transaction and must not change revision/sequence/file history. High-water/quotes/totals must never appear before successful quantity+financial commit. No inventory/equipment observation or return workflow is reintroduced.

Run the full clean build; then follow [M5 manual acceptance](MILESTONE_5.md): imported baseline disclosure, integer item counters, Papyrus/purpose wording, signed/current/subtotal/stale/exact values, high-water cycle/new-maximum behavior, Bank GP legacy withdrawal/return/new cash, optional confirmed statistical expense and cancel, unchanged automatic mobile return, restart/history/isolation, and live responsiveness/scroll/drag/config regression. M5 is not accepted from a build alone. M6 is unauthorized.

## Final M5 UI/quantity polish

MilestoneFivePolishTest adds 22 cases: exact/date-free/escaped bounded tooltips, singular/plural/missing values, all financial tile surfaces, actual tooltip size, absent ordinary status subtitles and preserved errors, centered equal-height Played Elsewhere tiles, centered red reset, compact instructions/checkbox, centered green Start states/safeguards, original start/typed-reset confirmations and cancellation, visible-card sizing and synthetic previews. OsrsQuantityFormatterTest adds 5 cases: extended thousands range through 9,999,999, uppercase K/M threshold/sign matrices, no decimals/rounding, fitted wider/extreme labels with unchanged baseline/clip/graphics, and unchanged monetary formatting.

Expected labels now include 10000→10K, 999999→999K, 1000000→1000K, 1740000→1740K, 9999999→9999K, 10000000→10M and corresponding negatives. Tooltip preview is 258x62 logical pixels. The prior 611 tests retain accounting/price/persistence/cache/recovery/performance coverage. No financial or storage implementation change occurred in this pass. Use [the latest manual checklist](MILESTONE_5_POLISH.md) for real RuneLite scaling, bank labels, hover, keyboard, dialog cancellation and responsiveness; a synthetic image is not live acceptance.

## M5 final-withdrawal and presentation regressions

The latest clean build passes **654 tests, 0 failures/errors/skips, 45 suites**, retaining the previous 638 with only superseded presentation expectations updated. Added:

- BankOnlyPipelineTest: four controller/observer/real-local-persistence/actual-overlay cases for four-copy stackable and unstackable withdrawal, with native placeholders on/off. Verify final delta, -4/original-ID counter, zero physical placeholder copies, absent-slot correction search, unchanged tab/search/reopen observations, restart, returns 1/2/4, immutable baseline and no new gain events.
- WealthPipelineTest: final withdrawal to a native placeholder retains negative signed market value through restart, with unchanged financial history.
- GpFormatterTest: six cases covering all requested positive/negative examples, K/M boundaries, no decimals/rounding, every small signed integer, long limits and grouped exact tooltip detail.
- FinalCorrectionsUiTest: five cases covering a visible readable optional GE-storage note without a new observation gate; 14-pixel Start gap/nearby attestation; centered automatic-update instructions/equal tiles; shared cached Purple partyhat 1046; all four financial tiles using integer GP with exact tooltips.

Existing null/partial-bank rejection, migration/exact-byte archives, save failure/CAS/recovery, character isolation, rapid FIFO updates, 1,200-ID/large-history performance, custom colors, original quantity preservation and partial-row clipping remain covered. Physical/financial reducers, price service and persistence implementation are unchanged by this correction pass. Run the [latest manual procedure](MILESTONE_5_FINAL_CORRECTIONS.md) for real native placeholder IDs/appearance, no-placeholder behavior, net worth, restart, sprite clarity, setup/GE guidance and live banking responsiveness. Only the user can accept those behaviors in-game.

## M6 Grand Exchange validation

M1–M5 are user accepted at v0.5.0-wealth. The M6 clean build passes **723 tests, 0 failures/errors/skips, 52 suites**, preserving 655 earlier tests and adding 68 cases:

| Suite | Tests | Coverage |
| --- | ---: | --- |
| GeObservationTest | 15 | Cumulative quantities/actual reported-GP differences, independent slots, partial cancellation, duplicate/out-of-order packets, initial EMPTY, reuse/terminal regression, pre-start lifecycle without clock inference, unchanged resume context, capped/zero/exempt estimated tax |
| GeSourceLifecycleTest | 8 | Real API-interface copies, login clears, missing/late arrays, confirmed clear, reconnect, invalid/missing packets, bounds and beyond-max-cash longs |
| GePipelineTest | 25 | Source → FIFO controller → real Filepath journal/checkpoint → financial/projection path; buys/sales/partial fills/refunds/returns, concurrent budgets, high-water exclusions and unrelated/excess gains, no duplicate item/GP/tax, uncollected bank-only worth, restart/offline uncertainty, pre-start/unknown origins, acknowledgment, isolation, history sides, platinum units, 80-event FIFO and 400-event overload recovery, shared immutable overlay-label cache |
| GePersistenceTest | 9 | Roundtrip offers/fees/budgets, genuine format-6 exact-byte migration, unchanged identity/start/initial/P/B/financial state, broken data preservation, missing GE rejection, interrupted migration/save, lost acknowledgment, idempotency/CAS and bounded immutable history pages |
| GeDashboardTest | 4 | Exactly two matching GE tiles in the required location, side-specific read-only navigation, estimated-fee display without Bank GP changes |
| GeHistoryViewTest | 6 | Name search, fill/uncertainty filters, numeric sorting, read-only cells, exact GP/unknown profit/collection details, average-price presentation and page-size bounds |
| GePerformanceTest | 1 | 1,200 physical IDs and 128 forced GE commits, bounded retained state, lazy 25-row page and restart equality, worker CPU versus wall diagnostics |

The historical format-5 fixture explicitly removes the new schema-7 field before encoding old bytes; this preserves meaningful older migration coverage. Current fees remain estimates with a documented incomplete exemption catalog, not verified native tax. Initial/offline identical-slot ambiguity and late bank/trade ordering cannot establish complete history or profit. The [M6 report](MILESTONE_6.md) contains exact measurements, constraints and the required user-run manual procedure. No automated test or synthetic render establishes live correctness or responsiveness.

## M7 legacy buyback validation

Retain all 723 applicable M1–M6 tests. Added suites:

| Suite | Tests | Coverage |
| --- | ---: | --- |
| BuybackModelTest | 39 | All fixed prices and exclusions, sufficient/exact/insufficient GP, unchanged real P/raw B/L/gains, one paid copy, natural duplicates, pouch standard/Divine/Trouver upgrade/revert/note/content isolation, one family unlock, Void tier/pricing/no duplicate copy, all six complete outfits and incomplete rejection, spending/corrections/worth, purchase after earlier legacy deficits and bounded returns |
| BuybackPipelineTest | 23 | Actual observer/controller/Filepath/projection path; acknowledged-only purchase, available coin counter and worth, double confirmation, stale bank/shop, closure, insufficient budget, live-definition rejection, restart/withdraw/return, family substitution, six-piece one-commit overlay, error/retry, preserved GE and independent bank/manual history, character isolation, paid-copy floor progression through bank returns/restart |
| BuybackPersistenceTest | 10 | Genuine format-7 exact-byte migration with retained identity/start/L/P/B/financial/GE data, interrupted migration/publication, missing/corrupt payload/history/archive, idempotent lost-ack retry, stale CAS, unpaid/duplicate entitlement rejection |
| BuybackUiTest | 6 | One pouch entry through either search, categories, two matching tiles and required placement, available/spent/gain presentation, separate one-row readable history, cached sprite IDs and confirmation budget wording |
| BuybackPerformanceTest | 1 | All 26 purchases with 1,200+ IDs, 250 derived projections, 64 ordered forced bank commits, bounded current history, restart equality and separate worker CPU/wall diagnostics |

Older format-5/6 migration fixtures remove the new buyback payload before encoding genuine old bytes. No old behavior test is deleted; controller fixtures add an explicit synthetic untradeable-definition boundary. Existing full M3 clipping/settings/painter, price, GE, bank responsiveness, corruption/recovery and UI/reset regressions remain.

Follow [M7 user-only acceptance](MILESTONE_7.md) with the existing run: migrated data unchanged; enough GP/physical legacy item; cancel and unavailable cases; one confirmed payment with no real coin/item movement; one pouch family across forms/withdrawal/restart; outfits/Elite pricing only when intentionally buying them; normal banking/GE and custom overlay regressions. Do not reset, buy unnecessary OSRS items or automate game input for testing. Synthetic fixtures cannot confirm native definitions, visuals or gameplay performance.

Final M7 clean build: **802 passed, 0 failures/errors/skips, 57 suites**, including all 723 previous tests and 79 new cases. Worker and UI diagnostic measurements, full changed-file list, exact catalog IDs and known limitations are recorded in the M7 report. Manual acceptance remains pending.

## Post-M7 refinement regression coverage

Retain the 802 M1–M7 tests and adapt the old pouch-purchase fixtures to explicitly attest an empty pouch. Existing section-order assertions now use the requested exact order. No production bypass or arbitrary grant command exists.

New suites:

- RuneLitePouchReaderTest: actual six varbit pairs and rune mapping, standard/Divine slots, repeated types/large sums, zero with retained type, unavailable session/definitions and invalid mappings. Reported empty remains unreviewed.
- PouchPipelineTest: actual tick observer/controller/FIFO/Filepath/overlay/financial path; empty standard/Divine and unknown rejection; loaded preparation; earned100+legacy500; multiple types; finite/idempotent holds; excess/unrelated arrivals; explicit earned restoration; retirement without debt; pending/offline/restart/upgrade/revert; no duplicate payment; acknowledgment failure, stale revision and character isolation.
- PouchPersistenceTest: genuine format-8 paid/unpaid histories; exact archives; unchanged balances/history/payment/entitlement; one-time review without repeat payment; interrupted migration/adjustment recovery; required-field corruption and archive damage.
- PouchStateTest: confirmation, allowlist, nonnegative/conserved/overflow-safe preparation, no consumption from zero/negative bank deltas and completed-plan replay rejection.
- PouchPerformanceTest: 1,200-ID bank, finite 500-rune plan and 64 ordered commits; only 140 of 640 arrivals remain new progress, one commit per observation.
- PostM7UiTest: exact six-section order/reset-last, compact two-column aligned heights, unclipped labels and larger-font expansion, unchanged navigation/icon identities, unknown/reported-empty unchecked confirmations, named contents and relevant-only workflow.

Run the complete `./gradlew clean build` after targeted suites. See [post-M7 validation results and exact manual procedure](POST_MILESTONE_7.md). Synthetic source reports cannot establish banked varbit freshness, physical rune provenance, live responsiveness or manual acceptance.

Final post-M7 validation: 850 tests / 0 failures / 0 errors / 0 skipped, 63 suites (802 baseline + 48 new tests). Final timing data and live acceptance procedure are in docs/POST_MILESTONE_7.md.
