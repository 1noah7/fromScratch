# Milestone 2 — Initial snapshot and persistence

## Status and scope

M1 passed user-run in-game acceptance and was committed by the user before this work began. The user has now isolated the remaining live failure: populated inventory/equipment work, empty locations fail. **The empty-state adapter correction awaits user retesting; M2 is not accepted in-game.** Existing progression files, schema and persistence implementation are unchanged. No M3 feature, commit, push, tag, publication, PR, or agent interaction with RuneScape was performed.

## Empty inventory/equipment correction

Root cause: no positive-item-count validation was found. SnapshotAssembler and ContainerSnapshot already accept zero-length arrays, all-empty slots, empty maps and zero occupied slots. The adapter instead represented absent INV/WORN API objects as unknown with no way to verify an actually empty location. Previous tests supplied non-null empty lists, so they did not cover the live representation exposed by the user's empty-versus-populated experiment. RuneLite's getter is nullable; this does not mean every null is empty or that all empty locations always return null. No actual live API diagnostic trace was supplied, so the underlying server/container-allocation timing is not claimed as proven.

Source evidence: inspected installed 1.13.3 API/core source plus the official RuneLite cs2-scripts revision `1d418ffb6bce96c2d0712eb2652712d28e01b115` (2026-10-20). inventory_init creates all 28 slot children; inventory_drawitem explicitly sets hidden BLANKOBJECT/quantity 1 for empty slots. wear_initslot creates item child 1 and empty icon child 2; wear_updateslot hides the item and shows the icon when empty. An unequipped item's ID can remain on its hidden child, so ID=-1 alone is insufficient. Ring/ammo must be checked, not player appearance. Pinned source links and exact supported APIs are in [COMPATIBILITY.md](COMPATIBILITY.md).

Exact change: PersonalContainerReader prefers complete API data, including zero-length/all-empty-slot containers. Only a null INV/WORN observation invokes EmptyContainerVerifier. It checks mounted, visible normal inventory or bank-side inventory (all 28 initialized empty children), or normal equipment (all 11 logical slot components with hidden item/visible initialized empty icon). The full layout must verify on two different game ticks; repeating a read/confirmation in one tick cannot advance verification. Missing/default/partial/hidden widgets stay unknown. A populated widget without an API container rejects any cached observation instead of reconstructing populated quantities from widgets. WidgetReadNode is a read-only supported-API adapter.

Candidate verification resets on relevant container events, personal interface load/close, Begin and session/identity cancellation. Verified empty observations then obey the existing 60-second setup cache/change rules. The three-container snapshot architecture, complete current BANK requirement, immutable preview/baseline, explicit confirmation, zero virtual economy, persistence/schema and run/reset protections remain intact. No bank item-widget capture, gameplay action, dependency or external service was added.

Files changed for this correction: FromScratchPlugin.java and RuneLiteSnapshotObserver.java wire the verifier lifecycle; BankSnapshotReader.java documents the unchanged observation contract. Added EmptyContainerVerifier.java, PersonalContainerReader.java, WidgetReadNode.java and PersonalContainerReaderTest.java. Updated README, ARCHITECTURE, ACCOUNTING, TEST_PLAN, CHANGELOG, AGENTS, COMPATIBILITY and this report.

Tests: 29 new regressions, all previous 173 retained (202 total). All four inventory/equipment combinations pass with actual observed arrays and with null empty personal containers independently verified in sequential normal-tab observations. Tests also cover empty slot sentinels, null/no evidence, null bank, two-tick settling, bank-side inventory, partial/default/hidden/unmounted views, missing equipment/icon data, occupied items contradicting an incomplete layout, ring/ammo, stale item IDs after unequipping, candidate invalidation, explicit confirmation and unchanged-codec round-trip. Existing persistence byte-preservation, restart/isolation/corruption/reset cases remain. Final clean-build totals appear in Verification below.

Manual retest (user only):

1. Launch `.\gradlew.bat run` and log in using the linked Jagex-account instructions below. Preserve any Active run; use an uninitialized character and Cancel Setup between preview cases. Do not delete saves or reset progression for this fix.
2. Start with empty inventory and equipment. Click Begin. If a personal location is Waiting, close the bank and manually show its normal tab for about two seconds; show the other tab similarly if needed. Reopen the normal bank within 60 seconds, enter any PIN manually and wait for settling. Expect all three Ready, with zero types/occupied slots/units for both empty personal locations. Carrying/equipping an item must be unnecessary.
3. Cancel without activating and repeat populated inventory/empty equipment, empty inventory/populated equipment, and both populated. Check item quantities and that a ring/ammo item is not mistaken for empty equipment. Bank tab/search totals must still represent the complete bank.
4. With bank closed or PIN/incomplete data, Confirm must stay disabled. No should leave setup uncommitted. Confirm Yes only for the final desired baseline, wait for Active, check zero virtual economy, then restart/toggle and verify the saved baseline is preserved.
5. If blocked, collect only `From Scratch capture:` DEBUG lines after about 12 seconds in each manual stage. The new `emptyVerification[INV=...,WORN=...]` fields show UNKNOWN (no complete view), NON_EMPTY (UI shows an item without container data), SETTLING (first complete empty view) or EMPTY (verified on different ticks). API `liveSizes=-1` can now coexist with a valid independently verified empty observation. No item lists/identity are logged. Unsupported future layouts remain blocked and require diagnosis.

Permanent persistence decision: local-only storage on one designated host computer. Cloud synchronization, cloud storage, cloud credentials and a third-party backend are permanently excluded. Mobile/other-computer gameplay is reconciled when returning to the designated host; this remains M4 work, not part of this fix. Manual backup/restore can deliberately move the host role without merging separate runs.

## Second live failure — multi-phase correction (historical)

Supported cause: BankSnapshotReader.capture previously required BANK, INV and WORN in a single call after all bank gates passed. requireContainer(INV) threw the exact reported Inventory message; no earlier personal observation existed to reuse. The panel inferred bank readiness from the existence of the entire preview, masking the successful bank stage. The adapter returned null for either a missing container or missing item array. Why that read is unavailable on the user's real client remains unproven; the new diagnostics distinguish those cases.

API evidence: inspected the actual cached 1.13.3 API/client source jars. Gameval BANK=95, INV=93 and WORN=94 agree with the deprecated InventoryID enum. Client.getItemContainer(int) and its enum overload are @Nullable. Core BankPlugin reads BANK and tracks BANKMAIN through WidgetLoaded/Closed; ItemStatPlugin independently null-checks INV and ItemStatOverlay independently null-checks WORN. Simultaneous availability and interface-triggered loading are not guaranteed by this contract. Sources are identified in COMPATIBILITY.md; the IDs were not changed or guessed.

Synthetic runtime evidence before edits: IntelliJ paused in requireContainer at line 61 with inventoryId=95 and a non-null bank list, then at the same line with inventoryId=93 and items=null. Stack: missingInventoryRemainsAnActionableFailure -> expectUnavailable -> capture -> requireContainer. A suspending unit-test probe was used after the short-lived test session exited before non-suspending events could be drained. Agent breakpoints were removed and the unit session stopped; the two disabled user breakpoints were preserved. This is evidence of the rejection path, not an inspection of the user's live game.

Implementation:

1. Begin creates a fresh ephemeral observation session. During setup, read available INV/WORN every game tick even when BANK is closed/unavailable. Independently validate, aggregate and freeze each observation. Valid event payloads can supply personal data when a later getter is null.
2. Personal observations last at most 60 seconds in the same uninterrupted verified-character setup session. Null reads never refresh age or imply empty. Invalid reads expire the location. Changes expire the affected observation; a changed previously observed bank (or unknown update to that bank) also invalidates both personal caches. Container updates require a settling tick before combination.
3. Always read the complete BANK container while BANKMAIN is mounted, its normal item layer visible, no PIN UI is mounted, and updates have settled. Never reuse a closed bank or visible widget item lists. First bank loading does not by itself prove an inventory/equipment change.
4. Combine only valid observations into a complete immutable preview. On every setup tick and confirmation recheck readiness/age, identity, available personal contents and the current bank. Changed contents require another review token. Logout, identity change, disable, cancellation and a fresh Begin discard ephemeral observations. Nothing modifies an Active baseline.
5. The UI independently shows Bank, Inventory and Equipment Ready/Waiting with partial summaries. Missing personal data instructs the user to close the bank, manually open the indicated normal interface and reopen within 60 seconds without moving items. All three must be valid before Confirm enables. Setup instructions appear once before initialization and disappear afterward. Equal immutable views do not rebuild Swing every tick.

No persistence class or schema changed. All earlier 147 tests are retained, with wording/read-boundary assertions updated for independent collection. Added 26 regressions: 18 reader cases, 3 controller cases, 3 panel cases and 2 rate-limit cases (173 total). Existing byte-preservation tests and a new real-reader/controller test verify active data stays unchanged when personal containers disappear.

DEBUG evidence for the next live test: launch with the existing `run` task, which already passes `--debug`. During unconfirmed setup, wait at least 12 seconds in each state: bank open; bank closed with normal inventory visible; normal equipment visible; bank reopened. Collect only lines containing `From Scratch capture:` and label the manual stage. Do not send complete logs, credentials or save contents.

```text
From Scratch capture: loggedIn=true initializing=true liveSizes[BANK=...,INV=-1,WORN=...] bankMounted=true bankVisible=true pin=false settling=false predicate=INVENTORY_UNAVAILABLE_OR_STALE bank={liveSize=...,observed=true,ageMs=0} inventory={liveSize=-1,observed=false,ageMs=unknown} equipment={...}
```

This is an illustrative format, not captured live output. `liveSizes`: -1 means no container; -2 means no item array; nonnegative means array length (zero is valid). Per-location `liveSize=-2` instead means rejected/invalid copied data; `notRead` means the bank gate prevented reading. `observed`/`ageMs` identify a previously validated observation, including an expired one. Predicate identifies the precise gate: BANK_NOT_MOUNTED, BANK_ITEMS_HIDDEN, BANK_PIN, BANK_PIN_SETTINGS, SETTLING, BANK/INVENTORY/EQUIPMENT_UNAVAILABLE_OR_STALE, or READY. Logging is at most one line per ten seconds across successes/failures, so an intermediate transition may be omitted. No item IDs/lists/quantities, names, hashes, profile keys or paths are logged.

If INV/WORN remain absent even during normal interface visits, collection stays blocked. The exact live diagnostic sequence above is required to determine the remaining cause; opening an interface is a suggested observation step, not a promise that the API will supply missing data. The implementation is ready for a controlled retest, not a claim of resolved live behavior.

## First bank-detection correction (historical; failed subsequent live acceptance)

Root cause: opening a bank was incorrectly treated as requiring a new quantity-change event, and a failed capture cleared the only retry trigger. The original IDs were correct; the lifecycle assumption was wrong.

1. RuneLiteSnapshotObserver cleared freshBank on closure/session reset and set it only in ItemContainerChanged.
2. WidgetLoaded merely marked setup dirty. Opening an unchanged populated bank need not emit another quantity event, so freshness could remain false despite the visible bank.
3. SetupController cleared dirty before capture and retried only when dirty became true again. Widget-before-data ordering could therefore leave initialization stuck even after containers became available.
4. The sidebar labeled every absent preview Waiting for bank, masking these distinct readiness failures, while two separate sections repeated the capture instructions.

Evidence: installed/resolved RuneLite 1.13.3 source inspection, including its official BankPlugin; a synthetic debugger probe observed bankOpen=true and freshCompleteBank=false at the rejection guard, with three container lists supplied; a new controller regression reproduced one failure when the bank became available without another container-change event. This is unit-runtime evidence, not inspection of the user's game or save. Logpoints/breakpoints were removed after collecting the test stack and event; the temporary test debugger was stopped without changing user breakpoints.

First correction: BankSnapshotReader checked mounted normal-bank/PIN interfaces, visible Bankmain.ITEMS_CONTAINER, a settling game tick and three current non-null containers. RuneLiteSnapshotObserver supplied client-thread getComponentTable/getWidget/getItemContainer reads. WidgetLoaded/Closed woke capture and GameTick retried it. This removed the quantity-change gate, but the simultaneous-container assumption still failed live and has now been replaced above.

First UI correction consolidated setup instructions, but inferred bank readiness from the full preview. The current UI reports all three locations independently, as described above. Active runs show no setup instructions; the existing help key controls optional hover detail.

API check: InterfaceID.BANKMAIN (12), InventoryID.BANK (95), Bankmain.ITEMS_CONTAINER, BANKPIN_KEYPAD (213) and BANKPIN_SETTINGS (14) are present in the installed API. Production uses those gameval constants. The official BankPlugin sets bankOpen via WidgetLoaded/WidgetClosed and uses ItemContainerChanged for quantity-cache invalidation. Sources: [official BankPlugin](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/bank/BankPlugin.java), [container event contract](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/events/ItemContainerChanged.java).

Regression coverage: 26 new tests, with all previous 121 retained (147 total). The full mapping of the 12 requested bug-fix cases is in TEST_PLAN.md. No save deletion/reset is needed to install or test this correction; a regression verifies existing run bytes remain identical across bank lifecycle changes.

## Implementation

- Begin -> Initializing -> complete preview -> explicit confirmation -> background save -> Active.
- Require a logged-in verified character, current complete mounted normal bank with visible item layer/no PIN after settling, and valid personal observations from this setup session. No quantity-change event is required on opening.
- Copy available complete containers on the client thread; combine within the short-lived observation policy above. Aggregate duplicate slots with checked long arithmetic, retain exact variant IDs, exclude empty/zero/placeholder/filler slots and all special storage.
- Keep the confirmed legacy baseline immutable. Physical changes after activation do not recapture or modify it. All virtual items/GP/wealth/unlocks remain zero; no acquisition engine is running.
- Revalidate identity and current contents on confirmation. Expired or changed previews require review again. Cancel/logout/disable before confirmation never saves an incomplete baseline.
- Load the same run across logout, restart, plugin toggles and supported character/config-profile changes. Disabling does not reset or end the run.
- Reset requires a dialog, verifies an archive first, and writes Not Started without creating a replacement run. No real game items or GP are changed.

## Snapshot and threading architecture

RuneLiteSnapshotObserver reads supported APIs only. BankSnapshotReader combines current normal-bank data with short-lived validated personal observations under the policy above. CaptureProgress carries partial results to Swing. Tabs/search widgets never define ownership; missing data is unknown, not empty. The API exposes no independent full-packet receipt or shared container-revision flag, so live ordering still needs retesting. A closed-bank cache or null container cannot manufacture a valid snapshot.

SnapshotAssembler is pure validation/aggregation. ContainerSnapshot and LegacySnapshot defensively copy and expose immutable data. InitializationSession keeps unsaved previews/review tokens separate from committed RunRecord state. Its candidate is not considered active until persistence succeeds.

SetupController confines mutable workflow state to the client thread, runs all storage on a bounded single worker, and returns completions through ClientThread.invokeLater. Lifecycle/context epochs discard callbacks after disable, logout or character change. Shutdown uses shutdownNow without waiting and creates no scheduled task. Swing receives immutable SetupView data; confirmation dialogs initiate local plugin requests only.

M2 has no acquisition/transfer accounting, restrictions, menu changes, overlays, buybacks, GE tracking, price lookups, external services or play-time tracking.

## Persistence and recovery

The live account hash and RuneScapeProfileType must match exactly one selected RuneLite RS profile. SHA-256 of their namespaced versioned tuple supplies a stable opaque storage key. It is independent of display name and random configuration-profile key, and separates standard/temporary profile types. Raw identifiers/inventories are not logged.

Production data lives only under the supported plugin Filepath directory:

```text
.runelite/plugin-data/from_scratch/characters/<opaque-key>/
  current.json
  checkpoint-<revision>.json
  archive-<old-run-id>-<unique-id>.json
  run.lock
```

The injected Gson is used through an explicit JSON-tree codec with serialize-nulls derived from the injected instance. Envelope format 1 and lifecycle header schema 1 are separately validated. Exact integer parsing, strict required fields, identity checks, domain constructor validation and a SHA-256 payload checksum detect incomplete/corrupt/incompatible data. M1 wrote no run files, so no older persisted schema exists to migrate. Future schema/nonzero-economy data is refused intact, not converted into zero state.

All reads/writes, directory creation and plugin-directory lookup occur on the storage worker. A nonblocking per-character Filepath channel lock rejects conflicting clients. Current data is never replaced by a default startup value. Files larger than the supported 4 MiB bound are rejected.

Commit order:

1. Create, force and validate an immutable checkpoint.
2. Create and force a pending current file.
3. Atomically replace current.json where supported; otherwise use a forced copy with the checkpoint retained.
4. Read/validate the published current file before acknowledging success.

Reset first copies, forces and validates a unique archive of the active run, then commits a Not Started record. This prevents an old active backup being silently promoted after reset. Original archives/checkpoints are retained without automatic pruning. Missing current data with recovery evidence blocks setup. Corrupt/incompatible current data is preserved and shown as an error with Retry Load. Filesystem/hardware loss is not fully preventable; forcing and retained copies reduce risk rather than guarantee all power-loss scenarios.

Recovery is manual and requires all clients closed plus a private backup of the entire directory. Preserve the damaged current file, deliberately select a valid same-character checkpoint/archive, copy it to current.json and reload. Archive selection can restore an old active run; a reset checkpoint retains Not Started. Do not edit identity/checksum fields or share saves publicly. See [README recovery instructions](../README.md#data-backup-and-reset). No import/export/restore UI or general transaction journal is implemented yet.

Tests establish Filepath roots through a test-only escape hatch confined to JUnit synthetic temp directories. Production never uses Filepath.Unchecked or raw filesystem paths for run data.

## Changed files

Modified production/test files:

- `src/main/java/com/fromscratch/FromScratchPlugin.java` — event/lifecycle wiring and background directory provider.
- `src/main/java/com/fromscratch/FromScratchConfig.java` — setup-help wording; original config key/default preserved.
- `src/main/java/com/fromscratch/model/RunState.java` — documentation of the persisted record boundary; schema contract unchanged.
- `src/main/java/com/fromscratch/ui/FromScratchSidebar.java` — immutable view updates, action binding, cleanup.
- `src/main/java/com/fromscratch/ui/FromScratchPanel.java` — workflow, dialogs, required scope, summaries and zero metrics.
- `src/test/java/com/fromscratch/ui/FromScratchSidebarTest.java` — retain foundation behavior assertions with updated M2 help/disclosure text.

New production files:

| Package | Files |
| --- | --- |
| controller | SetupController.java |
| model | ContainerSnapshot.java, InitializationSession.java, LegacySnapshot.java, RunRecord.java, SnapshotLocation.java, VirtualEconomy.java |
| observation | BankSnapshotReader.java, CaptureProgress.java, CharacterIdentity.java, DiagnosticRateLimit.java, EmptyContainerVerifier.java, ObservedItem.java, PersonalContainerReader.java, RuneLiteSnapshotObserver.java, SnapshotAssembler.java, SnapshotSource.java, WidgetReadNode.java |
| persistence | RunRecordCodec.java, RunRepository.java, SaveException.java, SaveIntegrity.java |
| ui | SetupActions.java, SetupView.java |

All production packages are under `src/main/java/com/fromscratch/`.

New test files under `src/test/java/com/fromscratch/`:

- SnapshotFixtures.java
- controller/SetupControllerTest.java
- model/InitializationSessionTest.java
- observation/CharacterIdentityTest.java
- observation/BankSnapshotReaderTest.java
- observation/DiagnosticRateLimitTest.java
- observation/PersonalContainerReaderTest.java
- observation/SnapshotAssemblerTest.java
- persistence/RunRecordCodecTest.java
- persistence/RunRepositoryTest.java
- ui/SnapshotPanelTest.java

Updated administration: README.md, ARCHITECTURE.md, ACCOUNTING.md, TEST_PLAN.md, CHANGELOG.md, AGENTS.md, docs/COMPATIBILITY.md, docs/PUBLICATION.md and the subsequent-acceptance note in docs/MILESTONE_1.md. Added this report. Existing Java 11 build configuration, metadata version, Gradle Wrapper, resources and Linux/Windows CI workflow are unchanged; CI already discovers all tests.

## Verification

Requested command:

```powershell
.\gradlew.bat clean build
```

**Final requested clean build passed on Java 11 / RuneLite 1.13.3: 202 passed, 0 failed, 0 errors, 0 skipped.** All 173 previous tests plus 29 empty-container regressions are retained, including all 27 foundation tests. Coverage and correction matrices are mapped in [TEST_PLAN.md](../TEST_PLAN.md). No previous test was removed. Earlier 147/173 passing tests did not establish live correctness.

Coverage includes complete empty/populated snapshots, coins/gear/variants/placeholders/copies/long quantities, duplicate/changed/incomplete setup, immutable baselines, save/reload/restart, character isolation, strict corrupted/incompatible-data handling, protected reset, locking, and simulated interruption after checkpoint/archive writes. Controlled queues test stale callbacks and acknowledgement ordering without sleeps or real accounts.

An intermediate compilation error from moving the publication boundary was corrected; the final clean build has no compilation or test failures. The inherited launcher generic-varargs warning and Gradle 8.10 deprecation summary remain. No new production/test dependency was added. Hosted CI has not run in this session. M1's historical 1.13.2 compatibility result is not an M2 result.

## Manual acceptance

The agent can offer to launch the development client, but must never control the game. From the repository root:

```powershell
.\gradlew.bat run
```

Follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) to log in. For this correction:

1. Preserve any existing Active run; verify its start time/baseline survive restart/toggle and that no setup instructions appear. Use a character without an active run for initialization cases, or repeat an unconfirmed setup with Cancel Setup.
2. With bank closed, click Begin: expect Bank: Waiting and disabled confirmation. Inventory/Equipment may already be Ready. Open the normal bank manually and enter any PIN yourself. After settling expect Bank: Ready. If Inventory/Equipment remain Waiting, close the bank, manually visit normal inventory and equipment tabs, and reopen within 60 seconds without moving items. All three must be Ready with summaries before confirmation enables. Do not reset/delete any existing run to test this fix.
3. Cancel Setup while leaving the bank open, then Begin again. It must retry without requiring a quantity-change event; missing personal data may require the manual interface step. Close/reopen the unchanged bank: confirmation must disable on closure and recover when all observations are valid. PIN/settings or delayed/missing/stale data must never produce a false complete snapshot.
4. Change tabs/search, including no-match searches: summaries must still cover the whole bank. Check empty-bank behavior if such a character is available; null/incomplete data must show a diagnostic instead of a false empty snapshot.
5. If a personal getter is unavailable and relying on cached data, leave the bank open over 60 seconds: that location must return to Waiting and disable confirmation; fresh available getters may keep it Ready. Manually change a stack during unconfirmed setup and ensure the preview refreshes or requests recapture. Choose No in the dialog first; no run should activate. Confirm Yes only when satisfied. Wait for Active, verify zero economy and unchanged real items, then relog/restart and verify preservation. Active must hide setup instructions. If still blocked, gather the diagnostic sequence above.

The extended M2 procedure remains in [TEST_PLAN.md](../TEST_PLAN.md#m2-manual-acceptance--user-only). No save deletion, migration, or reset is required for this fix.

Wait for Active/reset confirmation before closing or disabling. Only the user can validate real bank event timing, profile availability and dialogs. M2 is **ready for acceptance testing**, not confirmed correct in-game. Stop here; M3 requires authorization.

## Remaining limitations and publication work

- No activity after initialization is accounted for. The baseline remains historical, not a current physical/virtual bank browser.
- Special storage is excluded, and raw variants are not canonically merged.
- The mounted normal bank and complete current bank data are mandatory, with valid personal observations no more than 60 seconds old from this setup session. Persistent missing data remains blocked with diagnostics; normal interface visits cannot guarantee container availability. The API has no independent full-packet receipt or shared revision flag.
- Storage is permanently local-only on one designated host; no cloud synchronization, cloud credentials or backend is planned. Gameplay elsewhere is reconciled on return to that host in future M4. Backups and explicit manual host migration remain local procedures.
- Recovery/retention controls, import/export, the transaction journal and future migrations remain later work. Conflicting clients surface a retryable lock error rather than merge writes.
- Play time is unimplemented; active panels disclose this.
- Manual M2 acceptance, hosted CI, screenshots, full later milestones, final recovery/performance/compliance audits and current Hub revalidation remain publication gates. No Hub acceptance is implied.

## Git state

Branch master, based on the user-created M1 commit. The combined uncommitted M2 work remains for review; this fix incrementally extends it without restarting the milestone. No run data, generated classes, caches or temporary compatibility scripts are included. No commit, push, tag, release or submission was performed. Final Git status is reported separately.
