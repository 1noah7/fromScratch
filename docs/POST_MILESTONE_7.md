# Post-Milestone 7 — Compact tiles and legacy pouch contents

By **Pilots r kwl**. M7 is the user-approved baseline. This targeted refinement awaits manual acceptance; M8 is not implemented. No actual character save was accessed, reset or edited during development. No commit, push, tag or publication was performed.

## Layout

Statistic tile minimum height changes **112 → 98px** (12.5% smaller); vertical padding **6 → 4px**, gaps **3 → 2px**. Standard action rows change **102 → 90px** (11.8%); Played Elsewhere changes **132 → 116px** (12.1%). Natural content height may exceed these targets for captions, longer text or larger fonts. Two columns stay equal and the existing components, icons, labels, values, tooltips and actions remain intact. No overlay geometry or configuration changes.

Normal order is exactly:

1. From Scratch header and statistics.
2. Legacy Buybacks.
3. Played Elsewhere?
4. Grand Exchange.
5. Bank History.
6. Reset Progress, last.

Actionable error/migration notices remain conditional. No pouch section was added to the primary dashboard.

## API investigation

Verified the **resolved RuneLite 1.13.3 API and client source JARs**, particularly `plugins/runepouch/RunepouchOverlay.java` and `plugins/cluescrolls/ClueScrollPlugin.java`. The core overlay reads six gameval type/quantity pairs; Clue Scroll uses the same rune enum. The supported definitions are:

| Slot | Type varbit | Quantity varbit |
| --- | ---: | ---: |
| 1 | RUNE_POUCH_TYPE_1 (29) | RUNE_POUCH_QUANTITY_1 (1624) |
| 2 | RUNE_POUCH_TYPE_2 (1622) | RUNE_POUCH_QUANTITY_2 (1625) |
| 3 | RUNE_POUCH_TYPE_3 (1623) | RUNE_POUCH_QUANTITY_3 (1626) |
| 4 | RUNE_POUCH_TYPE_4 (14285) | RUNE_POUCH_QUANTITY_4 (14286) |
| 5 | RUNE_POUCH_TYPE_5 (15373) | RUNE_POUCH_QUANTITY_5 (15375) |
| 6 | RUNE_POUCH_TYPE_6 (15374) | RUNE_POUCH_QUANTITY_6 (15376) |

Production uses `VarbitID` constants, not those numerical literals. Types resolve through `Client.getEnum(net.runelite.api.EnumID.RUNEPOUCH_RUNE).getIntValue(type)`. This version has no gameval EnumID class; the core plugin uses the supported API EnumID. Resolved item IDs must belong to the explicit genuine-rune allowlist. Zero quantities are ignored even when a stale type remains. Positive quantity with missing/invalid type, absent definitions, invalid values or unavailable logged-in client context produces unknown.

These APIs report account varbits without a content-observation timestamp, physical-instance identity or documented guarantee of freshness while a pouch is banked. The core Rune Pouch overlay normally draws for inventory items; Clue Scroll also checks personal-container presence for its own requirements. From Scratch does **not** add that observation gate or copy its personal-container tracking. Metadata is read once on a relevant shop request on the client thread, including while banked, and is labeled **reported, possibly stale**. Even reported zero requires explicit player verification. No tab, banking, emptying, input or other game action is automated.

The existing family remains exactly one quantity-1 role across normal **12791**, normal Trouver **24416**, Divine **27281** and Divine Trouver **27509**, using the existing verified catalog constants. Rune pouch note **24587** does not qualify. Read all supported slot pairs defensively rather than assuming a fixed 3/4 slots; this supports the standard/Divine contents without ignoring additional supported core slots.

## Preparation and accounting

The original loophole was that M7 persisted permission for the pouch but no record of its contained legacy runes. When those runes later entered the bank, the ordinary positive bank delta and high-water classifier had no source exclusion. The new finite, audited preparation closes that known-source path without retroactively guessing old contents.

Select the pouch row in the existing Buyback Shop. Before its first new payment it opens **Rune Pouch Contents** instead of a purchase confirmation. Existing paid-but-unreviewed pouches use that same review action and retain their unlock/payment.

- Inspect reported named runes/quantities; manually correct the list against the actual pouch. Confirm it was checked **before moving** anything and choose **Record Legacy Contents**.
- The registration is an explicit durable action. Next, manually empty and bank those runes; keep unrelated same-type deposits separate during preparation. Reopen the shop review after banking.
- Check recorded, remaining and held quantities. Restore any wrongly held ordinary earnings explicitly. Confirm held deposits were legacy, prior unrecorded deposits are accounted for, and the pouch is empty.
- If recorded runes never reached the bank (for example consumed elsewhere), explicitly retire the remainder. This creates no phantom bank debt.
- After READY, the original shared **1,000,000 GP** purchase proceeds with its existing funds/physical-copy/revision checks and confirmation. Upgrading/reverting never charges again or repeats preparation.

The durable phases are UNREVIEWED, PREPARED and READY. Per rune, recorded = remaining + held + retired. Only PREPARED reserves a new positive bank increase, bounded by the still-expected recorded amount:

```
D = P' - P
T = min(max(D, 0), remaining)
B' = B + D - T
remaining' = remaining - T
held' = held + T
```

The whole complete physical bank observation P advances normally. B excludes only this source-limited new arrival. For **P100/B100 earned nature runes plus 500 recorded legacy pouch runes**, the resulting **P600/B100** retains all earned units. The 500 creates no extra bank worth, high-water gain, unpriced historical gain or Items Unlocked. No deduction is made against the previously earned 100. Other rune IDs and amounts exceeding the finite budget track normally. Withdrawals are ordinary signed bank movements; once a budget is consumed it cannot be applied twice on repeated events/reopen/restart.

Rune provenance is not directly provable. A held amount is visible as a provisional source reservation until the player confirms it. **Restore earned amount** releases a wrongly matched amount back to B and replenishes the legacy expectation; ordinary current high-water recognition applies using the quote at release, never a fabricated earlier historical quote. Positive B cannot exceed observed physical P. Empty confirmation retires explicitly unarrived quantities without debiting earned stacks. Completed reservations remain audited but no longer intercept deposits.

Pouch adjustments, P/B, financial recognition/exclusions and GE attribution evidence share one operation on the existing serial worker. GE matching receives the actual progression delta rather than the excluded legacy arrival. No separate GP expenditure is created by preparing contents. The 1M buyback remains one transaction and one shared entitlement. Raw coin progress, cumulative commitments, GE taxes/trades and paid equipment copy floors retain their accepted definitions.

## Unknown contents and existing purchases

No absent/stale reading establishes emptiness. The dialog provides named rune editing and explicit empty/prior-deposits-accounted attestations, with a targeted link to **Manual Bank Corrections**. If contents were already moved before recording, do not guess their quantities. Correct known incorrect bank balances explicitly and confirm the uncertainty. Earlier unverified historical gain prices cannot be reconstructed or automatically erased by this workflow; they are preserved, with this limitation disclosed. There are no invented historical debits.

Existing M7 pouch purchases retain the original payment, copy allowance, entitlement and history. Migration sets a one-time UNREVIEWED state without inventing contents or revoking use. The old-buyer review shows current reported contents as information and leaves the legacy list empty, because those runes may have been earned later. Only quantities the player positively identifies as legacy may be entered. Review is visible within the shop when selecting the family; it never blocks ordinary bank/GE tracking or requires another purchase. The plugin remains an honor-system tool and cannot prevent players from using legacy runes in-game.

## Persistence and recovery

**Format 9** adds explicit pouch state within BuybackState and typed POUCH action / per-bank-transfer evidence in BankAudit. It records only allowlisted rune quantities, phase, preparation identity, held/remaining/retired quantities, reason and the migration archive reference. All data is local, character/run-specific and independent of the immutable initial snapshot.

Before upgrading format 8, verify the whole linked history and archive every original JSON checkpoint/ledger byte-for-byte using the existing forced manifest/hash protocol. Append POUCH_BASELINE, retaining run/start, legacy snapshot, P/B, paid roles/tiers/copy floors, commitments, purchases, financial and GE history, and corrections. No bank anchor, rebase or reset is introduced. Older verified migrations remain supported.

Only acknowledged journal/checkpoint/current writes publish new state to UI/overlays. Repository validation recomputes finite reservation and financial/GE evidence; stale CAS, malformed state, corruption and conflicting clients reject safely. Interrupted writes retain recovery evidence and can retry without duplicated offsets/payment. Missing required format-9 state fails closed instead of defaulting to empty. Pending/completed plans survive restart and automatic offline net bank comparison. Unobserved source changes remain a player-reviewed limitation.

## Files

- Layout: `bank/BankPanel.java`, `bank/BankTile.java`.
- Pouch logic/UI: new `pouch/PouchState`, `PouchAction`, `PouchEngine`, `PouchObservation`, `PouchRunes`, `RuneLitePouchReader`, `PouchPreparationView`, `PouchPreparationDialog`.
- Shop: `buyback/BuybackState`, `BuybackEngine`, `BuybackView`, `BuybackDialog`.
- Integration: `bank/BankActions`, `BankAudit`, `BankController`, `BankEngine`, `BankRun`, `BankSource`, `RuneLiteBankSource`; `wealth/FinancialEngine`.
- Storage: new `persistence/PouchCodec`; updated `BuybackCodec`, `BankRunCodec`, `BankRepository`.
- Tests: new reader/state/pipeline/persistence/performance and PostM7Ui suites; existing buyback fixtures/section-order assertions updated for explicit preparation and the new layout contract.
- Documentation: README, ACCOUNTING, ARCHITECTURE, TEST_PLAN, AGENTS, CHANGELOG, MILESTONE_7 and this report.

The working tree already contained the previous M7 implementation when this pass began; its pre-existing changes are preserved. No real player data or generated build output was added to Git.

## Validation

Ran `.\gradlew.bat clean build` with Java 11-compatible compilation. Final results: **850 tests, 0 failures, 0 errors, 0 skipped across 63 suites**. This preserves the 802-test baseline and adds 48 tests in six new suites. Gradle retains its existing Gradle-9 deprecation notice and the existing test launcher unchecked warning; no dependency/toolchain changes were made. `git diff --check` reports no whitespace errors.

Synthetic final-run measurements (monotonic wall time / thread CPU; not live latency):

| Workload | Result |
| --- | --- |
| Pending pouch, 1,200 bank IDs, 64 forced ordered commits | 3,611.179ms wall; 1,015.625ms worker CPU; 2,595.554ms IO/scheduling remainder; 56.425ms mean/commit |
| All 26 buybacks, 1,200 IDs, 64 bank commits | 4,801.930ms worker wall; 2,703.125ms CPU; 2,098.805ms IO/scheduling remainder |
| 250 buyback overlay projections, 1,200 IDs | 46.596ms total |
| 100 reused sidebar updates, 1,200-ID fixture | 8.209ms EDT wall; CPU counter resolution reported 0ms |

The new preparation matching is bounded by at most the explicit rune allowlist and adds no extra bank commit or per-frame work. All forced writes remain on the worker. IO/scheduling remainder is wall minus thread CPU, not a disk-only profiler. Measurements do not establish live/native banking latency or a statistical before/after comparison. Automated tests cannot establish banked varbit freshness, physical provenance or live acceptance. Offscreen Swing rendering was inspected for compact alignment and complete text; genuine sprites and UI scaling still need user confirmation in RuneLite.

Git at validation: **26 modified tracked files, 30 untracked files**, including the pre-existing M7 working tree; no files staged and no commit/push/tag/reset. HEAD remains `90bbee1` (master). All earlier working changes were retained.

## Manual acceptance (user only)

Launch with `.\gradlew.bat run` (the agent can offer to run it). Follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) guide for development-client login. Do not reset your existing run or buy extra items just to test.

1. Verify the exact six-section order, compact matching two-column tiles, unchanged sprites/text/tooltips/actions and red reset section at bottom. Resize/use your usual UI scaling; labels/captions must remain fully visible. Cancel reset and verify no change.
2. Open the bank and Buyback Shop. For an eligible unreviewed pouch, select its one shared row and **Review Pouch Contents**. Verify reported rune names/quantities against the actual pouch manually; adjust the list if stale. Cancel before recording and verify no save or payment.
3. For an empty pouch, leave confirmations unchecked: preparation/purchase must not proceed. Explicitly verify empty contents and account for any old unrecorded deposits, then confirm. The normal one-1M confirmation must still show both forms and the correct remainder. Cancel without paying if you do not intend to purchase.
4. For a loaded pouch, **record the verified contents before moving them**. Note the current physical/virtual bank rune counts, gained value, bank worth and unlock count. Manually empty and bank those recorded runes, returning the pouch to the bank. P must increase by their quantities while B, worth, gains and rune unlock count do not. Example: existing earned100 + legacy500 becomes physical600 / virtual100.
5. Reopen review and verify held/remaining amounts. Confirm source and an empty pouch. Purchase intentionally only if desired: exactly one 1M expenditure, one family entitlement/history row, no rune unlock. Withdraw/redeposit the pouch and upgrade/revert only through normal gameplay when convenient: one paid-copy allowance and no new charge. Positive earned runes remain owned.
6. If an unrelated same-ID earned deposit was held, use **Restore earned amount**, review its confirmation, then bank the actual recorded legacy amount separately. The restored earnings must remain credited and the legacy deposit must not credit again. Amounts beyond a finite plan and other rune IDs must track normally.
7. If some recorded contents never reached the bank, finish only after the explicit remaining-runes confirmation. Verify no phantom deficit and future ordinary deposits track. If contents were moved before recording or readings are unavailable/stale, use the named manual list/correction option rather than guessing; no automatic historical debit should appear.
8. Restart during pending preparation, then bank the remaining known legacy runes; they must be held once. Restart after completing and purchasing; payment, entitlement, source record, rune balances and history must survive. Existing pre-safeguard buyers must see the review option without losing the old purchase or paying again.
9. Recheck ordinary legacy withdrawal/return, new deposits, signed counters, native placeholders, search/tabs/fast scroll/partial rows, custom shading/colors and bank responsiveness. GE fills/collections must remain independent and normal bank/mobile-return updates must require no personal tabs or review.

Live confirmation is required before this refinement is considered accepted. No M8 work is included.

## Latest refinement — Automatic pouch deficits (supersedes preparation UI)

The manual pouch recording/review wizard is removed. The sidebar heading is now **Grand Exchange History**, with its layout, icons and actions unchanged.

A new family purchase requires a short-lived verified reading: manually open exactly one eligible inventory pouch, leave its interface open through a settled tick, then return it to the bank and buy it. The adapter detects the interface lifecycle (including reopen), matches displayed rune IDs/quantities against all six supported varbit/enum slots, and verifies the same sole banked form, unchanged contents, current character/session and a two-minute expiry at preview and confirmation. Empty is accepted only from the observed complete display. An unavailable, mismatched, stale or ambiguous reading blocks with one instruction. No inventory ownership/continuous equipment tracking is added.

The purchase commits **B(rune) -= contained quantity**, one 1M GP commitment, one family entitlement and purchase-linked automatic history rows atomically. Raw physical P and the initial legacy snapshot are unchanged. Ordinary deposits add their physical deltas back to B: 100 earned minus500 becomes -400, then depositing500 restores100. Historical high-water marks are retained, so returning those runes does not recognize another gain. Negative contributions remain visible in net worth until banked or explicitly corrected; they are not consumption expenses. Only the 1M buyback increases confirmed spending. Restart/refill cannot replay a receipt.

Format9 is reused with optional verified pouch/form and rune quantities on new purchase receipts. Old receipts decode unchanged; existing payments, entitlements, signed adjustments and archived history are not recreated or revoked. Historical preparation data/reducers remain for saved-data compatibility. Existing paid pending plans retain their finite recorded exclusions; only overlapping units of an old unpaid plan convert to the new verified purchase deficits; non-overlapping known pending arrivals remain protected. No automatic historical quantity guesses or new schema migration.

History lists **Automatic — Legacy Rune Pouch**, rune, signed amount, purchase association and timestamp. The success dialog appears only after save acknowledgment; Cancel writes nothing. Manual Bank Corrections remains available for genuine mistakes, with no normal pouch wizard.

Validation for this narrow pass uses incremental compile and only directly affected tests; no clean build or full suite is run. The previous 850-test result above describes the prior refinement, not this pass. Live pouch UI matching still needs user acceptance. If the native display cannot be matched reliably, the purchase remains blocked rather than assuming emptiness. Existing purchases with unknown original contents receive no inferred historical debit.

Latest-pass validation: `gradlew.bat compileJava compileTestJava test` with filters for RuneLitePouchReaderTest, PouchPipelineTest, PouchPersistenceTest, BuybackPipelineTest, BuybackPersistenceTest, BuybackModelTest and PostM7UiTest: **107 tests passed; 0 failures/errors/skips; 7 classes**. Incremental compilation passed. No `clean`, `build` or full-suite execution occurred in this simplification pass. The old 850-test result applies only to the preceding implementation. Git whitespace validation passed.

Files changed in this simplification: FromScratchPlugin; bank/BankActions, BankController, BankEngine, BankHistory, BankHistoryDialog, BankPanel, RuneLiteBankSource; buyback/BuybackPurchase, BuybackEngine, BuybackView, BuybackDialog; persistence/BuybackCodec, BankRepository; pouch/PouchObservation, RuneLitePouchReader, PouchEngine. Deleted PouchPreparationView and PouchPreparationDialog. Updated only related pouch/buyback fixtures and focused tests, plus these notes, ACCOUNTING, README, CHANGELOG and AGENTS. No overlay, price-service or financial-formula edits.

Manual check: open one eligible pouch in inventory, leave its contents visible through the following game tick, return it to the bank within two minutes and confirm the 1M purchase. Check immediate negative rune counters/history, partial and complete deposits, preserved earned runes, restart and one shared entitlement. Cancel and unverified/ambiguous contents must write nothing. Existing paid users must not be charged or assigned guessed original contents. Confirm the Grand Exchange History heading and unchanged section order. Only the user can establish native UI proof availability and live acceptance.

Git at this pass: 29 modified tracked files, 28 untracked, none staged; this includes the pre-existing M7/refinement working tree. HEAD remains 90bbee1. No commit, push, tag, progression reset or M8 work.

## Banked-pouch regression correction (current observation path)

Cause: the simplification added an inventory-only proof gate to RuneLiteBankSource.pouch(): it required a Rune Pouch interface-open event, exactly one inventory pouch, widget-item/varbit equality, a cached pouchProof, and a two-minute age limit. A pouch left in the bank could never create that proof, so valid account varbits were rejected before purchase. RuneLitePouchReader.read() retained the earlier direct six-slot account-varbit decoding. The local v0.7.0-buybacks tag resolves to 90bbee1 (M6), with no pouch source in its tree; Git cannot recover the earlier uncommitted M7 reader. No whole-version revert was made.

Resolved RuneLite1.13.3 core RunepouchOverlay reads all six type/quantity varbit pairs directly and resolves EnumID.RUNEPOUCH_RUNE. Client.getServerVarbitValue is also a supported public API documented as the last server-set value, independent of client changes. The restored adapter reads directly on preview and confirmation with a current complete BankObserver observation, client-thread/logged-in identity, loaded enum and one unambiguous qualifying banked form. All client/server pairs must agree; a pending prediction, invalid mapping or unavailable data blocks with its actual cause. Observed zero amounts are valid empty slots, not null-data fallbacks. BankObserver already settles opening/container updates; do not age-gate its cached timestamp/tick (a bank already open when enabling the plugin can have a valid tick0 observation). No pouch withdrawal, inventory/equipment reads, pouch-window polling or extra bank debounce remains.

RuneLite provides no per-pouch refresh timestamp or physical-instance identity. Validation therefore relies on current server/client account state and the complete current bank lifecycle, not an invented proof of an item-specific network response. Multiple banked pouches remain ambiguous and are rejected explicitly. Login/prediction updates can require the next tick; the instruction is to keep the bank open and retry, never to withdraw it.

The automatic purchase transaction, format9 receipts, 1M commitment, shared entitlement, signed rune deficits, high-water protection, atomic writes and replay guards are unchanged. No saves were read or reset during development. The Grand Exchange History heading and accepted layouts/rendering remain unchanged.

Validation: incremental `gradlew.bat compileJava compileTestJava test --tests '*RuneLitePouchReaderTest' --tests '*PouchPipelineTest'` passed: **22 tests, 0 failures/errors/skips**. No clean/full build or full suite. Tests cover banked standard/Divine, empty/unavailable, valid old bank timestamps, server/client disagreement, automatic rune debits, earned100 preserved through -400→100, partial/full deposits, cancel, failure and restart/repeated purchase. Pipeline fixtures now use the real banked reader via the observer without withdrawing/opening the pouch.

Files for this correction: RuneLitePouchReader, PouchObservation, RuneLiteBankSource, FromScratchPlugin, BankController, BuybackDialog and the old error strings in BankEngine/BuybackEngine; BankFixtures, RuneLitePouchReaderTest and PouchPipelineTest; this note and directly affected README/ACCOUNTING/AGENTS wording. No persistence schema or ledger changes.

Manual acceptance: leave the eligible pouch in the bank throughout. Open Buyback Shop, review 500 nature/200 law at1000K, cancel once (no changes), then intentionally buy. Existing nature+100 must become -400, law0→-200; bank the stored runes to restore nature+100/law0 without extra historical gains. Check a genuinely empty pouch, restart, and the same one-payment family entitlement. If data is initializing, wait a tick with the bank open. Native API behavior and in-game acceptance remain user-verified.
