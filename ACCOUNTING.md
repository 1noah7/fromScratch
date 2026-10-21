# Bank accounting policy

**Current contract: M1–M5 accepted; M6 passive GE records/attribution await manual acceptance.** The original multi-container ownership policy is historical and lives in [the archived specification](docs/history/ACCOUNTING_MULTI_CONTAINER.md). It is not a source for current acquisition, transfer or session-gate behavior.

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
6. **No wallet/debt inference:** Bank GP is B(COINS); withdrawing coins does not prove spending. No unobserved inventory/GE wallet exists.
7. **Fixed historical valuation:** current prices cannot alter B or previously recognized gain values.
8. **Character isolation:** verified identity namespaces and CAS isolate runs.
9. **Restart equivalence:** committed P/B survive reload; first next comparison applies only its net difference.
10. **Uncertainty preservation:** null/partial bank data and corrupt/newer saves cannot replace trusted state.
11. **No automation:** observations/rendering/storage never send normal game actions.
12. **Corrections persist:** B-P changes only through explicit confirmed correction/anchor policy, not ordinary bank movement.

See [TEST_PLAN.md](TEST_PLAN.md) for current integration, persistence, UI and accepted M3 regression coverage. Synthetic tests are not manual in-game acceptance.

UI/performance polish changes scheduling, queueing and presentation, not the signed equation or save format. Complete snapshots commit FIFO on the worker; no speculative balances appear as committed. See [the polish report](docs/MILESTONE_4_POLISH.md).

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
