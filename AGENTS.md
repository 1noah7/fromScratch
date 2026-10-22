# RuneLite Plugin Development — Agent Guidelines

## From Scratch project scope

- Read `docs/technical/ARCHITECTURE.md`, `docs/technical/ACCOUNTING.md` and `docs/technical/TEST_PLAN.md` before behavior changes. The [documentation index](docs/README.md) separates current guides from historical notes; older full instructions remain in [the archive](docs/history/AGENTS_PRE_M10.md).
- Milestones 1 through 9.2 are owner-accepted. Gameplay is frozen at M9.2. M10 authorizes documentation, genuine sample-component previews and safe local beta preparation only. M11 exclusively owns official submission/publication. No commit, push, tag, public upload, friend distribution, submission or modification of real progression without explicit approval.
- Identity: From Scratch, author Pilots r kwl, canonical repository PilotsRKwl/fromScratch, package `com.fromscratch`, config `fromscratch`, internal name `from_scratch`.
- Bank is the only ongoing quantity authority. Use the complete API container, never visible tabs/search/filter widgets. A mounted normal bank, no PIN and complete observation on the next GameTick establish readiness. Empty non-null arrays are valid; missing/partial data is unknown, never zero. No continuous inventory/equipment ownership tracking or retired observation/session gates.
- Signed progress is B' = B + (P' - P), with checked long arithmetic. Negative balances are valid. Positive corrections cannot exceed current physical P. The confirmed initial legacy snapshot is immutable. Bank withdrawals do not prove consumption or spending. Offline/mobile reconciliation observes the next net bank change, not unseen transactions.
- Initial setup uses the player's banking attestation, a complete full-bank observation and confirmed Start. Filtering activates only after the initial baseline is successfully committed. No active run or settings OFF means Full Bank. Preserve explicit OFF preferences, the default-on first settings checkbox, native-tab intersections, accurate title, immutable membership and re-entrant close protections. Never add a duplicate sidebar toggle or repeated activation loop.
- Production uses `bank/BankController`, `RuneLiteBankSource`, `BankSidebar`, `persistence/BankRepository` and the `ge` source/engine/history. Retired personal-container/controller models stay test-only for verified migration. Never reconnect them to production.
- All progression IO uses Filepath on the worker. Forced journals/checkpoints precede atomic current replacement; only acknowledged saves become renderer/UI authority. CAS rejects stale editors/clients. Guard callbacks by character/run/lifecycle. Preserve corrupt/newer/interrupted evidence; failed load never creates a new run. Reset archives before acknowledgment. M10 adds no save schema/migration.
- Preserve native geometry, clipping at both viewport edges, #333333 / 50% default tint, #1DFF00 configurable virtual text and M9.2's accepted public quantity-display mode / #A0A0A0 physical redraw. Do not change quantities, sprites, slots, menus or input. Nonpositive progress remains locked. Placeholder templates contribute zero; resolve placeholder originals for rendering only. Keep absent negative balances searchable without fabricated widgets.
- Bank overlays and compact GP retain native K/M formatting. Examine always shows exact grouped signed quantities, configured color only for prefix/numbers, balanced bounded markup and normal words. Preserve normal game/core-price ordering before one local supplemental message. No new Examine menu, fake bank inventory or expanded statistics page.
- Cached bulk RuneLite price requests run on a separate worker, never rendering/client/EDT/storage HTTP. Unknown, zero and stale prices differ. Peak Progress Value stays monotonic at reliable checkpoints, using signed wealth plus confirmed permanent expenditures once. Historical gain quotes remain auditable. Ordinary withdrawals, corrections, GE conversions and legacy offsets do not independently earn value.
- GE observations use durable slot generations and bounded once-consumed uncertain attribution. They never change bank quantities directly. Public history derives one completed/canceled-positive-partial trade per lifecycle; raw evidence remains. No pending/zero/slot-cleared rows, guessed collection destination, verified-tax claim for estimates or finalization replay.
- Preserve catalog-defined one-copy roles, committed virtual GP spending and atomic purchase evidence. Rune/Divine pouch family costs 1M once; open/closed Looting Bag costs 10K once. On-demand supported contents proofs and negative legacy adjustments commit with payment/entitlement/audit. Unknown is not empty; no wizard, second charge, refill/restart offset or invented historical contents. Naturally earned qualifying copies remain free. Keep old legitimate purchases and migration evidence.
- Format-9 compatibility, original migration archives and old preparation evidence remain intact. Current automatic contents offsets supersede the historical manual reservation workflow. `.fsp` import validates same character, previews replacement, preserves the existing run and restores atomically; never merge histories. Saves/exports remain local-only and private, without cloud synchronization.
- Time Played counts host logged-in ticks with efficient run-specific checkpoints, not wall-clock run age or mobile time. No Development Bank Diagnostics public control. Useful diagnostics are DEBUG, local and rate-limited, without account identifiers, credentials, whole snapshots or private paths.
- M10 beta/preview source sets and Windows tools are excluded from the production JAR and standard Hub source build. Preview rendering uses actual production components, clearly labeled synthetic data and an isolated empty home. Do not fabricate RuneScape screenshots or read real profiles for marketing.
- Compilation, fixtures and a logged-out client start are not live acceptance. Never operate RuneScape using computer-use tools. Only the owner tests gameplay and authentication. Preserve all current progression and approval boundaries.
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
