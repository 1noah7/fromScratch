# Bank accounting policy

Current Milestone 9.2 rules. M10 does not change balances, purchases, saves or gameplay. Earlier equations, migrations and superseded experiments are preserved in [ACCOUNTING_PRE_M10.md](../history/ACCOUNTING_PRE_M10.md). [Architecture](ARCHITECTURE.md) and the [reviewer source map](../PLUGIN_HUB_REVIEW.md) provide implementation pointers.

## Signed bank progress

Let `P` be the last complete physical bank, `P'` the next complete physical bank, and `B` the saved signed virtual balance. Ordinary changes use checked long arithmetic:

```text
B' = B + (P' - P)
```

Initial `B` is zero. The confirmed initial legacy snapshot is immutable. Negative balances are valid; they are not automatically clamped or forgiven. Positive corrections cannot exceed the current observed physical quantity. Placeholder templates contribute zero physical quantity. Normalization for rendering does not merge unrelated accounting IDs.

Bank tabs, searches and Virtual Bank visibility never alter `P`. The complete API container remains authoritative. Missing data is unknown, never an empty bank. Withdrawals are not evidence of consumption, spending, equipment use or provenance. Offline/mobile reconciliation records the next net bank change, without inventing unseen transactions.

## GP and paid legacy copies

Raw signed coins remain part of `B`. Available virtual GP is raw signed coins minus cumulative committed buyback payments. Effective projections subtract the payment once and grant only the catalog's qualifying physical-copy roles; they never fabricate physical items or give a buyback a GE market value.

A purchase validates current bank/identity/revision, legacy possession, approved variants, definitions, funds and any storage-content proof. The payment, entitlement, adjustments and audit commit atomically. A failed commit spends nothing and grants nothing. Unique purchase IDs and durable entitlements prevent a second charge after restart or a family variant change. Naturally earned copies do not require a legacy buyback.

Rune Pouch/Divine Rune Pouch share one **1,000,000 GP** payment. Closed/open Looting Bag share one **10,000 GP** payment. A storage buyback unlocks the equipment, not its pre-existing contents. Verified contents create linked negative signed balances in the purchase transaction, including coins for the bag. Unknown/stale contents are not assumed empty. Banked Looting Bag emptiness uses the accepted bank-side proof/invariant, not absence of an item container alone.

Example: earned nature runes `+100`, legacy pouch contents `500`. Purchase records `-500`, leaving `-400`. Depositing the old 500 produces `+100`. The earned 100 survive; the legacy return is neutral. Offsets are not consumable losses, income or additional GP charges. Later refills do not create new offsets. Old legitimate purchases/adjustments remain preserved; no historical contents are invented.

## Grand Exchange

GE observations record cumulative executed quantity/reported GP, direction, state, durable local slot generation and uncertainty. They do not directly mutate bank quantities or GP. Bounded, once-consumed possible conversion budgets prevent reliable observed exchanges from creating duplicate financial gains; they do not assert collection destination or realized profit.

Player history is a derived finalized-offer view over retained audit evidence. Completed offers and canceled positive partial fills produce one row per lifecycle. Pending/active partials, zero fills, duplicates and slot clears do not appear. Restart preserves evidence and slot reuse creates a separate generation. Aggregating history creates no new economic transaction.

Tax is estimated where the client does not expose a verified settlement. Labels disclose that fact. Pending reserved GP and unexecuted canceled quantities are not permanent expenditures. A GP/item conversion is not new wealth merely because it later reaches the bank.

## Values and progress

Current Bank Net Worth uses the existing **signed** effective balances, including available GP once, with cached guide prices. Unknown, zero and stale prices remain distinct. Missing prices yield a disclosed subtotal; stale prices are estimates. Untradeable entitlements are not added as market assets.

Peak Progress Value is the monotonic highest reliable priced virtual wealth plus confirmed permanent progression expenditures. Committed buybacks count once. Legacy offsets, returns, ordinary bank transfers and GE conversions do not independently earn progress. Missing/stale checkpoints cannot invent a reliable new peak. Older runs begin with a conservative measured baseline; unknown historic peaks are not reconstructed as exact.

Total Value Lost / Spent includes confirmed expenditures and clearly labeled supported estimates, not ordinary withdrawals or legacy-content deficits. A genuinely new run displays `0 GP`; an estimate uses a leading `~`. Incomplete historical measurement is disclosed rather than fabricated as zero.

Earlier item high-water/fixed-price gain evidence is retained for audit and compatibility. Its historical lifetime-gain tile was replaced by Peak Progress Value; it is not another current wealth metric.

## Durability and correction

Forced journals/checkpoints, atomic replacement, CAS and guarded callbacks protect each commit. Migration archives original evidence before conversion and never silently rebases a run. Corruption/load failure preserves evidence and requires safe recovery; reset requires successful archive and confirmation.

Manual Bank Corrections remain explicit signed-balance corrections. Export/import replaces a verified same-character run after preview/confirmation and backs up existing progression; it never merges divergent histories. All records are local to the designated host. Time Played is an independent logged-in host-tick counter, not a financial input.
