# RuneLite compatibility evidence

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
