# Milestone 5 — Final UI and quantity corrections

> Historical presentation-pass evidence. [The subsequent final corrections](MILESTONE_5_FINAL_CORRECTIONS.md) replace Papyrus/Diary with Purple partyhat on both manual tiles, share integer formatting for GP, add explicit automatic-update text/storage guidance and correct native-placeholder balance lookup. Earlier pass timings/test totals below remain historical.

By **Pilots r kwl**. Targeted presentation pass; **manual acceptance remains pending**. Bank-only accounting, financial formulas, high-water recognition, historical prices, cache/worker behavior, format 6 and local saves are unchanged. No actual progression was read/reset, and no game input, commit, push, tag, publication or M6 work occurred.

## Tooltips

BankTooltips builds escaped, width-limited Swing HTML. Estimated Value Gained shows exact known GP, "Tracks bank progress, not every acquisition," and a grammatically correct unknown historical-price count when needed. There is no measurement timestamp, ISO date, nanoseconds, schema or migration diagnostic in the ordinary tooltip. Underlying dates/quotes remain stored unchanged.

Net worth, Bank GP and recorded-expense tips follow the same concise format. Current-price gaps/older retained prices remain clear; null/unavailable/overflow presentation does not manufacture a value. HTML title/body text is escaped. All label/icon/value surfaces of each financial tile share the same explanation.

Actual JToolTip preferred size is tested at approximately 250–300 logical pixels wide. The legacy Swing HTML renderer expands CSS px widths, so the implementation uses logical pt widths and a standard Dialog font. An offscreen tooltip preview verifies natural wrapping and readable four-line gain information. Actual RuneLite theme/DPI/window-boundary behavior still requires user testing.

## Sidebar and setup

- Ordinary Not Started/Active subtitle components are removed, leaving no status-row gap. Run state is untouched; actionable error/recovery notices and Retry Loading remain.
- Played Elsewhere's left tile is centered horizontally/vertically, with a paragraph break between the question and banking instruction. It remains noninteractive. The matching right tile retains Manual Bank Corrections, Fix an incorrect count, Papyrus and its existing action. Both retain the two-column equal-height row.
- Reset Progress is centered in a fixed-width wrapper inside its unchanged danger border/warning area. Solid red, white text, hover/pressed styling and the original exact-RESET confirmation/archive/revision safeguards remain. Opening/cancelling is non-mutating.
- The setup gap came from a textarea height frozen at a narrower measuring width. Intrinsic-height wrapped labels now place the confirmation panel immediately below instruction 2. Checkbox dimensions follow preferred font height rather than a fixed cap; outline, checkmark, focus/hover and whole-row/Space activation remain.
- Setup card sizing follows the visible card, preventing the hidden dashboard from reserving excess setup height. Layout invalidation occurs only when switching cards, not on ordinary bank events.
- BankPrimaryButton supplies centered white text on solid green (enabled), lighter hover, darker pressed and readable gray disabled states. It is centered, at least 186x44 logical pixels, within the sidebar. It starts disabled and stays gated by attestation, complete bank and existing safe state. Clicking uses the original Start From Scratch? / Start Run / Cancel dialog and the unchanged controller revalidation. A package-private confirmation test seam verifies both cancellation paths without opening a game or desktop dialog; production always uses BankDialogs.

## Item quantity formatting

| Absolute quantity | Presentation |
| --- | --- |
| 0–9,999 | Exact integer, no commas |
| 10,000–9,999,999 | Integer division by 1,000 + uppercase K |
| 10,000,000+ | Integer division by 1,000,000 + uppercase M |

Examples: 1740, 9999, 10K, 999K, **1000K**, **1740K**, 9999K, 10M, 25M, 100M. Negative values preserve the sign at the same thresholds: -1740, -10K, -1000K, -10M. Billions remain integer millions. Long bounds use division without negation overflow. There are no decimals or upward rounding.

The dedicated VirtualQuantityFormatter is still used by cached bank projections. Monetary GpFormatter is **unchanged**: an item quantity of 1,740,000 is 1740K, while 1,740,000 GP can remain 1.74m GP with exact details.

Accepted item bounds, viewport intersection/clipping, normal quantity labels and locked tint are preserved. Normal labels use the existing path. Exceptional labels exceeding available slot width are drawn with a child Graphics2D horizontal scale, keeping the same bottom-right anchor/baseline and clip. Parent transforms/font/color/clip are restored. Extreme long labels can be visually dense at tiny slot sizes; exact counts remain in the correction interface. No item IDs, sprites, widgets, click zones, menus or physical quantities are changed. No prices, disk writes or formatting-map reconstruction is added to per-frame rendering.

## Files changed in this pass

- Added production: bank/BankTooltips.java, bank/BankPrimaryButton.java.
- Changed production: bank/BankPanel.java, bank/BankTile.java, bank/BankAttestation.java; overlay/VirtualQuantityFormatter.java, overlay/BankItemPainter.java.
- Added tests: bank/MilestoneFivePolishTest.java. Extended OsrsQuantityFormatterTest; adapted BankItemPainterTest, LegacyBankDeficitsTest, BankUiPolishPassTest, BankPanelTest and WealthPipelineTest expectations for requested wording/thresholds.
- Documentation: README, AGENTS presentation rule, ARCHITECTURE, TEST_PLAN, CHANGELOG, MILESTONE_5 and this report. Existing assets, config keys, Gradle Wrapper/dependencies/CI, metadata and licensing remain unchanged.

BankEngine, FinancialEngine, FinancialState, BankController, observation adapters, pricing service, persistence implementation/codecs and financial formulas were not edited in this pass. All tests use synthetic data and temporary storage. Existing staged M4 work and prior unstaged M5 work are preserved.

## Automated verification

Added coverage includes exact/concise/date-free/escaped/wrapped tooltips and grammar; actual tooltip size; all financial tile surfaces; missing values; centered two-column instructions/actions; absent ordinary status subtitles and visible errors; setup gap/visible-card sizing; unchecked/default/start gating; green/white/disabled/hover/pressed painting; both original confirmation paths and non-mutating Cancel; item thresholds/signs/long bounds/no decimals/no rounding; fitted painting/right-edge/baseline/graphics state; separate monetary formatting; synthetic narrow-sidebar screenshots.

Final `.\gradlew.bat clean build`: **BUILD SUCCESSFUL in 59 seconds**, 7 tasks executed. **638 tests passed, 0 failures, 0 errors, 0 skipped, 43 suites**. All previous 611 cases remain, with obsolete item-label/tooltip expectations updated. Added **27 tests**: MilestoneFivePolishTest has 22 new cases; OsrsQuantityFormatterTest adds 5 cases to its existing 6. Full controller→ledger→dashboard, gain/cache/expense/migration/recovery/character-isolation, clipping/tint/custom-color and performance suites pass.

The rendered gain tooltip is **258x62 logical pixels**, within the requested width. Offscreen unchecked/checked-green setup and active-panel previews confirm compact spacing, centered actions and complete text. Ordinary setup preferred height excludes the hidden dashboard. HTML escaping and actual Swing preferred-size assertions complement pixel/layout checks.

Final synthetic diagnostic output:

```text
BANK_UI_1200_ITEMS updates=100 wall_ms=8.677 edt_cpu_ms=0.000
BANK_1200_ITEMS capture_100_ms=22.420 commits=256 worker_wall_ms=14073.683 worker_cpu_ms=4468.750 io_and_scheduling_ms=9604.933 early_mean_ms=49.928 late_mean_ms=49.165
WEALTH_1200_ITEMS value_100_wall_ms=28.817 value_cpu_ms=31.250 financial_commits=96 worker_wall_ms=8029.911 worker_cpu_ms=3296.875 io_scheduling_ms=4733.036 durable_events=1296
```

100 sidebar refreshes took 8.677 ms total; the previous M5 sample was 12.100 ms. These are variable synthetic samples, not a guaranteed speedup or native banking/FPS result. Short Windows CPU timing has coarse quanta, explaining zero or CPU greater than wall time. IO/scheduling is a wall-minus-CPU estimate, not direct disk-only timing. No new IO, network or history scanning was introduced. Pre-existing nonfatal build notes remain: unchecked development launcher operations and Gradle APIs deprecated for Gradle 9. Java 11, RuneLite 1.13.3 and Wrapper 8.10 are unchanged.

Ignored build/reports previews are synthetic Swing components with fallback icons, not in-game screenshots. The unchecked/checked setup states, enabled green button, centered active dashboard and compact tooltip were visually inspected. A passing build is not live acceptance or a native latency measurement.

## Manual acceptance

1. Keep the existing run and launch `.\gradlew.bat run` from the project root; the agent can launch it on request. Follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Do not reset the accepted run for UI testing.
2. Hover Total Value Gained over the value, label and icon. Expect a short Estimated Value Gained tip, exact GP and correct singular/plural unknown-price wording, naturally wrapped around 250–300 logical pixels. No technical date or migration text. Inspect net worth, Bank GP and expense tips too; estimates/unknown/older prices must remain accurate.
3. Check the title goes directly to dashboard tiles without Active. Played Elsewhere instructions must be centered in the left tile; the equally tall right tile still opens Manual Bank Corrections with Papyrus. Ordinary mobile return still needs only opening the bank.
4. The bottom red reset button must be centered with readable text and hover/pressed feedback. Open and Cancel/Escape/window-close its dialog. Typed RESET remains required; do not confirm a reset on the accepted run merely to test.
5. On a separate unused/not-started character if available, confirm no Not Started subtitle, compact instructions/checkbox spacing and a visible unchecked checkbox. Start should be gray/disabled until both attestation and a complete bank are ready. Click the label or use Space: the checkbox shows its tick and Start becomes green/white. Start must open the original confirmation; Cancel must not create a run. Unchecking, missing bank data, loading or errors must keep Start disabled. No inventory/equipment API checks are introduced.
6. Inspect available stack boundaries: 1740→1740, 9999→9999, 10000→10K, 999999→999K, **1000000→1000K**, 1740000→1740K, 9999999→9999K, 10000000→10M and 25600000→25M. Negative progress uses the same thresholds/sign. Do not create artificial accounting corrections solely to manufacture test amounts in the accepted run.
7. Exercise top/bottom partial rows, tabs/search/empty search, rapid scroll, dragging, resize/UI scale and custom text/tint settings. Wider counters must fit without moving their right edge or affecting normal labels/controls. Close/disable/re-enable: remove/re-register once. Real-client clarity at your scale remains a manual check.
8. Perform ordinary deposits/withdrawals, restart and open the bank again. Signed counts, net worth, Bank GP, historical gains/quotes, corrections/history, items count and duration must behave as before. Compare native bank response and counter response enabled/disabled if concerned. No accounting, save or monetary-format change is expected.

No new Plugin Hub-sensitive gameplay behavior, network access, dependency or data transmission is introduced. Publication review remains separate.

## Git handoff

Branch master, HEAD 5b7c49f. The **124 pre-existing staged paths** are preserved; no staging or commit was performed. Entire workspace currently has **33 unstaged tracked changes and 21 untracked paths**, including prior M5 work. This pass adds two production UI helpers, one test suite and this report; other edits are scoped above. One unrelated pre-existing untracked file remains untouched. `git diff --check` passes. No actual save was inspected, migrated or reset by development work. Stop for user-run manual acceptance.
