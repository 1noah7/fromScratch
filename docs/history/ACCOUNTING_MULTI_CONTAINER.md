> Historical, superseded by Milestone 4 bank-only tracking. This is not the current product contract.

# Accounting policy and invariants

**M4 implements item accounting and confirmed host reconciliation. Financial accounting remains planned for M5 and later.** The original legacy snapshot, including coins, is immutable. Initialization starts all virtual quantities at zero. Item updates require a current-session baseline, validated observations and durable ledger commit. GP/wealth fields stay zero. M4 automated evidence does not establish in-game acceptance.

## M3 ownership contract

For exact item ID i, persist total T(i) separately from allocations A(i,BANK), A(i,INVENTORY), A(i,EQUIPMENT), A(i,GRAND_EXCHANGE). All values are nonnegative long quantities. Checked sum S(i) = sum A(i,l) must satisfy S(i) <= T(i). U(i) = T(i) - S(i) is unallocated ownership; it is not silently bank-available. Missing records/locations mean zero, not corrupt state. No physical quantity, legacy baseline, market price or rendered slot can create virtual ownership.

The M4 correction deliberately extends the original M3 display contract: persist nonnegative outstanding legacy bank withdrawals `D(i)` separately. The signed purple counter is `A(i,BANK) - D(i)`, never total ownership. Earned quantities stay nonnegative. Placeholders have no earned availability; fillers have no counter. Missing physical slots are never invented; Account Tools retains outstanding deficits, including absent items. Explicit noted/unnoted links share their deficit identity; other variants remain distinct.

Ownership objects and run revisions are immutable. Format 4 adds required nonnegative legacy withdrawal counters and signed counter deltas to the item checkpoint/journal. Verified formats 1/2/3 migrate after forced exact-byte backups. Only verified M2 format 1 initializes zero ownership. Established format-3 history is verified using its original digest encoding, then extended with a migration marker, not rewritten. Counters begin at zero at upgrade because old omitted deficits cannot always be reconstructed safely; explain any older outstanding withdrawals through Advanced quantities in Update Progress. Missing/corrupt/newer data never takes a default-zero path.

Setup may combine a current complete open-bank container with validated inventory/equipment observations from the same uninterrupted initialization session, at most 60 seconds old. Null never means empty. Personal change events expire the affected observation; a changed previously observed bank invalidates cached personal observations. Incomplete/stale data prevents confirmation. These are setup observations only, not acquisition or transfer accounting. Confirmed baseline data and persistence are unchanged by this correction.

Zero occupied slots is a valid observed state. If an empty personal location has no API container, a complete mounted/visible normal interface must independently verify every initialized empty slot on two different game ticks before supplying a zero-item observation. Unknown/partial views are not zero. All three required locations and explicit confirmation remain intact. Storage is permanently local-only on one designated host; M4 mobile/other-computer reconciliation uses that host's trusted baseline without cloud synchronization or remote save merging.

## M4 item accounting contract

The historical LegacySnapshot is never the current legacy balance. For an observed, reconciled location, current locked copies are the residual `L(i,l) = P(i,l) - V(i,l)`. Require `0 <= V(i,l) <= P(i,l)`. Current quantities may change; historical baseline quantities do not. Known virtual copies retained outside supported containers remain `U(i)` (Elsewhere), counted in total but unavailable in the bank. New Elsewhere allocations cannot exceed previously virtual ownership. Future GE allocations remain unsupported.

1. Match ordinary same-ID source decreases and destination increases before considering acquisitions. For physical movement `q`, transfer `v = min(q, V(i,source))`, subtract `v` at source and add `v` at destination. Total virtual quantity is conserved, even with mixed legacy stacks. Apply this virtual-first policy to bank, inventory and equipment transfers.
2. Pair explicit noted/unnoted definition links in the same settled batch. Preserve raw IDs in records, but conserve copies across the pair and canonicalize their lifetime unlocked identity to the unnoted base. Never merge doses, charges, recolors or crafting outputs by name/variation group.
3. For a supported ordinary net increase, first exclude known returned copies, then credit the remaining quantity at its observed location. Gifts/trades follow the same rule as other acquisitions. Physical coins create neither virtual items nor GP in M4.
4. On a confirmed one-item Eat decrease, remove virtual copies first and record CONSUMPTION. On a matching Drop decrease, remove virtual first, record LOSS and retain removed legacy/virtual quantities in ReturnBalance. A menu action alone never changes holdings. Other unexplained decreases require review before commit and default to virtual-first reduction with a return reserve. Ammunition, resources and unsupported consumables are reviewed rather than inferred from every disappearance.
5. Match future increases against return reserves **legacy first**, then virtual. A restored virtual copy does not produce a second acquisition, lifetime unlock or earned value. A previously preserved Elsewhere copy moves back to a location instead of increasing total ownership; its ambiguous return requires review. Conservatively retained reserves may also absorb genuinely new same-ID acquisitions; use an explained correction when the player knows otherwise.
6. Different-ID losses/gains, coin-funded outputs, known unsupported collection and unmatched banking increases without valid receipt evidence require review. Uncertain transformation/collection outputs remain zero-owned by default. Special-storage interfaces/provenance are not exhaustively observable; the honor-system user must correct legacy retrieval. No automatic conversion of locked legacy inputs is permitted.
7. Corrections must fit each observed physical location, preserve nonnegative totals and explain overrides. Rejecting a proposed gain removes its uncommitted acquisition entry/unlock; accepting part records only that part. Preserving a previously virtual copy is not a new acquisition. Corrections append new ledger entries and do not rewrite committed history.

Observation correlation waits for a full quiet game tick. The collector retains same-session aggregate increases observed outside banking before a later deposit; a verified ground Take action is a bounded corroborating hint. These survive bank opening until the exact observation is acknowledged. A later matched deposit relocates ownership rather than erasing that evidence. An unmatched banking increase without evidence remains a review. Missing containers never mean deletion; duplicate/out-of-order batches cannot apply twice.

At login/re-enable, collect a safe complete current-session observation automatically. Unchanged or fully matched transfer-only comparisons establish a new session checkpoint without manual Update Progress. They credit no new acquisitions. Unknown data stays pending; a meaningful unresolved gain/loss needs review under Account Tools. Net increases propose only net holdings after known ownership/returns; net decreases use virtual-first allocation. Physical sharks `100 -> 130` supports 30 new holdings, never an invented 50 acquired/20 consumed history. Completely unobserved activity cannot be reconstructed. Displaying/canceling an editor never commits it.

### Outstanding legacy withdrawals

For a matched bank withdrawal, `legacyMoved = q - v`, then `D' = D + legacyMoved`. A transfer of earned copies alone leaves D unchanged. For a matched legacy deposit, `D' = max(0, D - legacyMoved)`. Equipment/inventory transfers outside the bank do not repay D. A confirmed unmatched legacy bank decrease also retains its deficit; known legacy returns can repay it. All arithmetic is checked and journaled separately from earned deltas.

Example: 100 legacy shields, earned bank 0, D=0 -> withdraw one: display -1; another: -2; return both: 0. Earn/deposit three: earned bank 3, D=0, display 3. With earned bank 3 and D=2, display is 1. Depositing earned items does not erase D; only returning legacy copies or an explicit audited deficit correction does. Mixed withdrawals remain virtual-first: withdraw four from three earned plus legacy -> three earned move out, one legacy deficit appears. Returning those four restores the earned bank balance and repays only the one legacy copy.

Zero/negative display is visually locked even if some earned copies coexist with a larger deficit. This is an informational net balance, not negative ownership or a game-action restriction. If the last physical bank copy disappears, list the retained deficit in Account Tools. Update Progress -> Show unchanged items -> Resolve legacy withdrawal (or Advanced quantities) can resolve/declare older deficits explicitly. This never changes lifetime acquisitions or the original snapshot.

The difference editor hides unchanged rows by default, includes fully vanished legacy-only items, supports name search, numeric sorting, Gains/Losses/Needs Review filters and bulk decisions by exact selected IDs. Newly Earned uses the net gain, or one copy for an explicitly revealed unchanged row; Existing Legacy rejects new ownership while preserving previously earned copies, including Elsewhere. Returned Item suppresses duplicate acquisition; Lost / Consumed resolves a missing-copy classification. All decisions are summarized and revalidated before a compare-and-swap commit. Newer local progress cannot be overwritten.

Items Unlocked counts distinct legitimately acquired identities (explicit notes share their base), including approved new manual ownership, even after all copies disappear. Recent Acquisitions excludes ordinary transfers, drop returns and retained Elsewhere copies. The elapsed duration is wall-clock time since activation, including offline time; financial and gameplay-time metrics remain deferred.

## Ownership

For supported item identity `i` and location `l`, record physical `P(i,l)`, locked legacy `L(i,l)`, and virtual `V(i,l)` quantities separately, using nonnegative long values. Preserve raw item/variant IDs; normalize notes only through explicit policies, preserving doses/charges correctly. Placeholders contribute zero owned quantity.

For a complete, reconciled observation: `P(i,l) = L(i,l) + V(i,l)`.

Unknown containers, in-flight changes, and unsupported storage are not forced into that equation. Preserve reliable history and a reconciliation record; never create negative quantities to make observations match. Virtual ownership includes supported and explicitly unresolved recorded positions. A vanished bank stack alone does not prove loss.

`V_total(i) = sum(V(i,l))` over nonoverlapping positions.

Transfer `q` from A to B: `delta V(i,A) = -q`, `delta V(i,B) = q`, `delta V_total(i) = 0`. Location movement changes neither aggregate basis nor earnings. Identical physical copies cannot be individually identified; reliable transfers need deterministic allocation, uncertain transfers need review. Moving/transforming locked legacy inputs does not make their outputs virtual.

Initialization sets all observed quantities to legacy and all virtual quantities/cash to zero. Natural acquisition of another Fire Cape credits only the new copy. Buyback shifts only the purchased quantity from locked legacy to virtual; physical counts do not change.

## Virtual currency — planned M5

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

## Historical basis and lifetime gained value — planned M5

Proposed policy: pooled weighted-average historical basis per supported economic item identity and run, independent of location. Store total basis `B` and quantity `Q`, not a rounded unit average. External acquisitions add historical reference value. Cash-funded purchases add actual cost and no newly generated value. Gifts use the same rules as other acquisitions.

Removing `q` units allocates `b = floor(B*q/Q)` basis; removing all `Q` units takes all remaining `B`. Retain the remainder with the pool. Use exact JDK intermediate arithmetic (such as BigInteger) and validate the final long. This prevents rounding drift and multiplication overflow. Transfers do not remove aggregate basis. Transformations carry virtual input basis into outputs with deterministic remainder allocation; uncertain transformations require review.

Let `A` be historical external item acquisitions/direct GP earnings, `R` cumulative signed realized trading gains, and `M` audited historical-value corrections:

`Total Value Gained = A + R + M`.

For a sale: `R_delta = actual net proceeds - removed basis`. Record gross/tax components of one sale and use net proceeds once. Sale receipts increase cash but are not entirely newly earned GP/value. Loss-making sales create negative realized gain. Transfers, purchase principal, commitments/refunds, buybacks, and price refreshes create no lifetime gain.

Example: acquire an item at reference value 100 -> gained 100, basis 100. Sell for net 120 -> gained 120 overall, cash 120. Selling for net 80 instead -> gained 80 overall, cash 80. Later collection adds no gain. Repricing an unsold item to 150 changes current worth, not its historical 100 acquisition value.

Unpriced acquisitions have a missing-valuation marker and provisional zero reference value. Filling historical missing data requires an explicit audited correction, not a market refresh. Untradeables without reliable prices default to zero valuation and remain recorded as unlocked.

## Current worth and expenditure — planned M5+

`Current Net Worth = S + C + market value(virtual non-cash assets in nonoverlapping positions)`.

Include supported bank/inventory/equipment/GE positions. Uncollected proceeds separate from spendable cash count exactly once. Negative cash reduces worth. Do not count physical coins, legacy assets, or both offer goods and their collected replacements.

Use cached current RuneLite market estimates and show missing/stale valuation status. High alchemy is a separately labeled metric, not a universal GE fallback. Historical basis never changes on a market refresh.

Keep separate summaries for purchase expenditure, buybacks, death costs, consumption basis, other losses, and realized trading losses. Define the displayed Lost / Spent aggregation when implemented, with categories visible. Purchase cash converted into an asset is expenditure, not immediate economic loss; net worth must not deduct it twice. Consumption removes quantity/basis without erasing past gained value. Net worth is not generally lifetime gained minus all cash spending.

Buybacks reduce cash, release legacy quantities, create no gained value, and assign no earned acquisition basis to unlocked legacy assets. Keep their virtual cost as expenditure. An unpriced untradeable buyback reduces current market worth by its virtual price.

## Ledger and recovery

M4 uses immutable batches with character/run/session, UUID, sequence, timestamp, prior revision/head and a resulting-checkpoint checksum. Item entries include their own UUID, raw item ID, quantity, signed virtual total/location deltas, source/destination, reason and confidence. Implemented classifications: ACQUISITION, TRANSFER, CONSUMPTION, LOSS and RECONCILIATION_ADJUSTMENT. Confidence is OBSERVED, NET_RECONCILED or MANUAL. Cash, reference valuations, basis and financial classifications are deferred until their engines exist.

Validate the whole candidate and verify ledger deltas against ownership, then force the immutable journal segment and recoverable checkpoint before publishing current. Publish UI/render state only after storage succeeds. Duplicate commits are idempotent, stale revisions fail, and only the checkpoint's validated referenced chain is authoritative. Orphan interrupted segments are preserved without automatic replay. Load verifies identity, sequence, checksums, event uniqueness and head consistency; failure preserves files and blocks tracking. Future GP deductions/buyback pieces must share the same all-or-nothing commit boundary. Corrections append adjustments instead of editing history.

Offline reconciliation compares complete reliable combined totals. 100 legacy sharks -> 130 after offline play credits at most net 30 after accounting for known ownership; do not invent 50 acquired/20 consumed. Decreases require supported allocation/loss or uncertainty without new earnings. Missing bank data cannot erase trusted history.

## Required invariants

| # | Invariant | Meaning | Automated gate |
| --- | --- | --- | --- |
| 1 | Ownership conservation | Movement does not change total virtual ownership | M4 transfer/equipment/note tests; Elsewhere return regression |
| 2 | No duplicate earnings | Repeated deposits/withdrawals create no gained value | M4 no duplicate acquisitions/returns; value equation deferred M5 |
| 3 | Legacy isolation | Legacy becomes virtual only by legitimate acquisition or explicit buyback | M2/M4/M7 snapshots/duplicates/transforms |
| 4 | Buyback atomicity | Cash expenditure and unlocks occur together or neither occurs | M7 insufficient funds and crash boundaries |
| 5 | Item quantities | Virtual quantities never become negative | M3/M4/M7 validation/underflow |
| 6 | GP debt | Only supported debt actions create negative cash | M5 death/approved correction/ordinary rejection/repayment |
| 7 | Historical valuation | Market updates never modify acquisition values | M5 repricing and audited correction |
| 8 | Character isolation | One character cannot affect another run | M1 header identity; M2+ repository/ledger |
| 9 | Persistence | Reload/restart preserves equivalent ownership/currency | M2/M3/M4 journal/checkpoint/return/migration/restart; currency M5 |
| 10 | Uncertain events | Ambiguity never overwrites trusted history or invents earnings | M4 pending review, rejected gains, incomplete/offline observations; GE M6 |
| 11 | No automation | No feature performs normal player actions | Every milestone source/API review and user acceptance |

M1–M4 tests cover initial isolation, immutable baselines, ownership/location conservation, uncertainty gates, character separation and journal-backed persistence/recovery. Debt, buyback atomicity, historical financial valuation and total gained-value equations remain deferred with their engines. Source/API review supports the no-automation design; only the user can perform in-game acceptance. See [TEST_PLAN.md](TEST_PLAN.md).
