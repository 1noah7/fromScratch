> Historical, superseded by Milestone 4 bank-only tracking. This is not the current product contract.

# Test plan

## M4 live-failure correction — current acceptance gate

The first 376 passing cases did not reproduce the user's live acquisition/UI failure. They bypassed the collector in their controller acquisition test. The new LiveItemPipelineTest first failed before production changes; debugger evidence confirmed banking context sent its observed acquisition to review. It now uses the actual collector, controller, local repository and VirtualBankOverlay pass, including wooden shield ID 1171. This is synthetic runtime evidence, not a trace from the user's account.

All 376 prior cases remain; assertions change only for the deliberately requested signed legacy display, automatic safe baseline and new dashboard/editor. Four added suites contain 41 cases (417 total, 28 suites). The final `.\gradlew.bat clean build` passed: 417 passed, 0 failed/errors/skipped.

| Suite | Cases | Evidence |
| --- | ---: | --- |
| LiveItemPipelineTest | 11 | Pickup/inventory/bank, pre-quiet-tick deposit, legacy deposit, ambiguous source, named and unchanged-shield correction through actual renderer, -1/-2/0, mixed stack, absent slot/restart, cancel/stale token and ground pickup hint |
| LegacyBankDeficitsTest | 8 | Nonnegative ownership, separate D, earned versus legacy repayment, no lifetime change, automatic transfer-only session baseline, notes, signed extremes and overflow rejection |
| LegacyDeficitPersistenceTest | 7 | Restart/isolation, audited resolution, delta validation, exact verified format-3 migration/history, corrupt chain preservation, interrupted retry, required format-4 fields |
| UpdateProgressEditorTest | 15 | 1,000 unchanged plus three changes, filters/name search/numeric sorting/selected-row bulk mapping, immutable proposals, location-correct earning, one-copy missed-acquisition correction, returned deficit once, vanished legacy types, no changes, secondary tools/collapsed history, absent deficit display, default-off single-item trace, preserved Elsewhere, invalid bulk atomicity |

Run `.\gradlew.bat clean build`. Final totals and warnings are recorded in [the correction report](docs/MILESTONE_4_CORRECTIONS.md). Follow its exact user-only retest before acceptance. Retain all accepted M3 pixel/clipping/lifecycle tests; no agent may interact with RuneScape. Manual testing must cover same-run migration, actual acquisition provenance, -1/-2/0, mixed earned/legacy returns, meaningful-only startup review, editor bulk/cancel/stale behavior and real performance. Diagnostics are opt-in, per-stage throttled DEBUG for one item only. No account credentials, real snapshots or production grant command are used by tests.

## Milestone 4 evidence — manual acceptance pending

Historical first-implementation evidence follows. Its unconditional approval workflow and format 3 are superseded by the correction gate above, and the original 376-test build did not establish live acceptance.

All **285** M1–M3 tests are retained. Two existing suites update assertions for the intentional guided checklist, elapsed timer and current envelope version. M4 adds **91** cases, for **376 tests** across 24 suites. Final clean-build results are recorded in [MILESTONE_4.md](docs/MILESTONE_4.md). Tests use pure fixtures, fake client/container boundaries, EDT Swing and temporary Filepath roots; no OSRS account, game input, private inventory or production grant command is used.

| New suite | Tests | Evidence |
| --- | ---: | --- |
| ItemAccountingEngineTest | 24 | Legacy withdrawal/redeposit, repeated item type, mixed stacks, all ordinary location transfers, eating, legacy/virtual drop return, unknown loss, offline net gains/losses, transformation review, notes, duplicate/order/session guards, immutable baseline, long quantities, ledger deltas, banking/source guards, coins alongside loot and Elsewhere preservation/return |
| ItemObservationCollectorTest | 23 | Observed empty/populated/unknown, reliable caches, personal events/expiry, quiet tick, missing bank/PIN, sorting/search aggregation, session reset, fast bank closure, unsupported-context and same-tick acknowledgment guards, confirmed-empty handoff |
| ItemTrackingSessionTest | 10 | Restored-session approval, unchanged baseline, fresh activation, changed/closed/missing review invalidation, correction bounds and reason, stale token/session, no repeated unchanged prompting |
| ItemJournalPersistenceTest | 17 | Save/reload, full separate history/bounded checkpoint, idempotence/CAS, orphan publication failure, missing/corrupt chain, characters, return identity, reset/archive, exact-byte format-2 migration, format-3 requirements, delta validation, rejected/partial gains, cursor validation and Elsewhere return restart |
| ItemTrackingControllerTest | 9 | Load gate, explicit reconciliation, durable publication, stale character callbacks, disable/re-enable, changed/closed previews, failed save preserves trusted state and requires Retry Load |
| ItemTrackingPanelTest | 8 | Guided empty steps, reconciliation buttons, bounded correction table, history/metrics, duration/clock rollback, saving guard and no duplicated technical setup labels |

### M4 requested coverage mapping

| Requested behavior | Automated evidence / user-only boundary |
| --- | --- |
| 1–3 Empty/populated inventory/equipment and checklist | Retained PersonalContainerReaderTest, new collector and ItemTrackingPanelTest; real widget availability remains manual |
| 4–5 Legacy withdrawal/redeposit | ItemAccountingEngineTest.legacyWithdrawalAndRedepositNeverAcquireOrUnlock |
| 6–8 Acquisition/existing legacy type/mixed stack | legitimateNewItemAndExistingLegacyTypeAreCredited; mixedStackWithdrawalsUseVirtualFirstAndConserveTotals |
| 9–11 Transfers | Mixed stack, baseline-transfer, equipment/direct-bank and unequip conservation cases; live event ordering manual |
| 12–13 Stackable/unstackable | checkedLongStackAndUnstackableCopiesAreNotLimitedToInts; retained quantity/split aggregation cases |
| 14 Consumption | confirmedEatingReducesVirtualFirstAndRetainsLifetimeUnlock; unexplained resource decreases route to review |
| 15–16 Drop return | legacyDropAndPickupDoNotCreateOwnership; virtualDropAndPickupRestoreCopiesWithoutDuplicateAcquisition; persistence return identity |
| 17–19 Ordering/absence/rearrangement | Collector quiet-tick, event serial acknowledgment, cache expiry, fast closure, missing data and aggregate ordering cases; reducer duplicate/out-of-order rejection |
| 20 Restart | Controller lifecycle, ItemTrackingSession load gate and completeItemStateAndHistorySurviveRestart |
| 21–23 Offline and review | Net-only gain/loss cases; changed/session token guards; rejected/partial proposed gains; no confirmation means no save |
| 24 Isolation | Existing identity/repository cases plus journal namespace/stale-character callback cases |
| 25 Migration | Existing frozen format-1 fixtures plus formatTwoMigrationKeepsNonzeroOwnershipAndExactBackup; no real save touched |
| 26 Ledger | Separate complete history, checked deltas, duplicate commit, missing/corrupt chain, cursor and orphan cases |
| 27–28 Bank quantities/visual regression | Existing 53 painter/projection/overlay-pass tests retained without renderer changes; real positive M4 quantities, interactions and performance require manual acceptance |

### M4 manual acceptance — user only

Use the detailed sequence in [docs/MILESTONE_4.md](docs/MILESTONE_4.md#manual-acceptance-procedure). Preserve the existing run; **do not reset it** to test M4. Close RuneLite and privately back up the entire plugin directory before the first development launch. Do not attach private saves to bug reports. Launch `.\gradlew.bat run` and follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Agents must never operate RuneScape.

Verify login comparison and cancellation, observed empty tabs, ordinary acquisition, mixed/legacy transfers, equipment/notes, Eat/Drop return, ambiguous loss/crafting/source correction, durable history and host-offline net comparison. Repeat M3 scrolling/search/tabs/partial edges/resizing/configuration/disable checks using positive and zero counts. Financial values must remain explicitly unimplemented zeros. Net snapshots cannot prove gross offline history or every source. Stop after M4 and report live results before acceptance; a build or clean JVM launch is insufficient.

## Milestone 3 evidence (historical; subsequently accepted)

M1/M2 passed user acceptance; the clean M3 starting commit was `9e6b757`. The user subsequently accepted M3, including its final appearance, and committed it as `5b7c49f`. Its initial visual refinement had 268 passing tests but did not cover the upstream callback-list omission of a top-clipped row. The final viewport/darker-shading correction adds 16 actual overlay-pass cases and a background-contrast regression. All 285 M3 tests remain, with the accepted 48% opacity. Hosted CI and formal profiling remain unverified. Historical evidence follows; M4's current behavior and procedure above supersede statements about fixture-only positive balances.

| New suite | Tests | Evidence |
| --- | ---: | --- |
| VirtualOwnershipTest | 14 | Zero/sparse ownership, exact variants, stack/unstackable copies, independent locations/totals, negatives/overflow/over-allocation, immutable inputs and unchanged legacy baseline |
| OwnershipPersistenceTest | 16 | Nonzero/maximum-long round-trip, isolation, frozen independent format-1 fixtures, active/reset migration, exact backups, preserved old checkpoints, repeated restart, interrupted migration, unsupported/corrupt/incomplete data preserved |
| BankItemPainterTest | 29 | Compact long labels, bottom-right coordinates, moved bounds, readable quantity glyphs, uniform full-height 48% tint, subdued bank background, tall/colored items, unfaded purple pixels, unchanged positive/disabled pixels, clipping/offscreen, graphics restoration, integer/fractional scale, component/empty/filler/BLANKOBJECT/placeholder handling |
| BankOverlayStateTest | 8 | Bank rather than total labels, inactive/error/logout/disabled guards, defaults/live configuration, one registration, bank-only hook, wrong-character/closed-bank gates |
| VirtualBankOverlayTest | 16 | Real overlay-pass pixels with synthetic widget observations: first/full/middle/partial top and bottom rows, precise viewport/control exclusion, unshifted counters, resize, repeated scrolling, tabs/search/empty results, closure, disable/re-enable, positive balances, missing viewport, nonrectangular clip restoration and cached/offscreen definition reads |

### M3 requested coverage mapping

| Requirement | Automated evidence / manual boundary |
| --- | --- |
| 1 Zero initial ownership | VirtualOwnershipTest.initializationLeavesAllLegacyTypesVirtuallyZero |
| 2 Stackable records | stacksUseLongQuantitiesWithoutPhysicalBounds; maximumLongVirtualStackSurvivesSaveAndReload |
| 3 Unstackable records | unstackableCopiesRemainAggregateQuantities |
| 4 Distinct IDs | distinctRawVariantIdsAreNeverMerged (normal/noted whip) |
| 5 Positive fixtures | fixture-backed projection, round-trip and positive-paint tests; no production grant path |
| 6 No negative quantities | negative total/allocation, overflow and codec rejection tests |
| 7 Legacy unchanged | ownershipUpdatesNeverModifyLegacySnapshotOrOriginalRun; migration baseline equality |
| 8 Physical independence | legacy quantity plus independently replaced ownership, maximum virtual stack independent of physical stack |
| 9 Bank vs total | bankAllocationIsIndependentOfTotalAndOtherLocations; projectionUsesBankQuantityInsteadOfTotal |
| 10 Formatting | compactLabelsCoverThousandsMillionsAndBillions; exact zero and Long.MAX_VALUE |
| 11 Bottom-right | textIsRightAlignedToCurrentItemBounds; custom color pixels in lower-right region |
| 12 Original labels | ordinaryQuantityGlyphsRemainReadableThroughTint; neutralTintMutesItemsAndPreservesQuantityContrast; real client labels need manual verification |
| 13 Bank only | onlyNormalBankItemComponentsAreEligible; onlyTheNormalBankLayerIsRegistered |
| 14 Closed bank | closedBankAndWrongCharacterNeverReachRendering |
| 15 Disabled plugin | disabledPluginRejectsLateCallbacksAndClearsOwnership; gated render |
| 16 Tabs/search | ID-based projection and moved-bounds tests plus prior full-container search tests; actual tabs/empty search/locked tabs are manual |
| 17 Scroll/clipping | VirtualBankOverlayTest partial-origin/viewport/scroll/resize cases; parentViewportClipIsPreserved; completelyOffscreenItemPaintsNothing; graphics-transform scale test; real widget lifecycle is manual |
| 18 Placeholders | placeholdersDisplayZeroEvenWithVirtualCopiesElsewhere; eligible zero-quantity slot |
| 19 Empty slots | emptySlotsAndFillersAreExcludedButPlaceholdersAreEligible |
| 20 Lock/unlock appearance | zeroOwnedSpriteAreaIsDimmed; positiveBankOwnershipLeavesSpriteAreaNormal |
| 21 Single registration | repeatedEnableDisableHasExactlyOneRegistration |
| 22 Virtual save/reload | nonzeroVirtualAssetsRoundTripThroughRestart; maximum-long round-trip |
| 23 Legacy persistence | activeMilestoneTwoSaveMigratesWithoutChangingRunOrLegacyData; prior repository/codec tests |
| 24 Isolation | virtualStateCannotCrossCharacterNamespaces; previous identity and stale-callback tests |
| 25 M2 migration | active/reset frozen format-1 fixtures, forced exact-byte backup assertion, restart/idempotence |
| 26 Corruption | corruptOwnershipIsPreservedAndNeverReplacedWithZeros; malformed location/total tests |
| 27 Restart Active | nonzero restart test and prior controller disable/logout/restart cases |
| 28 No failed-load zeros | corrupt ownership, unsupported M2 currency, missing current with migration evidence and interrupted migration |
| 29 Toggle settings | configurationChangesReplaceSettingsWithoutReloadingOwnership; bothDisabledFeaturesLeavePixelsUnchanged |
| 30 Color | defaultsEnableBothFeaturesWithPurpleColor; customQuantityColorIsActuallyDrawnAtBottomRight |
| 31 Hide legacy totals | Updated SnapshotPanelTest.activeDashboardHidesLegacyTotalsButKeepsRealStartTime |
| 32 Dashboard usability | Existing six-metric/sidebar/EDT/setup/error/reset coverage retained |

### M3 manual acceptance — user only

Launch `.\gradlew.bat run` and follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Agents must not control RuneScape. Use the existing accepted M2 run; do not reset or delete it. Backups/migration are automatic on that designated host. See docs/MILESTONE_3.md for exact migration and rollback behavior.

1. Wait for Active: same run/start time, dashboard present, no original bank/inventory/equipment counts or legacy GP. No initialization dialog should be needed for the existing run.
2. Open the normal bank manually. Check the first full row and middle rows: every occupied real item should show purple zero at bottom-right, ordinary quantity values unchanged and glyphs readable. Locked items use darker #505050 at approximately 48% opacity uniformly from top to bottom, without the old horizontal split. A subtle background tint is expected with this fallback; true sprite grayscale is not claimed. Check tall/colorful equipment and yellow/white/green quantity labels. Placeholders show zero; fillers and empty/BLANKOBJECT slots have no counter. Bank-tab/header icons and inventory/equipment/shared storage have no effect.
3. Scroll slowly until the top row is only partly visible just below the tabs, then examine a partly visible bottom row. Both visible fragments must be tinted identically to middle rows; nothing may draw on the tabs, search area or outside the content viewport. Counters keep their normal item-relative positions and may be clipped, never shifted into view. Scroll rapidly up/down, switch normal and locked/unlocked tabs, search matching and no-match terms, resize the client and change UI scale. No stale rectangles or misalignment may remain. Test other bank layouts/plugins separately for corner conflicts.
4. Manually drag from a full row and a clipped top row, hover, right-click and deposit/withdraw if desired. Interactions/tooltips/ordinary physical quantities must work normally; all virtual counts stay zero in M3. Supplied drag rectangles are used; when RuneLite omits a clipped-origin drag rectangle, its floating effect is temporarily omitted rather than misplaced, and returns on release. The plugin must perform no game action itself.
5. Toggle quantities and dimming independently in From Scratch configuration, then change text color. Verify immediate changes without restart. Close/reopen the bank, including PIN UI where applicable; no overlays may remain on the closed bank.
6. Disable/re-enable several times: exactly one overlay/nav button, no leftover effect. Relog/restart and switch characters/configuration profiles; only the verified character's Active run renders, and its original run/start time remains. No reset/new snapshot is required after migration.
7. Report version, UI mode/scale, enabled bank plugins, observations and redacted errors. Do not edit saves or grant positive test balances. Positive live accounting is deferred; automated fixtures cover nonzero painting/storage.

Stop after M3. A build, synthetic pixels or clean JVM start cannot establish real-client layout/performance behavior or Plugin Hub approval.

### Locked-item refinement regressions

The original mask skipped the first 12 pixels and filled the remainder with 57%-opaque black. Existing painter regressions prevent that split; the intermediate 30% full-item tint fixed it but did not exercise the item source. Installed 1.13.3 renderer inspection identified a separate origin-inside-clip condition before a WidgetItem enters the callback list. Painter-only tests could never detect that omission. The new VirtualBankOverlayTest invokes the real overlay pass through a synthetic widget-observation context, including top origins outside the viewport, and verifies visible intersections, both edges, control exclusion, label positions and lifecycle. It does not simulate the real game widget implementation or game input.

Appearance regressions now require approximately 48% dark-neutral tint, at least half the original contrast, readable small-font yellow/green/white quantity glyphs, a bank-background color remaining close to its original, and full-color purple counters. Uniform/tall/colored sprites, moved bounds, fractional scaling, positive/disabled pixels and graphics restoration remain covered. No live screenshot or reference-image pixel matching was performed by the agent.

The item source makes one linear pass over current bank widgets, with intersection checks before painting or definition lookup. It creates no per-frame item map, sprite image/cache, IO or accounting work; placeholder lookups remain cached. Repeated synthetic scrolling and offscreen/cache tests verify those observable boundaries, not real frame rate. Compare a large bank's scrolling/input response with the effect on/off in-game. Positive balances remain fixture-only until the later engine is authorized.

## Milestone 2 evidence (historical; subsequently accepted)

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

## Remaining test matrix and historical milestone boundaries

Test accounting independently of RuneLite. Use synthetic/recorded event batches for adapters and user-run acceptance for rendering/game behavior.

| Area | Required scenarios |
| --- | --- |
| Initialization M2 | Accepted; existing automated regressions retained |
| Ownership/Bank M3 | Accepted; existing model/persistence/rendering regressions retained |
| Transfers/Offline M4 | Automated coverage and user-only acceptance above; unsupported transformations/storage stay reviewed |
| Currency M5 | Earn/spend/reserve/refund, exact bounds/overflow rejection, insufficient funds, death debt, ordinary debt rejection, repayment, physical legacy cash never recredits spent virtual GP |
| Valuation M5 | Acquisition, pooled basis/rounding conservation, sales above/below basis, price fluctuation, consumption, untradeable/unpriced, cash/items once, buyback effects |
| GE M6 | Buy/sell, partial fills, committed goods/cash, completed/uncollected positions, tax gross/net once, cancellation/refund, collection deduplication, actual vs offer price, uncertainty |
| Buybacks M7 | Fire Cape, natural duplicate with old copy locked, multiple copies, Infernal price, Ancient Sceptre flat price, full catalog, Graceful complete/partial/natural pieces, Elite Void total, Barrows Gloves/Royal Seed Pod/tradeable exclusions, insufficient/negative GP, atomic crash recovery |
| Persistence M2/M9 | Logout/login, restart, characters/profiles, incomplete initialization, corruption/truncation, schema migration, backup/restore/reset, interrupted append/checkpoint/rename, filesystem fallback, idempotent replay |
| UI M8 | Examine location/quantity/GE/HA, zero/N/A, original message retained, duplicate prevention, outgoing chat untouched, totals, large numbers, timers/mobile disclosure, notification deduplication |

## Release gates

Every milestone: review changed APIs/dependencies for compliance, run meaningful tests and build, then ask the user to accept changed behavior in-game. Keep ACCOUNTING.md's invariant mapping current. Add tests with their implementation; do not assert nonexistent features.

M9 adds migration/recovery fixtures, full regression, bounded-work profiling, and user-run bank/UI/GE acceptance. M10 rechecks current Hub requirements, metadata, art, licensing, privacy, release notes, and approved source commit after all acceptance gates pass.
