# Milestone 5 — Final withdrawal and UI corrections

By **Pilots r kwl**. Targeted M5 correction pass; **manual acceptance remains pending**. No M6, GE accounting, inventory/equipment tracking, cloud, commits or publication. No actual character save was read, migrated or reset for development.

## Root cause and correction

The code-supported defect was in placeholder presentation. Before this correction, VirtualBankOverlay recognized a placeholder but called `frame.balance(itemId, placeholder)` with its **placeholder variant ID**. Bank quantities and signed balances use the original physical item ID. The missing variant record therefore returned zero, hiding the original deficit after the last copy was withdrawn.

The accounting path already handled the last withdrawal: BankItems excludes placeholder templates/zero quantities, and BankEngine compares the union of previous and current IDs. A complete aggregate without an item means physical zero, so 4 → 1 → 0 produces balances 0 → -3 → -4. BankRun omits zero records but retains negative balances. No reducer, equation, financial calculation, observer, journal or save format was changed in this pass.

The renderer now caches placeholder status **and original balance identity**, then uses the original ID for the signed balance and cached label. The placeholder keeps its existing OSRS sprite, widget, item quantity, click zone and bounds. Accepted tint, drag handling and viewport intersection clipping remain unchanged. No definition lookup is added to repeated frames after caching. A stale positive committed balance cannot appear as positive on a zero-physical placeholder.

Verified against the resolved **RuneLite 1.13.3 API source**:

- `ItemComposition.getPlaceholderTemplateId() != -1` identifies a placeholder.
- `getPlaceholderId()` is bidirectional: on a placeholder it returns the normal item, but on a normal item it returns the placeholder. Only resolve that link for placeholder templates; do not merge notes, charges or other normal variants.
- `ItemID.WOODEN_SHIELD = 1171`, `ItemID.Placeholder.WOODEN_SHIELD = 14114`.
- `ItemID.COINS = 995`, `ItemID.Placeholder.COINS = 14440`.
- `ItemID.PURPLE_PARTYHAT = 1046`.

These are API/source and synthetic integration findings, not captured live client state. The user must verify the actual native-placeholder appearance/events in-game.

When OSRS removes a slot without keeping a placeholder, there is nothing to draw there. The original -4 stays saved, remains in signed net worth when priced, and is searchable by name/ID in Manual Bank Corrections. It reappears on the normal slot after a deposit. The plugin does not create slots or enable OSRS placeholder settings. Always set placeholders is the user's optional native setting.

## Formatting and UI

GpFormatter.compact now delegates to the same dedicated integer formatter as bank labels:

| Absolute amount | Rule | Example |
| --- | --- | --- |
| 0–9,999 | Exact, no separators | 1740 GP |
| 10,000–9,999,999 | Truncate / 1,000, uppercase K | 980K GP; 1000K GP; 3963K GP |
| 10,000,000+ | Truncate / 1,000,000, uppercase M | 25M GP |

Preserve signs, never round up or use decimals, and safely divide both long bounds without negation. Exact grouped tooltips remain, e.g. 3,963,708 GP. Every financial tile already uses GpFormatter, including priced subtotals/stale indicators. Historical gain values, current-value formulas, prices, balances, high-water marks and expenses are unchanged.

Both manual-history and Manual Bank Corrections tiles request the supported ItemManager sprite for Purple partyhat (1046). The per-ID cache shares that one image request. Automatic Changes keeps Banker's note. Navigation artwork and all tile actions/text remain.

Setup retains the three bank-only instructions and the unchecked attestation next to step 2. Start remains centered, green, gated by attestation/complete bank/safety state and followed by the original confirmation. Its wrapper now supplies an effective **14-pixel** top gap. The optional note below Start says:

> Items in Grand Exchange offers or other storage aren't included. Collect them into your bank first if you want them treated as legacy.

The vague special-storage sentence is removed. There is no GE observation or empty-offer requirement. Previously owned excluded-storage items collected after starting may appear as progression and need a Manual Bank Correction. Reliable GE handling remains planned M6 work, not part of this pass.

Played Elsewhere remains two equal columns. Its centered, nonclickable instruction tile says:

> Played on mobile or another PC? Bank your items and open your bank here. Your progress updates automatically.

Manual Bank Corrections / Fix an incorrect count remains the adjacent action. Reset safeguards, danger styling, checkmark/focus states and confirmation dialogs are unchanged.

## Files touched in this correction pass

- Production: `overlay/VirtualBankOverlay.java`, `wealth/GpFormatter.java`, `bank/BankIcons.java`, `bank/BankPanel.java`.
- New tests: `wealth/GpFormatterTest.java`, `bank/FinalCorrectionsUiTest.java`.
- Extended/updated tests: BankOnlyPipelineTest, WealthPipelineTest, PipelineOverlayFixture, BankUiPolishPassTest, WealthModelTest and OsrsQuantityFormatterTest.
- Documentation: README, CHANGELOG, ARCHITECTURE, ACCOUNTING, TEST_PLAN, AGENTS, MILESTONE_5, MILESTONE_5_POLISH and this report.

Other existing uncommitted M5 changes and pre-existing staged M4 changes remain preserved. No new schema/migration, configuration key, dependency or network service is introduced.

## Automated validation

`.\gradlew.bat clean build` passed in **56 seconds**, all seven tasks executed. **654 tests passed; 0 failures, 0 errors, 0 skipped, 45 suites.** The prior 638 tests are retained with obsolete icon/money expectations updated; 16 regression tests were added (four full-withdrawal pipelines, one signed-valuation/restart pipeline, six GP-format cases and five UI cases). Gradle reports its existing Gradle-9 deprecation notice and an existing unchecked test-harness warning; neither is a test failure. `git diff --check` passes.

New controller → full aggregate → serial worker → real Filepath journal/checkpoint → committed projection → actual overlay tests cover stackable/unstackable four-copy withdrawals with and without native placeholders. They verify original immutable baseline, final -1 delta, -4 retained balance, zero placeholder physical holdings, original-ID overlay pixels, correction search without a slot, unchanged tab/search/reopen observations, restart, returns of 1/2/4 and no fabricated gain/history duplication. A financial integration case verifies -400 GP signed net worth across a placeholder and restart using a synthetic 100-GP quote.

New formatting tests cover requested GP examples/signs, suffix boundaries, no decimals/rounding, every small signed value, both long bounds and exact grouped details. UI regressions exercise all four financial tiles, exact tooltips, cached 1046 requests for both manual icons, readable storage note, bank-only attestation gating, 14-pixel Start gap, checkbox proximity and centered/equal Played Elsewhere tiles with automatic-update text. Previous M1–M5 accounting, migration/recovery, history, clipping/custom colors, confirmations and performance suites remain required.

Offscreen 225-pixel setup/dashboard previews are synthetic, use bundled fallback artwork and do not establish live sprite quality, DPI behavior or native bank responsiveness. Full-suite performance fixtures still cover 1,200 IDs, rapid FIFO observations, large history and background forced IO. This renderer-only identity cache adds no bank delay; live latency must still be verified by the user.

Clean-build synthetic timings: 100 sidebar refreshes **7.362 ms**; 100 full 1,200-ID aggregate captures **26.117 ms**; 256 forced bank commits **11,313.363 ms worker wall / 3,968.750 ms CPU**; 100 cached 1,200-ID valuations **21.767 ms wall**; 96 financial commits **6,681.477 ms worker wall / 3,234.375 ms CPU**, retaining 1,296 durable valuation events. These are aggregate fixture workloads, not native bank input/server latency. Windows thread-CPU readings are quantized; wall minus CPU includes IO and scheduling and is not a direct disk measurement. There is no new client-thread IO or per-frame price work.

Git at report time: master, HEAD 5b7c49f; **124 pre-existing staged paths, 36 unstaged tracked paths, 16 untracked status entries** (directories are grouped by Git). The index SHA-256 is unchanged from the initial check. Counts include earlier M4/M5 work and an unrelated untracked file, preserved. No staging, commit, push, tag, run reset or publication was performed.

## Manual acceptance procedure

1. Launch the development client from the repository using `.\gradlew.bat run`. For a Jagex Account follow the official [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions. Only the user operates RuneScape.
2. Preserve the current accepted run. Choose an item whose current physical count/balance are known. For the exact legacy example, use four physical copies with B=0. Record the existing values first; do not reset merely to test. Any intentional count correction uses its normal confirmation/audit trail.
3. With OSRS Always set placeholders enabled, withdraw three copies: B=-3. Withdraw the last: the native placeholder must display -4. Search/filter/change tabs, scroll both clipped edges and close/reopen; the counter must remain associated with that item. Its placeholder must not count as an additional copy.
4. Open Manual Bank Corrections and search the item; verify physical 0 / signed -4, then Cancel. Confirm net worth includes the four-copy negative contribution at the available current price. Ordinary withdrawals must not increase estimated gains or recorded spending.
5. Restart the development client while physical count is zero. Open the bank, verify -4 without duplication, then deposit one, a second, and the remaining two: B=-3, -2, 0. Check Bank History reflects real net bank changes, with no phantom placeholder acquisition.
6. Repeat using OSRS's normal placeholder-disabled behavior. There should be no invented empty slot/counter. The saved deficit remains searchable/valued after reopen/restart and the counter returns when the item is deposited. Exercise an unstackable item and a stackable item.
7. Inspect wealth labels at relevant amounts: 1740 GP, 50K GP, 980K GP, 1000K GP, 3963K GP, 9999K GP, 10M GP and signed negatives. Use real progression/current values or existing details; do not alter financial history merely for display tests. Hover for full grouped exact amounts; no decimal compact suffixes or upward rounding.
8. Verify both manual tiles show the purple **partyhat**, retain their labels/purpose, and open the correct separate views. Played Elsewhere must be centered/readable and explicitly state automatic updates. Ordinary device return still requires only banking/opening the bank here.
9. Inspect Before Starting only on a separate unstarted test character/profile, without resetting this run. The optional storage/GE note must fit; checkbox remains unchecked, visibly outlined and close to step 2. Start has a modest larger gap, stays disabled until attested + valid complete bank, and opens the unchanged confirmation. Cancel must not start a run. No GE tab or offer inspection is required.
10. Recheck scroll/search/tabs/resizing/partial rows, signed-label alignment, original OSRS quantities, custom colors/tint, dragging/context menus, plugin disable/re-enable and deposit responsiveness. Cancel reset/expense/correction dialogs when no actual adjustment is intended.

Stop for user acceptance. No commit, push, tag, publication, reset or M6 follows automatically.
