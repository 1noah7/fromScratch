# Test plan

M1–M4 are accepted. **M5 bank wealth and specified UI corrections await manual acceptance.** The old 417 tests remain executable, with retired controllers/readers/review code moved into test-only historical fixtures. Their old source-attribution/session-gate assertions are compatibility evidence, not the new live contract. See [fixture policy](src/test/HISTORICAL_FIXTURES.md) and [historical plan](docs/history/TEST_PLAN_MULTI_CONTAINER.md).

Run the full suite with Java 11 and the existing Wrapper:

```powershell
.\gradlew.bat clean build
```

No test needs a game account, credentials, private progression, external service or game UI automation. Tests use Filepath roots under JUnit TemporaryFolder. The production plugin JAR excludes historical fixtures. The development shadow/launcher classpath includes test fixtures but registers only the current plugin.

Accepted M4 baseline: **544 passed, 0 failures/errors/skipped, 37 suites**. M5 foundation: 611 passed. Final M5 UI/quantity polish clean build: **638 passed, 0 failures/errors/skipped, 43 suites**, adding 27 targeted tests. All applicable earlier cases remain; obsolete UI/tooltip/item-label expectations match the authorized contract. Current timings/manual steps are in [the polish report](docs/MILESTONE_5_POLISH.md). Historical tests remain compatibility evidence rather than the current live UX contract.

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

Follow [MILESTONE_4_POLISH.md](docs/MILESTONE_4_POLISH.md) for native-versus-purple timing comparison, exact diagnostic phase fields, two-column tile/icon validation, all dialog/keyboard/DPI checks and live configurable shading tests. The older collapsed-history/debug-feed assertions are deliberately updated to the new requested UX; bank accounting remains unchanged.

| Added suite | Tests | Evidence |
| --- | ---: | --- |
| BankPolishTest | 15 | Equal two-column cards, placeholders, component identity, history navigation/correction placement, RESET gate, sprite fallback/cache/failure, wrapping/resize/minimum bounds, one-boundary cached observations/null retry, monotonic sample phases, offscreen layout/text fit |
| LockedTintSettingsTest | 6 | #333333/50% default and green text, custom live independent settings, clamping, transparent/positive behavior, full-bounds partial clipping, signed green pixels and black text shadow over coin colors |
| BankPerformanceTest | 3 | 1,200-ID aggregation, 256 forced commits, early/late history costs, real worker/client separation, 100 FIFO transitions, read-only manual/automatic pagination through 1,240 changes |
| BankUiBenchmarkTest | 1 | Same 1,200-ID/100-refresh workload before and after, wall/EDT CPU diagnostic output |

The latest clean build passed **544 tests**. Synthetic timings are not a claim that native OSRS lag is fixed. No agent operated the game. Manually compare enabled/disabled native quantities and virtual values; temporarily enable sampled DEBUG timings if needed. Test start/reset cancellation without resetting the authoritative accepted run. All local schema/history/legacy snapshot safeguards remain.

## UI polish pass

BankUiPolishPassTest adds 19 cases: new config defaults and custom override preservation; exact verified item IDs; centered/cached/fallback sprites; transparent purple navigation artwork; two equal Played Elsewhere columns and right-tile navigation; solid red reset normal/hover/pressed painting; visible unchecked/checked checkbox pixels, distinct hover/focus, synthetic label click and keyboard Space; default-unchecked/start attestation and missing-bank gates; unchanged setup observation and 225-pixel layout. LockedTintSettingsTest adds the twentieth new case for green signed text over gold/yellow fixtures. Existing M3 clipping, full controller/persistence/overlay and performance regressions remain passing.

Follow [MILESTONE_4_UI_POLISH.md](docs/MILESTONE_4_UI_POLISH.md) for exact manual steps, saved-setting preservation, live icon/contrast/scaling checks and safe dialog cancellation. Offscreen setup/dashboard PNGs under build/reports are synthetic test artifacts, not live acceptance. There is no accounting/schema/scheduling change in this pass.

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

Run the full clean build; then follow [M5 manual acceptance](docs/MILESTONE_5.md): imported baseline disclosure, integer item counters, Papyrus/purpose wording, signed/current/subtotal/stale/exact values, high-water cycle/new-maximum behavior, Bank GP legacy withdrawal/return/new cash, optional confirmed statistical expense and cancel, unchanged automatic mobile return, restart/history/isolation, and live responsiveness/scroll/drag/config regression. M5 is not accepted from a build alone. M6 is unauthorized.

## Final M5 UI/quantity polish

MilestoneFivePolishTest adds 22 cases: exact/date-free/escaped bounded tooltips, singular/plural/missing values, all financial tile surfaces, actual tooltip size, absent ordinary status subtitles and preserved errors, centered equal-height Played Elsewhere tiles, centered red reset, compact instructions/checkbox, centered green Start states/safeguards, original start/typed-reset confirmations and cancellation, visible-card sizing and synthetic previews. OsrsQuantityFormatterTest adds 5 cases: extended thousands range through 9,999,999, uppercase K/M threshold/sign matrices, no decimals/rounding, fitted wider/extreme labels with unchanged baseline/clip/graphics, and unchanged monetary formatting.

Expected labels now include 10000→10K, 999999→999K, 1000000→1000K, 1740000→1740K, 9999999→9999K, 10000000→10M and corresponding negatives. Tooltip preview is 258x62 logical pixels. The prior 611 tests retain accounting/price/persistence/cache/recovery/performance coverage. No financial or storage implementation change occurred in this pass. Use [the latest manual checklist](docs/MILESTONE_5_POLISH.md) for real RuneLite scaling, bank labels, hover, keyboard, dialog cancellation and responsiveness; a synthetic image is not live acceptance.

## M5 final-withdrawal and presentation regressions

The latest clean build passes **654 tests, 0 failures/errors/skips, 45 suites**, retaining the previous 638 with only superseded presentation expectations updated. Added:

- BankOnlyPipelineTest: four controller/observer/real-local-persistence/actual-overlay cases for four-copy stackable and unstackable withdrawal, with native placeholders on/off. Verify final delta, -4/original-ID counter, zero physical placeholder copies, absent-slot correction search, unchanged tab/search/reopen observations, restart, returns 1/2/4, immutable baseline and no new gain events.
- WealthPipelineTest: final withdrawal to a native placeholder retains negative signed market value through restart, with unchanged financial history.
- GpFormatterTest: six cases covering all requested positive/negative examples, K/M boundaries, no decimals/rounding, every small signed integer, long limits and grouped exact tooltip detail.
- FinalCorrectionsUiTest: five cases covering a visible readable optional GE-storage note without a new observation gate; 14-pixel Start gap/nearby attestation; centered automatic-update instructions/equal tiles; shared cached Purple partyhat 1046; all four financial tiles using integer GP with exact tooltips.

Existing null/partial-bank rejection, migration/exact-byte archives, save failure/CAS/recovery, character isolation, rapid FIFO updates, 1,200-ID/large-history performance, custom colors, original quantity preservation and partial-row clipping remain covered. Physical/financial reducers, price service and persistence implementation are unchanged by this correction pass. Run the [latest manual procedure](docs/MILESTONE_5_FINAL_CORRECTIONS.md) for real native placeholder IDs/appearance, no-placeholder behavior, net worth, restart, sprite clarity, setup/GE guidance and live banking responsiveness. Only the user can accept those behaviors in-game.
