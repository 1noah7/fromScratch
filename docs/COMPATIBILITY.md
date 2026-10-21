# RuneLite compatibility evidence

## Milestone 2 update

Empty-state follow-up: code inspection found no positive-count requirement; existing zero-item maps/arrays are already valid. The user's live test distinguishes null empty INV/WORN from populated API containers. The adapter now verifies emptiness independently rather than treating every null as empty. Supported Widget.getChild/getItemId/getItemQuantity/isSelfHidden/getSpriteId and mounted-interface/visibility checks are available in the installed 1.13.3 source and compile successfully.

Reviewed RuneLite's decompiled client scripts at revision `1d418ffb6bce96c2d0712eb2652712d28e01b115` (2026-10-20): [inventory_init](https://github.com/runelite/cs2-scripts/blob/1d418ffb6bce96c2d0712eb2652712d28e01b115/scripts/%5Bclientscript%2Cinventory_init%5D.cs2), [inventory_drawitem](https://github.com/runelite/cs2-scripts/blob/1d418ffb6bce96c2d0712eb2652712d28e01b115/scripts/%5Bproc%2Cinventory_drawitem%5D.cs2), [wear_initslot](https://github.com/runelite/cs2-scripts/blob/1d418ffb6bce96c2d0712eb2652712d28e01b115/scripts/%5Bproc%2Cwear_initslot%5D.cs2), [wear_updateslot](https://github.com/runelite/cs2-scripts/blob/1d418ffb6bce96c2d0712eb2652712d28e01b115/scripts/%5Bproc%2Cwear_updateslot%5D.cs2). Inventory creates all 28 children and explicitly hides BLANKOBJECT (gameval ItemID.BLANKOBJECT=6512) for empty slots. Equipment creates item child 1 and icon child 2; the empty branch hides the item and shows its initialized icon, including when the item child retains an earlier ID. Core 1.13.3 InventoryGridOverlay reads all 28 children; MenuEntrySwapperPlugin reads equipment child 1. All normal logical equipment components SLOT0/1/2/3/4/5/7/9/10/12/13 exist in gameval InterfaceID.Wornitems.

The production fallback only observes these supported widget fields; it never invokes scripts or changes game widgets. Every slot must prove empty on two different game ticks; partial/default/hidden layouts fail closed. Ring/ammo are included. Client/API sources still do not promise that every empty container will exist, and no live API log was supplied; the user's empty-versus-populated result plus the prior null rejection identify the adapter representation gap. Unexpected future layouts may require updating the verifier. Real in-game acceptance remains mandatory.

Second live failure: the previous 147-test correction still rejected a null player inventory. The cached Maven `net.runelite:runelite-api:1.13.3` sources verify gameval InventoryID.INV=93, WORN=94, BANK=95; the deprecated enum maps INVENTORY/EQUIPMENT/BANK to the same values. Client.java marks both getItemContainer overloads @Nullable. The corresponding `net.runelite:client:1.13.3` sources show ItemStatPlugin.getCurrentGP checking INV for null, ItemStatOverlay checking WORN for null, and BankPlugin checking BANK independently. These sources establish supported identifiers and nullable contracts, not a guarantee about live timing or the reason this user's inventory is absent. [Tagged Client API](https://github.com/runelite/runelite/blob/runelite-parent-1.13.3/runelite-api/src/main/java/net/runelite/api/Client.java).

The collector combines a current complete mounted-bank observation with validated personal observations from the same initialization session (60-second maximum age). Personal events/invalid reads, bank changes, identity/session changes and cancellation invalidate caches as documented in ARCHITECTURE.md. A null personal container requires the independent empty-view verification above. The UI reports each location independently. DEBUG diagnostics remain throttled to one line per ten seconds and include emptyVerification status. Unknown data remains blocked. No production save/schema changes. Actual live behavior remains unverified.

The bank-readiness correction rechecked Gradle resolution: client/API 1.13.3. Its embedded official BankPlugin sets bank-open state in WidgetLoaded/WidgetClosed for InterfaceID.BANKMAIN and uses ItemContainerChanged for cached quantity invalidation. Group BANKMAIN is 12, InventoryID.BANK is 95, Bankmain.ITEMS_CONTAINER is the normal bank item layer, BANKPIN_KEYPAD is group 213 and BANKPIN_SETTINGS is group 14. Production references the gameval constants rather than these numeric literals.

Bank detection checks live WidgetNode entries in Client.getComponentTable, item-layer visibility, PIN absence, a settling tick and a non-null current BANK container. It uses complete API containers independently of displayed widgets. Quantity-change events are not required to reopen an unchanged bank. GameTick polls pending setup; all API reads stay on the client thread and immutable views move to Swing. The API does not expose a separate full-packet receipt flag; bank lifecycle/availability behavior remains subject to manual acceptance. Source reference: [official BankPlugin](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/bank/BankPlugin.java).

The current default build still resolves RuneLite 1.13.3. Actual dependency sources were inspected for ConfigManager RS profiles, RuneScapeProfileType, OAuth account identity, ItemContainerChanged, gameval InventoryID/InterfaceID/ItemID, item placeholder definitions, ClientThread and Filepath. The implementation compiles without new dependencies and uses those supported APIs. M2's full-container freshness and real identity behavior still require manual acceptance. The M1 1.13.2 compatibility result below is historical, not an M2 test result.

Production data I/O uses Filepath from Plugin.getPluginDirectory, obtained on the worker. Nonblocking file locks, force, move/copy and bounded readers operate through Filepath. Test-only roots use Filepath.Unchecked in synthetic JUnit temp directories; production never uses that escape hatch.

The existing Java 11/Gradle Wrapper and standard Hub build configuration are unchanged. Hosted CI remains pending; see TEST_PLAN.md for current exact test counts.

## Milestone 1 evidence

Checked for M1 on 2026-10-20. This is source/build evidence, not in-game acceptance or Hub approval.

## Build

- Existing Gradle 8.10 Wrapper and distribution checksum preserved; installed Temurin Java 11.
- Compiler release remains 11; test verifies production class major version 55.
- Template dependency layout retained: RuneLite compileOnly, Lombok compileOnly/annotationProcessor, JUnit and RuneLite test launcher. No new dependencies.
- `latest.release` resolved to RuneLite client/API 1.13.3. Dynamic resolution follows the [official template](https://github.com/runelite/example-plugin/blob/master/build.gradle); record/recheck the actual version on later builds.
- The retrieved [Hub version file](https://github.com/runelite/plugin-hub/blob/master/runelite.version) listed 1.13.2. An additional clean build forced client/jshell 1.13.2 through a temporary Gradle init script: all 27 tests passed. The script is not part of the plugin or its dependency configuration. This checks the observed Hub target without changing the template's default resolution.
- Metadata uses `build=standard`; the retained optional template shadow task is not the Hub artifact.
- Local clean build passed, 27 tests. Gradle reports deprecated features incompatible with Gradle 9; the retained Wrapper is 8.10. The template launcher emits a generic-varargs unchecked warning from loadBuiltin; compilation still passes.

## APIs inspected

The actual cached 1.13.3 client source archive was read, alongside current upstream sources.

| API | Capability | Status |
| --- | --- | --- |
| PluginDescriptor | name/configName/internalName | Used; internal name `from_scratch` is lowercase snake case |
| PluginPanel | Scrollable dark-theme wrapped panel | Used |
| NavigationButton / ClientToolbar | Builder and add/remove registration | Used; toolbar queues client UI updates |
| ImageUtil | Class-relative loadImageResource | Used for 24x24 bundled PNG |
| ConfigManager | getConfig and RS profile APIs | Preferences used; profile integration planned M2 |
| Filepath | Safe joins, readers/writers, channels, move/copy, chooser | Available; progression storage planned |
| ItemContainerChanged | Container quantity event; transfers emit multiple updates | Observation planned, not provenance proof |
| Core ExaminePlugin | Normal menu observation and queued local chat | Companion informational line planned M8 |

Sources: [PluginDescriptor](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/PluginDescriptor.java), [Filepath](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/util/Filepath.java), [container event](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/events/ItemContainerChanged.java), [Examine](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/examine/ExaminePlugin.java).

## Limits

APIs expose observations rather than a complete transaction-provenance service. A full current bank requires opening it. Identical instances are indistinguishable. Offline gross acquisitions/consumption and unsupported storage cannot be reconstructed. GE tax/partials/collection, variant mapping, and examine context/deduplication need their milestone tests. Visual warnings are selected; no approval is assumed for conditional gameplay blocking.

The current [Hub README](https://github.com/runelite/plugin-hub#creating-new-plugins) allows root icon.png up to 48x72. Ours is 48x48; sidebar uses 24x24. Recheck before publication. [Review](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review) considers security and game rules; compilation is insufficient.
