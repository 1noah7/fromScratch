# Milestone 4 — Final dashboard, dialogs, shading and performance polish

> Historical performance/dialog evidence. The subsequent [UI polish pass](MILESTONE_4_UI_POLISH.md) supersedes the appearance defaults, history icon choices, Played Elsewhere layout and attestation/reset styling below. Accounting, scheduling and format 5 remain unchanged.

By **Pilots r kwl**. Bank-only accounting is confirmed working in live gameplay. **This polish pass awaits manual acceptance.** No M5, real-run reset, private-save inspection, commit, push, tag or publication occurred. Java 11, RuneLite 1.13.3, Gradle Wrapper, format-5 saves, local-only storage and signed accounting are preserved.

## Banking-delay diagnosis

A synthetic real-controller scenario was debugged in IntelliJ with read-only logpoints, then one suspension to retain the fast test long enough to drain events. Runtime evidence:

- A bank change was signaled at test tick 4.
- Its first scan at tick 5 reset changedAt to 5.
- The predicate tick <= changedAt + 1 rejected ticks 5 and 6.
- Capture became Ready only at tick 7. Stack: BankOnlyPipelineTest → BankController.tick → BankObserver.observe.

This proves an avoidable **three-boundary scheduling path** in the plugin. Given the documented approximately 600 ms GameTick, the old gate could add roughly **1.2–1.8 seconds before persistence**, plus storage/callback time. Those seconds are derived from the scheduling rule, not a measured real-client deposit. The actual API GameTick documentation states that it occurs after packet processing.

Ordinary disk/JSON work was already off the client thread; no blocking client/EDT filesystem path was found. The sidebar did rebuild components per publication, but the measured cost below was far smaller than two seconds. Full journal chains are verified on load; ordinary saves read only current/head and append a new segment, not the entire history. No prices are queried. Sprite requests now occur only when building the UI and use RuneLite caching.

**Native-bank uncertainty:** no agent interacted with RuneScape. We cannot establish whether the original OSRS sprite/physical quantity itself appeared late, whether only purple counters lagged, or whether network/other plugins caused native delay. The plugin does not alter native quantities or bank actions. An enabled/disabled user comparison is still required; the two-second live symptom is not declared fixed from tests.

## Responsiveness changes

- Capture a complete mounted, visible, PIN-free normal bank at the first GameTick boundary after invalidation. Null/partial data remains unknown; empty remains valid. No first-scan restart/two-extra-tick wait.
- Cache unchanged immutable bank observations until BANK/interface invalidation. Already-open banks and missing-data retry still work. Search/tab/scroll widgets never supply accounting quantities.
- Move automatic difference calculation, candidate/audit construction and serialization/save onto the single storage worker.
- Queue distinct complete captured snapshots FIFO while a save is pending. Drain after acknowledgment without waiting another game tick. A real-worker test preserves 100 sequential snapshot transitions and their transaction sequence. Same-tick raw events still net-batch under the existing contract.
- Keep published bank balances/cursors strictly committed. Failed writes, CAS, migration, archive, recovery and character/lifecycle safeguards remain. A 256-observation backlog stops visibly rather than silently overwriting trusted state; reload recovery compares current bank with the durable anchor.
- Reuse dashboard components. Coalesce only EDT presentation callbacks, never ledger operations. Reuse the overlay projection until the committed revision changes.

## Measurements and their limits

Synthetic JUnit data only; no account, game input or real save. Same 1,200-ID bank fixture and 100-refresh UI workload, with 20 warm-up updates. Filesystem samples use real Filepath checkpoints, forced writes and journals in a temporary local directory.

| Measurement | Before | After |
| --- | ---: | ---: |
| Scheduling after invalidation | First scan restarts two-tick gate; Ready at third boundary | Ready at first following GameTick |
| Scheduling contribution at 600 ms/tick (derived) | About 1.2–1.8 s before IO | About 0–0.6 s before IO; no arbitrary debounce |
| 100 sidebar updates, 1,200 IDs (wall) | 65.389 ms | See final build measurements below |
| Native OSRS input → physical bank item | Not measured | Not measured; user comparison required |

Representative targeted run: 100 bank aggregations 20.795 ms; 256 durable commits 11,397.230 ms wall / 3,234.375 ms worker CPU; first 32 mean 50.024 ms, last 32 mean 42.279 ms. The subsequent clean build is recorded below. Ordinary-write time did not grow with the referenced history in these samples. These are diagnostics, not portable performance thresholds or live FPS claims.

Wall minus CPU includes filesystem waits, scheduling and possible GC; it is an **IO/scheduling proxy**, not an isolated disk measurement. Windows CPU timing is coarse: the refreshed UI can report 0 CPU ms at that resolution, which does not mean zero CPU cost. Aggregate fixtures use synthetic Item arrays and a simple placeholder predicate; actual item-definition/API costs are measured by live diagnostics. UI benchmarks use the bundled fallback sprites; real asynchronous sprite completion is a manual check.

## Opt-in live timings

Enable **Development: bank diagnostics** temporarily in the plugin configuration, with DEBUG logging (the development Gradle run already supplies --debug). At most one sample starts per five seconds; a sampled commit and final presentation produce bounded DEBUG lines. No account IDs, inventories, item lists or private paths are logged.

Phases are elapsed monotonic milliseconds from EVENT:

`EVENT → CAPTURE → AGGREGATE → DIFFERENCE → PREPARED → WRITE_START → WRITE_END → PUBLISHED → OVERLAY / SWING`

CAPTURE records an available full API array; AGGREGATE follows validated aggregation. DIFFERENCE/PREPARED occur on the worker, WRITE spans validation/JSON/filesystem work, PUBLISHED follows durable acknowledgment on the client, OVERLAY is the plugin draw pass and SWING is its EDT update. A failed save logs save_failed with the phases reached. An offscreen/closed bank or coalesced superseded frame can leave presentation pending; that does not imply missing persistence. These phases cannot measure server/input/native-OSRS painting before the container event.

For a useful isolated sample, wait five seconds, deposit one item, wait for the purple count, then read From Scratch timing lines. If latency persists, report the elapsed values plus whether the normal OSRS item/quantity was already visible. Repeat the same action manually with the plugin disabled. Disable diagnostics afterward. Never supply private bank snapshots or credentials.

## Dashboard and OSRS icons

Six equal statistic cards use two columns at normal RuneLite sidebar width, compact padding, purple/gold details and RuneLite fonts. Values/components update in place; the panel scrolls as a whole, with no inner statistic scrollbars. Top content is only From Scratch and concise status.

| Tile | Verified gameval ItemID | Behavior |
| --- | --- | --- |
| Total Value Gained | PLATINUM (13204) | Coming soon |
| Current Bank Net Worth | CASKET (405) | Coming soon |
| Spendable GP | COINS (995) | Coming soon |
| Items Unlocked | CRYSTAL_KEY (989) | Count of currently positive bank IDs |
| Active Run Duration | GIANT_STOPWATCH (27414) | Saved-start elapsed duration, including offline time |
| Total Value Lost / Spent | ASHES (592) | Coming soon |

IDs/signatures were checked in actual resolved 1.13.3 sources. ItemManager.getImage returns supported cached AsyncBufferedImage sprites; addTo(JLabel) handles repaint on completion. Each icon is requested once per UI icon cache. A missing/failed sprite uses the bundled purple coin icon. No remote images, copied assets, prices or image-generation dependency were added.

Bank History is a permanent heading with Automatic Changes and Manual Corrections tiles, displaying concise recent counts. Their read-only dialogs load 25 named signed rows at a time from the immutable journal, with timestamps, page-local search/Gains/Losses, sorting, Previous/Next/Close and Escape. At most 64 segments are examined per page request; older files are not rendered all at once or rewritten. A 1,240-entry regression verifies complete pagination and automatic/manual separation.

Played Elsewhere follows history, with the requested short automatic-update description and Bank Corrections. The internal host-session summary is preserved but its technical On return button is removed. No return workflow or personal-container gate was added.

Reset Progress is the absolute bottom danger area, with red accents and a short explanation. Its separate confirmation requires exact typed RESET; opening, closing or cancelling is non-mutating. Archive-before-reset and all prior backups remain.

## Dialog layout

The old short confirmation used a zero-row JTextArea inside JScrollPane, so pack produced a small clipped viewport. Shared BankDialogs now provides wrapped standard Dialog-font text with measured preferred height, content sizing, sensible minimums, resizing, display-bound limits and centering relative to the RuneLite owner. Short explanations have no scroll pane; only content exceeding the display gets scrolling. Buttons remain outside scrolling. Enter uses the default action, Escape/Cancel/window close dismiss, and an accepted action disables itself against double confirmation.

Start From Scratch? now reads:

> Your current bank will become your legacy baseline. All virtual bank balances will start at zero.

Buttons: **Start Run** / **Cancel**. Bank Corrections and history use resizable layouts and appropriate scrollable lists/tables; their explanatory text/actions remain visible. Revision/current-bank checks remain in the controller, not bypassed by UI styling. Dialog placement, high-DPI behavior and keyboard behavior still need manual desktop testing.

## Locked-item shading

New compatible config keys: lockedItemBackgroundColor = **#444444**, lockedItemBackgroundOpacity = **45** (0–100). The default composite uses alpha 115/255; darker tint RGB reduces outlines/midtones slightly while retaining about 55% original contrast. Arbitrary imported opacity is clamped. Color alpha is separated from the explicit opacity setting. The purple text color remains independent.

Existing locked-item toggle remains; ConfigChanged replaces immutable settings immediately. Drawing uses the same full item bounds and accepted top/bottom intersection clipping. Zero/negative shade, positive stays normal. No sprite/quantity/ID/widget/menu/input mutation. No sprite processing during rendering.

## Validation, files and remaining acceptance

Current build evidence is appended below after the final complete run. Prior 500 tests remain, with old UI assertions updated to the deliberately changed design. New tests cover reusable equal tiles/placeholders/icons/fallbacks/navigation, correction placement/danger confirmation, wrapping/resize/minimum bounds, offscreen width/text fit, monotonic phases, single-boundary observation/cache/retry, custom/default tint, 1,200-ID workloads, real off-client-thread writes, 100 ordered transitions and 1,240-entry pagination. Existing atomicity/recovery/migration/isolation and M3 clipping tests are retained. Offscreen Java2D inspection uses only the synthetic sidebar with fallback icons; it is not an in-game screenshot.

Production changes: BankController/Observer/Observation/View/Engine/Source/Sidebar/Panel/Actions, new BankTimings/Icons/Tile/Dialogs/History/HistoryDialog/CorrectionDialog, BankRepository read-only history paging, config and overlay settings/painter/projection timing. No save-codec/schema/equation change. Test changes: BankPanelTest, BankPolishTest, BankUiBenchmarkTest, BankPerformanceTest, LockedTintSettingsTest. README, architecture, accounting, agent guidance, test plan, changelog and M4 reports are updated.

Manual checklist:

1. Launch with `.\gradlew.bat run` and follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Keep the existing run; do not reset for testing. The agent can offer to launch but never operates RuneScape.
2. Compare normal OSRS quantity/sprite response separately from purple response, first enabled, then disabled, manually repeating comparable deposits/withdrawals. Use isolated opt-in timing samples if delayed. No live-native latency claim is made yet.
3. Verify legacy withdrawal/return, new deposits, withdrawals, burning outside bank/reopen, rapid sequential updates, restart/mobile return and saved corrections. Purple counts must reflect committed signed state without extra tab/review requirements.
4. Inspect the two-column six-card dashboard, distinct genuine OSRS icons after loading, correct positive-ID count/duration, Coming soon financials, no timestamps/feed/debug buttons and no duplicate components on refresh.
5. Open both history tiles, check correct categories/names/signs/times, sort/filter/search this page, page through older changes, close/back/Escape. These views must not alter balances. Corrections remain under Played Elsewhere, with search/preview/confirm/cancel/stale guards.
6. Open Start confirmation only on a separate new run/test character if desired; check concise text/Start Run/Cancel and no unnecessary scrollbar. Audit corrections/history/reset dialogs at 100/125/150/200% scaling, resizing and on smaller/multiple displays; buttons/text/Enter/Escape/window close should work. Cancel reset; a non-RESET input must never enable reset. Do not reset the accepted run merely to test.
7. Try locked tint color and 0/25/45/100% opacity live; reset to #444444 / 45% for evaluation. Verify purple color remains unchanged, original quantities remain readable, positive sprites stay normal, partial top/bottom rows/tabs/search/scroll/resize/drag are aligned, and closure/disable removes effects.

Known limits: one game-tick server boundary plus actual storage/callback/paint time remains; slow disks/other plugins/network can still delay response. Complete same-tick changes are net-batched, not invented click history. Queue overload stops visibly for recovery. History search/filter is page-local, with a bounded recent count on the dashboard. Financial/lifetime statistics remain unimplemented. Exact live timing, genuine-sprite appearance, dialogs/high DPI and final tint require user acceptance. No external service, cloud, gameplay automation, native/reflection code or conditional menus were added; Plugin Hub approval remains separate.

## Final build evidence

Final `.\gradlew.bat clean build`: **BUILD SUCCESSFUL**, 7 tasks executed; **524 passed, 0 failures, 0 errors, 0 skipped, 36 suites**. Previous 500 cases are retained with old UI expectations updated for this authorized design. Added 24 cases: BankPolishTest 15, LockedTintSettingsTest 5, BankPerformanceTest 3, BankUiBenchmarkTest 1. Existing core pipeline, atomic migration/recovery, character isolation and partial-row rendering regressions pass.

Final clean-run benchmark output:

```text
BANK_UI_1200_ITEMS updates=100 wall_ms=6.727 edt_cpu_ms=15.625
BANK_1200_ITEMS capture_100_ms=26.868 commits=256 worker_wall_ms=15385.671 worker_cpu_ms=4812.500 io_and_scheduling_ms=10573.171 early_mean_ms=56.049 late_mean_ms=57.800
```

Thus 100 sidebar refreshes fell from 65.389 ms to 6.727 ms in the final sample. Other revised runs ranged from 2.500 to 4.551 ms; this variability is why these are reported as measurements, not guarantees. Aggregation averaged 0.269 ms per synthetic bank in the final run. Windows CPU sampling uses a coarse quantum (approximately 15.625 ms here), so a very short UI measurement can report zero or a CPU delta larger than measured wall time. Do not interpret that short CPU number as precise. The longer worker CPU sample is more informative. File writes/serialization remain outside client/EDT paths and keep forced durability.

The 1,240-entry history test reads at most 25 rows per page and verifies the full count/category separation without mutating progression. Offscreen inspection at 225-pixel sidebar width verifies equal cards, text fit, correct section alignment and no repeated component reconstruction. Genuine OSRS sprite appearance, window chrome, DPI and native responsiveness still require the user.

Nonfatal existing build notes: unchecked development-launcher operations and deprecated Gradle APIs for Gradle 9; the Wrapper remains 8.10. No CI/server/account credentials are needed. Debugger sessions and agent breakpoints were removed; the two pre-existing disabled user exception breakpoints were preserved. `git diff --check` passes.

## Git status at handoff

Branch master, HEAD 5b7c49f (accepted M3). Working tree: 24 modified tracked paths, 16 prior main-source removals/moves, 91 new/untracked paths, zero staged changes. This includes earlier uncommitted M4 work. See [file inventory](MILESTONE_4_FILES.md). No commit/push/tag/publication, actual-run reset or M5 work. No retired multi-container controller/review classes are present in the production JAR. Stop for manual acceptance.
