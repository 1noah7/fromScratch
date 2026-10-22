# RuneLite Plugin Development — Agent Guidelines

## From Scratch project scope

- Read `ARCHITECTURE.md`, `ACCOUNTING.md`, and `TEST_PLAN.md` before behavior changes.
- M1–M8 are the user-approved baseline, checkpoint v0.8.0-quality-of-life. M9 hardening, exact Examine counts, guarded native Virtual Bank and local backup/recovery are authorized and await acceptance. Do not begin M10, commit, push, tag, publish or submit a PR without approval.
- M8 is informational: no new Examine menu or standalone bank/statistics viewer. Core Examine queues its price line at priority 0; From Scratch queues its single GAMEMESSAGE at -1, using virtualQuantityColor only for the prefix/numbers and escaped normal-color names/words. Catalog/receipt evidence supplies buyback status, never a second price list. Interactive Virtual Bank is restored with a default-on first checkbox, preserving explicit off preferences and initial Full Bank capture. Use immutable cached membership, deferred guarded lifecycle operations and changed-membership-only refreshes. Preserve user tags/layouts and the convenient Full Bank toggle; no synthetic game actions. Time Played remains host-only logged-in GameTicks with independent Filepath checkpoints; no wall-clock conversion or claimed mobile time. See docs/MILESTONE_8.md.
- Identity: From Scratch, author Pilots r kwl, package `com.fromscratch`, config `fromscratch`, internal name `from_scratch`.
- Bank is the only ongoing quantity authority. Do not read inventory/equipment or reintroduce observation/session/reconciliation gates. Setup relies on explicit player attestation that inventory and gear were banked, a complete current bank and confirmed Start.
- Signed bank progress is B' = B + (P' - P), checked long arithmetic. It is not earned ownership or lifetime acquisition/wealth. Negative balances are valid. Corrections may set signed B but positive B must not exceed current observed physical P.
- Confirmed initial baselines and imported legacy snapshots are immutable. Never infer outside-bank consumption, equipment, transfers, provenance or lifetime earnings from bank changes.
- Bank opening is an interface lifecycle, not a quantity-change guarantee. Support already-open/reopened banks; use mounted BANKMAIN, visible bank content, no PIN, complete non-null InventoryID.BANK and a complete observation at the next GameTick boundary. Cache until invalidation; do not restore multi-tick debounce. Aggregate actual IDs from the full API container, never visible tabs/search widgets. Empty arrays are valid; null/partial is unknown, never zero.
- Filepath storage is permanently local-only on one designated host. No cloud, remote credentials/backend or player-data transmission. Mobile/other-PC progress is the automatic net change at next complete host bank observation.
- Format 6 adds financial high-water marks, fixed-price gain events, explicit expense records and measurement-start disclosure to the accepted bank state. Verify/force/archive every original format-1–5 checkpoint/ledger byte-for-byte before migration. Format 5 preserves P/B/run/start/history and establishes an imported financial baseline without inventing earlier gains. Formats 1–4 retain the one-time bank anchor safeguard. Never silently rebase, clamp or reset.
- All IO uses Filepath on the background worker. Journal and forced checkpoint precede current replacement; only acknowledged saves update renderer/UI authority. CAS rejects stale editors/clients. Preserve corrupt, newer, missing and interrupted recovery evidence. Guard callbacks by character/context/lifecycle. Reset must archive successfully before acknowledgment.
- Preserve accepted M3 draw-only geometry, configurable #333333 / 50% default tint and #1DFF00 default quantity text, intersection clipping at both viewport edges, original quantities, menus and input. Never overwrite existing custom config values. Nonpositive bank progress stays locked; positive is normal. Negative absent items remain searchable in Bank Corrections. No new slots or widget/item/sprite mutations.
- Production uses bank/BankController, RuneLiteBankSource, BankSidebar, BankRepository and the M6 ge/RuneLiteGeSource/GeEngine/history components. Retired personal observation/correlation/review code lives in src/test as historical fixtures. Never reconnect it to production. Historical domain/format-1–4 journal models exist only for verified migration.
- Item-stack and compact GP formatting share exact integers below 10,000, floored uppercase K below 10,000,000, and floored uppercase M thereafter, preserving signs (1,000,000 = 1000K). Money appends GP; tooltips retain grouped exact amounts. Financial tooltips are concise, escaped/wrapped and omit technical timestamps. Prices come from RuneLite's bulk ItemClient feed on a separate worker, never rendering/client/EDT/storage HTTP; note definitions are resolved only on the client thread. Unknown/zero/stale records are distinct. Current signed valuation never rewrites historical gain quotes; manual count corrections never earn value. Ordinary withdrawals are not spending.
- Native placeholder templates contribute zero physical quantity. For rendering only, resolve their getPlaceholderId() back to the original item; do not apply that bidirectional link to normal items. Keep absent negative balances and their valuations/search entries without creating widgets.
- Schema 7 adds GE observations, durable local slot generations, bounded possible attribution budgets and independently estimated seller fees. Verify/archive all format-6 files exactly before migration; preserve P/B/initial/run/start/financial history without replay. GE events never change bank quantities/GP. Ignore initial EMPTY, keep offline/slot-reuse uncertainty and never infer collection destination or realized profit. Possible conversion exclusions raise H without creating new gains; existing historical gains are never silently rewritten.
- GE packets and complete bank observations share the background forced FIFO/CAS path. No inventory/equipment ownership or GE review gate. Missing GE data does not block banking. Match budgets are finite, consumed once and explicitly uncertain; history remains immutable when bounded matching context expires. Initial setup stays bank-only/attested; known pre-start offers use lifecycle evidence rather than clock comparisons.
- Schema 8 adds catalog-versioned one-copy equipment roles, purchase evidence and cumulative committed buyback GP. Archive verified M6 files exactly before migration. Preserve raw P/B/legacy/GE/financial history. Available GP = raw B(COINS) minus committed payments; effective item/currency projections never create physical copies. One pouch-family payment covers standard/Divine/Trouver variants, not notes or runes. Purchases require fresh bank, live untradeable definition, revision/funds/legacy validation and one atomic acknowledgment. No generic grants or refunds.
- Schema 9 retains historical pouch preparation for compatibility. New pouch purchases use current banked-pouch client/server varbit reads and atomically apply purchase-linked signed rune deficits, one 1M payment and one shared entitlement. No inventory/pouch-open prerequisite, manual preparation UI, new schema, continuous personal ownership tracking, inferred old contents or automatic debt forgiveness. Existing payments/adjustments survive. See the latest section of docs/POST_MILESTONE_7.md.
- Development diagnostics are hidden from the public UI, local, opt-in, rate-limited DEBUG availability and monotonic phase timings, disabled by default. No whole snapshots, account identifiers, credentials or private paths. Preserve existing config keys; personal-container tracking, cloud and M10 remain outside scope.
- M9 retains economic format 9/playtime format 1. ProgressBackup uses Filepath.Chooser and a bounded checksummed local export. Confirmed import validates full history/identity, retains old bytes, rejects stale previews and atomically switches a character-local directory selector shared by both repositories. Never auto-replay orphan candidates or silently replace valid progression. Examine quantities are exact grouped signed integers; overlays/GP remain compact. See docs/MILESTONE_9.md.
- A successful build or synthetic fixture is not live acceptance. Do not operate RuneScape using computer-use tools; only the user tests gameplay. Offer the Gradle development launch, official Jagex Accounts instructions, and exact manual tests.

## Logging

- Use `log.debug()` for developer/diagnostic logging.
- Do not use `log.info` for per-frame or per-event logging - RuneLite runs at INFO level in production, so high-frequency info logs will pollute user logs. `log.info()` is fine for one-time startup/shutdown messages or infrequent events.

## Threading & Concurrency

- Never use `Thread.sleep()`.
- Never block on `shutDown()` or `startUp()` — don't call `executor.awaitTermination()` in shutdown, just use `shutdownNow()`.
- Never do blocking network IO or disk IO on the client thread. The OkHttp thread pool can be used for blocking network requests.
  If you need to call back into `client` from the okhttp threadpool, such as from the response queued with `enqueue()`, use `clientThread.invoke()`
- Explicitly cancel scheduled tasks (e.g. `ScheduledFuture`) on shutdown, in addition to shutting down the executor.
- For batching async work, use `CompletableFuture.allOf()` — not `CountDownLatch`.
- If you must use `Process.waitFor()`, always pass a reasonable timeout.

## Performance

- Don't scan the entire scene every tick or frame. Use events such as object and npc (de)spawn to track what you care about and maintain your own collection.
- Keep the computations in Overlays, which are run each frame, to a minimum.

## API Usage

- Use `net.runelite.api.gameval` package constants — `ItemID`, `InterfaceID`, `ObjectID`, etc. Never hardcode magic numbers when gameval constants can be used instead.
- Use `LinkBrowser` to open URLs, not `java.awt.Desktop`
- When looking up Widgets, pass the component ID from gamevals (eg `client.getWidget(InterfaceID.DomEndLevelUi.LOOT_VALUE)`) - do not manually combine interface + component child IDs.
- Use of Java reflection is forbidden.

## HTTP & JSON

- Use OkHttp for all HTTP requests. `@Inject OkHttpClient` to get the HTTP client. Do not use `HttpURLConnection`, `java.net.http.HttpClient`, or Apache HttpClient.
- Use `@Inject Gson` to get a Gson instead, never create your own from scratch. You can use `.newBuilder()` to create one derived from the base `Gson.`
- Do not add transitive dependencies from `runelite-client` directly to `build.gradle`, such as gson, guice, or okhttp.
- Never execute okhttp calls on the client thread. Prefer using `enqueue()` which places the request on the okhttp threadpool.

## File I/O

- All file i/o must go through the Filepath utility. Use of Filepath requires setting PluginDescriptor `internalName` to the internal name of the plugin.
- Use `Filepath.Chooser` instead of `JFileChooser`
- To migrate a legacy plugin data folder from .runelite automatically, set PluginDescriptor `legacyDataDirectory` to the name of the legacy folder.

## Config

- Config group names must be specific — e.g. `"deadman-prices"`, not `"deadman"`.
- Never rename a config key or config group without providing a migration. Renaming silently resets users' saved settings.
- If you add a `@ConfigItem` that toggles a feature involving a third-party server, it must:
  - Be **disabled by default** (opt-in)
  - Have a `warning` field set to: `"This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"`

## Plugin Setup & Packaging

- Rename everything from the template. Do not leave `com.example`, `ExamplePlugin`, `ExampleConfig`, or `example` as the config group. Rename the package path, class names, config group, `build.gradle` group, `settings.gradle` project name, and `runelite-plugin.properties`.
- Do not include a `META-INF/services/net.runelite.client.plugins.Plugin` file.
- Do not commit build artifacts — no `.class` files, `out/` directories, or `.tmp` directories.
- `build.gradle` must target Java 11** and match the structure of the example-plugin template.
- Retain a permissive license, such as BSD-2.

## Resources & Assets

- Optimize icon PNGs. Java loads images at full resolution in memory (`width × height × 4` bytes), so a seemingly small file can use significant memory.
- Ensure PNGs are actually PNGs — do not rename JPEGs or ICOs to `.png`.

## Cleanup

- Remove unused config classes, fields, and imports.
- Clean up subscriptions, listeners, and overlays in `shutDown()`.
- Do not mix code reformatting with feature changes in the same commit — it makes diffs unreadable for reviewers.

## Testing

For narrow corrections use incremental compile and directly affected test filters. Run the full regression suite only at the next milestone/release or when explicitly requested or a concrete broader failure requires it. Preserve Java 11, existing dependencies and focused diffs; do not spawn agents for these tasks.

You cannot verify plugin behavior yourself. Even if you have screen-capture or computer-use tools available, **do not use them to interact with RuneScape** — automating game input violates Jagex's third-party client guidelines and will get the user's account banned. Only the user can confirm a plugin works in-game.

After completing a task, do not declare it done. Instead:

1. Offer to launch RuneLite for the user by running `./gradlew run` from the plugin's root directory.
2. Instruct the user to follow the "Using Jagex Accounts" instructions found at https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts to login to the development client.
3. Tell the user *what to test* — the specific behavior you changed, the golden path, and any edge cases worth exercising.
4. Wait for the user to confirm the feature works in-game before considering the task complete. A clean JVM start is not a passing test.

## Submitting

When submitting a pull request to runelite/plugin-hub, append an attribution footer to the PR description structured like:

Generated-by: Claude Code

---

# Plugin Rules & Restrictions

Features that are **forbidden or restricted** in RuneLite hub plugins.
Sourced from [Jagex's Third-Party Client Guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) and RuneLite's [Rejected or Rolled-Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features).

**If your plugin does any of the things listed below, it will be rejected.**

## Forbidden Language Features

- All code must be Java 11 compatible
- No use of reflection
- No use of JNI or JNA
- No direct access to native memory access via Unsafe or LWJGL
- No executing external processes, including with Process or ProcessBuilder
- No downloading or dynamic loading of code, including classloading
- No runtime generation of code
- No use of Java (de)serialization

## Boss & Combat Restrictions

Applies to all bosses, Raids sub-bosses, Slayer bosses, Demi-bosses, and wave-based minigames (Fight Caves, Inferno, etc.):

- No next-attack prediction (timing or attack style)
- No projectile target/landing indicators
- No prayer switching indicators
- No attack counters
- No automatic indicators showing where to stand or not stand (manual tile marking is allowed)
- No additional visual or audio indicators of a boss mechanic, unless it is a manually triggered external helper
- No advance warning of future hazards (highlighting currently active hazards is OK)
- No "flinch" timing helpers
- No combat prayer recommendations
- No NPC focus identification (which player the NPC is targeting)
- No content simulation (e.g. boss fight simulators)

New high-end PvM boss plugins are not accepted as a blanket policy.

## PvP Restrictions

- No removing or deprioritising attack/cast options in PvP
- No opponent freeze duration indicators
- No PvP clan opponent identification
- No PvP loot drop previews
- No identifying an opponent's opponent
- No PvP target scouting information
- No player group summaries (attackable counts, prayer usage, etc.)
- No level-based PvP player indicators (highlighting attackable players or those within level range)
- No spell targeting simplification (removing menu options to make targeting easier)

## Menu Restrictions

- No adding new menu entries that cause actions to be sent to the server
- No conditional menu entry removal based on NPC type, friend status, etc. (can be overpowered)

## Interface Restrictions

- No unhiding hidden interface components (special attack bar, minimap)
- No moving or resizing click zones for 3D components
- No moving or resizing click zones for combat options, inventory, equipment, or spellbook
- No resizing prayer book click zones
- No resizing spellbook components
- No removing inventory pane background or making it click-through
- No detached camera world interaction (interacting with the game world from a camera position that isn't the player's)

## Input Restrictions

- No injecting input events, including mouse and keyboard events
- No autotyping — plugins must not programmatically insert text into the chatbox input (includes pasting, shorthand expansion)
- No modifying outgoing chat messages after the user sends them

## Data & Privacy Restrictions

- No exposing player information over HTTP
- No crowdsourcing data about other players (locations, gear, names, etc.)
- No credential manager plugins that stores account credentials

## Content Restrictions

- No adult or overtly sexual content
- No plugins that use player-provided IDs for their entire functionality (causes moderation issues)
