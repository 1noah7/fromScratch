# Publication preparation

Milestones 1–9.2 are owner-accepted; gameplay is frozen. Milestone 10 prepares an **unpublished** Windows beta and reviewer package. **Milestone 11 exclusively owns official Plugin Hub submission/review/launch.** No commit, push, tag, public upload, distribution, settings change or publishing PR is authorized here.

## Prepared locally

- Canonical repository [PilotsRKwl/fromScratch](https://github.com/PilotsRKwl/fromScratch); `master` baseline `a2436132c9fb72cc25d015c35309628398486280`, matching remote master at verification. Older milestone tags have not been moved.
- From Scratch / **Pilots r kwl** / `com.fromscratch` / `fromscratch` / `from_scratch`, Java 11, BSD-2-Clause, existing small PNG icons.
- Version `0.10.0-beta.1`, separate bootstrap and unmodified RuneLite 1.13.3 runtime JARs, manifest/checksums/provenance, installer/desktop shortcut/update/safe uninstall. See [installation](INSTALLATION.md).
- Promotional [README](../README.md), original title/social artwork, pending real-screenshot list, [release draft](RELEASE_DRAFT.md) and [metadata recommendations](REPOSITORY_METADATA.md).
- [Reviewer map](PLUGIN_HUB_REVIEW.md), [features](FEATURES.md), [privacy](DATA_AND_PRIVACY.md) and [beta checklist](BETA_TESTING.md). Runtime widget-display and filtering behavior is disclosed explicitly.
- Local official standard-template compile passed. This supports `build=standard` suitability; it is not hosted packaging/scanning or Plugin Hub approval.

## Owner release gates

1. Review [Milestone 10 evidence](MILESTONE_10.md); test actual beta graphical launch, official Jagex authentication and gameplay, preserving real progression. A help smoke alone is insufficient.
2. Inspect hosted CI diagnostics. The latest master run failed on **both** platforms; do not claim green CI from local tests or blame an informational Ubuntu notice. Approve any later push/rerun only after reviewing changes.
3. Capture/redact/approve genuine [screenshots](SCREENSHOTS.md), then insert actual links. Review the rendered README/social preview.
4. Review the diff and private-data exclusions. After explicit approval to commit, record the real resulting revision, rebuild provenance/checksum and run only validation invalidated by changes. No tag rewriting.
5. Approve the GitHub Release/assets and About/topics separately. Use actual download links only after publication. A checksum is not a publisher signature. Friend distribution remains unauthorized until approved.
6. In Milestone 11, recheck current [Hub rules](https://github.com/runelite/plugin-hub), [review process](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review) and [rejected features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features), then obtain approval for the publishing PR.

## Future submission

The eventual `plugins/from_scratch` Hub marker must use the canonical Git URL and an **approved, existing immutable full commit**. None is prepared with a guessed future SHA. Root `runelite-plugin.properties` requests the standard build; the Hub replaces local Gradle configuration and compiles production source only. No service-provider registration file is included.

The eventual PR should disclose scoped physical-counter display flags, native Bank Tags/tab filtering, local informational chat, Filepath persistence/imports, the price feed, finite buyback offsets and beta/bootstrap separation. Include validation, limitations, artwork/license provenance and any attribution footer required by applicable instructions at submission time. No PR has been opened; review can request changes.
