# Publication preparation

This is an early checklist, not a release candidate or submission. M1/M2 passed user acceptance. M3 ownership, save migration and bank visuals await manual acceptance; M4-M10 remain unimplemented.

## Foundation prepared

- From Scratch / Pilots r kwl / `com.fromscratch` / config `fromscratch` / internal name `from_scratch`.
- Hub description/tags, `build=standard`, development version `0.1.0-dev`.
- BSD 2-Clause license and template/asset provenance.
- Root 48x48 and bundled 24x24 transparent PNGs with format/dimension tests.
- Java 11 wrapper build, JUnit tests, CI configuration, issue templates, ignored build/private-data paths.

## Release gates outstanding

- Implement authorized milestones; complete accounting, persistence/recovery, migration, and regression tests.
- Obtain user-run in-game acceptance, privacy-safe screenshots, performance measurements.
- Recheck [Plugin Hub](https://github.com/runelite/plugin-hub), [template](https://github.com/runelite/example-plugin), [review](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review), [rejected features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features), [Jagex guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1).
- Audit APIs/dependencies: no reflection/native/processes, automation, outgoing input/chat changes, unsafe storage, or unapproved enforcement.
- Verify public owner/repository URL, license/attribution, metadata, icon readability, clean source tree, no credentials/local paths/private progression in tracked content/history.
- Final clean build and hosted CI; record RuneLite version, release version, changelog, accurate limitations.
- Explicit approval before commit/push/release/submission. No release candidate exists yet.

## Future Hub manifest

The Hub's own repository needs a marker under `plugins/from_scratch` pointing to the verified public Git URL and approved 40-character source commit:

```properties
repository=<verified public repository HTTPS Git URL>
commit=<approved full source commit hash>
```

These are documentation placeholders, not a usable manifest. Public owner and approved release commit are not established here. Do not include META-INF/services/net.runelite.client.plugins.Plugin in this project.

Prepare the eventual PR description with released behavior, storage/compliance choices, validation, limitations, and licensing. AGENTS.md explicitly requires this footer for a Plugin Hub PR:

```text
Generated-by: Claude Code
```

No PR has been created. Review may request changes; approval is not guaranteed.
