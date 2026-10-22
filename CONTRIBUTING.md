# Contributing

From Scratch is by **Pilots r kwl**, under the [BSD 2-Clause license](LICENSE). Milestone 9.2 gameplay is frozen. Milestone 10 prepares documentation and an unpublished beta; official Plugin Hub submission belongs to Milestone 11.

Read [AGENTS.md](AGENTS.md), the [architecture](docs/technical/ARCHITECTURE.md), [accounting policy](docs/technical/ACCOUNTING.md) and [test plan](docs/technical/TEST_PLAN.md) before proposing behavior changes. The [reviewer map](docs/PLUGIN_HUB_REVIEW.md) identifies production code and sensitive APIs. Historical milestone notes are reference evidence, not current scope or installation instructions.

## Development and validation

Use Java 11 and the existing Gradle wrapper. Prefer incremental `compileJava compileTestJava` and actual affected test-class filters. Do not routinely clean or run every gameplay test for documentation/asset changes. Both Windows and Ubuntu CI passed at the current baseline; meaningful production changes require appropriate new validation. See the test plan for exact commands.

Keep disk and price IO off the client thread and EDT. Use the complete bank container for quantities, Filepath for progression, checked signed arithmetic and guarded asynchronous acknowledgments. Do not reconnect historical personal-container tracking, add gameplay automation/withdrawal blocking, rename config keys, introduce reflection/native calls or duplicate RuneLite transitive dependencies.

## Documentation and previews

[Installation](docs/INSTALLATION.md) is the authoritative beta guide. Player-facing copy belongs in README; technical details belong under `docs/technical` and the reviewer guide. Preserve root AGENTS/CLAUDE discovery, the license and attribution. Check local links with `scripts/Test-Documentation.ps1`.

`scripts/Prepare-UiPreviews.ps1` renders actual Swing components with sample data and official RuneLite sprites. Its separate `src/preview` source set is excluded from production/beta JARs. No game client, real progression or credentials are used. Label previews as sample UI and obtain owner screenshots for genuine gameplay.

## Safety and review

Never use automated input to operate RuneScape. The owner performs actual login/gameplay acceptance. Keep test IO isolated under `build`; never test destructive recovery/reset against real character saves. Do not include artifacts, profiles, secrets, exports or private logs in Git.

No commit, push, tag, upload, distribution, Plugin Hub submission or publishing PR without explicit owner approval. Changes should remain small, reviewable and separate from unrelated formatting. The [beta testing guide](docs/BETA_TESTING.md) contains a privacy-safe report template.
