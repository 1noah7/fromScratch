# Milestone 9.1 — Native bank tabs and finalized GE history

Awaiting manual acceptance. No commits, tags, publication, progression reset or save migration.

## Native tabs: feasibility and implementation

RuneLite 1.13.3 core Bank Tags intentionally closes its active tag for native tab selections. A dynamic tag alone cannot preserve the requested intersection. Its supported bank scripts provide the composition:

- [BankMainBuild](https://github.com/runelite/runelite/blob/runelite-parent-1.13.3/runelite-client/src/main/scripts/BankMainBuild.rs2asm) exposes `bankBuildTab`. The ordinary native-tab path bypasses item search filtering; single-tab build mode calls the item predicate.
- [BankSearchFilter](https://github.com/runelite/runelite/blob/runelite-parent-1.13.3/runelite-client/src/main/scripts/BankSearchFilter.rs2asm) exposes `bankSearchFilter`, whose return value can deny an item without replacing native interactions.
- BANK_TAB_1–9 identify contiguous ranges in the complete BANK container. BANK_CURRENTTAB selects a range; All uses all eligible IDs. Placeholder aliases reuse the existing validated definition cache.

After a native-tab click, core closes the tag normally. From Scratch continues through those script callbacks with cached membership: selected native range intersected with positive effective virtual item IDs. There is no repeated `openBankTag`, cancelled menu action, synthetic gameplay action, fabricated widget, physical quantity change or tab reassignment. Initial activation still uses the accepted dynamic tag. Existing deferred lifecycle operations, state-before-close guards, failure latching and initial-snapshot safeguards remain.

Membership rebuilds only on relevant committed balances/physical state or native tab/slot metadata changes. Per-item predicates perform set lookups only; no definitions, price requests, ledger reads, disk work or redraws. The normal native rebuild applies tab changes without another refresh; later membership changes refresh only when the set differs. OFF removes both paths. The friendly title remains tied to actual filtering.

The existing settings-only key/default is unchanged: ON for new users, explicit OFF preserved, no filtering before an acknowledged active run. Native search explicitly returns to Full Bank and clears the same preference because single-tab build mode closes the search input. Selecting another user Bank Tag also exits; saved tags/layouts remain untouched. Ordinary native item tabs 1–9 and All are supported, not separate special storage interfaces. Mixed stacks retain full physical quantities and virtual overlays; this is not withdrawal enforcement.

## Finalized GE history

The append-only audit already stores cumulative executed quantity/GP and durable offer generations. History derives one row when quantity reaches the requested total, or a cancellation confirms nonzero execution. It uses cumulative amounts rather than summing/replaying fill deltas. Outcomes: Bought, Bought (Partial), Sold, Sold (Partial).

Pending/active partial offers, zero-fill cancellations, slot-clears and lifecycle rows stay internal. Stable summary IDs derive from the existing generation; deduplication spans pagination. Reused slots remain independent. Restart during an active partial does not finalize it. Completion time is the local observation of completion/cancellation, not reconstructed server execution time. A first-seen completed cumulative snapshot retains unknown-origin disclosure. Unobserved trades and incomplete completion evidence are not invented.

Existing history consolidates on read, preserving every original audit byte and recovery/replay evidence. No new persisted financial transaction or save format is needed. Finalized pages contain up to 25 offers with one-row lookahead. Reads inspect at most 64 immutable segments per worker turn, then yield the FIFO; empty intermediate scan batches never become public pages. Search, full/partial filters, centered/grouped numbers, sprites, scrolling and previous/next controls remain.

Fill deltas, conversion budgets, bank observations and fee accounting still run when observed, including active partials. Viewing/finalizing history does not debit GP, credit items, add gains/spending, change peaks or replay taxes. The API does not expose verified tax settlement: displayed sale tax is explicitly estimated from cumulative average price using the existing exemption/cap helper; preexisting-offer tax stays unknown. Financial totals retain their original per-fill estimates, which may differ with rounding/varying prices. Gross/net settlement, collection destination and realized profit remain unverified.

## Validation

Incremental `compileJava` and `compileTestJava` passed. **63 distinct targeted tests passed**, no failures/errors: 22 VirtualBankMode, one production-plugin EventBus registration, 30 GE pipeline, eight GE history UI, one finalized-pagination/immutable-history, one GE worker performance. No full suite or clean build. The pagination case was rerun alone after strengthening cross-page deduplication.

Coverage includes native tabs 1/2/All, signed eligibility, placeholder normalization, unchanged physical accounting, deposits/withdrawals, search exit, Full Bank, initial snapshot acknowledgment, restart/character changes, incomplete metadata, re-entrant close, cumulative fills, terminal duplicates, slot reuse, partial cancellations, unknown-origin completion, saved financial state and worker yielding. Pagination yields 41 finalized offers across pages of 25 and 16 despite duplicate completion evidence separated by more than a page. Existing audit bytes remain unchanged.

The isolated 1,200-item fixture performed 128 forced GE commits in 8.128 seconds on the storage worker (2.391 seconds CPU), retaining 30 recent audit rows and bounded matching state; its unfinished offer produces no public row. These are worker/filesystem measurements, not live banking latency or proof that historical freezes are eliminated.

## Manual acceptance

1. Put positive virtual items and locked legacy items in two native tabs. Enable Virtual Bank; visit each tab and All. Verify the intersection, placeholders, green quantities, friendly title and scrolling. Mixed stacks keep physical quantities.
2. Deposit/withdraw to add/remove eligible types while a tab is selected. Compare balances/history around view-only changes: no synthetic transactions. Disable/re-enable filtering; verify Full Bank/native title. Native search/user tags explicitly exit filtering and update Settings.
3. Reopen repeatedly, restart and disable/re-enable the plugin. Before a run, always see Full Bank. Start only after the complete snapshot is saved. Confirm no startup errors/freezes or character-membership leakage.
4. Place pending buys/sells and get active partial fills: no public row. Complete each offer: one cumulative row. Cancel unfilled offers: no row. Cancel partial buys/sells: one partial row for only executed quantities/GP.
5. Collect/reopen, reuse the slot and restart during a partial. Verify stable distinct finalized rows and no duplicate spending, tax, bank gains or peak effects. Check search/full/partial filters, large centered numbers, pagination and estimated/unknown tax disclosures.

Only the player tests RuneScape. Development launch: `.\gradlew.bat run`; use the official [Jagex Accounts instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) if needed. Full release regression remains separate.

## Files in this correction

- Native integration: `FromScratchPlugin.java`, `bank/VirtualBankMode.java`, test `bank/VirtualBankModeTest.java`.
- GE projection/UI: `ge/GeHistory.java`, `ge/GeTrade.java`, `ge/GeHistoryDialog.java`, `ge/GeHistoryView.java`.
- Query plumbing: `bank/BankActions.java`, `bank/BankController.java`, `persistence/BankRepository.java`.
- GE tests: `GeHistoryViewTest.java`, `GePipelineTest.java`, `GePersistenceTest.java`, `GePerformanceTest.java`.
- Documentation: `ARCHITECTURE.md`, `ACCOUNTING.md`, `TEST_PLAN.md`, `CHANGELOG.md`, this report.

Preexisting uncommitted Milestone 8/9 work is preserved. Rune Pouch/Looting Bag purchase logic, the GE reducer, economic checkpoint schema, backup/import/export, Examine, overlays, playtime and real save data are unchanged.
