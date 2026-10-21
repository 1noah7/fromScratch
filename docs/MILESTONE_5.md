# Milestone 5 — Bank wealth and UI corrections

> The [latest correction report](MILESTONE_5_FINAL_CORRECTIONS.md) supersedes earlier placeholder, icon and compact-money descriptions. The [UI polish](MILESTONE_5_POLISH.md) records earlier tooltip/sidebar work. Accounting formulas, prices and format 6 are unchanged. Current manual acceptance is pending.

By **Pilots r kwl**. M1–M4 are accepted. **M5 passes automated validation; manual acceptance is pending.** No GE accounting, buybacks, personal-container accounting, cloud, commit or publication is authorized.

## Design established before implementation

- Preserve signed quantity rule B' = B + (P' - P), immutable initial bank, ordered forced commits and accepted overlay geometry.
- Item labels and compact GP: exact integers below 10,000; floored uppercase K below 10,000,000; floored uppercase M thereafter. Handle signed long bounds without negation overflow. Money appends GP, with grouped exact GP tooltips.
- Prices: injected supported RuneLite ItemClient bulk GE-guide feed on a separate daemon worker, never on client/EDT/storage worker. Preserve record presence (verified zero differs from missing). Cache 30 minutes, retry failed refreshes after a minute, mark retained values stale. Resolve noted IDs using supported ItemComposition once per exact ID on the client thread; do not merge charged/degraded variants. No account/item list is transmitted; the request is RuneLite's shared bulk price endpoint.
- Net worth N = sum B(i) * p(i), including negative balances. Coins have exact p=1 and occur once. Missing prices produce a labeled priced subtotal, not invented zeros. Checked arithmetic reports unavailable/overflow rather than wrapping. Price refresh updates only a derived immutable dashboard projection, without disk writes.
- Estimated gains: H(i) is a nonnegative per-exact-ID progression maximum. On automatic change, q=max(0,B'(i)-H(i)); update H=max(H,B'). Persist q and the price/source/received-at used with that bank audit. Known values add q*p to the fixed historical total. Unknown/stale historical quotes remain explicitly unpriced (never filled later with a newer price). Manual corrections/anchors can raise H but never lower it or create gain value. This deliberately undercounts replenishment and cannot identify sales/conversions as economic profit.
- Losses: ordinary withdrawals never create spending. Explicit confirmed GP loss/expense records are separate statistical events, without changing physical bank, progress or Bank GP. No records means Not tracked; record classification/reason is auditable.
- Format 6 checkpoint includes financial state; each new audit includes immutable valuation events. Verify formats 1–5 and preserve exact original checkpoint/journal bytes in a forced manifest-verified archive before migration. Format 5 uses a linked migration event, preserves run/start/initial/P/B/history and establishes H=max(0,current B), with zero historical totals and an imported measurement-start disclosure. Existing format-1–4 anchor safeguards remain. No replay is used to guess earlier financial totals.
- Both Manual Bank Corrections and Manual Corrections history use verified gameval PURPLE_PARTYHAT=1046. Preserve two columns and all existing setup/reset safeguards.

## Implementation and UI

M4 signed bank accounting and observation timing remain intact. All quantity/history/financial work commits in FIFO order on the existing storage worker. A separate pricing worker supplies immutable bulk cache snapshots; it never delays bank changes waiting for a network response. Current valuation runs after acknowledged bank commits or a changed cache version, using revision/identity/lifecycle guards. It creates no disk write, new ledger entry or overlay-label rebuild for a price-only refresh. Older rolling gain summaries omit quote lists already retained in their immutable original journals/checkpoints; host comparisons retain quantity summaries. Full financial evidence is never erased, and recent explicit expenses remain visible.

The two-column dashboard keeps all six tiles and genuine cached item sprites. Total Value Gained estimates historical bank high-water progression, with exact known GP and grammatically correct unknown-price counts in a compact wrapped tooltip. Technical timestamps/migration details are no longer shown there; they remain stored internally. All-unpriced gains show Unpriced. Current Bank Net Worth includes negative values and coin progress once. Partial prices show Priced subtotal; no priceable nonzero balances show Unavailable, and older retained prices remain identified. Intermediate sums use arbitrary precision, with out-of-long-range totals unavailable rather than wrapped. Bank GP reads the actual COINS ID's signed balance. Compact GP now shares the integer K/M item policy, with grouped exact GP details.

Total Value Lost / Spent remains Not tracked until explicit records exist. Click/focus+Space/Enter on that tile to record a positive verified GP amount and reason. Review and confirm before saving; the tool explains this is statistical only, not a second deduction from bank coins. Recent expense amount/reason/time is readable in the dialog and complete event evidence stays in linked journals. Normal bank movement never creates an expense.

Played Elsewhere explains that progress updates automatically. Its right tile is Manual Bank Corrections with Fix an incorrect count, using **gameval PURPLE_PARTYHAT=1046**, verified in the installed 1.13.3 source. The correction window says: "Use this only if From Scratch recorded an incorrect bank quantity." Search/signed edits/current physical limit/preview/confirmation/Cancel/CAS/audit behavior is preserved. The Manual Corrections history tile uses the same cached Purple partyhat (1046). No manual tool is needed for normal return from mobile.

Item overlays share one integer formatter: 0–9,999 exact; 10,000–9,999,999 uppercase K; ten million+ uppercase M. Examples: 1740, 10K, 1000K, 1740K, 9999K, 10M, -1740, -1000K; 2,147,483,647 becomes 2147M. Long.MIN_VALUE uses safe division. Actual quantities, bottom-right anchor, viewport clip and shading remain. Labels are cached per committed revision; exceptional wide labels are fitted horizontally inside the slot. Exact counts remain available in corrections.

## Persistence and migration

Format 6 adds a financial checkpoint and valuation evidence inside each new bank audit. High-water quantities, known historical GP, unknown historical quantities, expense GP/count, event count, receipt/source/stale metadata, deterministic financial IDs and measurement start persist. The parent audit/journal supplies character/run/session/time. Persisted bank balances remain the only Bank GP record; no duplicate wallet is introduced.

The repository verifies a format-5 chain first, then copies all original JSON checkpoint/journal/recovery files into a forced exact-byte manifest-verified archive. It appends a linked financial-baseline operation preserving run/start/initial bank/P/B/host summary/older bank history. H starts at current positive B, financial totals at zero; the tooltip discloses the imported measurement boundary. No earlier income is invented, no quantity changes replay, and an already anchored M4 run does not need another bank-anchor dialog. Formats 1–4 retain their existing verified archives and one-time bank anchor.

New checkpoints/journals precede current publication as before. Transition validation recomputes financial state from evidence, rejects missing/duplicate/mismatched events, checked historical-GP overflow and stale revisions. Publication failure retains the prior cursor/financial totals/UI authority. Orphan recovery files are not replayed; successful lost acknowledgments reload current. Original and financial migration archives remain verified on subsequent full loads. Price-only UI refresh does not write files. Actual private saves were neither read nor migrated by development work.

## Files changed

- New production wealth/: PriceQuote, PriceService, BankValuation, FinancialState, FinancialEngine, ValuationEvent, GpFormatter.
- New persistence/FinancialCodec and bank/BankExpenseDialog.
- Extended BankRun, BankAudit, BankEngine, BankController, BankView, BankActions, BankSource/RuneLiteBankSource, BankRunCodec/BankRepository and plugin lifecycle wiring.
- UI: BankPanel, BankTile, BankIcons, BankCorrectionDialog; VirtualQuantityFormatter; BankItemPainter comment corrected to accepted tint defaults only. No geometry, resource, config-key, color-default, observation-debounce or Gradle/dependency change.
- New tests: WealthModelTest, WealthPipelineTest, FinancialPersistenceTest, WealthPerformanceTest and OsrsQuantityFormatterTest. Adapted existing BankPanelTest/BankPolishTest/BankUiPolishPassTest, BankItemPainterTest, LegacyBankDeficitsTest and BankPersistenceTest to current labels/formatting/schema.
- Updated README, ARCHITECTURE, ACCOUNTING, TEST_PLAN, CHANGELOG, AGENTS scope, COMPATIBILITY and accepted-M4 report pointers; this report contains the M5 design/validation/manual procedure.

## Known limits and review considerations

- This is estimated gross bank progression, not full lifetime income, sale profit or reconstructed acquisition history. Replenishing below an earlier peak is not recognized again. Conversions across distinct IDs can establish new peaks; GE/cost-basis accounting is not implemented. Items earned and consumed entirely outside the bank are invisible.
- Missing/stale historical gain prices remain unpriced forever in this milestone. Newer prices cannot repair past missing evidence. Current net worth can recover through cache refresh. Bulk GE-guide prices and receipt time are estimates, not real-time execution prices or a certified server-update timestamp.
- Untradeables lacking a reliable price are unknown, not fabricated market values. Present zero-price records are distinct. Only noted price identity is normalized; charged/degraded variants stay distinct. Buyback values are not used.
- Explicit expenses are statistical declarations with reason/confirmation, not inferred costs or wallet deductions. There is no financial-record reversal editor or future GE/buyback implementation. Normal withdrawals do not become spending.
- First cache acquisition/network failure can produce Unavailable/Unpriced states. Banking still commits promptly. Storage latency and one packet/tick boundary remain; synthetic timings cannot establish native OSRS responsiveness or FPS.
- Purple partyhat clarity, final real-client tooltip/layout/contrast/high DPI and actual banking responsiveness require user acceptance. Headless previews use fallback icons, not copied OSRS assets or live screenshots.
- Supported public ItemClient/ItemPrice/ItemComposition APIs, local Filepath storage and injected dependencies are used. No new dependency, gameplay action, packet/menu/widget/sprite mutation, cloud, credential, external account system or player-data request. RuneLite's bulk endpoint is independent of holdings. Plugin Hub review still applies; compilation is not approval.

## Manual acceptance — user only

1. Keep the accepted character/run; do not reset. Launch `.\gradlew.bat run` from the root (the agent can offer to launch it). Follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). On a format-5 run, expect preserved run age/balances/history and no repeated inventory/equipment/anchor walkthrough. The financial measurement boundary remains stored; ordinary tooltips now omit technical dates and migration details.
2. Open bank only. Check labels around 9,999/10,000 and 9,999,999/10,000,000 where available: 1740, 10K, 1000K, 1740K, 9999K, 10M, including negatives. Compact GP shares these integer thresholds; detailed tooltips retain grouped exact GP. Original OSRS labels, custom colors and right alignment must remain.
3. Wait for the background feed. Hover Current Bank Net Worth for exact GP and concise unpriced/older-price counts. Positive progress adds value; negative legacy deficits subtract it; zero legacy possessions do not count. Coins are included once. Missing records must show a priced subtotal or Unavailable, not fabricated full wealth. Guide estimates can differ from executable market offers.
4. Test bank coins: legacy withdrawal decreases Bank GP, returning the same coins restores it, depositing additional coins increases it. No inventory tab or confirmation should be needed. Check exact GP tooltip and signed compact value. This is not a spendable wallet.
5. For a known priced item whose bank progress can exceed its current high-water mark, deposit two additional units: recognized gain increases by two times the recorded unit price. Withdraw one and redeposit it: historical gain must stay fixed. Deposit one above the earlier maximum: recognize only one further unit. Withdraw/return a legacy-only item: no gain. Current market changes may alter net worth, never historical recognized gain.
6. Open Manual Bank Corrections: Purple partyhat icon, Fix an incorrect count and the purpose sentence must be readable. Search Wooden shield, preview a valid signed correction, then Cancel first (no mutation). If a correction is actually needed, confirm it; it changes the bank count/history without creating gain GP. An upward correction and later withdrawal/return must not manufacture gains. Purple partyhat is also used for manual history.
7. Loss tile initially says Not tracked. Open its recording dialog by mouse or keyboard; inspect wrapping/amount/reason validation. Cancel/Escape/window close must change nothing. If you have a real expense to record, enter its positive exact GP/reason, review and confirm once. Only recorded-expense total should increase; bank quantities/Bank GP stay unchanged. Recent explicit record and reason should be visible when reopening. Ordinary withdrawals must never add expenses.
8. Compare native bank response and counter response with enabled/disabled plugin. Try rapid deposits/withdrawals, searching/tabs/reordering, top/bottom partial rows, scrolling/dragging, resizing and display scaling. No financial work/network wait should obstruct gameplay. Existing opt-in sampled DEBUG phases remain available if delayed.
9. Disable/re-enable, logout/restart, and open bank: balances, fixed gains/unpriced evidence, high-water marks, corrections and history must persist without duplicate financial events. Returning from mobile means bank items/open bank here; no special editor is required. Test another character only if convenient: no shared run/financial state.
10. Verify six tiles, read-only history pages, Played Elsewhere, purple navigation coins, default/custom shading/text settings and visible setup checkbox remain accepted. Open/cancel Reset Progress; never reset the accepted run merely to test. New-run setup may be checked on a separate unused character, with unchecked attestation and explicit Start confirmation.

## Automated build and performance evidence

Final required `.\gradlew.bat clean build`: **BUILD SUCCESSFUL in 1 minute 1 second**, 7 tasks executed. **611 tests passed, 0 failures, 0 errors, 0 skipped, 42 suites**. Retained all applicable M1–M4 cases; updated only obsolete UI/format/schema expectations. Added 67 tests:

| Suite | Added tests |
| --- | ---: |
| WealthModelTest | 31 |
| WealthPipelineTest | 15 |
| FinancialPersistenceTest | 13 |
| WealthPerformanceTest | 2 |
| OsrsQuantityFormatterTest | 6 |

New recovery tests include both pre-publication failure and successful-publication/lost-acknowledgment retry: historical gain remains counted once. The compact-summary test verifies original journal bytes/quotes remain intact, the earlier in-memory audit is immutable and reload state matches. Existing 256-commit/100-FIFO-transition, migration, character isolation, corruption and accepted clipping/configuration tests pass.

Gradle dependencyInsight confirms **net.runelite:client:latest.release → 1.13.3**, targeting JVM 11; Wrapper remains **8.10**. Installed primary source verification covers ItemClient.getPrices, ItemManager.getItemPriceWithSource zero ambiguity, ItemComposition note linkage and PAPYRUS=970. No new dependency was added. Nonfatal existing notes: unchecked development launcher operations and Gradle APIs deprecated for Gradle 9.

Final synthetic diagnostic output:

```text
BANK_UI_1200_ITEMS updates=100 wall_ms=12.100 edt_cpu_ms=15.625
BANK_1200_ITEMS capture_100_ms=30.692 commits=256 worker_wall_ms=14867.291 worker_cpu_ms=4687.500 io_and_scheduling_ms=10179.791 early_mean_ms=54.667 late_mean_ms=88.262
WEALTH_1200_ITEMS value_100_wall_ms=24.687 value_cpu_ms=31.250 financial_commits=96 worker_wall_ms=7126.608 worker_cpu_ms=3515.625 io_scheduling_ms=3610.983 durable_events=1296
```

Cached valuation averaged about **0.247 ms**; 100 Swing refreshes averaged **0.121 ms** each. The initial M5 financial benchmark copied older quote lists into rolling checkpoints: 96 commits took 11,215.413 ms wall / 7,640.625 ms CPU. Compact summaries reduced those measured totals to 7,126.608 ms / 3,515.625 ms while retaining full immutable journal evidence. This is a diagnostic improvement on the synthetic workload, not a guaranteed live latency.

The 1,200-ID benchmark includes 1,296 durable financial events and 96 later forced commits; the retained bank benchmark covers 256 commits and a separate 1,240-change paginated history. Filesystem/GC/scheduling variability explains different early/late write means; wall-minus-CPU is **IO plus scheduling**, not a direct disk-only measurement. Short Windows CPU samples use coarse quanta and may exceed wall time. CPU/IO stay outside client/EDT critical paths. No native OSRS delay/FPS has been measured by the agent.

Offscreen 225-pixel dashboard/setup previews were inspected. A clipped HTML subtotal caption was replaced with a separate standard-font caption; the final Priced subtotal, signed GP and three-line Manual Bank Corrections label fit. The preview is synthetic with bundled fallback icons. Real Purple partyhat/OSRS sprites, display scaling and native bank responsiveness still need user acceptance.

## Git handoff

Branch **master**, HEAD **5b7c49f**. The user had **124 staged M4 paths** before this task; that index was preserved. M5 has **32 unstaged tracked changes and 16 new files**, listed in [the M5 file inventory](MILESTONE_5_FILES.md). One unrelated pre-existing untracked file was left untouched (17 untracked paths total). `git diff --check` passes. No commit, push, tag, private-save reset, publication, game interaction or M6 work. Stop for manual acceptance.
