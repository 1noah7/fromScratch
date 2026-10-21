package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.overlay.*;
import com.fromscratch.persistence.*;
import java.awt.Color;
import java.util.*;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.bank.BankFixtures.*;

/** Real bank aggregation/quiet ticks -> live controller -> Filepath journal/checkpoint -> actual M3 renderer. */
public class BankOnlyPipelineTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private Filepath root;
	private BankRepository repository;
	private final Source source = new Source();
	private Worker worker;
	private BankController controller;
	private final BankOverlayState overlay = new BankOverlayState();
	private final List<BankView> views = new ArrayList<>();
	@Before public void setup() throws Exception
	{
		root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		repository = new BankRepository(new BankRunCodec(SnapshotFixtures.gson()), SnapshotFixtures.codec()); overlay.enable(); launch(); settle();
	}
	@After public void stop() { controller.stop(); }
	private void launch() { worker = new Worker(); controller = new BankController(source, repository, Runnable::run, v -> { views.add(v); overlay.updateBank(v); }); controller.start(() -> root, worker); worker.runAll(); }
	private void tick() { source.observer.tick(); controller.tick(); worker.runAll(); }
	private void settle() { tick(); tick(); tick(); tick(); }
	private BankView view() { return views.get(views.size() - 1); }
	private BankRun saved() throws Exception { return repository.load(root, source.character).get(); }
	private void start() { controller.startRun(true); worker.runAll(); settle(); assertTrue(view().getRun().isActive()); }
	private void change(long n) { source.quantity(n); settle(); }
	private void label(String expected) { assertEquals(expected, overlay.getProjection().label(SHIELD)); }

	private void fullWithdrawal(boolean placeholders, boolean stackable) throws Exception
	{
		int id = stackable ? ItemID.COINS : SHIELD;
		int placeholder = stackable ? ItemID.Placeholder.COINS : ItemID.Placeholder.WOODEN_SHIELD;
		source.items = stackable ? new Item[]{new Item(id, 4)} : new Item[]{new Item(id, 1), new Item(id, 1), new Item(id, 1), new Item(id, 1)};
		source.observer.changed(); settle(); start(); assertEquals(0, saved().balance(id));
		source.items = new Item[]{new Item(id, 1)}; source.observer.changed(); settle(); assertEquals(-3, saved().balance(id));
		// Native placeholders may carry a widget/container quantity; their template, not that
		// number, establishes that no physical copies remain. Test both zero and nonzero forms.
		source.placeholders.add(placeholder);
		source.items = placeholders ? new Item[]{new Item(placeholder, stackable ? 0 : 1)} : new Item[0];
		source.observer.changed(); settle();
		assertEquals(0, saved().physical(id)); assertEquals(-4, saved().balance(id)); assertEquals(0, saved().balance(placeholder));
		assertEquals(Long.valueOf(-1), saved().getRecent().get(0).getPhysicalChanges().get(id));
		assertEquals(4, saved().getInitialBank().quantity(id));
		if (placeholders)
		{
			java.awt.image.BufferedImage expected = PipelineOverlayFixture.render(overlay, id, 0, true, id);
			java.awt.image.BufferedImage actual = PipelineOverlayFixture.render(overlay, placeholder, stackable ? 0 : 1, true, id);
			assertArrayEquals(expected.getRGB(0, 0, 100, 100, null, 0, 100), actual.getRGB(0, 0, 100, 100, null, 0, 100));
		}
		else
		{
			assertTrue(BankCorrections.search(view(), Integer.toString(id)).contains(id));
			assertEquals(Color.RED.getRGB(), PipelineOverlayFixture.render(overlay, id, false).getRGB(15, 15));
		}
		BankRun before = saved(); source.observer.changed(); settle(); assertEquals(before, saved()); // tab/search refresh
		source.open(false); settle(); source.open(true); settle(); assertEquals(before, saved());
		controller.stop(); source.observer.reset(); launch(); settle(); assertEquals(-4, saved().balance(id));
		for (int quantity : new int[]{1, 2, 4})
		{
			source.items = new Item[]{new Item(id, quantity)}; source.observer.changed(); settle();
			assertEquals(quantity - 4, saved().balance(id)); assertEquals(0, saved().getFinancial().getGainedGp());
		}
		assertEquals(0, saved().balance(id)); assertEquals(0, saved().getFinancial().getEventCount());
	}
	@Test public void finalUnstackableWithdrawalWithoutPlaceholderRetainsDeficit() throws Exception { fullWithdrawal(false, false); }
	@Test public void finalUnstackableWithdrawalWithNativePlaceholderRendersOriginalDeficit() throws Exception { fullWithdrawal(true, false); }
	@Test public void finalStackableWithdrawalWithoutPlaceholderRetainsDeficit() throws Exception { fullWithdrawal(false, true); }
	@Test public void finalStackableWithdrawalWithNativePlaceholderRendersOriginalDeficit() throws Exception { fullWithdrawal(true, true); }
	@Test public void bankOnlySetupNeedsNoPersonalContainers() throws Exception { start(); assertEquals(0, saved().balance(SHIELD)); assertEquals(1, saved().getInitialBank().quantity(SHIELD)); }
	@Test public void attestationRequired() { controller.startRun(false); worker.runAll(); assertNull(view().getRun()); assertTrue(view().getMessage().contains("Confirm")); }
	@Test public void completeEmptyBankCanStart() throws Exception { change(0); start(); assertTrue(saved().getInitialBank().getQuantities().isEmpty()); }
	@Test public void legacyWithdrawalAndReturnAreMinusOneAndZero() throws Exception { start(); change(0); label("-1"); assertEquals(-1, saved().balance(SHIELD)); change(1); label("0"); }
	@Test public void successiveNewDepositsAndWithdrawal() throws Exception { change(0); start(); change(1); label("1"); change(2); label("2"); change(1); label("1"); assertEquals(1, saved().unlocked()); }
	@Test public void burnOutsideBankDoesNotChangeProgressOnReopen() throws Exception { change(0); start(); change(2); change(1); BankRun before = saved(); source.open(false); settle(); /* outside-bank actions have no source at all */ source.open(true); settle(); label("1"); assertEquals(before, saved()); }
	@Test public void repeatedWithdrawalsAndDepositsDoNotCreateLifetimeEarnings() throws Exception { start(); BankRun initial = saved(); for (int i = 0; i < 5; i++) { change(0); change(1); } label("0"); assertEquals(0, saved().unlocked()); assertEquals(initial.getInitialBank(), saved().getInitialBank()); }
	@Test public void mixedLegacyAndNewStackUsesSignedNetProgress() { source.quantity(100); settle(); start(); change(103); label("3"); change(99); label("-1"); change(103); label("3"); }
	@Test public void reorderingTabsAndSearchDoNotChangeAggregate() throws Exception { source.items = new Item[]{new Item(SHIELD, 1), new Item(OTHER, 2)}; source.observer.changed(); settle(); start(); BankRun before = saved(); source.items = new Item[]{new Item(OTHER, 2), new Item(SHIELD, 1)}; source.observer.changed(); settle(); assertEquals(before, saved()); }
	@Test public void unavailableBankIsNotDeletion() throws Exception { start(); BankRun before = saved(); source.items = null; source.observer.changed(); settle(); assertFalse(view().ready()); assertEquals(before, saved()); label("0"); }
	@Test public void nullSlotIsIncompleteNotEmpty() throws Exception { start(); BankRun before = saved(); source.items = new Item[]{null}; source.observer.changed(); settle(); assertFalse(view().ready()); assertEquals(before, saved()); }
	@Test public void pinDoesNotAcceptCachedBank() { source.pin = true; source.observer.changed(); settle(); controller.startRun(true); worker.runAll(); assertNull(view().getRun()); }
	@Test public void widgetOpenBeforeContainerWaitsThenRecovers() { source.items = null; source.open(true); settle(); assertFalse(view().ready()); source.quantity(1); settle(); start(); label("0"); }
	@Test public void alreadyOpenBankDoesNotNeedContainerChangedEvent() { start(); label("0"); }
	@Test public void sameTickIntermediateUpdatesAreCombined() throws Exception { start(); source.quantity(2); controller.tick(); source.quantity(3); controller.tick(); assertEquals(0, saved().balance(SHIELD)); settle(); label("2"); }
	@Test public void repeatedEventsAndNoChangeDoNotAppendJournal() throws Exception { start(); BankRun before = saved(); source.observer.changed(); controller.tick(); source.observer.changed(); settle(); assertEquals(before, saved()); }
	@Test public void absentNegativeItemIsSearchableWithoutInventingSlot() { start(); change(0); assertEquals(Collections.singletonList(SHIELD), BankCorrections.search(view(), "wooden")); assertEquals(Color.RED.getRGB(), PipelineOverlayFixture.render(overlay, SHIELD, false).getRGB(15, 15)); label("-1"); }
	@Test public void correctionsSearchWorksWithNoPendingChanges() { start(); assertEquals(Collections.singletonList(SHIELD), BankCorrections.search(view(), "Wooden shield")); }
	@Test public void correctionUpdatesDurableOverlayAndSurvivesLaterBankChanges() throws Exception { start(); controller.correct(view().getRun().getRevision(), SHIELD, 1); worker.runAll(); label("1"); assertEquals(1, saved().balance(SHIELD)); change(2); label("2"); change(1); label("1"); }
	@Test public void correctionCannotExceedPhysicalBank() throws Exception { start(); BankRun before = saved(); controller.correct(before.getRevision(), SHIELD, 2); worker.runAll(); assertEquals(before, saved()); assertTrue(view().getMessage().contains("physical")); }
	@Test public void correctionCancelPreviewIsNonMutating() throws Exception { start(); BankRun before = saved(); assertEquals(-2, BankCorrections.desired(before, SHIELD, "-2", false)); BankCorrections.search(view(), "shield"); assertEquals(before, saved()); }
	@Test public void staleCorrectionCannotOverwriteAutomaticUpdate() throws Exception { start(); UUID stale = view().getRun().getRevision(); change(2); BankRun before = saved(); controller.correct(stale, SHIELD, 0); worker.runAll(); assertEquals(before, saved()); label("1"); }
	@Test public void uncommittedNewPhysicalDataRejectsCorrection() throws Exception { start(); BankRun before = saved(); source.quantity(2); controller.correct(before.getRevision(), SHIELD, 1); worker.runAll(); assertEquals(before, saved()); }
	@Test public void negativeBalanceSurvivesRestartAndNoChangeIsNotReplayed() throws Exception { start(); change(0); UUID runId = saved().getRunId(); controller.stop(); source.observer.reset(); launch(); settle(); label("-1"); assertEquals(runId, saved().getRunId()); BankRun before = saved(); source.open(false); settle(); source.open(true); settle(); assertEquals(before, saved()); }
	@Test public void offlineNetGainsApplyAutomaticallyAndSummaryRemainsAfterCommit() throws Exception { start(); controller.stop(); source.quantity(4); source.observer.reset(); launch(); settle(); label("3"); assertEquals(Long.valueOf(3), saved().getHostSummary().getPhysicalChanges().get(SHIELD)); change(5); label("4"); assertEquals(Long.valueOf(3), saved().getHostSummary().getPhysicalChanges().get(SHIELD)); }
	@Test public void offlineNetLossAutomaticallyAppliesWithoutReview() throws Exception { start(); controller.stop(); source.quantity(0); source.observer.reset(); launch(); settle(); label("-1"); assertEquals(Long.valueOf(-1), saved().getHostSummary().getPhysicalChanges().get(SHIELD)); }
	@Test public void publishWaitsForDurableAcknowledgement() throws Exception { start(); source.quantity(2); source.observer.tick(); controller.tick(); source.observer.tick(); controller.tick(); source.observer.tick(); controller.tick(); assertFalse(worker.tasks.isEmpty()); label("0"); assertEquals(0, saved().balance(SHIELD)); worker.runAll(); label("1"); }
	@Test public void positiveItemsHaveAcceptedNormalAppearanceAndNegativeAreDim() { start(); assertNotEquals(Color.RED.getRGB(), PipelineOverlayFixture.render(overlay, SHIELD, true).getRGB(15, 15)); change(2); assertEquals(Color.RED.getRGB(), PipelineOverlayFixture.render(overlay, SHIELD, true).getRGB(15, 15)); change(0); assertNotEquals(Color.RED.getRGB(), PipelineOverlayFixture.render(overlay, SHIELD, true).getRGB(15, 15)); }
	@Test public void disableRemovesAllProjectionAndReenableRestoresSavedRun() { start(); controller.stop(); overlay.disable(); assertNull(overlay.getProjection()); overlay.enable(); source.observer.reset(); launch(); settle(); label("0"); }
	@Test public void verifiedIdentitySeparatesCharacters() throws Exception { start(); change(2); source.character = SnapshotFixtures.CHARACTER_B; source.observer.reset(); controller.tick(); worker.runAll(); settle(); assertNull(view().getRun()); assertEquals(1, repository.load(root, SnapshotFixtures.CHARACTER_A).get().balance(SHIELD)); }
	@Test public void placeholdersFillersAndVariantsUseExactIds() throws Exception { int variant = SHIELD + 1; source.items = new Item[]{new Item(SHIELD, 1), new Item(variant, 1), new Item(ItemID.BANK_FILLER, 1), new Item(ItemID.BLANKOBJECT, 1)}; source.placeholders.add(variant); source.observer.changed(); settle(); start(); assertEquals(Collections.singletonMap(SHIELD, 1L), saved().getPhysical()); source.placeholders.clear(); source.observer.changed(); settle(); assertEquals(1, saved().balance(variant)); assertEquals(0, saved().balance(SHIELD)); }
	@Test public void duplicateInitializationCannotReplaceOriginalRun() throws Exception { start(); BankRun before = saved(); controller.startRun(true); worker.runAll(); assertEquals(before, saved()); }
	@Test public void resetArchivesThenRequiresFreshAttestationAndBank() throws Exception { start(); controller.reset(saved().getRevision()); worker.runAll(); assertFalse(saved().isActive()); settle(); controller.startRun(false); worker.runAll(); assertFalse(saved().isActive()); controller.startRun(true); worker.runAll(); assertTrue(saved().isActive()); }
	@Test public void controllerStorageFailureDoesNotPublishOrAdvanceAnchor() throws Exception
	{
		start(); BankRun before = saved(); controller.stop(); worker = new Worker(); java.util.concurrent.atomic.AtomicBoolean fail = new java.util.concurrent.atomic.AtomicBoolean();
		controller = new BankController(source, repository, Runnable::run, v -> { views.add(v); overlay.updateBank(v); });
		controller.start(() -> { if (fail.get()) { throw new java.io.IOException("Synthetic unavailable storage"); } return root; }, worker); worker.runAll(); settle();
		before = saved(); fail.set(true); change(2); assertTrue(view().isError()); assertEquals(before, saved()); assertNull(overlay.getProjection());
		fail.set(false); controller.retry(); worker.runAll(); settle(); label("1"); assertEquals(1, saved().balance(SHIELD));
	}
	@Test public void migrationRequiresOnlyOneBankAnchorThenNormalChangesAreAutomatic() throws Exception
	{
		com.fromscratch.model.RunRecord old = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 0); new RunRepository(SnapshotFixtures.codec()).activate(root, old);
		controller.retry(); worker.runAll(); settle(); assertFalse(view().getRun().isAnchored()); assertNull(overlay.getProjection());
		controller.anchor(view().getRun().getRevision(), source.readBank(), false); worker.runAll(); settle(); label("0");
		change(0); label("-1"); change(1); label("0"); controller.stop(); source.observer.reset(); launch(); settle(); label("0"); assertTrue(saved().isAnchored());
	}
	@Test public void migrationAnchorRejectsChangedBankPreview() throws Exception
	{
		new RunRepository(SnapshotFixtures.codec()).activate(root, SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 0)); controller.retry(); worker.runAll(); settle();
		BankView preview = view(); source.quantity(2); settle(); controller.anchor(preview.getRun().getRevision(), preview.getObservation().getBank(), false); worker.runAll(); assertFalse(saved().isAnchored());
	}
	@Test public void staleResetAfterCharacterSwitchCannotResetPreviousRun() throws Exception
	{
		start(); BankRun before = saved(); source.character = SnapshotFixtures.CHARACTER_B;
		controller.reset(before.getRevision()); worker.runAll(); assertEquals(before, repository.load(root, SnapshotFixtures.CHARACTER_A).get()); assertNull(view().getRun());
	}
	@Test public void pendingSaveCannotPublishIntoAnotherCharacter() throws Exception
	{
		start(); source.quantity(2); source.observer.tick(); controller.tick(); source.observer.tick(); controller.tick(); source.observer.tick(); controller.tick();
		source.character = SnapshotFixtures.CHARACTER_B; source.observer.reset(); controller.tick(); worker.runAll(); settle();
		assertNull(view().getRun()); assertNull(overlay.getProjection()); assertEquals(1, repository.load(root, SnapshotFixtures.CHARACTER_A).get().balance(SHIELD));
	}
}
