# Architecture

Current guide for the owner-accepted Milestone 9.2 gameplay baseline. Milestone 10 changes documentation, previews and distribution tooling only. The [reviewer map](../PLUGIN_HUB_REVIEW.md) links each feature to its actual source files. Earlier designs and migration rationale remain in the [archive](../history/ARCHITECTURE_PRE_M10.md).

## Core and observation

`com.fromscratch.FromScratchPlugin` owns registration and shutdown. `bank/BankController`, `RuneLiteBankSource`, `BankSidebar` and `persistence/BankRepository` are the production path. Test-only historical controllers are not production dependencies.

The full `InventoryID.BANK` API container is the ongoing quantity authority. A mounted normal bank, no PIN and a complete observation at the next GameTick boundary establish readiness. A non-null empty array is valid; unavailable/partial data is unknown. Cached observations invalidate on relevant lifecycle/container changes. Visible widgets, tab contents and search results never define physical quantities.

Initial setup uses the player's inventory/equipment banking attestation and explicit confirmation. It captures the full physical container before an acknowledged initial save can activate Virtual Bank. No ongoing inventory/equipment ownership tracking or gameplay enforcement is introduced.

## Views and information

- `bank/VirtualBankMode` uses supported Bank Tags APIs and immutable eligible-ID snapshots. Native bank tab slot ranges intersect with positive virtual membership. Membership callbacks do set lookups, with no item-definition, disk, price or ledger work. Re-entrant close paths clear state first. Filtering requires the correct active run, identity, ready bank and saved preference; before setup and after reset the full bank remains available. Search/another Bank Tag can leave the virtual view safely. No repeated activation loop.
- `overlay/VirtualBankOverlay` and `overlay/BankItemPainter` preserves native interactions and geometry. M9.2 temporarily changes the public quantity-display mode, draws the physical counter in gray after the locked tint, and restores original modes when inactive. It does not change item IDs, sprites or quantities. Virtual counters retain configurable color and compact formatting. See the reviewer guide for the precise rendering disclosure.
- Native item Examine correlation waits for the game's normal response and RuneLite price handling before one local supplemental line. Exact signed quantities and numerical color segments are bounded to avoid native chat truncation. Prices are cached; no separate menu action or outgoing player chat is created.
- History windows query retained evidence asynchronously, with bounded pages. Item history is read-only. No expanded statistics page or fake bank inventory exists.

## Accounting services

See [ACCOUNTING.md](ACCOUNTING.md) for the signed equation, GP spending, valuations and finalized GE view. `BankEngine` applies checked arithmetic to immutable runs. GE observations preserve cumulative fill evidence and durable slot generations; presentation aggregation does not replay financial effects.

`buyback/BuybackCatalog` and `BuybackEngine` own approved prices, copy roles, variants and eligibility. Rune Pouch and Looting Bag purchases atomically commit one family entitlement, fixed GP payment and verified negative legacy-content adjustments. Existing legitimate balances survive; no manual contents wizard is part of normal purchase. Narrow contents readers are on-demand proofs, not an ongoing personal-container quantity authority.

`wealth/PriceService` obtains RuneLite bulk guide prices on a separate worker. It never sends holdings or character identity. Current signed valuation, confirmed expenditures and run-scoped peak measurement stay distinct. `playtime/PlaytimeTracker` counts logged-in host ticks and checkpoints efficiently; it does not use elapsed run age or claim mobile playtime.

## Persistence and threads

All progression IO uses Filepath on the background worker. A forced journal/checkpoint precedes atomic current replacement; acknowledged commits alone become UI/rendering authority. Revision/CAS checks reject stale work, and callbacks are guarded by identity/run/lifecycle. Existing saves, immutable baselines and exact migration/recovery evidence are preserved. A load failure is not permission to create a new run. Reset archives before acknowledgment.

The economic checkpoint format remains 9. Playtime and peak have small separate checked run-scoped records. M10 adds no progression migration. `.fsp` export/import verifies a bounded checksummed archive and matching character, previews replacement, preserves the prior run and atomically selects the restored tree. Histories are never silently merged.

Client APIs run on the client thread, Swing work on the EDT, disk/price work off both. Startup/shutdown do not wait synchronously between these threads. Listeners, overlays and optional view state are cleaned up on shutdown.

## Distribution boundaries

The production `src/main` plugin uses no custom backend, cloud sync, credential access, reflection, native process execution, synthetic gameplay input or withdrawal blocking. RuneLite handles its normal authentication and game connection.

The separate owner-side `src/beta`, Windows scripts and Gradle packaging are outside the Hub plugin. They launch an unmodified official RuneLite runtime through its supported developer entry point. `src/preview` renders the same production Swing components with explicit synthetic data and official sprites; it never launches the game or loads player progression. Neither source set enters the production JAR or standard Hub build.
