# Accounting policy and invariants

**M2 implements initialization only; the economic engine below remains planned.** Confirmed immutable bank/inventory/equipment quantities, including coins, are legacy. Virtual items, spendable GP, net worth, gained value and unlocked counts remain zero. Initial isolation, character separation, reload and reset preservation are tested. Review the future policy before implementing M5 valuation.

Setup may combine a current complete open-bank container with validated inventory/equipment observations from the same uninterrupted initialization session, at most 60 seconds old. Null never means empty. Personal change events expire the affected observation; a changed previously observed bank invalidates cached personal observations. Incomplete/stale data prevents confirmation. These are setup observations only, not acquisition or transfer accounting. Confirmed baseline data and persistence are unchanged by this correction.

Zero occupied slots is a valid observed state. If an empty personal location has no API container, a complete mounted/visible normal interface must independently verify every initialized empty slot on two different game ticks before supplying a zero-item observation. Unknown/partial views are not zero. This leaves all three required snapshot locations and explicit confirmation intact. Storage is permanently local-only on one designated host; future mobile/other-computer reconciliation uses that host's trusted baseline, without cloud synchronization or remote save merging.

## Ownership

For supported item identity `i` and location `l`, record physical `P(i,l)`, locked legacy `L(i,l)`, and virtual `V(i,l)` quantities separately, using nonnegative long values. Preserve raw item/variant IDs; normalize notes only through explicit policies, preserving doses/charges correctly. Placeholders contribute zero owned quantity.

For a complete, reconciled observation: `P(i,l) = L(i,l) + V(i,l)`.

Unknown containers, in-flight changes, and unsupported storage are not forced into that equation. Preserve reliable history and a reconciliation record; never create negative quantities to make observations match. Virtual ownership includes supported and explicitly unresolved recorded positions. A vanished bank stack alone does not prove loss.

`V_total(i) = sum(V(i,l))` over nonoverlapping positions.

Transfer `q` from A to B: `delta V(i,A) = -q`, `delta V(i,B) = q`, `delta V_total(i) = 0`. Location movement changes neither aggregate basis nor earnings. Identical physical copies cannot be individually identified; reliable transfers need deterministic allocation, uncertain transfers need review. Moving/transforming locked legacy inputs does not make their outputs virtual.

Initialization sets all observed quantities to legacy and all virtual quantities/cash to zero. Natural acquisition of another Fire Cape credits only the new copy. Buyback shifts only the purchased quantity from locked legacy to virtual; physical counts do not change.

## Virtual currency

Keep spendable cash `S`, committed cash `C`, directly earned GP, GP spent/lost/owed as checked longs. `C >= 0`. Physical coins must not determine `S` after initialization. Valuation counts virtual coins/cash once.

- Earn `e`: `S' = S + e`; directly earned GP and gained value increase by `e`.
- Reserve `r`: require `S >= r`; `S' = S - r`, `C' = C + r`; no new gain/spend.
- Refund/cancel `r`: `C' = C - r`, `S' = S + r`; no new gain.
- Settle purchase `p` from commitment: `C' = C - p`; acquire asset with basis `p`; no external earnings from conversion.
- Ordinary purchase/buyback: require nonnegative sufficient spendable cash, deduct atomically with the asset changes.
- Death fee `d`: `S' = S - d`, including supported debt. Approved manual debt corrections require an explicit reason.
- GP owed: `max(0, -S)`. New income repays negative cash before positive spendable cash accumulates.

Validate inputs and use exact add/subtract/multiply, rejecting overflow before mutation. Owed GP must also fit a signed long, so reject cash equal to Long.MIN_VALUE; the supported negative endpoint is -Long.MAX_VALUE. Never clamp, wrap, or partially apply. Unrepresentable valuation shows an unavailable/error result while retaining valid ownership/cash.

Buyback expenditure remains spent even if physical coins stay in place. Do not recompute spendable cash as physical GP minus initial legacy GP. Only reliably classified new inflows change cash.

## Historical basis and lifetime gained value

Proposed policy: pooled weighted-average historical basis per supported economic item identity and run, independent of location. Store total basis `B` and quantity `Q`, not a rounded unit average. External acquisitions add historical reference value. Cash-funded purchases add actual cost and no newly generated value. Gifts use the same rules as other acquisitions.

Removing `q` units allocates `b = floor(B*q/Q)` basis; removing all `Q` units takes all remaining `B`. Retain the remainder with the pool. Use exact JDK intermediate arithmetic (such as BigInteger) and validate the final long. This prevents rounding drift and multiplication overflow. Transfers do not remove aggregate basis. Transformations carry virtual input basis into outputs with deterministic remainder allocation; uncertain transformations require review.

Let `A` be historical external item acquisitions/direct GP earnings, `R` cumulative signed realized trading gains, and `M` audited historical-value corrections:

`Total Value Gained = A + R + M`.

For a sale: `R_delta = actual net proceeds - removed basis`. Record gross/tax components of one sale and use net proceeds once. Sale receipts increase cash but are not entirely newly earned GP/value. Loss-making sales create negative realized gain. Transfers, purchase principal, commitments/refunds, buybacks, and price refreshes create no lifetime gain.

Example: acquire an item at reference value 100 -> gained 100, basis 100. Sell for net 120 -> gained 120 overall, cash 120. Selling for net 80 instead -> gained 80 overall, cash 80. Later collection adds no gain. Repricing an unsold item to 150 changes current worth, not its historical 100 acquisition value.

Unpriced acquisitions have a missing-valuation marker and provisional zero reference value. Filling historical missing data requires an explicit audited correction, not a market refresh. Untradeables without reliable prices default to zero valuation and remain recorded as unlocked.

## Current worth and expenditure

`Current Net Worth = S + C + market value(virtual non-cash assets in nonoverlapping positions)`.

Include supported bank/inventory/equipment/GE positions. Uncollected proceeds separate from spendable cash count exactly once. Negative cash reduces worth. Do not count physical coins, legacy assets, or both offer goods and their collected replacements.

Use cached current RuneLite market estimates and show missing/stale valuation status. High alchemy is a separately labeled metric, not a universal GE fallback. Historical basis never changes on a market refresh.

Keep separate summaries for purchase expenditure, buybacks, death costs, consumption basis, other losses, and realized trading losses. Define the displayed Lost / Spent aggregation when implemented, with categories visible. Purchase cash converted into an asset is expenditure, not immediate economic loss; net worth must not deduct it twice. Consumption removes quantity/basis without erasing past gained value. Net worth is not generally lifetime gained minus all cash spending.

Buybacks reduce cash, release legacy quantities, create no gained value, and assign no earned acquisition basis to unlocked legacy assets. Keep their virtual cost as expenditure. An unpriced untradeable buyback reduces current market worth by its virtual price.

## Ledger and recovery

Each command produces one immutable transaction with multiple entries. Include UUID/sequence/time, character/run identity, raw item ID, item/location/legacy/virtual deltas, cash deltas, classification/source, valuation/basis changes, and confidence/reconciliation references. Classifications: ACQUISITION, TRANSFER, CONSUMPTION, TRANSFORMATION, PURCHASE, SALE, BUYBACK, DEATH_COST, LOSS, MANUAL_ADJUSTMENT.

Validate the whole candidate state, durably commit the whole transaction, then publish it. GP deduction and every bundle piece share a commit boundary. Duplicate references cannot apply twice. Recovery replays complete committed records only. Corrections append linked adjustments instead of editing history.

Offline reconciliation compares complete reliable combined totals. 100 legacy sharks -> 130 after offline play credits at most net 30 after accounting for known ownership; do not invent 50 acquired/20 consumed. Decreases require supported allocation/loss or uncertainty without new earnings. Missing bank data cannot erase trusted history.

## Required invariants

| # | Invariant | Meaning | Automated gate |
| --- | --- | --- | --- |
| 1 | Ownership conservation | Movement does not change total virtual ownership | M4 transfers/splits/equipment |
| 2 | No duplicate earnings | Repeated deposits/withdrawals create no gained value | M4/M5 overlapping/repeated batches |
| 3 | Legacy isolation | Legacy becomes virtual only by legitimate acquisition or explicit buyback | M2/M4/M7 snapshots/duplicates/transforms |
| 4 | Buyback atomicity | Cash expenditure and unlocks occur together or neither occurs | M7 insufficient funds and crash boundaries |
| 5 | Item quantities | Virtual quantities never become negative | M3/M4/M7 validation/underflow |
| 6 | GP debt | Only supported debt actions create negative cash | M5 death/approved correction/ordinary rejection/repayment |
| 7 | Historical valuation | Market updates never modify acquisition values | M5 repricing and audited correction |
| 8 | Character isolation | One character cannot affect another run | M1 header identity; M2+ repository/ledger |
| 9 | Persistence | Reload/restart preserves equivalent ownership/currency | M2/M5/M9 round-trip/replay/migration |
| 10 | Uncertain events | Ambiguity never overwrites trusted history or invents earnings | M4/M6 incomplete/offline observations |
| 11 | No automation | No feature performs normal player actions | Every milestone source/API review and user acceptance |

M2 tests cover initial legacy isolation/empty virtual economy, immutable quantities, character isolation and baseline persistence/recovery/reset. Movement, debt, buyback atomicity and valuation invariants remain deferred with their engines. See [TEST_PLAN.md](TEST_PLAN.md).
