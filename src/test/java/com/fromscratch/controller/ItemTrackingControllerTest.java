package com.fromscratch.controller;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.*;
import com.fromscratch.observation.*;
import com.fromscratch.overlay.BankOverlayState;
import com.fromscratch.persistence.RunRepository;
import com.fromscratch.tracking.*;
import com.fromscratch.ui.SetupView;
import java.util.*;
import java.util.concurrent.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.tracking.TrackingFixtures.*;

/** Real repository + reducer + controller, with deterministic client/storage dispatch and no account. */
public class ItemTrackingControllerTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private Filepath root;
	private RunRepository repository;
	private final Source source = new Source();
	private final Worker worker = new Worker();
	private final List<SetupView> views = new ArrayList<>();
	private final BankOverlayState bank = new BankOverlayState();
	private SetupController controller;
	@Before public void start() throws Exception
	{
		root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath()); repository = new RunRepository(SnapshotFixtures.codec());
		repository.activate(root, RunRecord.activate(SnapshotFixtures.CHARACTER_A, UUID.randomUUID(), snapshot(holdings(100, 0, 0)), TIME));
		bank.enable(); controller = new SetupController(source, repository, Runnable::run, view -> { views.add(view); com.fromscratch.overlay.LegacyOverlayFixture.update(bank, view); });
		controller.start(() -> root, worker); worker.runAll();
	}
	private SetupView view() { return views.get(views.size() - 1); }
	private void confirmBaseline()
	{
		controller.tick(); worker.runAll();
		assertEquals(SetupView.Mode.ACTIVE, view().getMode());
	}
	@Test public void restoredRunRequiresGuidedHostReviewBeforeAnyCredit() throws Exception
	{
		assertEquals(SetupView.Mode.ACTIVE, view().getMode()); source.observation = observation(holdings(100, 5, 0), 1); controller.tick();
		assertNotNull(view().getItemReview()); assertEquals(0, repository.load(root, SnapshotFixtures.CHARACTER_A).get().getOwnership().total(ItemID.SHARK));
		assertNull(bank.getProjection());
	}
	@Test public void acquiredAndDepositedItemsUpdateActualPurpleProjectionOnlyAfterCommit() throws Exception
	{
		confirmBaseline(); source.observation = observation(holdings(100, 5, 0), 2); controller.tick();
		assertEquals(0, view().getSaved().getOwnership().total(ItemID.SHARK)); worker.runAll();
		assertEquals(5, view().getSaved().getOwnership().total(ItemID.SHARK)); assertEquals("0", bank.getProjection().label(ItemID.SHARK));
		source.observation = observation(holdings(105, 0, 0), 3); controller.tick(); worker.runAll();
		assertEquals("5", bank.getProjection().label(ItemID.SHARK));
		assertEquals(1, view().getSaved().getEconomy().getItemsUnlocked());
		assertEquals(0, view().getSaved().getEconomy().getSpendableGp());
	}
	@Test public void legacyWithdrawalsDisplayDeficitsWithoutAcquisition()
	{
		confirmBaseline(); source.observation = observation(holdings(60, 40, 0), 2); controller.tick(); worker.runAll();
		assertEquals("-40", bank.getProjection().label(ItemID.SHARK)); assertTrue(view().getSaved().getItemProgress().getAcquisitions().isEmpty());
	}
	@Test public void repeatedTicksDoNotEnqueueOrApplyTheSameGainTwice() throws Exception
	{
		confirmBaseline(); source.observation = observation(holdings(100, 5, 0), 2);
		controller.tick(); controller.tick(); assertEquals(1, worker.tasks.size()); worker.runAll();
		controller.tick(); controller.tick(); assertTrue(worker.tasks.isEmpty());
		assertEquals(5, repository.load(root, SnapshotFixtures.CHARACTER_A).get().getOwnership().total(ItemID.SHARK));
	}
	@Test public void restartingControllerNeverRecreditsSavedGains() throws Exception
	{
		confirmBaseline(); source.observation = observation(holdings(100, 5, 0), 2); controller.tick(); worker.runAll(); controller.stop();
		source.observation = observation(holdings(100, 5, 0), 1, Collections.emptyMap(), UUID.randomUUID());
		controller.start(() -> root, new Worker()); // load remains queued until this new worker runs
		Worker restarted = new Worker(); controller.stop(); controller.start(() -> root, restarted); restarted.runAll();
		controller.tick(); restarted.runAll(); assertEquals(SetupView.Mode.ACTIVE, view().getMode()); assertNull(view().getItemReview());
		assertEquals(5, view().getSaved().getOwnership().total(ItemID.SHARK)); assertEquals(1, view().getSaved().getItemProgress().getAcquisitions().size());
	}
	@Test public void staleConfirmationAfterContainerChangeDoesNotWrite()
	{
		source.observation = observation(holdings(100, 5, 0), 1);
		controller.tick(); UUID token = view().getItemReview().getToken(); controller.observationChanged();
		controller.confirmItems(token, Collections.emptyMap(), "", false);
		assertEquals(SetupView.Mode.RECONCILING, view().getMode()); assertTrue(worker.tasks.isEmpty());
	}
	@Test public void temporaryUnknownDoesNotDeleteItemsOrRepeatedlyPrompt()
	{
		confirmBaseline(); ItemObservation full = source.observation;
		source.observation = new ItemObservation(SESSION, 2, TIME.plusSeconds(2), CaptureProgress.from(null), null, true, false, false, Collections.emptyMap());
		controller.tick(); assertEquals(SetupView.Mode.ACTIVE, view().getMode()); assertTrue(worker.tasks.isEmpty());
		source.observation = full; controller.tick(); assertEquals(SetupView.Mode.ACTIVE, view().getMode());
	}
	@Test public void logoutDuringQueuedWriteCannotPublishAnotherCharactersProjection() throws Exception
	{
		confirmBaseline(); source.observation = observation(holdings(100, 5, 0), 2); controller.tick();
		source.key = null; controller.tick(); worker.runAll();
		assertEquals(SetupView.Mode.SIGNED_OUT, view().getMode()); assertNull(bank.getProjection());
		assertEquals(5, repository.load(root, SnapshotFixtures.CHARACTER_A).get().getOwnership().total(ItemID.SHARK));
		assertFalse(repository.load(root, SnapshotFixtures.CHARACTER_B).isPresent());
	}
	@Test public void failedStorageStopsAutomaticRetriesAndNeverWritesEmptyProgression() throws Exception
	{
		confirmBaseline(); source.observation = observation(holdings(100, 5, 0), 2); controller.tick();
		Filepath current = root.join("characters", SnapshotFixtures.CHARACTER_A, "current.json"); current.write("{damaged"); worker.runAll();
		assertEquals(SetupView.Mode.ERROR, view().getMode());
		controller.tick(); controller.tick(); assertTrue(worker.tasks.isEmpty());
		try (java.io.Reader reader = current.openReader()) { char[] data = new char[8]; assertEquals(8, reader.read(data)); assertEquals("{damaged", new String(data)); }
	}
	private static final class Source implements SnapshotSource
	{
		private String key = SnapshotFixtures.CHARACTER_A;
		private ItemObservation observation = observation(holdings(100, 0, 0), 1);
		public String characterKey() { return key; }
		public LegacySnapshot capture(String expected) { return snapshot(holdings(100, 0, 0)); }
		public boolean supportsTracking() { return true; }
		public ItemObservation observeItems(boolean preparing) { return observation; }
	}
	private static final class Worker extends AbstractExecutorService
	{
		private final Queue<Runnable> tasks = new ArrayDeque<>(); private boolean stopped;
		void runAll() { while (!tasks.isEmpty()) { tasks.remove().run(); } }
		public void execute(Runnable task) { if (stopped) { throw new RejectedExecutionException(); } tasks.add(task); }
		public void shutdown() { stopped = true; } public List<Runnable> shutdownNow() { stopped = true; List<Runnable> pending = new ArrayList<>(tasks); tasks.clear(); return pending; }
		public boolean isShutdown() { return stopped; } public boolean isTerminated() { return stopped; } public boolean awaitTermination(long time, TimeUnit unit) { return stopped; }
	}
}
