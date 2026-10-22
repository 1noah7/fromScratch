> Historical, superseded by Milestone 4 bank-only tracking. This is not the current product contract.

# Milestone 4 — Item tracking and host reconciliation

By **Pilots r kwl**.

**Current status:** The first M4 implementation below failed live acceptance despite its 376 passing tests. The [correction report](MILESTONE_4_CORRECTIONS.md) is now authoritative for behavior, testing and retest instructions: retained acquisition evidence, signed legacy deficits, automatic safe session startup, Account Tools → Update Progress and save format 4. The original report below is preserved as the historical first implementation, not a claim of acceptance. M5 has not begun.

## Status and scope

M1–M3 passed user acceptance. M4 started from clean user-created commit `5b7c49f` on `master`. M4 is implemented for automated validation and **awaits user-run acceptance**. No commit, push, tag, publication, private-save inspection or progression reset was performed by the agent. No code was copied from purpleCounter. No agent operated RuneScape.

This milestone implements item observations, acquisitions, ordinary transfers, mixed-stack allocation, supported consumption/drop return, a durable local journal, confirmed host reconciliation and guided sidebar workflows. GP accounting, GE accounting, prices/wealth, death fees and buybacks remain outside M4. M5 has not begun. The accepted M3 renderer and its settings/resources remain unchanged.

## Implementation

Stages 4.1/4.2 replace technical setup labels with checked inventory, worn-equipment and bank steps. Observed empty locations count. Missing/partial data stays unknown; the existing independently verified empty-interface mechanism is preserved. Full API containers, not visible bank search/tab widgets, supply quantities. Session/time-bound observations invalidate on changes and wait a full quiet tick before comparison. Read-only Eat/Drop hints require later matching physical changes. Availability/age diagnostics are rate-limited DEBUG without inventory lists or account identifiers.

Stages 4.3/4.4 use a pure reducer over immutable Holdings and VirtualOwnership. Match transfers before acquisitions; allocate virtual copies first across ordinary bank/inventory/equipment movements. Explicit noted/unnoted links conserve copies while keeping raw IDs distinct. New ordinary positive deltas credit only the remaining net quantity. Matching one-item Eat decreases record consumption; drops retain legacy/virtual return identity. Legacy-first returns never manufacture ownership from dropped legacy items or count a virtual pickup twice. Copies explicitly preserved Elsewhere relocate on return without increasing total or Recent Acquisitions.

Unexplained decreases, unmatched banking increases, coin-funded changes and known unsupported collection/transformations require review. Ambiguous different-ID outputs remain locked by default. Review permits physical-bounded per-location corrections, a reason, permanent-loss choice and explicit confirmation. Elsewhere preserves only previously virtual copies. Rejected or partially rejected proposed gains leave only the accepted acquisition in history. The original legacy snapshot is never modified.

Stages 4.5/4.6 persist a separate per-character/run ledger and require a complete host-session comparison at each restored login/re-enable. The comparison uses last authoritative physical holdings, not a newly fabricated baseline. It proposes net offline changes only, never missing gross acquisitions/consumption. Tokens bind to session and all current holdings; changes or bank closure invalidate approval. Unchanged uninterrupted gameplay does not repeatedly prompt. A new initial activation with identical already-confirmed contents needs no second approval. Same-character ordinary world loading/hopping retains reliable session authority; a true logout/identity change resets it.

Stage 4.7 publishes only durably committed allocations to the existing purple counters. Legacy-only items remain zero/dimmed; positive bank-owned quantities render normally. Overlay source, tint, clipping, drag behavior and original quantity preservation are unchanged. During host reconciliation/loading/errors, overlays are withheld until current-session trust is established. Items Unlocked is a lifetime distinct-type count; recent acquisitions/history come from committed entries. Active Run Duration is elapsed wall-clock time including offline time. Financial fields remain zero with an unimplemented notice.

## Files changed

| Area | Added | Modified |
| --- | --- | --- |
| Observation | `ItemObservation`, `ItemObservationCollector` | `RuneLiteSnapshotObserver`, `SnapshotSource` |
| Pure tracking | `Holdings`, `ReturnBalance`, `ItemTransaction`, `ItemProgress`, `AccountingPlan`, `ItemAccountingEngine`, `ItemTrackingSession`, `ItemLedgerBatch` | No M3 ownership-model replacement |
| Persistence | `ItemJournalCodec`, `ItemJournalStore` | `RunRepository`, `RunRecordCodec`, `RunRecord` |
| Lifecycle/dashboard | `PreparationChecklist`, `ItemReview`, `ItemReviewTable`, `ItemReviewDialog`, `RunDuration` | `FromScratchPlugin`, `SetupController`, `VirtualEconomy`, `FromScratchPanel`, `SetupActions`, `SetupView` |
| Tests | Six M4 suites and `TrackingFixtures` | `OwnershipPersistenceTest`, `SnapshotPanelTest` assertions for version/checklist/timer |
| Documentation | This report | README, ARCHITECTURE, ACCOUNTING, TEST_PLAN, CHANGELOG, AGENTS, COMPATIBILITY, historical M3 acceptance note, privacy `.gitignore` patterns |

Production Java is under `src/main/java/com/fromscratch/`; test counterparts are under `src/test/java/com/fromscratch/`. No metadata, icon, license, Gradle Wrapper, dependency, CI or accepted overlay implementation change is needed. No generated build artifact or private save is tracked.

## Persistence and migration

Storage is permanently local-only under RuneLite Filepath's `from_scratch/characters/<opaque-character-key>/`, isolated by verified account/profile-type identity. Nothing is sent to a server or written into the checkout. All IO runs on the background worker; client/EDT code receives immutable results and rejects stale callbacks.

Envelope **format 3** retains header schema 1 and existing ownership, adding ItemProgress: physical checkpoint, return balances, lifetime unlocked set, bounded 50-entry recent/acquisition lists and a session/tick/time/journal cursor. The complete audit is separate immutable `ledger-<run>-<revision>.json` segments. Entries have UUID/time/item/quantity/virtual-location deltas/source/destination/category/confidence/reason; batch identity provides character/run/session/sequence and prior head/revision. No monetary accounting fields are invented.

Verified format-1/2 loads first force and verify exact-byte migration backups. Run ID/start time/character/original LegacySnapshot and nonzero M3 ownership survive. Only verified M2 format 1 may initialize empty ownership. Migrated progress is unestablished and requires the current host comparison. Older checkpoints/archives remain intact. Failed/corrupt/incomplete/newer data never becomes a blank run. Development tests migrated synthetic fixtures only, not the user's save.

Under the character lock, a commit compares the expected current record, validates transaction deltas, forces/verifies the immutable segment, forces a recovery checkpoint and publishes current with atomic replacement where supported or the existing recoverable-copy fallback. UI/overlay state changes only after success. Identical completed retries are idempotent; stale revisions fail. Interrupted orphan segments remain preserved but are not replayed. A failure blocks automatic accounting and offers Retry Load.

Loading validates the referenced chain's identity, sequence, checksums, unique entry IDs, bounded history references and current-head/result consistency. Missing/corrupt chain files block loading intact. Reset retains the old run's archive and journal, and starts no replacement. Backup/restore requires the **entire directory**, including referenced segments; current.json alone is insufficient. No general import/export UI is added.

## Automated verification

Final validation on 2026-10-21: `.\gradlew.bat clean build` **BUILD SUCCESSFUL**, exit 0, all seven tasks executed, using the existing Wrapper, Java 11 and resolved RuneLite 1.13.3. XML results report **376 passed, 0 failed, 0 errors, 0 skipped, 24 suites**. Reports: `build/reports/tests/test/index.html` and `build/test-results/test/` (ignored build artifacts). `git diff --check` passes.

The existing test launcher emits an unchecked-operation note; the retained Gradle 8.10 build reports deprecations for Gradle 9. Neither is a compilation/test failure. No dependency/Wrapper update was made as part of M4.

The 285 accepted-milestone cases remain; M4 adds 91 cases across six new suites (376 total). Pure reducer, observer, host-session, controller, persistence and EDT/table tests cover the requested acquisition/transfer/review boundaries. Existing 53 bank painter/state/overlay-pass cases remain unchanged. JUnit tests use synthetic data and temporary Filepath roots, with no account credentials or in-game input. See [TEST_PLAN.md](TEST_PLAN_MULTI_CONTAINER.md) for counts and requirement mapping.

No clean JVM launch, rendered screenshot, hosted CI run or live performance result is claimed. Live behavior cannot be established by Gradle or synthetic pixels.

## Known limitations and review considerations

- API events are observations, not atomic transaction/provenance records. A full quiet tick reduces intermediate comparisons; ambiguous bank/source changes still require explicit review. Missing data never means deletion.
- Null empty personal containers may require manually showing the normal tab for two settled ticks. Unsupported or incomplete layouts remain blocked with an instruction; no item needs to be carried/equipped to satisfy emptiness.
- Automatic ordinary net acquisition is honor-system accounting. Known unsupported collection actions are guarded, but custom/unknown storage sources are not exhaustively identifiable. Correct legacy retrieval manually; no intrusive anti-cheat is added.
- Explicit noted/unnoted conversions are supported; charges, degradation, doses, crafting, GE, special storage and most resource/ammunition disappearance remain reviewed. A matching ordinary Eat is narrower than a general consumption engine.
- Coin changes remain physical observations. Financial metrics and virtual GP stay zero. Shop/GE/special-source outputs require item review where detected; there is no cost, tax or offer engine.
- Identical dropped/missing copies cannot be identified individually. Legacy-first return reserves are conservative and persist across restart; genuinely new copies may need correction. Net-zero drop/reacquisition in one batch does not create fictitious history. Elsewhere returns require review rather than duplicate ownership.
- Offline comparison establishes only net holdings. It cannot reconstruct acquired-then-consumed items, provenance or detailed mobile transactions. Never infer a gross history from a net increase.
- The history dialog shows 50 recent entries; the durable ledger is complete. All immutable ledger segments and recovery checkpoints accumulate. Storage retention/compaction and large-history startup profiling remain M9 work; load validates the chain on the worker, and writes do not rescan the full chain.
- No measured live scrolling/frame-time or third-party plugin compatibility result exists. The accepted draw-only M3 tint fallback remains a tint of item bounds, not true grayscale.
- Supported API/source review introduces no new dependency, input automation, packet access, widget/quantity/menu mutation, reflection/native code or external data transmission. [Plugin Hub review](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review) approval is not guaranteed.

## Manual acceptance procedure

Only the user may operate RuneScape. **Keep the existing run; do not reset it for testing.**

1. Close clients and privately back up the whole `from_scratch` plugin data directory, including all old checkpoints. Launch `.\gradlew.bat run` from the repository root. Follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) to log in manually. An existing M2/M3 run should retain the same run/start time and have a migration backup, never a fresh initialization/reset.
2. Confirm **Reconcile Progress** appears. Open inventory, worn equipment and bank manually; each reliably observed step checks off, including empty inventory/equipment. If missing, display that normal tab for about two seconds and reopen bank within 60 seconds. Enter PIN manually. Do not carry/equip an item just to satisfy observation. Review is disabled until all three current-session observations are settled.
3. Open the comparison. Check physical Before/Now and proposed virtual allocations. Cancel first: the run/history/ownership must stay unchanged. Reopen; close the bank or move an item while reviewing, then attempt confirmation: the stale proposal must be rejected. Let observations settle, review fresh and confirm. Old legacy copies should remain zero-owned unless this return genuinely has net new holdings. For legacy items retrieved from excluded storage, correct the proposed gain to zero with a reason before approval.
4. With tracking active and bank closed, legitimately obtain a small ordinary resource or receive an item gift. Wait a few ticks. Expect a recent acquisition and a lifetime unlocked type, without financial values. Bank it manually: purple count equals only the earned quantity and the item is not dimmed. Withdraw/deposit repeatedly, including a full withdrawal: ownership moves between locations, no extra acquisitions/unlocks appear. A missing bank slot is not invented.
5. Pick a legacy-only item: withdraw and redeposit it. Purple remains zero and it stays locked. For an item with both old and newly earned copies, withdraw part of the stack: virtual copies move first. Example 100 virtual, withdraw 40 -> bank purple 60; redeposit -> 100. Split/combine/reorder stacks and close the bank immediately after a withdrawal; later reopening must not credit a new acquisition.
6. Earn a safe wearable item; equip/unequip and deposit from equipment manually. Total virtual copies must stay constant; bank quantity reflects only bank-held copies. Test a supported explicit noted/unnoted conversion and verify no acquisition/unlock duplication. If an emptied equipment API container becomes unavailable, show its normal tab to verify emptiness; saved ownership must not disappear.
7. Obtain and Eat one ordinary food item using its inventory action. Virtual quantity decreases but its lifetime unlock remains. Separately Drop and pick up a low-value legacy item: it must stay legacy. Repeat with a virtual item: previous ownership restores with no extra recent acquisition. Do not risk valuable items. Let each action settle; rapid net-zero actions need not fabricate intermediate history.
8. Exercise a safe ambiguous removal/transformation if desired. Review should be requested, not a silent new unlock. Show all three containers, inspect the proposal, correct physical-bounded virtual allocations with a reason, choose permanent removal only for confirmed destruction, and confirm. For a known virtual copy moved to unsupported storage, Elsewhere may preserve the existing total; return/review must relocate it without doubling ownership or acquisitions. Review/history records must remain auditable.
9. Log out/restart or disable/re-enable: the run, balances, recent history and lifetime unlocks persist. Exactly one new host comparison is required, even when quantities are unchanged. After confirming, uninterrupted unchanged gameplay must not repeatedly prompt. Test a world hop; reliable same-character state should survive ordinary loading. If identity/containers cannot be verified, follow the comparison rather than accepting speculative credits.
10. After logging out of the host, optionally play elsewhere and obtain/consume a known small quantity. Return to the host: review only net changes against the last saved physical checkpoint. Confirmed net gains credit only the net; losses reduce virtual first or accept your explained correction. Cancel leaves prior history intact. It must not invent gross acquisitions/consumption or count gains already recorded before logout.
11. Repeat accepted M3 checks with both zero and positive bank quantities: full/partial top/bottom rows, rapid scrolling, tabs/search/empty results, scaling/resizing, placeholders/fillers, dragging/tooltips/right-click and independent quantity/dim/color settings. Original OSRS quantities/inputs remain normal; no effects outside bank viewport. Closing/disable removes rendering. Re-enable prompts reconciliation, then restores exactly one overlay. Compare large-bank responsiveness with the effect on/off.
12. Where available, switch characters/configuration profiles: never mix characters' runs/history; return to the original run safely. Verify financial fields remain explicitly unimplemented zeros, active legacy totals stay hidden and elapsed duration includes offline time. Report RuneLite version, UI scale, enabled bank plugins, exact failed step and only redacted relevant DEBUG/errors. Do not attach saves or credentials.

New-run guided setup can additionally be tested on a character without a run; do not reset an established run solely for acceptance. Initial approval still requires the full bank, verified personal observations and an explicit unchanged snapshot. All virtual quantities begin zero.

## Acceptance and Git boundary

M4 is ready for **user-run acceptance** following the successful clean build. Functional acceptance and performance remain pending until the user confirms. Git remains on `master` at `5b7c49f`, with 22 modified tracked files and 25 new files; none are staged. No build artifacts or private progression files are tracked. No staging/commit/push/tag/reset/publish or M5 work was performed.
