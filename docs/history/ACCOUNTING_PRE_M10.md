# Bank accounting policy

**Current contract: M1–M9.2 accepted; M10 preserves economic behavior and save formats.** The original multi-container ownership policy is historical and lives in [the archived specification](ACCOUNTING_MULTI_CONTAINER.md). It is not a source for current acquisition, transfer or session-gate behavior.

M9 retains the signed bank equation and existing economic history. Restored native filtering never owns quantities; complete bank-container observations remain authoritative. Peak Progress Value replaces only the dashboard gain metric. Explicit local import/recovery never merges or replays histories and preserves prior evidence. See [M9](MILESTONE_9.md).

M8 does not alter the equations below, GP, gains, GE classification, buybacks or pouch offsets. Examine and the native Virtual Bank filter read acknowledged effective bank balances (including paid-copy projections); neither owns quantities or creates transactions. Item History shows physical movements and signed adjustments separately, labels observed GE executions without inventing collection provenance, and never calls a withdrawal consumption/spending. Time Played is a separate host-only logged-in GameTick counter, not an economic event or wall-clock run duration. Its first baseline does not reconstruct earlier gameplay or mobile time. See [M8](MILESTONE_8.md).

## Quantities and equation

For each exact API item ID i:

- `L(i)` is the immutable initial physical bank quantity (legacy baseline).
- `P(i)` is the last durably committed complete physical bank quantity.
- `B(i)` is signed bank progress, independent of original game quantities.
- Missing map records mean zero. Physical quantities are nonnegative. B may be negative.

A new run has `P = L`, `B = 0`, after player attestation and explicit Start confirmation. Personal inventory/equipment are not inspected; the player banks them manually first. Special storage is excluded.

For a new complete normal-bank observation `P'`:

```text
delta(i) = P'(i) - P(i)
B'(i)    = B(i) + delta(i)
```

All stored quantity arithmetic uses checked longs. A batch with overflow/invalid data is rejected atomically. Stored B excludes Long.MIN_VALUE for safe delta operations; its formatter can still handle either long bound without negation. `B(i) <= P(i)` when anchored; there is no lower bound tied to P, because an absent item may retain negative progress.

Without corrections, `B = P - L`. A correction introduces an explicit offset `C`, giving `B = P - L + C`. Later physical deltas preserve C. Corrections never rewrite L.

| Action | P | B |
| --- | ---: | ---: |
| Initial legacy axe | 1 | 0 |
| Withdraw axe | 0 | -1 |
| Return axe | 1 | 0 |
| New logs initially absent | 0 | 0 |
| Deposit two logs | 2 | 2 |
| Withdraw one log | 1 | 1 |
| Burn outside bank, reopen unchanged bank | 1 | 1 |

Mixed stacks use the same equation: legacy 100, deposit 3 -> P103/B3; withdraw 4 -> P99/B-1; return 4 -> P103/B3. No earned/legacy instance allocation or elsewhere ownership is inferred.

## Meaning and limits

Bank progress measures net bank holdings relative to the baseline and explicit corrections. A deposit alone does **not** prove an acquisition. A withdrawal does **not** prove spending, loss or consumption. Repeated withdrawal/redeposit cycles have zero net progress and create no lifetime acquisition count.

Items Unlocked counts current distinct IDs with `B > 0`; a withdrawal can reduce that count. Automatic Changes history reports signed physical bank changes, not earnings. M5 financial estimates below respect this measurement boundary; no trade/cost-basis engine, spendable wallet or acquisition attribution is claimed.

Exact IDs/variants remain distinct. No noted/unnoted, charged/degraded, crafting or special-storage equivalence is inferred. API bank placeholders/fillers/empty slots contribute no physical quantity. Rendering never invents absent slots; a negative absent item remains searchable in Bank Corrections. Only a positive progress balance removes the lock effect.

## Observation and durability

Only a currently mounted, visible normal bank without a PIN and complete non-null API bank data can advance P. A complete aggregate is read at the first game-tick boundary after invalidation, following packet processing, then cached until the next bank/interface event. An observed empty container is valid; unavailable/partial is not an empty bank. Search, tabs, scrolling and reordering do not change aggregate P. Closing before settling postpones the comparison, not a write of empty holdings.

The candidate anchor, balances and matching audit are committed as one forced journal/checkpoint publication. The old cursor remains authoritative until durable acknowledgment. Failed writes, unreferenced orphans, restarts and repeated bank openings cannot replay a committed delta. CAS rejects stale corrections and competing clients. A lost acknowledgment after successful publication is recovered by loading current, not by applying the delta again.

## Host sessions and corrections

There is no normal-session approval gate. The first complete bank observation in a new host session automatically compares against saved P, applies net deltas and stores a summary of that first comparison. Later same-session changes do not replace the summary. Same unchanged session/aggregate produces no journal append. A new session may record a no-change marker so its comparison remains visible.

Mobile/other-PC changes are exactly the difference between final bank holdings and committed P. Items earned and consumed entirely outside bank cannot be reconstructed. There is no cloud/remote merge.

Bank Corrections searches known current, baseline, history and signed-balance IDs even with no pending changes. Desired signed B or signed adjustment must satisfy positive B <= current observed P and checked arithmetic. Preview/confirmation binds to the committed revision and current complete bank; stale/closed/unavailable data prevents saving. Cancel writes nothing. Manual corrections have distinct audit entries and are not represented as acquisitions.

## Existing saves

Formats 1–4 are verified using their historical schema and journal encoding before exact-byte, forced, manifest-verified archives. Preserve run identity/start and original three-container snapshot/document. Import bank display as `old BANK allocation - old outstanding legacy deficit`; do not convert personal/Elsewhere ownership into bank progress.

Imported active runs have no bank-only comparison anchor until one player-confirmed bank observation. That anchor is a deliberate boundary, not an inferred replay of unrecorded earlier changes. Keep all valid balances; excessive positives require banking those copies or explicit approval of listed reductions. Record reductions in the anchor audit. Afterward every host session uses bank-only automatic deltas. Historical acquisitions remain archived, not appended to new financial/lifetime statistics.

## Invariants and automated evidence

1. **Net conservation:** equal final bank aggregates preserve B, irrespective of outside-bank activity.
2. **No duplicate earnings:** cycles/reopen/repeated events create no lifetime metric and do not replay deltas.
3. **Baseline isolation:** L and the original imported document never change during an active run.
4. **Atomic authority:** P, B and audit publish together after storage acknowledgment.
5. **Signed progress:** P >= 0, B <= P, absent IDs are zero, negative B remains valid/searchable.
6. **No wallet/debt inference:** raw bank GP is B(COINS); available GP subtracts durable buyback commitments once. Withdrawing coins does not prove spending. No unobserved inventory/GE wallet exists.
7. **Fixed historical valuation:** current prices cannot alter B or previously recognized gain values.
8. **Character isolation:** verified identity namespaces and CAS isolate runs.
9. **Restart equivalence:** committed P/B survive reload; first next comparison applies only its net difference.
10. **Uncertainty preservation:** null/partial bank data and corrupt/newer saves cannot replace trusted state.
11. **No automation:** observations/rendering/storage never send normal game actions.
12. **Corrections persist:** B-P changes only through explicit confirmed correction/anchor policy, not ordinary bank movement.

See [TEST_PLAN.md](../technical/TEST_PLAN.md) for current integration, persistence, UI and accepted M3 regression coverage. Synthetic tests are not manual in-game acceptance.

UI/performance polish changes scheduling, queueing and presentation, not the signed equation or save format. Complete snapshots commit FIFO on the worker; no speculative balances appear as committed. See [the polish report](MILESTONE_4_POLISH.md).

## M5 market and currency equations

`Bank GP = B(ItemID.COINS)`; `N = sum(B(i) * p(i))` over priced nonzero balances. Coins use p=1 exactly once. Legacy holdings contribute nothing unless B changes; negative balances subtract market value. Unknown prices are excluded with an explicit priced-subtotal label and count. Stale last-received values may support a current estimate with a warning, but do not establish a historical gain price. A present zero-price feed record is verified zero, not missing.

Price identity resolves only noted forms to their linked unnoted ID; bank quantities/high-water IDs remain exact. Charged/degraded variants are not arbitrarily merged. RuneLite ItemClient's bulk feed supplies GE guide prices; no private holdings are sent. Refresh every 30 minutes on a dedicated worker, retry failures after one minute, retaining stale last-good values. Receipt time is not a server market timestamp. Current caches are ephemeral; persisted historical quotes survive restarts. Arithmetic uses arbitrary precision for the signed sum and reports out-of-long-range totals unavailable; bank/history changes still use checked longs.

## Estimated recorded gains

H(i) starts at zero for a new run. For an automatic bank update only:

```text
q(i) = max(0, B'(i) - H(i))
H'(i) = max(H(i), B'(i))
known gain addition = q(i) * unit price recorded at recognition
```

Persist one uniquely identified event per changed high-water ID, tied to the parent bank-audit ID. Missing/stale price at recognition stores explicit unpriced quantity/evidence and contributes no asserted known GP. Never backfill with future prices. Recognized known GP and unpriced quantities only accumulate within the run. Market refresh writes no history.

Corrections/legacy anchors can raise H to current B, never lower it or generate gain events. Thus correction→withdraw→redeposit cannot manufacture gains. Example: H=2, B=2; withdraw to B=1 and return to B=2: q=0 both times. Later B=3: q=1. Legacy B=-1→0 creates no gain. This deliberately misses replenishment below a prior peak. Conversions/sales across IDs may appear as new progression maxima: this is an estimated gross bank-progression metric, not realized profit or full lifetime income.

Format-5 migration archives every original checkpoint/segment byte before changing current, then adds a linked FINANCIAL_BASELINE operation. Preserve P/B/run/start/initial bank and all older history; H=max(0,B), known/unpriced financial totals start at zero with an imported measurement timestamp. No M4 gain history is invented. Formats 1–4 retain their existing explicit bank anchor workflow. Financial archive manifests remain checked on load. No changes replay after restart.

## Explicit losses and expenses

Ordinary withdrawals produce no loss/spending evidence. No explicit records means **Not tracked**. A player can record a positive verified GP amount, reason and confirmation from the loss/spent tile. This statistical event increases the recorded expense total only: it does not deduct B(COINS), change P/B/H or alter gained value. No automatic inference or historical transfer reclassification occurs. Events retain identity/time/source/amount/reason in the same auditable durable chain. Corrections of financial records and future GE/buyback categories require a later explicit design; this milestone does not invent those transactions.

Both quantity and financial evidence publish atomically after forced persistence/CAS validation. Persistence recomputes financial transitions from the event evidence, rejects missing/duplicate/mismatched events and overflow, and never advances a failed observation cursor. Derived current valuation alone never writes a save.

## Final-copy withdrawal and native placeholders

An ID absent from a complete physical aggregate has P=0. The union of previous/current IDs therefore applies the final negative delta: legacy P=4/B=0 → P=1/B=-3 → P=0/B=-4. Negative B is retained independently of physical presence and contributes to signed net worth whenever priced. Returning 1, 2, then 4 copies produces B=-3, -2, then 0 without historical gains or expenses.

Placeholders represent zero physical copies, even if their variant has a nonzero widget/container quantity. Rendering resolves a native placeholder variant to its original item ID using the supported definition link; this is not an accounting conversion, new physical item or acquisition. No-placeholder disappearance removes only the native slot, not B/history/value/searchability. Null or partial bank data remains unknown, never P=0.

Only the bank participates in initial legacy setup. GE offers and other storage are excluded unless the player first collects/banks those items. Later collection can appear as positive bank progression and require an explicit Manual Bank Correction. There is no GE inspection or mandatory empty-offer verification; reliable GE accounting is planned for Milestone 6, which is not implemented here.

Compact GP now shares item-stack integer K/M thresholds; exact tooltips and every numeric quantity, market calculation, historical quote, high-water mark and financial event are unchanged.

## M6 exchange records and conservative conversions

P/B, Bank GP and current worth keep their existing definitions. A buy/sell/fill/cancel/collection observation never directly changes any of them. Reservations and uncollected items/cash stay outside bank worth. Reported execution GP and actual physical bank deposits are different measurements; do not apply both as quantity/currency movements.

Within a compatible offer generation, record `dq = filled' - filled` and `dv = reportedGP' - reportedGP`. Duplicate observations add nothing; regressions preserve uncertainty rather than negative/invented fills. Initial already-filled snapshots are recorded as cumulative existing evidence, not an asserted historical sequence. Compatible offline increments are explicitly conditional because identical unseen reuse cannot be ruled out. Status/clear events carry no new executed quantity. There is no verified cost-basis allocation or realized-profit figure.

Each fill/snapshot can create a bounded possible purchase/proceeds budget; terminal buy refunds and canceled sell returns can create return budgets. Compatible earlier bank withdrawals can be associated once. Positive bank movements consume compatible budgets once (FIFO, exact item identity, up to 24 hours; GP budgets can cover coins/platinum units). The immutable audit stores trade/bank references and actual matched units. These links are possible provenance, never certainty about identical copies or collection destination. Expiry/capacity removes only automatic matching context; original unmatched records remain in history.

For a new automatic bank high-water portion `q = max(0, B' - H)`, let `c` be compatible conversion units consumed by this bank operation:

```text
new estimated external-gain units = max(0, q - c)
H' = max(H, B')
B' = B + (P' - P)              [unchanged]
```

Only the remaining gain units receive fixed historical quotes. Possible conversion ceilings still prevent withdraw/redeposit from later manufacturing gain. Unrelated item IDs remain eligible. Because provenance is deliberately conservative, unrelated same-ID gains can be excluded; deposits beyond the compatible budget remain eligible. Missing, expired or late trade evidence cannot reconstruct every conversion, and earlier M5/M6 recognized history is not silently rewritten. The result remains an estimated bank-progression statistic, not complete lifetime income or profit.

Seller fee estimate for a newly observed executed increment is `quantity * min(5,000,000, floor(floor(reportedGP / quantity) / 50))`, except explicitly published exempt tools/bonds/Mind runes. Sub-50 average prices yield zero. This is not verified per-fill tax: other exemptions, varying unit executions, old rates and the API's undocumented seller gross/net basis may differ. Record method/source/amount once with the trade identity; never charge buyers. Legacy snapshots do not invent earlier fees. Display explicit expenses + estimated GE fees with an estimate marker, without reducing Bank GP a second time. Native settlement is counted only when physically observed in the bank.

Known pre-start offers are identified by actual setup lifecycle observations, not a fabricated initial GE baseline or clock ordering. Their compatible returns are excluded from estimated gains; signed bank balances still obey the bank equation. A concise Manual Bank Corrections notice addresses any legacy quantity offset the player wants corrected. No initial GE screen or normal review/session gate is introduced. Schema-7 migration preserves every prior financial total and quote; only future observations use this classifier.

## M7 paid legacy allowances

Raw P/B/L and the bank delta equation remain authoritative and unchanged. A buyback is a separate, explicit payment/permission event, never an observed bank deposit or new gain:

```text
S = sum(unique buyback GP payments)
A = B(COINS) - S
```

Available GP A funds purchases; reject a price greater than A. The dashboard/coin counter show A, while saved B(COINS) remains raw. Physical coins are untouched. Current worth sums effective item balances at current verified market prices with coins=A once. Do not deduct S again. Shop prices never become market quotes. Confirmed spending adds S once to existing explicit expenses and independently estimated GE fees; historic gains/quotes and GE records do not change on purchase.

Each approved equipment role has explicit quantity one, permission tier and nonpositive bank floor f. Raw family progress R is the sum of B over its allowlisted variants. At first purchase set f=min(0,R); each later bank/correction operation advances f=max(f,min(0,R)). A paid allowance is 1 when R>=f, otherwise 1+(R-f). Allocate positive natural aggregate progress and at most one paid unit only to physical bank capacity; nonpositive remainder remains signed. Natural positive units allocate first; the paid allowance can use only the purchased tier or lower. A Void upgrade preserves the existing floor/one copy and favors an Elite form. It cannot refill a withdrawn paid copy. Negative remainder uses a present or previously negative variant; absence never creates a widget. Other item IDs keep raw B.

The floor prevents earlier unrelated legacy withdrawals from absorbing a new purchase: legacy4/current1/raw-3, then buy one banked copy, displays1 with f=-3. Returning the other three advances f to0 and still displays1. Withdrawing one now displays0. It never grants four copies from one payment. Floors and quantity observations commit together, with repository recomputation and restart equivalence.

Rune Pouch / Divine Rune Pouch and their two verified Trouver forms share one `rune_pouch_family` role/payment, not one entitlement per ID. A withdrawn paid legacy pouch has R=-1 and zero banked availability; banking either form restores R=0 plus one paid allowance. Multiple forms cannot multiply that allowance. A genuinely positive additional aggregate bank unit remains separate. The pouch note, runes, minigame substitutes and unlisted variants do not participate.

Eligibility requires a current complete bank, a real untradeable definition and still-locked stock (`physical family total > max(0, raw family progress)`), plus the required physical permission tier. This is aggregate honor-system evidence, not proof of identical-copy provenance. A naturally positive copy alone needs no payment and cannot qualify as locked stock. Every outfit role must qualify together. Base-to-Elite pricing gives credit only for its already-paid base. One role cannot be purchased repeatedly.

Items Unlocked counts other effective positive bank IDs plus each durable paid role once. Natural and paid variants inside that role are not counted twice. The existing bank-only data cannot reconstruct a natural lifetime-acquisition count; no such history is invented. Corrections continue to edit raw B and preserve S, paid permissions, L and history. They never refund spending or create a second allowance.

Payment, entitlement and purchase history share one BUYBACK audit/checkpoint commit with no raw P/B delta or financial gain event. Repository validation recomputes price, funds, eligibility and state; stale CAS, duplicate purchase IDs and mismatched state reject the entire operation. Only acknowledged saves update projections. Format 8 archives all verified M6 bytes before adding empty buyback state; no previous payment is inferred. See [M7 catalog/IDs, limitations and acceptance](MILESTONE_7.md).

## Post-M7 — One-time legacy pouch transfer preparation (format 9)

The shared 1M entitlement covers the pouch, never its contents. `PouchState` is character/run-scoped through the existing BuybackState, with UNREVIEWED → PREPARED → READY phases. A direct UNREVIEWED → READY transition needs explicit empty-pouch and prior-deposits-accounted player confirmations. Reported zero varbits alone never qualify.

For each rune ID during PREPARED, let D=P'-P and R be the still-expected player-verified legacy quantity:

```
held now = min(max(D, 0), R)
B'       = B + D - held now
R'       = R - held now
held'    = held + held now
```

P always advances to the full complete bank observation. Only new matching positive arrivals consume this finite source plan. Existing B is never blanket-debited. With P100/B100 and a recorded 500 legacy nature runes, P600 remains B100; no high-water gain, added worth or rune unlock is created. Other IDs and any excess above the finite plan follow ordinary bank rules. This explicit legacy transfer exception and its evidence share the same atomic journal/checkpoint as P/B/financial/GE updates. The immutable initial bank is unchanged. GE conversion matching receives the actual progress delta, excluding these legacy transfers.

Identical banked runes have no provable physical provenance. The player records contents before moving them, banks them next, and verifies held amounts on finishing. Keep unrelated same-ID deposits separate until review. A confirmed RESTORE_EARNED releases a misidentified held amount back to B, replenishes the pending legacy plan and recognizes only legitimate current high-water progress with the quote at release; no earlier quote is fabricated. The normal positive-B<=P check still applies. Missing/consumed/unarrived runes can be explicitly retired on an empty-pouch confirmation, with no bank debit or phantom debt. READY stops reservations permanently. Repeated observations/reopens cannot apply a budget twice.

Recorded = remaining + held + retired, per ID, using checked longs. All state is immutable and source-allowlisted; every registration, release, finish and held transfer is journaled. Pouch permission, raw coins, committed buyback GP and other entitlements retain their M7 rules. Pouch purchase is blocked until READY; old purchases are never revoked or charged again.

If contents were moved before recording, quantity and source are unknown. Do not infer a debit. The relevant dialog links Manual Bank Corrections and requires explicit acknowledgment of prior corrections and empty contents. Manual corrections fix known bank counts; this workflow cannot reconstruct or automatically erase old historical gain prices. Preserve that history and disclose the uncertainty. No continuous personal-container accounting or automatic game actions are introduced. See [review procedure and limitations](POST_MILESTONE_7.md).

## M9.1 — Completed GE history is presentation only

Native-tab filtering never supplies physical bank quantities. GE history derives completed offers from original cumulative observations; active partial fills retain their existing immediate internal accounting while remaining hidden from history. Finalization and history reads create no economic events. Completed-offer tax is explicitly an average-price estimate; financial totals retain original per-fill estimates, which can differ with rounding/varying execution prices. No new verified-tax claim, bank delta, gain, spending entry or peak is created by aggregation. Existing audit files and economic save schemas are unchanged.

## Automatic Rune Pouch buyback adjustments (current workflow)

This supersedes the manual preparation workflow above for new purchases. After a current banked-pouch client/server varbit read and explicit purchase confirmation, the one BUYBACK audit applies B(rune)' = B(rune) - observed pouch quantity while P and the immutable initial bank stay unchanged. It simultaneously records the shared entitlement and exact 1M commitment. Rune debits are purchase-linked **Automatic — Legacy Rune Pouch** adjustments, never consumption/expense or new gains. H is unchanged; ordinary bank deposits restore B toward its prior value without repeat historical gains. Existing earned100 minus legacy500 becomes -400; depositing500 restores100.

New receipts persist the verified pouch form and positive legacy rune quantities; repository validation recomputes all negative deltas from that evidence. Receipt IDs, shared entitlement uniqueness, CAS and atomic acknowledgment prevent replay. Deficits remain signed through restart/refills; no automatic forgiveness or extra GP charge occurs. Current worth includes the signed rune amounts using the existing valuation formula. Existing receipts, entitlements and legitimate old preparation adjustments are preserved without invented historical quantities. Format9 remains in use; old preparation models are retained only for compatibility. Normal UI no longer exposes a preparation action.

## M9 correction: peak and legacy bag

Peak = max(previous peak, current signed virtual net worth + confirmed permanent expenditures). The candidate excludes purchased equipment allowances, includes bank coins once and buyback costs once, and includes explicit verified expenses. Estimated GE fees are not confirmed permanent-spending additions. All nonzero candidate quantities must have fresh known prices; unknown values are not zero. Historical fixed-quote gains remain stored but are no longer the dashboard metric. Initial peak measurements establish a current conservative baseline without claiming earlier maxima.

Looting bag (ItemID.LOOTING_BAG 11941) and open bag (ItemID.LOOTING_BAG_OPEN 22586) share looting_bag_family, one copy and one 10,000 GP payment. The note is excluded. Open/closed projection pools signed family progress without creating copies. An earned copy remains free; it does not unlock another legacy copy.

Verified original contents apply B(item) -= contents(item) atomically with the purchase. P and the initial snapshot never change. Later physical deposits naturally restore B, preserving earlier earned balances; their return below the previous financial high-water mark adds no historical gain. Coins follow the same adjustment: available GP = adjusted raw coin B - committed buybacks. Only the 10K cost is spending, never the contents offset. No automatic forgiveness, reapplication on refill/restart or guessed old debts. The history category is Automatic — Legacy Looting Bag. Banked contents support requires live acceptance; unavailable or mismatching native data blocks the purchase without a manual wizard.
