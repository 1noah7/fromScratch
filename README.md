# From Scratch

<img src="icon.png" width="48" height="48" alt="Purple coin stack" />

By **Pilots r kwl**. Java 11, RuneLite, BSD 2-Clause.

Start a bank-progress challenge on your existing Old School RuneScape character. Keep your levels and unlocks while building a separate signed bank balance. This is an honor-system challenge; the plugin never performs game actions.

**M1–M5 accepted; M6 awaits manual acceptance.** M5 is preserved as `v0.5.0-wealth`. M6 adds passive GE execution history and conservative bank attribution. No Plugin Hub submission or approval is claimed. Buybacks, cloud and personal-container ownership remain unsupported. Earlier multi-container M4 workflows are superseded.

## Build and launch

Use a Java 11 JDK and the existing Gradle Wrapper:

```powershell
.\gradlew.bat clean build
.\gradlew.bat run
```

In IntelliJ use Java 11 for Gradle and run the `run` task or `FromScratchPluginTest.main`. On Linux/macOS use `bash ./gradlew`. Follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions for the development client. Never put credentials or launcher tokens in the repository.

Enable **From Scratch** and open its purple coin sidebar. No public Plugin Hub installation is available yet. Screenshots await user acceptance; agents do not operate RuneScape.

## Before Starting

1. Deposit everything from inventory into your normal bank.
2. Unequip all gear and bank it.
3. Check **I confirm my inventory and equipped items are banked.**
4. Open your bank, then click **Start From Scratch** and confirm.

The complete bank becomes the permanent legacy baseline. Initial progress is zero, including coins; this does not establish spendable virtual GP. Items in Grand Exchange offers or other storage aren't included. Collect them into your bank first if you want them treated as legacy. No inventory/equipment or GE inspection is required. A Bank PIN, missing identity or incomplete bank data prevents starting. A genuinely empty complete bank is valid. Offers observed before starting are marked preexisting; later bank returns from them can still need a Manual Bank Correction because the immutable baseline remains bank-only.

## Bank progress

For each exact item ID:

`new balance = committed balance + (current physical bank quantity - committed physical bank quantity)`

- Withdraw a legacy axe: `0 → -1`; return it: `-1 → 0`.
- Deposit two new logs: `0 → 2`; withdraw one: `2 → 1`.
- Burn that log outside the bank, then reopen: balance stays `1`.

Configurable bank numbers (bright green by default) are signed **bank progress**, not total earned ownership. Zero/negative items remain dimmed; positive items render normally. Original OSRS quantities and bank interactions are unchanged. Bank searches, tabs, scrolling and rearranging do not create changes. The full bank API container supplies accounting; only actual on-screen item widgets supply overlay coordinates.

Updates capture complete bank data at the next game-tick boundary and publish after durable save. The repeated multi-tick debounce was removed. Unchanged observations are cached; queued bank changes commit in order on the storage worker. No inventory/equipment walkthrough, source review or ordinary confirmation is needed. If the bank closes before settling, the next opening catches the net change. Multiple actions inside one settling interval may become one net audit entry.

Six compact tiles show Total Value Gained (an estimate), Current Bank Net Worth, Bank GP, Items Unlocked, Active Run Duration, and Total Value Lost / Spent. Items Unlocked counts currently positive bank IDs. Genuine OSRS icons use cached RuneLite ItemManager sprites, with a bundled-icon fallback. Components are reused on deposits.

Item overlays and compact GP statistics share integer formatting: absolute values 0–9,999 are exact; 10,000–9,999,999 use truncated uppercase **K**; 10,000,000+ use truncated uppercase **M**. Examples: `1740`, `10K`, `1000K`, `1740K`, `9999K`, `10M`, `-1000K`. Billions use integer millions (`2147M`), without decimals or rounding up. Money adds ` GP`; `980640` GP displays as `980K GP`. Short, wrapped tooltips retain exact grouped GP, plain-language estimates and correct unknown-item grammar without technical dates.

Withdrawing the last copy still changes progress (four legacy copies withdrawn means `-4`). Native OSRS placeholders show the original item's signed counter but represent zero physical copies. If no placeholder is retained, the absent item's balance remains saved, valued and searchable in Manual Bank Corrections. No artificial slot is created; OSRS's own Always set placeholders option is optional.

**Current Bank Net Worth** sums signed bank progress times cached RuneLite GE-guide prices. Negative balances subtract value. Bank coins have unit price one and are included exactly once. Unpriced types produce a labeled **Priced subtotal**; `~`/tooltips identify older retained quotes. Prices refresh in the background every 30 minutes, retrying failures after a minute. These are estimates, not live execution prices. Untradeables without a feed record remain unpriced.

**Bank GP** is the signed progress for the actual coins item, not a spendable wallet. **Total Value Gained** estimates new per-item bank progression maxima. Withdrawing/redepositing below an earlier maximum never earns twice. Gain prices are fixed when recognized; missing/stale historical prices stay explicitly unpriced. Corrections raise the comparison ceiling without earning value. This estimate undercounts replenishment and cannot reconstruct out-of-bank activity, conversions, sales or profit. **Total Value Lost / Spent** says **Not tracked** until you explicitly record a verified expense by clicking that tile (or focusing it and pressing Space/Enter). Recorded expenses change the statistic only, not Bank GP or items; a reason and confirmation are required.

## Played Elsewhere?

The left information tile says: "Played on mobile or another PC? Bank your items and open your bank here. Your progress updates automatically." The adjacent **Manual Bank Corrections** tile fixes an incorrect count; returning from another device does not require it. Items earned and consumed outside the bank cannot be reconstructed.

## Manual Bank Corrections

Under **Played Elsewhere? → Manual Bank Corrections**, search a known item by name (exact ID distinguishes variants). Use this only if From Scratch recorded an incorrect bank quantity. The editor is available even with no automatic differences and includes negative items absent from the bank. Open your bank to save.

Select an item, inspect physical quantity/current signed balance, enter a desired balance or enable signed adjustment, review the preview, and confirm **Save Correction**. Positive progress cannot exceed physical bank quantity. Cancel writes nothing. A newer bank/save invalidates the editor; reopen it rather than overwriting newer progress. Corrections add audit entries and survive later bank movements. No arbitrary grant-items command exists.

**Bank History** has Automatic Changes and Manual Corrections tiles. Each opens a read-only view with item names, signed quantities, timestamps, sorting, page-local search/Gains/Losses filters and 25 rows per page. Previous/Next/Close and Escape are supported. Old journal segments load lazily; no transaction feed scrolls through the dashboard.

## Grand Exchange

Purchases and Sales tiles below Bank History open read-only, paginated histories. Observe placements, incremental fills, completion/cancellation and cumulative reported execution GP automatically, even without opening the GE sidebar. Search, sort and filter each page; select a row for exact totals, derived average, origin and uncertainty. Times are local observation times. First-seen already-filled offers are snapshots, not invented earlier fills; initial login EMPTY events never erase saved offers.

GE events never independently change bank items, Bank GP or net worth. Withdraw/buy/collect/deposit normally; only physical bank changes update counters. Uncollected assets stay outside bank worth. Compatible purchase/sale/refund/unsold-return budgets may exclude the associated portion from new high-water gains, without changing bank balances. These are **possible matches**, not proof of the origin of identical items. Matching is bounded to 24 hours/256 pending credits and may undercount unrelated same-ID gains; older/missing/late trade evidence cannot reconstruct or revise past estimates. Profit and exact collection destination remain unknown.

Seller fees are explicitly **estimated** from incremental reported GP and average unit price under the 2%/5M-per-item policy, with published tool/bond/Mind rune exemptions and sub-50 flooring. Unlisted exemptions, varying execution prices, old offer rates and the API's undocumented seller gross/net basis can differ from the estimate. Estimated fees appear once in Total Value Lost / Spent with `~`, separately from explicit expenses; they never debit Bank GP again. Buyers have no seller tax. See [sources, limits and manual checks](docs/MILESTONE_6.md).

Playing elsewhere still needs only banking/opening the bank here. Compatible saved offers can reveal a net change; trades completed and removed entirely elsewhere cannot be recovered. No GE visit, review wizard, inventory or equipment observation is required. A concise notice can suggest Manual Bank Corrections for a known preexisting return; it never silently rewrites the legacy snapshot.

## Existing runs, backups and reset

Storage is local-only under RuneLite's plugin Filepath directory: `from_scratch/characters/<opaque-character-key>/`. Never copy saves into this repository. There is no cloud sync or account backend. This PC is authoritative.

Format 7 preserves M5 data. Before a format-6 migration, verify the full chain and create exact-byte, forced, manifest-verified backups of all original checkpoints/journals. Keep run/start/legacy bank/P/B/financial history exactly; begin GE observation without reconstructing old trades or changing prior gains. Existing M5 runs need no new bank anchor. Format-5 migration retains its original imported financial baseline safeguards; older migrations and failed/interrupted recovery evidence remain supported.

Formats 1–4 still verify/archive the old save/journal, preserving original legacy documents and committed bank allocation minus legacy deficits. Inventory/equipment/Elsewhere records remain archived rather than becoming bank gains.

**One migration confirmation:** open your bank and select **Review Bank Anchor**. This keeps valid signed balances and establishes the physical comparison point without replaying old changes. If a saved positive balance exceeds physical bank quantity, bank the missing copies first or explicitly approve the listed reductions. No reduction is automatic. Subsequent sessions need only the bank. Use corrections for older ambiguous changes.

Back up the **entire character directory**, including current/checkpoints, both journal generations and migration directories, while the client is closed. Restore a matching complete directory on this host with the client closed. Hashes detect corruption, not cheating. The bottom **Reset Progress** danger area requires typing **RESET**, confirmation and a verified archive before replacing active progress. Opening/cancelling changes nothing; backups remain. It does not automatically create another run. Disabling/logging out preserves progress.

## Troubleshooting and limitations

- Reading/waiting: unlock any PIN, keep the normal bank open for a few ticks, then close/reopen if needed. Null/partial containers are never empty banks.
- Storage error: stop making corrections, check local access and free space, use Retry Loading. Do not delete recovery files to bypass errors. Missing/corrupt/future saves block new blank state.
- Use one client on the host. File locking/CAS prevent competing writers; stale callbacks are discarded after identity/lifecycle changes.
- Exact variants stay distinct. Noting, charges, crafting and special storage are not inferred as conserved transformations: only actual normal-bank ID changes count.
- Bank value is an estimate from GE-guide records, not guaranteed sale proceeds. High-water gains are conservative recorded progression, not complete lifetime income. Withdrawals do not prove consumption/spending. Unpriced historical gains cannot be valued later with a newer price.
- Optional development tracing emits rate-limited local DEBUG availability and sampled phase timings; disable it after diagnosis. Existing config keys remain compatible.

For bugs, use the GitHub issue templates with reproducible steps, expected/actual counters, RuneLite version and sanitized logs. Never attach credentials or private bank saves publicly. See [CONTRIBUTING](CONTRIBUTING.md), [architecture](ARCHITECTURE.md), [accounting](ACCOUNTING.md), [tests](TEST_PLAN.md), and the [M4 report](docs/MILESTONE_4.md). The curated buyback schedule in the master specification remains future scope, not available functionality.

## Appearance and timing diagnostics

Virtual quantity color defaults to **#1DFF00**. Locked Item Background Color defaults to **#333333**, with **50%** opacity (0–100). These independent settings apply live. Existing saved custom colors/opacity are retained; no configuration keys were renamed or overwritten. Partial-row clipping, signed labels and positive-item appearance are preserved.

The sidebar retains its transparent purple pixel-art coin stack. Automatic Changes uses Banker's note; Manual Corrections history uses the Horror from the Deep Diary (3846), while Manual Bank Corrections uses Purple partyhat (1046). GE Purchases/Sales use coins/platinum sprites. These OSRS sprites are cached through RuneLite. Setup and the red typed-RESET danger area retain all safeguards. See [M6 acceptance](docs/MILESTONE_6.md).

The setup screen has a compact, visible attestation row and centered green **Start From Scratch** action. Start remains disabled without attestation and a complete bank, then opens the existing confirmation. Ordinary Active/Not Started subtitles are removed; actionable warnings remain. Played Elsewhere instructions and the red reset action are centered. See the [M5 polish checklist](docs/MILESTONE_5_POLISH.md).

Optional Development: bank diagnostics emits sampled, monotonic DEBUG timings for EVENT, CAPTURE, AGGREGATE, DIFFERENCE, PREPARED, WRITE_START/END, PUBLISHED, OVERLAY and SWING. It contains no item lists/account identities. Disable after testing. See [the polish report](docs/MILESTONE_4_POLISH.md) for native-versus-counter comparison and measurement limits. Synthetic tests do not prove native banking lag is fixed.
