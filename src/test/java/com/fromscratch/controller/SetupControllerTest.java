package com.fromscratch.controller;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.RunRecord;
import com.fromscratch.observation.SnapshotSource;
import com.fromscratch.observation.BankSnapshotReader;
import com.fromscratch.observation.CaptureProgress;
import com.fromscratch.observation.ObservedItem;
import com.fromscratch.model.SnapshotLocation;
import java.util.Collections;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import com.fromscratch.persistence.RunRepository;
import com.fromscratch.ui.SetupView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import net.runelite.client.util.Filepath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/** Deterministic dispatch/worker queues exercise races without sleeps or a game client. */
public class SetupControllerTest
{
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();
	private Filepath root;
	private FakeSource source;
	private List<SetupView> views;
	private RunRepository repository;
	private SetupController controller;
	private ManualExecutor storage;

	@Before
	public void setUp()
	{
		root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		source = new FakeSource();
		views = new ArrayList<>();
		repository = new RunRepository(SnapshotFixtures.codec());
		controller = new SetupController(source, repository, Runnable::run, views::add);
		storage = new ManualExecutor();
		controller.start(() -> root, storage);
		storage.runAll();
	}

	private SetupView view() { return views.get(views.size() - 1); }
	private Filepath current() { return root.join("characters", SnapshotFixtures.CHARACTER_A, "current.json"); }
	private void activate()
	{
		controller.begin();
		controller.confirm(view().getReviewToken());
		storage.runAll();
		assertEquals(SetupView.Mode.ACTIVE, view().getMode());
	}

	@Test
	public void corruptedSaveBlocksBeginAndRetryNeverWritesAnEmptyReplacement() throws Exception
	{
		current().write("{damaged");
		long damagedSize = current().size();
		controller.retry();
		storage.runAll();
		assertEquals(SetupView.Mode.ERROR, view().getMode());
		controller.begin();
		storage.runAll();
		assertEquals(SetupView.Mode.ERROR, view().getMode());
		assertEquals(damagedSize, current().size());
	}

	@Test
	public void characterChangeBeforeQueuedConfirmationCannotInitializeEitherCharacter() throws Exception
	{
		controller.begin();
		UUID review = view().getReviewToken();
		source.character = SnapshotFixtures.CHARACTER_B;
		controller.confirm(review);
		storage.runAll();
		assertEquals(SetupView.Mode.NOT_STARTED, view().getMode());
		assertFalse(repository.load(root, SnapshotFixtures.CHARACTER_A).isPresent());
		assertFalse(repository.load(root, SnapshotFixtures.CHARACTER_B).isPresent());
	}

	@Test
	public void openingBankAfterBeginRetriesWithoutAnotherContainerChange()
	{
		source.bankOpen = false;
		controller.begin();
		assertNull(view().getPreview());
		source.bankOpen = true;
		controller.tick();
		assertNotNull("An open bank must be retried even without ItemContainerChanged", view().getPreview());
		assertEquals(SetupView.Mode.INITIALIZING, view().getMode());
		assertFalse(current().exists());
	}

	@Test
	public void delayedContainerArrivalRetriesWithoutAnAdditionalEvent()
	{
		source.dataAvailable = false;
		controller.begin();
		assertNull(view().getPreview());
		assertTrue(view().getMessage().contains("container data is unavailable"));
		int rendered = views.size();
		controller.tick();
		controller.tick();
		assertEquals("Repeated identical diagnostics should not rebuild Swing", rendered, views.size());
		source.dataAvailable = true;
		controller.tick();
		assertNotNull(view().getReviewToken());
		assertNotNull(view().getPreview());
		assertFalse(current().exists());
	}

	@Test
	public void beginningWhileBankAlreadyOpenNeedsNoFurtherContainerEvent()
	{
		controller.begin();
		assertNotNull(view().getPreview());
		assertNotNull(view().getReviewToken());
		assertFalse(current().exists());
	}

	@Test
	public void reopeningDuringInitializationRecapturesWithoutAQuantityChange()
	{
		controller.begin();
		UUID original = view().getReviewToken();
		source.bankOpen = false;
		controller.observationChanged();
		controller.tick();
		assertNull(view().getReviewToken());
		source.bankOpen = true;
		controller.tick();
		assertNotNull(view().getPreview());
		assertNotEquals(original, view().getReviewToken());
	}

	@Test
	public void existingSavedRunBytesStayUnchangedAcrossBankLifecycle() throws Exception
	{
		activate();
		String before = savedBytes();
		source.bankOpen = false;
		controller.observationChanged();
		controller.tick();
		source.bankOpen = true;
		source.coins = 500;
		controller.observationChanged();
		controller.tick();
		assertEquals(before, savedBytes());
		assertEquals(view().getSaved(), repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}

	private String savedBytes() throws Exception
	{
		try (java.io.Reader reader = current().openReader())
		{
			StringBuilder result = new StringBuilder();
			char[] buffer = new char[1024];
			int length;
			while ((length = reader.read(buffer)) != -1) { result.append(buffer, 0, length); }
			return result.toString();
		}
	}

	@Test
	public void loadingDoesNotWriteBlankStartupState()
	{
		assertEquals(SetupView.Mode.NOT_STARTED, view().getMode());
		assertFalse(current().exists());
	}

	@Test
	public void previewRequiresExplicitConfirmationBeforeAnySave()
	{
		controller.begin();
		assertEquals(SetupView.Mode.INITIALIZING, view().getMode());
		assertNotNull(view().getPreview());
		assertFalse(current().exists());
		assertTrue(storage.tasks.isEmpty());
	}

	@Test
	public void noBankMeansNoConfirmationAndNoIncompleteSave()
	{
		source.bankOpen = false;
		controller.begin();
		assertEquals(SetupView.Mode.INITIALIZING, view().getMode());
		assertNull(view().getPreview());
		assertNull(view().getReviewToken());
		controller.confirm(null);
		assertFalse(current().exists());
	}

	@Test
	public void activeAcknowledgementOccursOnlyAfterBackgroundSave()
	{
		controller.begin();
		controller.confirm(view().getReviewToken());
		assertEquals(SetupView.Mode.SAVING, view().getMode());
		assertFalse(current().exists());
		storage.runAll();
		assertTrue(current().exists());
		assertEquals(SetupView.Mode.ACTIVE, view().getMode());
		assertEquals(0, view().getSaved().getEconomy().getSpendableGp());
	}

	@Test
	public void changesWhileConfirmationIsOpenRequireASecondReview()
	{
		controller.begin();
		UUID oldToken = view().getReviewToken();
		source.coins = 20;
		controller.confirm(oldToken);
		assertEquals(SetupView.Mode.INITIALIZING, view().getMode());
		assertNotEquals(oldToken, view().getReviewToken());
		assertFalse(current().exists());
		controller.confirm(view().getReviewToken());
		storage.runAll();
		assertEquals(SetupView.Mode.ACTIVE, view().getMode());
	}

	@Test
	public void closingBankInvalidatesPreviouslyAvailableConfirmation()
	{
		controller.begin();
		UUID token = view().getReviewToken();
		source.bankOpen = false;
		controller.confirm(token);
		assertNull(view().getPreview());
		assertFalse(current().exists());
	}

	@Test
	public void cancelledInitializationDoesNotCreateASave()
	{
		controller.begin();
		controller.cancel();
		assertEquals(SetupView.Mode.NOT_STARTED, view().getMode());
		assertFalse(current().exists());
	}

	@Test
	public void logoutAndLoginRestoreTheSameRun() throws Exception
	{
		activate();
		RunRecord original = view().getSaved();
		source.character = null;
		controller.tick();
		assertEquals(SetupView.Mode.SIGNED_OUT, view().getMode());
		source.character = SnapshotFixtures.CHARACTER_A;
		controller.tick();
		storage.runAll();
		assertEquals(original, view().getSaved());
		assertEquals(original, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}

	@Test
	public void pluginDisableAndReenablePreserveTheCommittedRun()
	{
		activate();
		RunRecord original = view().getSaved();
		controller.stop();
		storage = new ManualExecutor();
		controller.start(() -> root, storage);
		storage.runAll();
		assertEquals(SetupView.Mode.ACTIVE, view().getMode());
		assertEquals(original, view().getSaved());
	}

	@Test
	public void cancelledQueuedSaveIsNotReportedAsActive()
	{
		controller.begin();
		controller.confirm(view().getReviewToken());
		controller.stop();
		assertFalse(current().exists());
		storage = new ManualExecutor();
		controller.start(() -> root, storage);
		storage.runAll();
		assertEquals(SetupView.Mode.NOT_STARTED, view().getMode());
	}

	@Test
	public void activeRunDoesNotRecaptureLaterPhysicalChanges()
	{
		activate();
		RunRecord original = view().getSaved();
		int captures = source.captures;
		source.coins = 1000000;
		controller.observationChanged();
		controller.tick();
		assertEquals(captures, source.captures);
		assertEquals(original, view().getSaved());
	}

	@Test
	public void characterSwitchHidesPreviousSnapshotAndRejectsOldReview() throws Exception
	{
		activate();
		RunRecord original = view().getSaved();
		source.character = SnapshotFixtures.CHARACTER_B;
		controller.tick();
		assertNull(view().getSaved());
		assertEquals(SetupView.Mode.LOADING, view().getMode());
		storage.runAll();
		controller.reset(original.getState().getRunId());
		assertEquals(SetupView.Mode.NOT_STARTED, view().getMode());
		assertEquals(original, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}

	@Test
	public void staleLoadCallbackCannotReplaceTheNewCharacterView()
	{
		Queue<Runnable> client = new ArrayDeque<>();
		controller.stop();
		controller = new SetupController(source, repository, client::add, views::add);
		storage = new ManualExecutor();
		controller.start(() -> root, storage);
		drain(client);
		storage.runAll(); // A load completes, but its client callback is held.
		source.character = SnapshotFixtures.CHARACTER_B;
		controller.tick();
		int before = views.size();
		drain(client);
		assertEquals(before, views.size());
		storage.runAll();
		drain(client);
		assertEquals(SetupView.Mode.NOT_STARTED, view().getMode());
	}

	@Test
	public void stalePostSaveCallbackAfterDisableIsIgnoredButRestartFindsDurableRun()
	{
		Queue<Runnable> client = new ArrayDeque<>();
		controller.stop();
		controller = new SetupController(source, repository, client::add, views::add);
		storage = new ManualExecutor();
		controller.start(() -> root, storage);
		drain(client);
		storage.runAll();
		drain(client);
		controller.begin();
		drain(client);
		controller.confirm(view().getReviewToken());
		drain(client);
		storage.runAll(); // Durable write, held UI acknowledgement.
		controller.stop();
		int before = views.size();
		drain(client);
		assertEquals(before, views.size());
		storage = new ManualExecutor();
		controller.start(() -> root, storage);
		drain(client);
		storage.runAll();
		drain(client);
		assertEquals(SetupView.Mode.ACTIVE, view().getMode());
	}

	@Test
	public void resetReturnsNotStartedAndRequiresFreshSetup()
	{
		activate();
		controller.reset(view().getSaved().getState().getRunId());
		assertEquals(SetupView.Mode.SAVING, view().getMode());
		storage.runAll();
		assertEquals(SetupView.Mode.NOT_STARTED, view().getMode());
		assertNull(view().getPreview());
		assertNull(view().getSaved().getState().getRunId());
	}

	private PhasedSource phasedSource()
	{
		controller.stop();
		PhasedSource phased = new PhasedSource();
		controller = new SetupController(phased, repository, Runnable::run, views::add);
		storage = new ManualExecutor();
		controller.start(() -> root, storage);
		storage.runAll();
		return phased;
	}

	@Test
	public void multiPhasePreviewConfirmsOnlyAfterBankArrivesAndPersistsZeroEconomy() throws Exception
	{
		PhasedSource phased = phasedSource();
		controller.begin(); // Observe empty but valid personal containers with the bank closed.
		assertNull(view().getReviewToken());
		assertTrue(view().getProgress().ready(SnapshotLocation.INVENTORY));
		phased.inventory = false;
		phased.equipment = false;
		phased.bank = true;
		phased.reader.gameTick();
		controller.tick();
		assertNotNull(view().getReviewToken());
		assertFalse(current().exists());
		controller.confirm(view().getReviewToken());
		storage.runAll();
		assertEquals(SetupView.Mode.ACTIVE, view().getMode());
		assertEquals(0, repository.load(root, SnapshotFixtures.CHARACTER_A).get().getEconomy().getSpendableGp());
	}

	@Test
	public void missingPersonalDataCanBeCollectedAfterClosingAndReopeningTheBank()
	{
		PhasedSource phased = phasedSource();
		phased.bank = true;
		phased.inventory = false;
		phased.equipment = false;
		controller.begin();
		phased.reader.gameTick();
		controller.tick();
		assertTrue(view().getProgress().ready(SnapshotLocation.BANK));
		assertFalse(view().getProgress().ready(SnapshotLocation.INVENTORY));
		assertFalse(view().getProgress().ready(SnapshotLocation.EQUIPMENT));
		controller.confirm(null);
		assertFalse(current().exists());
		phased.bank = false;
		phased.inventory = true;
		phased.equipment = true;
		controller.tick();
		phased.bank = true;
		phased.inventory = false;
		phased.equipment = false;
		phased.reader.interfaceChanged();
		phased.reader.gameTick();
		controller.tick();
		assertNotNull(view().getReviewToken());
		assertFalse(current().exists());
	}

	@Test
	public void activeSaveBytesAreUnchangedWhenPersonalContainersDisappear() throws Exception
	{
		PhasedSource phased = phasedSource();
		controller.begin();
		phased.bank = true;
		phased.reader.gameTick();
		controller.tick();
		controller.confirm(view().getReviewToken());
		storage.runAll();
		String before = savedBytes();
		phased.inventory = false;
		phased.equipment = false;
		phased.bank = false;
		controller.observationChanged();
		controller.tick();
		assertEquals(before, savedBytes());
		assertEquals(SetupView.Mode.ACTIVE, view().getMode());
	}

	private static final class PhasedSource implements SnapshotSource, BankSnapshotReader.Access
	{
		private final BankSnapshotReader reader = new BankSnapshotReader(this);
		private boolean bank;
		private boolean inventory = true;
		private boolean equipment = true;
		public String characterKey() { return SnapshotFixtures.CHARACTER_A; }
		public void beginInitialization() { reader.reset(); }
		public void endInitialization() { reader.reset(); }
		public CaptureProgress progress() { return reader.progress(); }
		public LegacySnapshot capture(String expected) { return reader.capture(); }
		public boolean isInterfaceOpen(int id) { return bank && id == InterfaceID.BANKMAIN; }
		public boolean isVisible(int id) { return bank && id == InterfaceID.Bankmain.ITEMS_CONTAINER; }
		public List<ObservedItem> items(int id)
		{
			if (id == InventoryID.BANK) { return bank ? Collections.emptyList() : null; }
			if (id == InventoryID.INV) { return inventory ? Collections.emptyList() : null; }
			if (id == InventoryID.WORN) { return equipment ? Collections.emptyList() : null; }
			throw new AssertionError("Unexpected container");
		}
	}

	private static void drain(Queue<Runnable> tasks)
	{
		while (!tasks.isEmpty()) { tasks.remove().run(); }
	}

	private static final class FakeSource implements SnapshotSource
	{
		private String character = SnapshotFixtures.CHARACTER_A;
		private boolean bankOpen = true;
		private boolean dataAvailable = true;
		private long coins = 10;
		private int captures;
		public String characterKey() { return character; }
		public LegacySnapshot capture(String expectedCharacter)
		{
			if (!bankOpen || !expectedCharacter.equals(character)) { throw new IllegalStateException("Open the bank for this character"); }
			if (!dataAvailable) { throw new IllegalStateException("Bank container data is unavailable; retrying automatically"); }
			captures++;
			return SnapshotFixtures.snapshot(coins);
		}
	}

	private static final class ManualExecutor extends AbstractExecutorService
	{
		private final Queue<Runnable> tasks = new ArrayDeque<>();
		private boolean stopped;
		void runAll() { drain(tasks); }
		public void execute(Runnable command) { if (stopped) { throw new RejectedExecutionException(); } tasks.add(command); }
		public void shutdown() { stopped = true; }
		public List<Runnable> shutdownNow() { stopped = true; List<Runnable> cancelled = new ArrayList<>(tasks); tasks.clear(); return cancelled; }
		public boolean isShutdown() { return stopped; }
		public boolean isTerminated() { return stopped && tasks.isEmpty(); }
		public boolean awaitTermination(long timeout, TimeUnit unit) { return isTerminated(); }
	}
}
