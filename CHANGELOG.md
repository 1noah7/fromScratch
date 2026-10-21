# Changelog

## Milestone 2 — Unreleased

M1 passed user-run acceptance. The user isolated the remaining M2 failure to empty inventory/equipment. The empty-state correction below awaits live retesting. No publication or release tag has been created.

### Empty personal-container correction

- Keep valid zero-length/all-empty-slot API containers accepted. No positive item count is required.
- Independently verify null empty INV/WORN through complete initialized personal interfaces on two different game ticks. Require all 28 inventory slots or all 11 logical equipment slots; reject missing/default/hidden views and populated widgets without API data. Never infer bank contents from widgets.
- Preserve three-location previews, explicit confirmation, cache age/change guards and existing persistence/schema. Players do not need to carry or equip an item to start.
- Add 29 regressions covering all four empty/populated combinations through both observed API data and null-container verification, unavailable/incomplete data, contradictory occupied slots, ring/ammo, settling and confirmation/codec preservation. All previous 173 tests remain (202 total).
- Permanently designate local-only storage on one host computer; exclude cloud synchronization, credentials and backends. Reconciliation after gameplay elsewhere remains future M4 work.

### Multi-phase personal-container correction (historical; empty-case follow-up below)

- Replace the simultaneous three-container requirement with validated inventory/equipment observations collected throughout setup, including before bank opening. Cache for at most 60 seconds; expire on changes, invalid data, identity/session changes or setup cancellation. Always read the current complete open bank.
- Copy valid personal ItemContainerChanged payloads, settle related updates on a game tick, invalidate personal caches after a previously observed bank changes, and revalidate on confirmation. Missing containers remain unknown.
- Show independent Bank, Inventory and Equipment readiness plus partial summaries and the manual interface step for missing data. No duplicate setup section.
- Add rate-limited DEBUG diagnostics for login/setup state, mounted bank, container sizes, cached observation ages and exact rejection predicate. No item lists or identity are logged.
- Add 26 regressions, retaining all 147 previous tests (173 total). Persistence implementation/schema and existing saves are unchanged. Automated results and remaining live uncertainty are documented in MILESTONE_2.md.

### First bank-readiness correction (historical; failed subsequent live acceptance)

- Remove the requirement for ItemContainerChanged on every bank opening. Detect the mounted normal bank, visible item layer and current containers, reject PIN/partial/null data, and allow a game-tick settling boundary.
- Retry unavailable capture data on game ticks; already-open and unchanged reopened banks require no quantity-change event. Show actionable diagnostics without repeating identical Swing updates.
- Show Bank snapshot: Ready with summaries and enabled confirmation. Closing the bank invalidates the preview; confirmation still rereads all contents.
- Replace duplicate instruction sections with one short message before initialization, capture status during initialization and no setup instructions after activation. Preserve the existing preference key for detailed hover help.
- Add 26 regressions; full suite is 147 tests, including the previous 121. Persistence code, save schema, valid runs and real items are unchanged.

- Add Begin/review/confirm workflow using the full normal bank, inventory and equipment containers; preserve exact IDs and checked long quantities in an immutable legacy baseline.
- Exclude placeholders, empty slots, bank fillers and special storage. Revalidate identity/contents on confirmation; no incomplete or automatic activation.
- Verify RuneLite character identity and separate account/profile-type storage with opaque keys.
- Add versioned checksummed Filepath checkpoints on a bounded background worker, recoverable replacement, explicit error/retry states and protected archival reset.
- Preserve active baselines across logout/restart/disable/profile switches. Reject duplicate starts and stale callbacks. Initial virtual economy remains zero despite legacy GP.
- Extend sidebar summaries, instructions, test coverage and documentation. The existing help config key, Java 11 target, metadata version and Gradle Wrapper are preserved.

No acquisition accounting, restrictions, overlays, buybacks, GE or valuation were added. Hosted CI and M2 manual acceptance remain pending.

## 0.1.0-dev — Unreleased

Foundation milestone; subsequently accepted by the user before M2 development.

- Rename package/plugin/config/project/metadata to From Scratch by Pilots r kwl.
- Add original purple coin assets, navigation lifecycle, placeholder dashboard, and local help preference.
- Define a validated immutable schema-v1 lifecycle header and stable statuses.
- Document architecture, accounting, milestones, tests, limitations, artwork, contributions, and publication gates.
- Add 27 foundation tests and Java 11 Linux/Windows CI configuration.

Local clean builds passed against RuneLite 1.13.3 and the observed Hub target 1.13.2. Hosted CI and in-game acceptance remain pending. No snapshots, virtual accounting, progression persistence, buybacks, bank overlays, or GE tracking are implemented yet.
