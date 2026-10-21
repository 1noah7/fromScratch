# Architecture

M1–M4 are user accepted. **M5 bank wealth/GP/valuation and specified UI corrections are authorized; manual acceptance is pending.** GE accounting, buybacks and personal-container tracking remain outside scope. The previous multi-container design is preserved only in [historical documentation](docs/history/ARCHITECTURE_MULTI_CONTAINER.md) and test fixtures, not live plugin wiring.

## Production responsibilities

| Component | Responsibility |
| --- | --- |
| FromScratchPlugin | Supported bank events, lifecycle, configuration and DI; no menu/action observation |
| bank/RuneLiteBankSource | Client-thread identity validation, mounted normal bank/PIN checks, full InventoryID.BANK copy; no personal-container reads |
| bank/BankItems | Exact-ID aggregation, empty slots/placeholders/fillers excluded, null/partial rejected |
| bank/BankObserver | Complete bank capture at the next GameTick after invalidation; cached until bank/interface events; unavailable vs empty |
| bank/BankEngine | Pure checked signed bank deltas, bounded corrections, explicit migration anchor, reset |
| bank/BankRun, BankAudit | Immutable physical anchor, signed progress, original bank, run metadata, recent audit and retained host-return summary |
| bank/BankController | Client/worker coordination, CAS-bound intents, automatic updates, lifecycle/identity rejection of stale callbacks |
| persistence/BankRunCodec, FinancialCodec | Explicit validated format-6 quantity/financial JSON trees/envelope hashes; no domain reflection/deserialization |
| persistence/BankRepository, BankStorageFiles | Background-only Filepath locking, verified archives, immutable journal/checkpoint, recoverable publication |
| bank/BankSidebar, BankPanel, BankCorrections | Reusable EDT tiles, BankIcons, BankDialogs, BankCorrectionDialog, lazy BankHistoryDialog, bottom typed-reset danger area |
| overlay/* | Accepted M3 draw-only item geometry/tint/clip, now reading signed bank progress |
| observation/CharacterIdentity | Existing verified account hash/profile type/RS profile namespace, cached on cheap identity fields |
| model/*, tracking/*, legacy persistence codecs | Immutable historical format-1–4 compatibility; not ongoing earned ownership/accounting |

The old SetupController, personal readers, multi-container collector/reducer/session, Update Progress editor and old sidebar live only under `src/test/java`. Their frozen regressions remain executable; the production JAR excludes them. See [test fixture policy](src/test/HISTORICAL_FIXTURES.md). Existing Lombok is also enabled for these test fixtures; no production dependency/service was added.

## Observation lifecycle

BankPlugin in the actual resolved RuneLite **1.13.3** uses `InterfaceID.BANKMAIN`, `InventoryID.BANK`, WidgetLoaded/WidgetClosed and ItemContainerChanged. The implementation follows those public constants and `Client.getItemContainer(InventoryID.BANK)`. WidgetLoaded alone does not guarantee item availability, and ItemContainerChanged need not fire on every reopening.

GameTick retries a mounted, visible normal bank excluding PIN interfaces. Interface/BANK events invalidate the cache. The next GameTick (API 1.13.3: after packet processing) captures it once. There is no first-scan restart or two-more-tick debounce. Unchanged ticks reuse the immutable snapshot. Already-open banks work through mounted interface checks. Zero-length complete arrays are valid; null arrays/containers, null slots and negative quantities never become zero observations. A stable API bank has no additional server completeness flag; the public observation boundary cannot prove hidden future packets will not arrive. Later events invalidate/retry it.

Exact API IDs and aggregate quantities are authoritative. Widget item layout/search/tab filters never enter accounting. Rearrangement with equal aggregates writes nothing. Personal and game menu events are not subscribed. Closing before a stable observation defers the net comparison until next opening. Outside-bank events have no accounting path. Ordinary world loading/hopping does not introduce an approval gate; identity/logout/plugin boundaries reset session observation safely.

## Commit path

1. Client thread captures a complete bank at a game-tick boundary and queues the immutable observation.
2. The single storage worker runs BankEngine and prepares the next audit against the last acknowledged run. Existing state remains authoritative.
3. One bounded background worker obtains Filepath/character lock, validates expected current revision and candidate deltas, writes/forces/verifies a new immutable journal and checkpoint, then publishes current via atomic move or recoverable forced-copy fallback.
4. After successful durable acknowledgment, a client-thread callback rechecks lifecycle/context/character and publishes the new run to overlay/Swing.
5. Failure retains the old in-memory and persisted cursor. Error/reload blocks inconsistent speculative state; orphan files are retained but never replayed. If publication completed before acknowledgment was lost, reload adopts the durable checkpoint without replaying its bank delta.

No IO runs on the client/EDT or renderer. Shutdown cancels queued work without blocking. Per-frame rendering only reads immutable cached balances/labels/settings and item geometry. Full journal/archive verification occurs on load; normal commits verify current/head rather than rescanning the entire history.

## Format 5 and migration

The new envelope stores character/run/start/previous-run identity, revision/sequence, immutable initial bank, physical bank anchor, sparse signed balances, anchor readiness, recent bank audit and persisted first-comparison host summary. A migration additionally retains the exact original envelope text plus a safe local archive reference. Legacy header schema 1 is preserved inside that original document; it is not reinterpreted as an active multi-container header.

Each `bank-ledger-<revision>.json` segment has a checksummed audit, character/run, sequence, previous revision and result-checkpoint digest. Checkpoints and pending files are immutable/recoverable; current is the only published authority. Recent UI history is bounded to 30 batches, but complete journal files remain. There is no replay of unreferenced orphan batches.

Verified formats 1–4 migrate through the historical codec and original-format journal digest verification. Before changing current, copy all original JSON save/ledger files into a unique migration directory, force each copy, compare exact-byte SHA-256, then force a manifest and verify it. Originals remain as well. Preserve run/start/baseline and old signed bank display. Legacy personal/Elsewhere/acquisition history is archived, never fabricated as bank earnings. One confirmed complete bank anchors an imported active run without replaying earlier changes. Excess positive balances require explicit listed reductions or physically banking those copies first. Reset has the same archive-before-acknowledgment discipline. No actual private save was inspected/migrated by development tests.

## Scope and publication

M5 implements conservative bank-based financial measurements below, not full lifetime acquisition/profit reconstruction. No cloud, HTTP player-data transmission, gameplay automation, synthetic input, packets, conditional menus or widget mutations are introduced. Standard Java 11/Gradle Wrapper, BSD license, metadata/icons and CI are preserved. Plugin Hub approval is not guaranteed; current review and manual performance/compatibility tests remain publication gates.

## M4 final polish

Format 5, signed accounting and all save/migration safeguards are unchanged. See [MILESTONE_4_POLISH.md](docs/MILESTONE_4_POLISH.md) for runtime evidence and measurements.

Complete captured snapshots queue FIFO during saves; acknowledgments drain the next without another tick wait. Identical observations are skipped. The 256-entry overload limit stops tracking visibly rather than silently discarding durable state; recovery reloads the committed anchor and compares the current full bank. Raw same-tick events remain net-batched by the existing bank-only contract.

BankPanel builds its component tree once and switches setup/active cards. BankSidebar coalesces only presentation callbacks on the EDT, never ledger operations. BankOverlayState only rebuilds preformatted labels on committed revision changes. BankIcons requests verified ItemID sprites from ItemManager once per ID. No prices, network or per-deposit sprite lookup are added.

History is read-only/worker-only, at most 25 rows and 64 segments per page request. It follows immutable cursors in the selected run and resolves a bounded page of item names on the client thread. Sorting and search/Gains/Losses filters apply to the displayed page. Older files are never rewritten. No full journal scan occurs during ordinary banking.

BankDialogs wraps standard-font explanations and packs short messages without scrollbars. It clamps bounds to the owner display, permits resizing, supplies sensible minimums, keeps buttons outside long-content scrolling, and supports Enter/Escape/window close. Reset requires exact RESET; controller revision/identity checks and archive-before-acknowledgment remain.

BankTimings is disabled by default, monotonic/thread-safe, sampled at most once every five seconds, and carries runtime-only traces through client/worker/EDT/renderer. The overlay timestamp is a plugin draw pass, not native OSRS paint/input/server timing. No account contents or identifiers are logged.

The [subsequent UI polish](docs/MILESTONE_4_UI_POLISH.md) changes presentation only. BankAttestation paints an explicit 22-pixel Swing indicator while retaining standard checkbox label/Space activation. BankDangerButton paints destructive normal/hover/pressed backgrounds independently of the dark look and feel. Both preserve existing controller actions and confirmation guards. Played Elsewhere uses a two-column information/action row matching history styling. BankIcons caches verified gameval sprites in centered 36x32 regions, including null/failure fallback. Config keys remain stable: only absent-setting defaults become #1DFF00 quantities, #333333 tint and 50% opacity. There is no progression schema, accounting, observer, scheduler or persistence change in this pass.

## M5 financial extension

The earlier format-5/polish sections describe the accepted M4 foundation. Current format **6** retains its quantity model and linked bank journal while extending BankRun with FinancialState and BankAudit with ValuationEvent evidence.

- PriceService owns a separate cancellable daemon scheduler. It calls injected public RuneLite ItemClient.getPrices only there, indexes the bulk GE-guide records and atomically replaces an immutable market snapshot. A failed refresh retains the prior data/time with stale status. No character IDs or holdings are sent. The cache refresh interval is 30 minutes with one-minute retries. Market receipt time is explicitly not the feed's market-update timestamp.
- RuneLiteBankSource resolves note→linked unnoted price identities on the client thread, once per encountered exact item ID. Other variants stay distinct. Cache registration/lookup performs no HTTP. Game data access is never moved to a storage/HTTP worker.
- FinancialEngine calculates high-water deltas on the existing serial storage worker using cached immutable quotes. No price availability gate delays a bank commit. Gain UUIDs are derived from the bank-audit UUID/kind/item. Unknown/stale historical quotes are persisted as unpriced evidence, never retroactively repriced. Manual count correction/anchor can only raise a comparison ceiling without gain.
- BankRepository validates quantity and financial transitions, then publishes journal/checkpoint/current with the existing force/CAS/recovery guarantees. A failed write cannot publish either new bank or historical financial state. New events remain linked to original bank audit/time/session/run/character context. Full evidence stays in journal segments; checkpoint totals/high-water maps avoid full-ledger scans during ordinary banking.
- Rolling checkpoints retain the new head's full gain evidence and compact older gain/host-comparison summaries. Complete quotes stay in the original immutable journal/checkpoint, never deleted or repriced. This avoids copying a large gain-price batch into 30 later snapshots; explicit recent expense records remain readable. A regression verifies old journal bytes and gain quotes remain unchanged.
- BankValuation runs on the worker as a derived calculation after committed state or market-version changes. Arbitrary-precision intermediate sums avoid order-dependent overflow; out-of-long-range totals are unavailable. Revision/lifecycle/character checks reject stale callbacks. Price-only refresh publishes a BankView without disk writes, new sequence or overlay-label reconstruction.
- Bank GP is the persisted signed balance for ItemID.COINS, never a second wallet. Explicit expense records are separate statistical events and do not alter P/B or coin balances. BankExpenseDialog requires positive GP, a reason and confirmation; the loss tile supports mouse and keyboard entry and displays recent explicit records. Normal withdrawals never become spending.
- BankPanel still constructs its two-column tree once, updates labels/tooltips only and retains M4 reset/attestation safeguards. Missing current prices show a Priced subtotal label. BankTooltips formats escaped, width-limited Swing HTML with exact GP and plain-language unknown/older-price counts; persistent timestamps and migration details stay outside ordinary tooltips. Item stack and compact GP labels share one integer formatter: exact below 10,000, uppercase K below ten million, uppercase M thereafter. Money appends GP while detailed tooltips retain grouped exact amounts. Both manual tiles use the cached Purple partyhat; correction purpose text distinguishes mistakes from ordinary mobile return.

Before a format-5 migration, verify the old full chain, archive all original JSON byte-for-byte with forced manifest/hash verification, then append a FINANCIAL_BASELINE event linked to the old head. Preserve original P/B/identity/start/snapshot/history. H=max(0,B), zero prior financial totals and an explicit imported measurement timestamp avoid invented history. The financial archive reference remains verified on load. Formats 1–4 retain existing historical decoding/archives and one-time bank anchor. No private save was used by development tests.

## M5 presentation correction pass

The [polish pass](docs/MILESTONE_5_POLISH.md) changes UI/stack text only; quantity/financial reducers, pricing/cache services, persistence/schema and bank observation are untouched. Status subtitles are removed without removing error/recovery notices. Visible-card sizing avoids reserving a hidden dashboard's height during setup; revalidation occurs on card changes only. Setup instruction labels use intrinsic HTML height instead of a frozen narrower textarea height. Attestation remains unchecked and required. BankPrimaryButton provides explicit green/white enabled, hover/pressed and readable disabled states, routed through the original confirmation dialog. Centering wrappers preserve the two-column alignment and red reset safeguards.

Swing's legacy HTML renderer expands CSS px widths; logical pt widths are verified against actual JToolTip preferred size (approximately 250–300 pixels). Text is escaped before HTML rendering. Centered informational text remains noninteractive. Accepted bank item/viewport geometry and tint remain; only exceptional over-wide labels use a child Graphics2D horizontal scale, keeping baseline/right edge/clip and restoring parent graphics state. Normal labels retain the existing inexpensive path. Formatting remains cached per committed revision, not per frame.

## M5 final-withdrawal and presentation corrections

VirtualBankOverlay caches both placeholder status and the original balance ID for each visible item definition. RuneLite 1.13.3 ItemComposition.getPlaceholderId is bidirectional: use it only when getPlaceholderTemplateId != -1. The native placeholder widget/sprite/bounds remain untouched; only the balance lookup resolves to the original ID. A zero-quantity widget without a variant keeps its own ID. Positive progress cannot appear on a placeholder while a previous frame awaits acknowledgment. Labels come from the same preformatted committed projection, including negative balances.

BankItems already excludes placeholders from physical P; BankEngine's old/new ID union already applies the final P=0 delta. Neither component nor persistence changes in this fix. Without a native placeholder, no slot exists to paint; saved negative B remains available to valuation and Manual Bank Corrections. The cached-definition fix adds no disk access, price requests, per-frame aggregation or new slots. Accepted clipping/drag geometry is unchanged.

Setup has a 14-pixel effective gap before its centered green Start button, with the attestation still adjacent to step 2. The optional storage/GE note is placed below Start to keep that short action sequence together; it adds no observation or gate. Played Elsewhere explicitly describes automatic updates. No format-6 financial or storage change occurs. See [final correction evidence](docs/MILESTONE_5_FINAL_CORRECTIONS.md).
