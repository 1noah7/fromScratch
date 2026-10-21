package com.fromscratch.controller;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.*;
import com.fromscratch.observation.*;
import com.fromscratch.overlay.BankOverlayState;
import com.fromscratch.persistence.RunRepository;
import com.fromscratch.ui.SetupView;
import com.fromscratch.ui.ItemReviewTable;
import com.fromscratch.overlay.PipelineOverlayFixture;
import java.util.*;
import java.util.concurrent.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.tracking.TrackingFixtures.*;

/** Real observation collector -> controller -> repository -> bank projection, without a game client. */
public class LiveItemPipelineTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private final Source source = new Source();
	private Worker worker = new Worker();
	private static final int ITEM = ItemID.WOODEN_SHIELD;
	private final BankOverlayState bank = new BankOverlayState();
	private final List<SetupView> views = new ArrayList<>();
	private Filepath root;
	private RunRepository repository;
	private SetupController controller;
	@Before public void start() throws Exception
	{
		begin(0, 0);
	}
	private void begin(long legacyBank, long legacyInventory) throws Exception
	{
		if (controller != null) { controller.stop(); }
		worker = new Worker(); views.clear(); source.collector.reset(); source.open = true;
		source.containers.put(InventoryID.BANK, copies(legacyBank)); source.containers.put(InventoryID.INV, copies(legacyInventory));
		root = Filepath.Unchecked.getRooted(temporary.newFolder().toPath());
		repository = new RunRepository(SnapshotFixtures.codec());
		repository.activate(root, RunRecord.activate(SnapshotFixtures.CHARACTER_A, UUID.randomUUID(),
			snapshot(holdings(items(ITEM, legacyBank), items(ITEM, legacyInventory), Collections.emptyMap())), TIME));
		bank.enable();
		controller = new SetupController(source, repository, Runnable::run, view -> { views.add(view); com.fromscratch.overlay.LegacyOverlayFixture.update(bank, view); });
		controller.start(() -> root, worker); worker.runAll();
		settle();
		assertEquals(SetupView.Mode.ACTIVE, view().getMode());
		assertNull(view().getItemReview());
	}
	private SetupView view() { return views.get(views.size() - 1); }
	private void tick() { source.collector.gameTick(); controller.tick(); worker.runAll(); }
	private void settle() { tick(); tick(); tick(); }
	private void bankOpen(boolean open) { source.open = open; source.collector.bankInterfaceChanged(); }
	private void change(int container, long quantity)
	{
		List<ObservedItem> items = copies(quantity);
		source.containers.put(container, items); source.collector.changed(container, items); controller.observationChanged();
	}
	private static List<ObservedItem> copies(long count) { return count == 0 ? Collections.emptyList() : Collections.singletonList(new ObservedItem(ITEM, count, false)); }
	private RunRecord saved() throws Exception { return repository.load(root, SnapshotFixtures.CHARACTER_A).get(); }
	@Test public void pickupFollowedByDepositBeforeQuietTickIsNotDiscarded() throws Exception
	{
		bankOpen(false); settle();
		change(InventoryID.INV, 1); tick(); // legitimate pickup observed, not yet a settled transaction
		bankOpen(true); change(InventoryID.BANK, 1); change(InventoryID.INV, 0); settle();
		assertEquals(SetupView.Mode.ACTIVE, view().getMode());
		assertEquals("1", bank.getProjection().label(ITEM));
		assertEquals(1, saved().getOwnership().total(ITEM));
		assertEquals(java.awt.Color.RED.getRGB(), PipelineOverlayFixture.render(bank, ITEM, true).getRGB(15, 15));
	}
	@Test public void ordinaryPickupInventoryBankWithdrawalAndRedepositConserveEarnedCopies() throws Exception
	{
		bankOpen(false); settle(); change(InventoryID.INV, 1); settle();
		assertEquals(1, saved().getOwnership().at(ITEM, VirtualLocation.INVENTORY));
		bankOpen(true); change(InventoryID.BANK, 1); change(InventoryID.INV, 0); settle(); assertEquals("1", bank.getProjection().label(ITEM));
		change(InventoryID.BANK, 0); change(InventoryID.INV, 1); settle(); assertEquals("0", bank.getProjection().label(ITEM));
		assertEquals(0, saved().getItemProgress().getLegacyDeficits().quantity(ITEM));
		change(InventoryID.BANK, 1); change(InventoryID.INV, 0); settle();
		assertEquals("1", bank.getProjection().label(ITEM)); assertEquals(1, saved().getItemProgress().getAcquisitions().size());
	}
	@Test public void depositingAnInitialLegacyInventoryItemDoesNotProveAcquisition() throws Exception
	{
		begin(0, 1); change(InventoryID.BANK, 1); change(InventoryID.INV, 0); settle();
		assertEquals("0", bank.getProjection().label(ITEM)); assertEquals(0, saved().getOwnership().total(ITEM));
		assertTrue(saved().getItemProgress().getAcquisitions().isEmpty());
		assertNotEquals(java.awt.Color.RED.getRGB(), PipelineOverlayFixture.render(bank, ITEM, true).getRGB(15, 15));
	}
	@Test public void ambiguousReceiptIsVisibleAndNamedApprovalCommitsThroughRenderer() throws Exception
	{
		bankOpen(false); settle(); source.collector.unsupportedAction(); change(InventoryID.INV, 1); settle();
		assertEquals(SetupView.Mode.RECONCILING, view().getMode()); assertEquals(0, saved().getOwnership().total(ITEM));
		bankOpen(true); settle();
		ItemReviewTable table = new ItemReviewTable(view().getItemReview(), Collections.singletonMap(ITEM, "Wooden shield"));
		table.decide(new int[]{0}, ItemReviewTable.Decision.EARNED);
		controller.confirmUpdate(view().getItemReview().getToken(), table.edits("New shield earned legitimately.")); worker.runAll();
		assertEquals(1, saved().getOwnership().at(ITEM, VirtualLocation.INVENTORY));
		change(InventoryID.BANK, 1); change(InventoryID.INV, 0); settle();
		assertEquals("1", bank.getProjection().label(ITEM));
		assertEquals(java.awt.Color.RED.getRGB(), PipelineOverlayFixture.render(bank, ITEM, true).getRGB(15, 15));
	}
	@Test public void bankOnlyIncreaseWithoutSourceEvidenceRemainsPendingUntilReviewed() throws Exception
	{
		change(InventoryID.BANK, 1); settle(); assertEquals(SetupView.Mode.RECONCILING, view().getMode());
		assertNotNull(view().getItemReview()); assertEquals(0, saved().getOwnership().total(ITEM));
		ItemReviewTable table = new ItemReviewTable(view().getItemReview(), Collections.singletonMap(ITEM, "Wooden shield"));
		table.decide(new int[]{0}, ItemReviewTable.Decision.LEGACY);
		controller.confirmUpdate(view().getItemReview().getToken(), table.edits("Existing legacy copy.")); worker.runAll();
		assertEquals("0", bank.getProjection().label(ITEM)); assertTrue(saved().getItemProgress().getAcquisitions().isEmpty());
	}
	@Test public void twoLegacyWithdrawalsAndReturnsDisplayMinusOneMinusTwoAndZero() throws Exception
	{
		begin(100, 0);
		change(InventoryID.BANK, 99); change(InventoryID.INV, 1); settle(); assertEquals("-1", bank.getProjection().label(ITEM));
		change(InventoryID.BANK, 98); change(InventoryID.INV, 2); settle(); assertEquals("-2", bank.getProjection().label(ITEM));
		assertEquals(0, saved().getOwnership().total(ITEM));
		assertNotEquals(java.awt.Color.RED.getRGB(), PipelineOverlayFixture.render(bank, ITEM, true).getRGB(15, 15));
		change(InventoryID.BANK, 100); change(InventoryID.INV, 0); settle(); assertEquals("0", bank.getProjection().label(ITEM));
		assertEquals(0, saved().getEconomy().getItemsUnlocked());
	}
	@Test public void mixedWithdrawalsUseEarnedFirstAndOnlyLegacyPartCreatesDeficit() throws Exception
	{
		begin(100, 0); bankOpen(false); settle(); change(InventoryID.INV, 3); settle();
		bankOpen(true); change(InventoryID.BANK, 103); change(InventoryID.INV, 0); settle(); assertEquals("3", bank.getProjection().label(ITEM));
		change(InventoryID.BANK, 99); change(InventoryID.INV, 4); settle();
		assertEquals("-1", bank.getProjection().label(ITEM)); assertEquals(3, saved().getOwnership().total(ITEM));
		change(InventoryID.BANK, 103); change(InventoryID.INV, 0); settle();
		assertEquals("3", bank.getProjection().label(ITEM)); assertEquals(1, saved().getItemProgress().getAcquisitions().size());
	}
	@Test public void deficitSurvivesEmptyBankAndRestartWithoutCreatingASlot() throws Exception
	{
		begin(1, 0); change(InventoryID.BANK, 0); change(InventoryID.INV, 1); settle();
		assertEquals("-1", bank.getProjection().label(ITEM));
		assertEquals(java.awt.Color.RED.getRGB(), PipelineOverlayFixture.render(bank, ITEM, false).getRGB(15, 15));
		RunRecord before = saved(); controller.stop(); source.collector.reset(); worker = new Worker();
		controller = new SetupController(source, repository, Runnable::run, view -> { views.add(view); com.fromscratch.overlay.LegacyOverlayFixture.update(bank, view); });
		controller.start(() -> root, worker); worker.runAll(); settle();
		assertEquals("-1", bank.getProjection().label(ITEM)); assertNull(view().getItemReview());
		assertEquals(before.getSnapshot(), saved().getSnapshot()); assertEquals(0, saved().getOwnership().total(ITEM));
	}
	@Test public void cancelAndStaleComparisonNeverChangeSavedOwnership() throws Exception
	{
		RunRecord before = saved(); controller.reviewItems(); settle(); UUID token = view().getItemReview().getToken();
		controller.cancelUpdate(token); assertEquals(before, saved()); assertEquals(SetupView.Mode.ACTIVE, view().getMode());
		controller.reviewItems(); settle(); token = view().getItemReview().getToken();
		ItemReviewTable table = new ItemReviewTable(view().getItemReview(), Collections.emptyMap());
		change(InventoryID.BANK, 1);
		controller.confirmUpdate(token, table.edits("Stale preview.")); worker.runAll();
		assertEquals(before, saved()); assertEquals(SetupView.Mode.RECONCILING, view().getMode());
	}
	@Test public void verifiedGroundPickupHintSurvivesImmediateBankOpening() throws Exception
	{
		bankOpen(false); source.collector.action(ITEM, ItemObservation.Action.ACQUIRE); change(InventoryID.INV, 1);
		bankOpen(true); change(InventoryID.BANK, 1); change(InventoryID.INV, 0); settle();
		assertEquals(SetupView.Mode.ACTIVE, view().getMode()); assertEquals("1", bank.getProjection().label(ITEM));
		assertEquals(1, saved().getOwnership().total(ITEM));
	}
	@Test public void explicitMissedAcquisitionCorrectionReclassifiesOneUnchangedShieldThroughRenderer() throws Exception
	{
		begin(1, 0); RunRecord before = saved(); controller.reviewItems(); settle();
		ItemReviewTable table = new ItemReviewTable(view().getItemReview(), Collections.singletonMap(ITEM, "Wooden shield"));
		assertEquals(0, table.getRowCount()); table.filter(ItemReviewTable.Filter.ALL, "wooden", true);
		table.decide(new int[]{0}, ItemReviewTable.Decision.EARNED);
		controller.confirmUpdate(view().getItemReview().getToken(), table.edits("Correcting a previously missed legitimate acquisition.")); worker.runAll();
		assertEquals("1", bank.getProjection().label(ITEM)); assertEquals(1, saved().getOwnership().at(ITEM, VirtualLocation.BANK));
		assertEquals(before.getSnapshot(), saved().getSnapshot()); assertEquals(1, saved().getEconomy().getItemsUnlocked());
		assertEquals(java.awt.Color.RED.getRGB(), PipelineOverlayFixture.render(bank, ITEM, true).getRGB(15, 15));
	}
	private static final class Source implements SnapshotSource, BankSnapshotReader.Access
	{
		private boolean open = true;
		private final Map<Integer, List<ObservedItem>> containers = new HashMap<>();
		private final ItemObservationCollector collector = new ItemObservationCollector(this);
		Source() { containers.put(InventoryID.BANK, Collections.emptyList()); containers.put(InventoryID.INV, Collections.emptyList()); containers.put(InventoryID.WORN, Collections.emptyList()); }
		public String characterKey() { return SnapshotFixtures.CHARACTER_A; }
		public LegacySnapshot capture(String expected) { return snapshot(holdings(0, 0, 0)); }
		public boolean supportsTracking() { return true; }
		public ItemObservation observeItems(boolean preparing) { return collector.poll(preparing); }
		public void acknowledgeItems(long version) { collector.acknowledge(version); }
		public void acknowledgeItems(ItemObservation observation) { collector.acknowledge(observation); }
		public boolean isInterfaceOpen(int id) { return id == InterfaceID.BANKMAIN && open; }
		public boolean isVisible(int id) { return open; }
		public List<ObservedItem> items(int id) { return containers.get(id); }
	}
	private static final class Worker extends AbstractExecutorService
	{
		private final Queue<Runnable> tasks = new ArrayDeque<>(); private boolean stopped;
		void runAll() { while (!tasks.isEmpty()) { tasks.remove().run(); } }
		public void execute(Runnable task) { tasks.add(task); }
		public void shutdown() { stopped = true; } public List<Runnable> shutdownNow() { stopped = true; return Collections.emptyList(); }
		public boolean isShutdown() { return stopped; } public boolean isTerminated() { return stopped; }
		public boolean awaitTermination(long time, TimeUnit unit) { return stopped; }
	}
}
