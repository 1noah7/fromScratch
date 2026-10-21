# Milestone 3 — Virtual bank and overlays

## Status and scope

M1/M2 passed user-run acceptance. This milestone started from clean commit `9e6b757` and implements only M3. M3 is functionally accepted. The latest visual correction addresses the omitted partly visible top row and the request for slightly darker, more natural shading; those visuals await user acceptance. Ownership storage, compatible migration, purple bank quantities, configuration and active-sidebar cleanup remain as functionally accepted. No acquisition/transfer engine, mobile reconciliation, GE accounting, buybacks, wealth valuation, grant-items control or M4 work was added. No code or assets were copied from purpleCounter. No commit, push, tag, progression reset or publication was performed.

The agent has not controlled RuneScape, inspected private saves or migrated real account files. Migration/recovery tests use synthetic JUnit temporary roots. The visual refinement makes no persistence or schema change and requires no new snapshot, migration or reset.

## Architecture and ownership

Stage 3.1 introduced VirtualOwnership, VirtualItemOwnership and VirtualLocation. Each exact item ID has an independent nonnegative long total and BANK/INVENTORY/EQUIPMENT/GRAND_EXCHANGE allocations. Checked sum of allocations cannot exceed total; the remainder is unallocated ownership. Sparse missing records/locations mean valid zero. Normal/noted variants remain separate. Stackability does not change aggregate quantity semantics.

All objects/maps are immutable and defensively copied. Pure replacement returns new ownership; RunRecord.withOwnership creates a new storage revision while retaining the same run header and original LegacySnapshot. Neither physical quantities nor legacy quantities set virtual balances. New and migrated M2 runs have empty ownership. Future accounting can produce validated replacements without changing this model; M3 does not observe earnings/transfers.

Stages 3.2/3.3 introduced VirtualBankOverlay, BankItemPainter, BankOverlayState, BankOverlayRegistration and VirtualQuantityFormatter. SetupController publishes committed Active views into an immutable render projection before Swing updates. The projection contains exact-ID ownership and preformatted bank labels; per-item lookups are O(1). Settings are independently cached and refreshed through ConfigChanged. No price, disk or financial-accounting work occurs per frame.

The overlay now uses a manual/dynamic Overlay with only InterfaceID.Bankmain.ITEMS registered. The enabled/Active/current-character/mounted-visible-normal-bank/PIN gates must all pass. The source reads actual graphic item children of the normal bank's ITEMS layer, requiring matching component, direct parent and visible state. Widget.getBounds supplies current item positions; tab/search/index arrays never assign screen coordinates. Items intersecting the content viewport are included even when their origin is above it. The clip is the ITEMS bounds intersected with ITEMS_CONTAINER bounds and the original graphics clip, then the full item bounds. Inventory, equipment, shared storage, tabs/search controls and specialized storage are excluded. Supplied RuneLite drag bounds are used where available.

Placeholders (definition-identified or zero displayed quantity) show zero bank availability even when virtual copies exist elsewhere. Empty/negative-ID slots, BLANKOBJECT sentinels and bank fillers are skipped. The actual bank widget decides whether a slot exists; From Scratch invents no slots. A physically missing item remains in ownership storage. These widget reads are for rendering only; initialization still uses the complete bank API container and its accepted validation rules.

Purple #B450FF text is right-aligned at the item's bottom-right with the RuneLite small font and black shadow. Compact integer formatting uses K/M/B/T/Q/E with floored precision, safely covering Long.MAX_VALUE. Positive bank quantities render normally; zero quantities receive uniform #505050 tint at approximately 48% opacity across the full item bounds. This is the permitted fallback, not full sprite grayscale. Ordinary quantity glyphs retain about 52% contrast and purple counters are drawn last at full color. Partial rows retain the same item-relative label position; clipped counters are not shifted into view. No game/widget quantity, ID, sprite, click zone or menu is modified. Graphics paint/font/clip are restored. UI/client transforms supply scale; no extra scale multiplier is guessed.

Stage 3.4 removed active legacy bank/inventory/equipment/GP summaries and the temporary milestone card, retaining setup previews, actionable errors, run start time, protected Reset and the main dashboard. Virtual bank quantities and Dim locked bank items default on; Virtual quantity color defaults #B450FF. All update immediately through RuneLite's normal From Scratch configuration panel. The old help key is preserved. Currency/wealth metrics and play-time tracking remain placeholders.

## Locked-item visual refinements

**Root cause:** BankItemPainter intentionally began its mask at bounds.y + 12 to reserve the ordinary quantity band. It filled only the remaining height with black at alpha 145/255 (approximately 57%). That left the top of tall/colorful sprites untouched and put a sharply bounded dark rectangle over the lower part. This source evidence matches the user's live observation; it was a painting rule, not a changed item balance or persistence failure.

**First change (superseded appearance):** replace that partial fill with RGB(128, 128, 128), alpha 77/255 (approximately 30%), over full item bounds and remove the band-height constant. It corrected the within-sprite color split, but the user requested slightly darker shading and reported that a partly visible top row still had no overlay. Painter-only clipping tests had not covered the upstream item enumeration.

**Top-row root cause:** read-only javap inspection of the installed official injected-client 1.13.3 renderer shows that canvas coordinates are updated for each item widget before it is admitted to the WidgetItem callback list. Admission tests only its origin against the clip. When scrolling leaves the origin above the viewport but the bottom of the sprite visible, the item never reaches WidgetItemOverlay. A bottom-clipped item with its origin inside can still be listed. This is evidence from installed code, not a live-client capture; no game session was operated. Production does not access obfuscated/private members or use bytecode inspection.

**Final viewport change:** replace WidgetItemOverlay enumeration with supported Overlay rendering at the same Bankmain.ITEMS draw hook. Read that layer's actual child widgets and include rectangles intersecting the content viewport. Clip to ITEMS intersected with ITEMS_CONTAINER and the incoming graphics clip; no tab bar, search controls or other UI may be tinted. Each item is visited once. Full item bounds determine both shade and label position, with identical clipping at the top/bottom. No item is accepted merely because a fixed bank-slot index suggests it should be visible. Existing bank snapshot rules are unchanged.

**Final appearance change:** use dark neutral RGB(80, 80, 80), alpha 122/255 (approximately 48%). This mutes highlights and colorful sprites more strongly while keeping the neutral tint closer to a dark bank background, reducing rectangular background contrast. Quantity glyphs retain approximately 52% contrast and purple counters are still drawn last, without fading. Positive ownership and disabled visual settings bypass the tint. This remains a full-bounds tint, including background pixels; it is not exact placeholder/grayscale sprite rendering or a verified match to reference-image pixels.

**Drag limitation:** retain the actual drag rectangle supplied in OverlayManager's current WidgetItem list. An active dragged widget omitted from that list has no supported drag offset available here; skip its transient floating effect rather than paint a ghost at the old slot or guess mouse offsets. Shading returns when released. Normal dragging, menus and input are not modified. This specific clipped-origin drag case needs user retesting and is a known visual fallback.

**Supported API investigation:** inspected the resolved RuneLite 1.13.3 ItemManager.getImage, Client.createItemSprite, SpritePixels.toBufferedImage, ImageUtil.grayscaleImage and Widget accessors. Cached grayscale copies can be generated, but the ItemManager image has preset border/shadow/quantity-mode options and can initially be blank off-thread. Replacing an already-rendered bank sprite requires matching its exact outline, shadow, quantity labels, opacity and drag state; Widget has no supported sprite-shadow getter. A generated grayscale image may cover ordinary labels or mismatch the existing silhouette/shadow. A transparent replacement also leaves the original colors underneath. The correction therefore adopts the user's allowed light neutral-gray fallback instead of claiming a reliable sprite-replacement implementation. No game asset is mutated, cached source image altered or unsupported API used. See COMPATIBILITY.md for source references.

**Performance and scope:** one linear pass over the normal bank's current child widgets and one clipped fill per visible locked item; no per-frame item/ownership map, sprite generation, grayscale conversion, image cache, IO, item-price call or accounting work is added. Offscreen widgets do not trigger definition lookups; visible placeholder definitions remain cached. Live frame rate has not been measured. This final follow-up changes VirtualBankOverlay.java, BankItemPainter.java, BankOverlayStateTest.java, BankItemPainterTest.java and descriptive documentation, and adds VirtualBankOverlayTest.java. Ownership, accounting, save codec/repository, legacy snapshot, configuration keys, overlay registration and bank snapshot reader remain untouched by it.

## Persistence and migration

RunRecordCodec writes envelope format 2, retaining lifecycle header schema 1. The new required ownership object stores each item's total and all four location fields. The previous virtualEconomy currency/wealth fields and reserved items object remain zero/empty until an explicit later migration; ownership is now the canonical virtual-item source. Missing format-2 ownership or locations, negative/fractional/overflowing quantities, over-allocation, mismatched identity, unknown formats/fields and checksum failures reject loading intact. Missing item records inside a valid ownership object are zero.

Loading verified M2 format 1 runs under the character lock on the background worker:

1. Validate the entire current save, including M2's empty-economy guarantee.
2. Copy exact bytes to migration-v1-<unique-id>.json through Filepath, force the file and validate logical equivalence.
3. Create a new revision with empty virtual ownership, preserving character, run ID, status, start time, previous-run/reset reference and the immutable baseline.
4. Force/validate a new checkpoint and publish current.json using the existing recoverable replacement path. Acknowledge Active only afterward.

Older checkpoints, reset archives and migration backups are retained. Format-2 restarts do not migrate again. Missing current data with any recovery/migration evidence blocks blank startup. Failed backup/validation/publication leaves recovery evidence and shows an error rather than substituting zero state. A durable but unacknowledged write is resolved by reload. The existing non-atomic filesystem fallback retains its known limitations; forcing/backups cannot guarantee every hardware failure.

Storage remains permanently local-only on one designated host, under the plugin Filepath directory. No cloud/service/credentials/backend or mobile reconciliation was introduced. M2 cannot read format 2; deliberate downgrade requires a compatible retained backup with all clients closed. Never delete recovery evidence, edit checksums or share private saves. No actual user save was read or rewritten during development.

## Files changed

New production files under src/main/java/com/fromscratch:

- model/VirtualLocation.java, VirtualItemOwnership.java, VirtualOwnership.java
- overlay/BankOverlayState.java, BankOverlayRegistration.java, VirtualBankOverlay.java, BankItemPainter.java, VirtualQuantityFormatter.java

Modified production files:

- FromScratchPlugin.java / FromScratchConfig.java — overlay lifecycle and live display settings.
- controller/SetupController.java — committed render projections and cleaned Active status text.
- model/RunRecord.java / VirtualEconomy.java — independent ownership/revisions; compatibility item totals accessor, currency placeholders retained.
- persistence/RunRecordCodec.java / RunRepository.java — strict format 2, version-1 decoding, backed-up migration and recovery guards.
- ui/FromScratchPanel.java — hide active legacy/debug details while keeping setup/error/dashboard flows.

New tests: model/VirtualOwnershipTest.java, persistence/OwnershipPersistenceTest.java, overlay/BankItemPainterTest.java, overlay/BankOverlayStateTest.java and overlay/VirtualBankOverlayTest.java. Updated RunRecordCodecTest, FromScratchSidebarTest and SnapshotPanelTest assertions for the intentional schema/UI changes. All prior cases remain.

Updated README.md, CHANGELOG.md, ARCHITECTURE.md, ACCOUNTING.md, TEST_PLAN.md, AGENTS.md, docs/COMPATIBILITY.md, docs/PUBLICATION.md and the subsequent-acceptance note in docs/MILESTONE_2.md. Added this report. Build files, Gradle Wrapper, Java 11, CI, plugin metadata/version, BSD license, icon assets and author Pilots r kwl are unchanged.

## Automated validation

The stages were validated incrementally with the complete regression suite. Final command:

```powershell
.\gradlew.bat clean build
```

**Final clean build after the viewport/darker-shading correction passed on Java 11 / RuneLite 1.13.3: 285 passed, 0 failed, 0 errors, 0 skipped.** This comprises 202 retained cases plus 14 ownership, 16 persistence/migration, 29 painter, 8 state/configuration/lifecycle and 16 overlay-pass tests. Earlier M3 builds had 258 and then 268 passing tests. All previous cases remain, with appearance thresholds/names adapted to the newly requested 48% opacity. The complete 32-requirement mapping is in TEST_PLAN.md. Positive quantities exist only in synthetic fixtures. `git diff --check` passed.

The final correction adds 16 overlay-pass tests with synthetic current-widget observations: first full row, top-clipped origin, bottom-clipped row, matching middle-row appearance, precise viewport/control exclusion, unshifted labels, resizing, repeated scrolling, tabs/search/empty results, closure, disable/re-enable, positive ownership, invalid viewport, exact restoration of a nonrectangular graphics clip, and cached/offscreen definition reads. A seventeenth regression checks that the darker tint stays close to a representative dark bank background instead of creating a bright gray tile. Existing full-height/tall/colored-item, readable quantity glyph, full-color purple, scaling, lifecycle, model and persistence cases remain passing. These tests do not simulate the actual game widget implementation, OSRS input, reference-image pixels or live frame rate.

The retained template launcher produces an unchecked generic-varargs warning; Gradle 8.10 reports inherited deprecations for Gradle 9. No dependency was added. Hosted CI has not run in this session. Automated pixels, compilation and model tests do not establish correct live widget lifecycle, all bank layouts or performance.

## Manual acceptance — user only

Launch from the repository root with `.\gradlew.bat run`; the agent can offer to launch it but must never control the game. Follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

1. Preserve the existing accepted run. Wait for loading to finish: same Active run/start time, no reset or fresh snapshot required, original legacy totals absent from the sidebar, dashboard still present. This refinement adds no migration.
2. Open the normal bank manually. Check the first fully visible row and middle rows, including tall equipment and colorful items: the #505050/48% tint should look subdued and slightly darker than the previous 30% tint, with no sharp horizontal divide. A subtle full-bounds background tint is expected with this fallback. Every occupied real item shows purple zero at bottom-right; ordinary OSRS quantity values remain unchanged and yellow/white/green glyphs readable. Purple counters must remain vivid. Placeholders show zero; fillers/empty/BLANKOBJECT slots and tab/header icons have no overlay. Inventory/equipment/shared/special storage remain unaffected.
3. Scroll slowly until a row is partly visible immediately below the tab bar. Its visible portion must be tinted exactly like middle rows. Repeat at the bottom edge; both boundaries must clip precisely with no shading on tabs, search controls or surrounding UI. Counters may be clipped but must never move to fit the remaining fragment. Scroll rapidly up/down, change normal/locked/unlocked tabs and searches (including no matches), resize/change UI scale and try existing bank layout plugins. No stale rectangle or misalignment may remain.
4. Manually hover, drag from full and partly clipped rows, right-click and deposit/withdraw if desired. All controls/tooltips/ordinary quantities must work normally. RuneLite-provided drag rectangles must be followed; a clipped-origin drag without supplied bounds temporarily has no floating tint and regains shading on release. No ghost should remain at the old slot. All virtual quantities remain zero because acquisition/transfer accounting is not implemented; the plugin performs no action itself.
5. Toggle quantities and dimming separately, then change color. Verify immediate changes. Close/reopen the bank, including PIN screens where applicable: no effect may remain when the bank is closed.
6. Repeatedly disable/re-enable: exactly one overlay and sidebar button, no leftover shading. Restart/relog and switch characters/RuneLite config profiles; only the verified Active character renders, with its run preserved. Compare scrolling/input responsiveness on a large bank with the effect on/off; manually test drag transparency, right-click menus and tooltips. The agent must not operate the game to perform these checks.
7. Report RuneLite version, client mode/scale, other bank plugins, expected/actual behavior and redacted errors. Do not edit saves or grant positive balances for testing. Positive live accounting belongs to later authorized work.

## Limits and publication considerations

No live acquisition/transfer, GE, reconciliation, buyback or valuation engine exists; production counts stay zero. No full Virtual Bank browser is built in this milestone. The final fallback is dark full-bounds neutral-gray tint at approximately 48%, not true silhouette-only grayscale; it also fades ordinary labels and the local bank background. A clipped-origin drag can temporarily lack a tint if RuneLite supplies no drag rectangle. Other item overlays may compete for the lower-right corner; no other plugin/config is altered to resolve that. M3 functional behavior was accepted, but the revised partial-row appearance, custom layouts, UI scaling, dragging and performance need user retesting. Future-GE allocations are stored only, not tracked.

Current RuneLite overlay APIs, Plugin Hub review/rejected-feature pages and Jagex guidelines were checked; see COMPATIBILITY.md. The feature is informational and draw-only, with no game automation, packets, synthetic input, interception, conditional menus, original quantity changes or external player-data transmission. This is source/design evidence, not guaranteed Hub approval. Manual acceptance, later milestones, real performance/compliance audits, screenshots and eventual publication revalidation remain outstanding. Stop after M3; M4 requires authorization.

## Git state

Branch master, based on accepted M2 commit `9e6b757`: 20 tracked files modified, 14 new files, nothing staged/deleted. Most changes predate this visual follow-up and remain as the uncommitted M3 milestone. No build output, real account data or credentials are included. No commit, push, tag, reset, release or submission was performed.
