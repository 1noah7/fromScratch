# Milestone 4 — UI polish pass

> Accepted by the user before authorizing [Milestone 5](MILESTONE_5.md). The remaining text records the original M4 handoff, not the current M5 validation status.

By **Pilots r kwl**. Bank-only accounting works in live gameplay; **this UI pass awaits user acceptance**. M5 has not begun. No commit, push, tag, publication, real-run reset, private-save inspection or RuneScape input occurred.

## Scope and preservation

This pass changes configuration defaults, bundled artwork and Swing presentation. It does not change BankEngine, BankController, BankObserver, BankRepository, BankRunCodec, signed arithmetic, snapshot/anchor rules, event ordering, Filepath storage or migration. Format 5, original legacy baselines, character isolation, audit history and recoverable backups are unchanged. No progression or config migration runs on the user's files.

Six existing two-column statistic tiles remain: Total Value Gained, Current Bank Net Worth, Spendable GP, Items Unlocked, Active Run Duration, Total Value Lost / Spent. Four financial tiles retain Coming soon. Positive-bank-ID count and saved-start duration remain supported; lifetime earnings/losses are not inferred.

## Defaults and rendering

| Setting | Default |
| --- | --- |
| Virtual quantity color | #1DFF00 |
| Locked item background | #333333 |
| Locked item background opacity | 50% (alpha 128 after rounding) |

Existing configuration group/keys are unchanged. Only default methods and render fallbacks change; no setConfiguration calls overwrite saved values. A player who already saved purple/custom text or another tint/opacity keeps those settings. Select the new colors manually to evaluate them on an existing installation. ConfigChanged still replaces the immutable render settings immediately; it does not rebuild ownership or write progression.

Text is painted after tint, with the existing black shadow. Negative values remain signed and nonpositive items remain dimmed. Accepted item coordinates, partial-row intersection/clipping, placeholders, bank searches, tabs and drag handling are untouched. At alpha 128, synthetic ordinary glyph contrast retains 127 of 255 levels; true readability still requires live acceptance on coins/bright items.

## Item icons and branding

Verified directly against the resolved RuneLite **1.13.3** `net.runelite.api.gameval.ItemID` source, not guessed numeric constants:

| Tile | Sprite | API constant | ID |
| --- | --- | --- | ---: |
| Automatic Changes | Banker's note | LEAGUE_BANKERS_NOTE | 28767 |
| Manual Corrections | Diary, Horror from the Deep | HORROR_DIARY2 | 3846 |
| Bank Corrections | Enchanted quill | VEOS_QUILL | 21260 |

BankIcons requests each sprite once through ItemManager.getImage, retaining AsyncBufferedImage.addTo for supported asynchronous completion/repaint. All icon labels reserve centered 36x32 bounds. Missing/failed sprites use the bundled icon; null results are also cached, preventing repeated lookup on bank updates. No external assets are downloaded or item sprites stored in the repository.

Navigation uses a newly generated transparent purple pixel-art coin stack without numbers. Exports remain 24x24 (706 bytes) and Hub 48x48 (2,246 bytes), with nearest-neighbor fitting. See [artwork provenance and exact prompt](../ARTWORK.md). Other purple/gold branding is preserved.

## Sidebar interactions

- Bank History retains permanent Automatic Changes and Manual Corrections action tiles. Read-only history, paging and filters are unchanged; no feed is restored.
- Played Elsewhere has exactly two columns with matching 132-pixel tile heights. Left: noninteractive wrapped instructions, "Played on mobile or another PC? Bank your items and open your bank here." Right: Enchanted quill and clickable Bank Corrections, routed to the existing editor. Hover/cursor distinguishes the action tile. Existing automatic bank updates need no review/tab walkthrough.
- Reset Progress remains the last danger section. Its button explicitly paints solid red, centered white text and darker pressed/lighter hover states, avoiding look-and-feel suppression of the red fill. Exact typed RESET, confirmation, revision validation and successful archive-before-reset remain mandatory. Opening/cancelling is non-mutating.
- Setup uses a bordered dark-purple confirmation row with a 22-pixel checkbox indicator. Unchecked: light-gray outline/dark interior. Checked: purple outline/fill with white tick. Hover brightens the outline; keyboard focus adds a gold ring. The standard JCheckBox label hit area and Space binding remain. It defaults unchecked. Start remains disabled without both attestation and a complete bank; no inventory/equipment inspection was added.

The component tree and icons are created once; ordinary banking updates existing labels/enable states. No per-bank sprite loading, dialog creation, layout reconstruction, observer wait or disk processing was added. Synthetic component identity and 1,200-ID performance regressions remain in the full suite.

## Files changed in this pass

- Production: FromScratchConfig; overlay/BankOverlayState defaults only; bank/BankPanel, BankIcons; new BankAttestation and BankDangerButton.
- Assets: root icon.png and src/main/resources/com/fromscratch/icon.png.
- Tests: new BankUiPolishPassTest; updated BankPolishTest, BankPanelTest, BankOverlayStateTest, BankItemPainterTest, LockedTintSettingsTest.
- Documentation: README, ARCHITECTURE, TEST_PLAN, CHANGELOG, AGENTS default-color guidance, ARTWORK, COMPATIBILITY, MILESTONE_4, prior POLISH report pointer, file inventory and this report.

## Automated verification

Focused UI/overlay tests passed. Required `.\gradlew.bat clean build`: **BUILD SUCCESSFUL in 40 seconds**, 7 tasks executed; **544 tests passed, 0 failures, 0 errors, 0 skipped, 37 suites**. All previous 524 cases remain, with authorized default/tile expectations updated. Added BankUiPolishPassTest (19 cases) and one signed-green-on-coins painter case (20 total). Coverage includes verified IDs, cached centered sprites/fallback, transparent purple navigation artwork, two-column instructions/action navigation, reset normal/hover/pressed pixels, visible unchecked/checked/hover/focus checkbox states, synthetic label clicks, Space activation, untouched setup observation, missing-bank gating and minimum width.

Full-run synthetic diagnostics (not native client measurements):

```text
BANK_UI_1200_ITEMS updates=100 wall_ms=2.964 edt_cpu_ms=0.000
BANK_1200_ITEMS capture_100_ms=17.579 commits=256 worker_wall_ms=13240.304 worker_cpu_ms=3343.750 io_and_scheduling_ms=9896.554 early_mean_ms=60.790 late_mean_ms=50.001
```

The 100-refresh UI sample remains small versus the prior 6.727 ms sample; this is diagnostic variability, not a guaranteed speedup. Windows CPU quantization can report zero for a short EDT workload. Persistence/order/recovery and 1,200-ID regressions pass unchanged. Nonfatal pre-existing build notes remain: unchecked development-launcher operations and Gradle APIs deprecated for Gradle 9 (Wrapper remains 8.10).

Offscreen Java2D previews at 225-pixel sidebar width were inspected: all tiles fit; setup row is readable, the checkbox visible before hover, and reset is solid red. These are synthetic Swing components with fallback sprites, not in-game screenshots or manual acceptance. The tests never operate the game.

## Manual acceptance

1. Launch `.\gradlew.bat run` from the project root, or ask the agent to launch it. Follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions. Retain the accepted run; do not reset it for UI testing.
2. Confirm the purple coin navigation icon is recognizable, transparent and has no numeric label. Inspect all six statistic tiles and Coming soon values at normal/narrow widths and your display scale.
3. Confirm Automatic Changes shows Banker's note, Manual Corrections the Horror from the Deep Diary, and Bank Corrections an Enchanted quill. Allow asynchronous loading. Both history views remain read-only and corrections still opens the existing editor.
4. Played Elsewhere: left tile contains the concise instructions and does nothing when clicked; right tile opens corrections. Heights/spacing match. Verify no old feed, session wizard or inventory/equipment requirement appears.
5. Existing saved colors must remain selected. To evaluate the new defaults, set quantity #1DFF00, background #333333 and opacity 50% in plugin settings. Change each independently while bank is open: immediate response, unchanged balances. Check green negative/zero/positive counters on coins and bright items; original quantities remain readable. Restore preferred custom values if desired.
6. Exercise top and bottom partially visible rows, rapid scrolling, tabs/search/empty results, resize/scaling and dragging. Positive balances are undimmed, nonpositive shaded; no overlay on controls/outside viewport. Close/disable/re-enable: clean removal and no duplicate overlay.
7. Withdraw a legacy item, return it, deposit new logs, withdraw one and consume it outside the bank, then reopen. Signed balances and timing must match accepted bank-only behavior. Restart and verify saved balances/history/corrections persist.
8. On a separate unused/new-run character only, inspect setup without altering the accepted run. Checkbox starts unchecked with a visible border; click its label or Tab then Space to toggle. The checkmark/focus/hover must be distinct. Start is disabled without attestation or available bank data. Open Start confirmation, then Cancel: no run is created. Empty inventory/equipment must not require special tab checks or items.
9. Inspect the solid red reset action and hover/press colors. Open its dialog and Cancel/Escape/window-close. Empty, lowercase or partial input must not enable reset. Do not confirm RESET against the accepted run merely to test styling.

## Limits and publication

Live sprite clarity, color contrast, bank responsiveness, real window scaling and keyboard behavior still need user confirmation. Existing custom settings intentionally do not automatically become the new defaults. Synthetic UI/performance timings cannot prove native OSRS latency or FPS. No new network/dependency/unsupported game API, sprite mutation, input injection or gameplay automation is introduced. Plugin Hub review and final publication remain separate future work.

## Git handoff

Branch master, HEAD 5b7c49f. Entire uncommitted M4 working tree: 28 modified tracked files, 16 prior main-source deletions/moves, 95 untracked files, zero staged changes. The removals predate this UI pass and preserve retired behavior as historical test fixtures; no accounting/persistence file was changed in this pass. See [complete working-tree inventory](MILESTONE_4_FILES.md). No commit, push, tag, run reset or publication. Stop for manual acceptance.
