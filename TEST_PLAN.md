# Test plan

## Milestone 2 evidence

The requested `.\gradlew.bat clean build` passed on Java 11 / RuneLite 1.13.3: **202 passed, 0 failed, 0 errors, 0 skipped**, preserving all previous 173 and all 27 foundation tests. The user isolated the remaining live failure to empty INV/WORN. The new 29-test suite exercises real reader/verification/preview composition with synthetic containers and complete client-script-shaped widget layouts. Live acceptance and hosted CI remain pending.

| Suite | Tests | Evidence |
| --- | ---: | --- |
| FoundationResourcesTest | 5 | Existing metadata/assets/bytecode/config coverage |
| RunStateTest | 16 | Existing validated lifecycle header coverage |
| FromScratchSidebarTest | 6 | Existing registration/cleanup/metrics/EDT/help coverage |
| SnapshotAssemblerTest | 20 | Empty/populated bank, inventory/equipment/coins, placeholders, copies, variants, checked large quantities, incomplete data, immutability |
| InitializationSessionTest | 14 | Explicit empty-bank activation, preview/confirmation/cancel, duplicate starts, changed contents, empty virtual economy and character guards |
| CharacterIdentityTest | 8 | Login/hash/type/selected-profile verification, ambiguity, renames/config-key stability and isolated namespaces |
| RunRecordCodecTest | 12 | Round-trip, reset record, checksum/version/field validation, exact integer parsing, overflow and nonzero future-data rejection |
| RunRepositoryTest | 17 | Save/reload/restart, character isolation, duplicate/idempotent starts, corrupt/missing current protection, archival reset, locks, interrupted publication |
| SetupControllerTest | 25 | Retry/open/reopen, multi-phase confirmation, missing personal data recovered after interface visits, unchanged active save bytes |
| SnapshotPanelTest | 12 | Single instruction section, independent partial readiness/summaries, confirmation gating and instruction-free active state |
| BankSnapshotReaderTest | 36 | All previous bank gates plus independent nullable observations, event payloads, stale/invalid data, change invalidation, immutable caches and diagnostic fields |
| DiagnosticRateLimitTest | 2 | One diagnostic per ten seconds across tick/state changes; arbitrary monotonic origin |
| PersonalContainerReaderTest | 29 | All four empty/populated combinations with API data and verified null empties; full layouts, default/hidden/partial/missing rejection, occupied contradictions, ring/ammo, tick settling, invalidation, mandatory bank/confirmation and codec round-trip |

Tests use synthetic data and controlled dispatch/executor queues. Storage tests use Filepath under JUnit temporary roots, never real RuneLite profiles. No test sleeps, starts a game, requires credentials, or performs game actions. Existing Linux/Windows Java 11 CI automatically runs the expanded suite; no workflow change is needed.

### Empty-container regression matrix

| Inventory | Equipment | Observed API path | Independently verified null-empty path |
| --- | --- | --- | --- |
| Empty | Empty | observedEmptyInventoryAndEmptyEquipmentAreAccepted | nullEmptyInventoryAndNullEmptyEquipmentAreIndependentlyVerified |
| Populated | Empty | observedPopulatedInventoryAndEmptyEquipmentAreAccepted | populatedInventoryAndVerifiedNullEmptyEquipmentAreAccepted |
| Empty | Populated | observedEmptyInventoryAndPopulatedEquipmentAreAccepted | verifiedNullEmptyInventoryAndPopulatedEquipmentAreAccepted |
| Populated | Populated | observedPopulatedInventoryAndPopulatedEquipmentAreAccepted | populatedContainersRequireNoEmptyInterfaceVerification |

Empty sentinel arrays also count as valid. Null without a complete initialized mounted/visible view remains unknown. Missing slot 27/ammo parent/item/icon, default inventory ID=-1, hidden or unmounted roots, hidden/uninitialized empty icons, populated UI without a container, same-tick repeat reads, and missing BANK cannot manufacture readiness. Equipment validation includes ring/ammo, not just visible player appearance. Existing empty observations are invalidated before re-verification after a real populated read. A verified empty preview still requires explicit confirmation and round-trips through the unchanged save codec.

Manual retest: use Cancel Setup between the four combinations so no saved run must be reset. Test with both locations empty first. Begin, manually visit normal inventory and equipment tabs for about two seconds each if Waiting, then open the bank within 60 seconds. Empty locations must show zero types/slots/units and all three must become Ready. Test populated/empty, empty/populated and populated/populated, including a ring/ammo item if available. With bank closed or PIN/incomplete data, confirmation must remain disabled. Confirm only one desired final baseline, verify zero virtual economy and restart preservation. Preserve any existing active run; no data deletion is required. If blocked, send only throttled From Scratch capture lines including emptyVerification values (UNKNOWN, NON_EMPTY, SETTLING, EMPTY).

### Second live failure: personal-container regression evidence (historical)

Before edits, a synthetic debugger test stopped in BankSnapshotReader.requireContainer: BANK=95 returned a non-null list, then INV=93 returned null. The stack was capture -> requireContainer, matching the user's exact rejection message. The unit debugger was stopped and agent breakpoints removed; user breakpoints were left unchanged. This proves the rejection path, not why INV is null on the user's client.

| Requested case | New regression coverage |
| --- | --- |
| Bank available, inventory null | bankReadyAndNullInventoryReportsIndependentAvailability |
| Bank available, equipment null | bankReadyAndNullEquipmentReportsIndependentAvailability |
| Personal observations before bank | personalObservationsBeforeBankRemainUsableWhenGettersBecomeNull |
| Bank arrives after personal data | lateBankContainerCombinesIndependentPersonalObservations |
| Empty valid inventory | emptyValidInventoryIsReadyAndCanBeRetained |
| Stale data | nullInventoryNeverRefreshesTheAgeOfAnEarlierValidObservation; equipmentObservationExpiresWhileAReadyPreviewIsAwaitingConfirmation; refreshingAnExpiredContainerRestoresReadiness |
| Changes during setup | unknownInventoryChangeInvalidatesCachedContents; eventPayloadRemainsUsableIfTheGetterIsUnavailable; bankChangesInvalidateUnavailablePersonalObservations; bankChangeEventInvalidatesPersonalCacheBeforeConfirmation; unknownBankChangeInvalidatesPreviouslyCachedPersonalData |
| Confirmation | Controller multiPhasePreviewConfirmsOnlyAfterBankArrivesAndPersistsZeroEconomy; panel completePreviewShowsAllLocationsReady/stalePartialProgressCannotEnableACompletePreview |
| Persistence unchanged | Controller activeSaveBytesAreUnchangedWhenPersonalContainersDisappear; all previous persistence/isolation/restart cases |

Additional coverage validates malformed cache rejection, reset/session cache clearing, immutable source copies, backwards time, partial UI summaries and throttled privacy-safe diagnostics. No production persistence/schema changes.

### First bank-detection regression evidence (historical)

Before changing production behavior, openingBankAfterBeginRetriesWithoutAnotherContainerChange failed against the old controller (one test, one expected failure). A temporary debugger probe in the existing assembler test observed bankOpen=true and freshCompleteBank=false at the rejection guard. This is synthetic runtime evidence of the event gate, not an observation of the user's account. Temporary probes were removed and the test session stopped; user breakpoints were preserved.

| Requested regression | Coverage |
| --- | --- |
| 1 Begin before bank open | Reader beginBeforeBankOpensDoesNotReadCachedBankData; controller openingBankAfterBeginRetriesWithoutAnotherContainerChange |
| 2 Begin with bank open | Reader alreadyOpenBankWorksWithoutWidgetLoadedOrContainerChangedEvents; controller beginningWhileBankAlreadyOpenNeedsNoFurtherContainerEvent |
| 3 Widget before data | Reader openingWidgetWaitsForATickAndDelayedContainerData; controller delayedContainerArrivalRetriesWithoutAnAdditionalEvent |
| 4 Populated bank | Reader populatedBankReadsTheCompleteBankInventoryAndEquipmentContainers |
| 5 Empty bank | Reader genuinelyEmptyNonNullBankIsAccepted |
| 6 Null rejection | Reader nullBankContainerIsNeverCountedAsAnEmptyBank |
| 7 PIN/incomplete interface | Reader pinInterfaceRejectsEvenAnApparentlyPopulatedBank and incompleteBankInterfaceRejectsEvenWhenContainerExists |
| 8 Tabs/search | Reader searchingOrChangingTabsNeverRestrictsTheCompleteContainer; existing unchanged-content review-token test |
| 9 Closure before confirmation | Reader bankClosureRejectsAFormerlyReadyCapture; existing controller closingBankInvalidatesPreviouslyAvailableConfirmation |
| 10 Confirm enable/disable | Updated panel initializationConfirmationIsDisabledUntilCompletePreviewExists exercises unavailable -> Ready -> closed |
| 11 No duplicate instructions | Three new panel tests verify one before setup, capture-only setup, and none after activation |
| 12 Existing data unchanged | Controller existingSavedRunBytesStayUnchangedAcrossBankLifecycle plus all prior repository/reload/isolation tests |

### M2 required scenario mapping

| Requested scenario | Automated coverage |
| --- | --- |
| 1 Empty bank initialization | SnapshotAssemblerTest.emptyBankIsACompleteValidSnapshot; InitializationSessionTest.completeEmptyBankCanBeExplicitlyActivatedWithZeroVirtualWealth |
| 2 Populated bank | SnapshotAssemblerTest.populatedBankPreservesEveryTypeAndQuantity |
| 3 Inventory | SnapshotAssemblerTest.inventoryItemsRemainInInventoryBaseline |
| 4 Equipment | SnapshotAssemblerTest.equippedItemsRemainInEquipmentBaseline |
| 5 GP zero | InitializationSessionTest.initialVirtualEconomyIsCompletelyEmptyDespiteLegacyCoins |
| 6 Placeholders | SnapshotAssemblerTest.placeholdersAndEmptySlotsAreNotOwnedItems |
| 7 Large stacks | SnapshotAssemblerTest.largeStacksAggregateBeyondSignedIntegerRange; codec long-range round-trip |
| 8 Multiple copies | SnapshotAssemblerTest.multipleUnstackableCopiesAggregateWithoutLosingSlotCount |
| 9 Distinct variants | SnapshotAssemblerTest.distinctNotedVariantsKeepTheirExactIds |
| 10 Bank not open | SnapshotAssemblerTest.initializationWithoutAnOpenBankIsRejected; controller no-bank test |
| 11 Incomplete bank | SnapshotAssemblerTest stale/absent-bank/null-slot tests; codec missing-container test |
| 12 Duplicate initialization | InitializationSessionTest duplicate tests; repository active overwrite/idempotence tests |
| 13 Immutable baseline | SnapshotAssemblerTest defensive-copy and unmodifiable maps; controller active non-recapture |
| 14 Save/reload | RunRepositoryTest.saveAndReloadPreserveAllBaselineAndIdentityFields |
| 15 Logout/restart | SetupControllerTest.logoutAndLoginRestoreTheSameRun; new repository restart test |
| 16 Character isolation | CharacterIdentityTest, repository namespace/mismatched-save tests, controller stale/switch tests |
| 17 Corrupt data | Codec validation, repository preservation, controller error/no-empty-replacement tests |
| 18 Backup/reset | Repository archive verification/idempotence/interrupted reset and controller reset tests |
| 19 Disable/reenable | SetupControllerTest.pluginDisableAndReenablePreserveTheCommittedRun; stale post-save callback test |
| 20 Zero initial wealth | Empty VirtualEconomy model, session assertions and active panel zero metric assertions |

### M2 manual acceptance — user only

Run `.\gradlew.bat run` and follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Record RuneLite version, OS, UI scale, expected/actual counts and redacted errors. Agents must not control RuneScape. Test on a character/run you are comfortable resetting; reset changes only virtual plugin data and first archives it.

1. Open the sidebar while logged out. Confirm Status: Not Started, disabled Begin, setup scope and special-storage exclusions.
2. Log in and wait for loading. Click Begin with the normal bank closed: Status: Initializing, bank waiting, confirmation disabled. Cancel Setup; confirm no active run was created.
3. Begin again and manually open the normal bank, entering any PIN yourself. Allow game ticks for settling. Verify Bank: Ready, Inventory: Ready and Equipment: Ready with enabled confirmation and all summaries. If a personal location is Waiting, close the bank, manually visit the indicated normal inventory/equipment interface and reopen within 60 seconds without moving items. A non-null complete empty container is valid; null is not. Cancel with bank open and Begin again; it must retry without a new quantity-change event, though unavailable personal data may require the manual interface step.
4. Change tabs/search, including no-match searches: totals must still represent the full bank. Close the bank: confirmation must become unavailable. Reopen without changing any item quantity: capture must retry and become Ready. Choose No in the confirmation dialog, change a stack if desired and verify the updated summary before confirming again.
5. Confirm Yes with bank open; wait for Active. Verify start date/time, all three legacy summaries, zero spendable GP/net worth/gained/unlocked values, and unchanged real items/coins. Later manually moving items must not change the saved baseline.
6. Log out/in, disable/reenable (including rapid toggles), and restart RuneLite. Verify the same active run/start time/baseline, with no duplicate navigation or accidental reset. No later earnings are expected in M2.
7. Switch to another character where available: it must load its own run or Not Started, never the first character's summary. Switching RuneLite configuration profiles or renaming a character must not create a new economic identity. Return to the original character and verify preservation.
8. Reset -> No must leave the run active. Reset -> Yes must wait for archival success, return Not Started and create no replacement run. Real items must remain untouched. Begin again and confirm a fresh snapshot with a new start time; the old archive must remain.
9. Check UI scale/scrolling and disabled buttons while saving/loading. Before initialization there must be exactly one short Setup instruction. During initialization show capture status/diagnostics/controls without that instruction; Active must hide all setup instructions. Detailed setup tooltip changes hover detail only. Storage errors must offer Retry Load, not Begin/Reset. No save deletion or reset is needed to test this correction; existing active runs should remain intact. Corrupt/incompatible cases already have automated coverage.

Report results before M2 is accepted. A build or clean JVM launch does not prove in-game behavior. Do not begin M3 without explicit authorization.

## Foundation evidence

Historical M1 clean build on Java 11 / RuneLite 1.13.3 passed: **27 JUnit tests, no failures/errors/skips**. A separate M1 compatibility clean build against retrieved Hub target 1.13.2 also passed all 27. M1 has since passed user-run acceptance; hosted CI remains unverified. The launcher main method is not a test.

| Suite | Tests | Coverage |
| --- | ---: | --- |
| RunStateTest | 16 | Empty/versioned header, all states, valid restore, character identity, invalid schema/status/identity/times |
| FromScratchSidebarTest | 6 | Single registration, cleanup, reenable, help/disclosure, zero metrics, EDT guard |
| FoundationResourcesTest | 5 | Metadata, PNG format/dimensions/alpha, Java 11 bytecode, config default |

Headless Swing tests exercise the real panel and fake toolbar boundary without starting RuneScape. No game account or new test dependency is required.

Run `.\gradlew.bat clean build` or `bash ./gradlew clean build`. HTML results: `build/reports/tests/test/index.html`; XML: `build/test-results/test/`. CI uses Temurin 11 on Linux/Windows and the Wrapper; hosted execution is pending.

## M1 manual acceptance — user only

Launch `.\gradlew.bat run` and follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Agents must not interact with RuneScape. Record OS, RuneLite version, UI scale, result, and redacted errors.

| Case | Steps | Expected |
| --- | --- | --- |
| Identity | Find plugin in development client | Correct name/description, no template greeting |
| Logged-out preview | Enable; click coin button before login | One navigation button, title, preview warning, six zero metrics, recent-acquisition message |
| Logged-in preview | Log in manually | Same preview; no run begins or game items/GP change |
| Cleanup | Disable while open | Navigation/panel removed without error |
| Repeated toggles | Enable/disable repeatedly, including rapid toggles | No duplicate/stale buttons; working panel when enabled |
| Help setting | Toggle Show foundation help | Explanation hides/shows; placeholder warning remains |
| Resize/scale | Resize client and change UI scale | Readable labels/amounts, vertical scrolling, recognizable icon |
| Restart | Close and relaunch | Clean preview; no claim of restored progression |

These M1 cases passed user-run acceptance. M2's current procedure is above. User screenshots may be added after removing account details; automated agents do not capture or interact with gameplay.

## Future test matrix — not implemented yet

Test accounting independently of RuneLite. Use synthetic/recorded event batches for adapters and user-run acceptance for rendering/game behavior.

| Area | Required scenarios |
| --- | --- |
| Initialization M2 | Automated coverage implemented above; user-run acceptance pending |
| Ownership M3/M4 | New item and existing legacy type, multiple copies, deposit/withdraw, split/combine, equip/unequip, fully withdrawn bank stack retained elsewhere |
| Bank UI M3 | Tabs/search, placeholders, clipping/scroll, purple bottom-right count, normal count retained, gray zero quantity, scaling, bank close/plugin disable |
| Transfers M4 | Coalesced event orderings, duplicate batches, legacy transformations, partial visibility, unknown storage, uncertainty, no new wealth from movement |
| Offline M4 | Net increase/decrease, 100->130 sharks, existing virtual quantities, incomplete bank, preserve history, audit corrections, no fabricated gross history |
| Currency M5 | Earn/spend/reserve/refund, exact bounds/overflow rejection, insufficient funds, death debt, ordinary debt rejection, repayment, physical legacy cash never recredits spent virtual GP |
| Valuation M5 | Acquisition, pooled basis/rounding conservation, sales above/below basis, price fluctuation, consumption, untradeable/unpriced, cash/items once, buyback effects |
| GE M6 | Buy/sell, partial fills, committed goods/cash, completed/uncollected positions, tax gross/net once, cancellation/refund, collection deduplication, actual vs offer price, uncertainty |
| Buybacks M7 | Fire Cape, natural duplicate with old copy locked, multiple copies, Infernal price, Ancient Sceptre flat price, full catalog, Graceful complete/partial/natural pieces, Elite Void total, Barrows Gloves/Royal Seed Pod/tradeable exclusions, insufficient/negative GP, atomic crash recovery |
| Persistence M2/M9 | Logout/login, restart, characters/profiles, incomplete initialization, corruption/truncation, schema migration, backup/restore/reset, interrupted append/checkpoint/rename, filesystem fallback, idempotent replay |
| UI M8 | Examine location/quantity/GE/HA, zero/N/A, original message retained, duplicate prevention, outgoing chat untouched, totals, large numbers, timers/mobile disclosure, notification deduplication |

## Release gates

Every milestone: review changed APIs/dependencies for compliance, run meaningful tests and build, then ask the user to accept changed behavior in-game. Keep ACCOUNTING.md's invariant mapping current. Add tests with their implementation; do not assert nonexistent features.

M9 adds migration/recovery fixtures, full regression, bounded-work profiling, and user-run bank/UI/GE acceptance. M10 rechecks current Hub requirements, metadata, art, licensing, privacy, release notes, and approved source commit after all acceptance gates pass.
