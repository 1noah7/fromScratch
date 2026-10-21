package com.fromscratch.controller;

import com.fromscratch.model.InitializationSession;
import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.RunRecord;
import com.fromscratch.model.RunStatus;
import com.fromscratch.observation.RuneLiteSnapshotObserver;
import com.fromscratch.observation.SnapshotSource;
import com.fromscratch.observation.CaptureProgress;
import com.fromscratch.persistence.RunRepository;
import com.fromscratch.persistence.SaveException;
import com.fromscratch.ui.FromScratchSidebar;
import com.fromscratch.ui.SetupActions;
import com.fromscratch.ui.SetupView;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Filepath;

/**
 * Coordinates client-thread observations and background-only storage. Epoch checks
 * discard stale load/save callbacks after logout, profile change, or plugin disable.
 * Acknowledged activation always follows durable storage, never the reverse.
 */
@Singleton
public final class SetupController implements SetupActions
{
	private final SnapshotSource source;
	private final RunRepository repository;
	private final Consumer<Runnable> clientDispatch;
	private final Consumer<SetupView> display;
	private final AtomicLong lifecycle = new AtomicLong();
	private volatile boolean enabled;
	private volatile ExecutorService worker;
	private DirectoryProvider directoryProvider;
	// The remaining fields are confined to the client dispatcher.
	private String character;
	private long context;
	private InitializationSession session;
	private boolean busy;
	private SetupView lastView;

	@Inject
	public SetupController(RuneLiteSnapshotObserver source, RunRepository repository,
		ClientThread clientThread, FromScratchSidebar sidebar)
	{
		this(source, repository, clientThread::invokeLater, sidebar::update);
	}

	SetupController(SnapshotSource source, RunRepository repository,
		Consumer<Runnable> clientDispatch, Consumer<SetupView> display)
	{
		this.source = source;
		this.repository = repository;
		this.clientDispatch = clientDispatch;
		this.display = display;
	}

	public void start(DirectoryProvider directoryProvider)
	{
		ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(8), task ->
			{
				Thread thread = new Thread(task, "fromscratch-storage");
				thread.setDaemon(true);
				return thread;
			});
		start(directoryProvider, executor);
	}

	// Controlled executors let tests reproduce interruption and stale callback orderings.
	void start(DirectoryProvider provider, ExecutorService executor)
	{
		directoryProvider = provider;
		worker = executor;
		enabled = true;
		long epoch = lifecycle.incrementAndGet();
		clientDispatch.accept(() ->
		{
			if (!enabled || lifecycle.get() != epoch) { return; }
			character = null;
			session = null;
			busy = false;
			lastView = null;
			context++;
			displayView(SetupView.signedOut());
			tick();
		});
	}

	public void stop()
	{
		enabled = false;
		lifecycle.incrementAndGet();
		ExecutorService executor = worker;
		if (executor != null) { executor.shutdownNow(); }
	}

	/** Poll only pending setup; valid cached personal observations must expire even without events. */
	public void tick()
	{
		if (!enabled) { return; }
		String observed = source.characterKey();
		if (!Objects.equals(observed, character))
		{
			source.endInitialization();
			character = observed;
			context++;
			session = null;
			busy = false;
			if (character == null) { displayView(SetupView.signedOut()); }
			else { load(); }
			return;
		}
		if (!busy && session != null && session.isInitializing())
		{
			try
			{
				session.observe(source.capture(character));
				publish(SetupView.Mode.INITIALIZING, "Complete snapshot available. Review the summary before confirming.");
			}
			catch (IllegalStateException | IllegalArgumentException ex)
			{
				session.invalidatePreview();
				// Retry unavailable data on game ticks, but do not enqueue an
				// identical Swing rebuild every tick while the player is waiting.
				publish(SetupView.Mode.INITIALIZING, ex.getMessage());
			}
		}
	}

	public void observationChanged()
	{
		if (!busy && session != null && session.isInitializing())
		{
			session.invalidatePreview();
			publish(SetupView.Mode.INITIALIZING, "Checking updated containers. Wait for a fresh preview before confirming.");
		}
	}

	@Override
	public void begin()
	{
		dispatch(() ->
		{
			if (busy || session == null) { return; }
			session.begin();
			source.beginInitialization();
			publish(SetupView.Mode.INITIALIZING, "Checking bank availability. Nothing is saved until confirmation.");
			tick();
		});
	}

	@Override
	public void confirm(UUID reviewToken)
	{
		dispatch(() ->
		{
			if (busy || session == null) { return; }
			LegacySnapshot fresh;
			try { fresh = source.capture(character); }
			catch (IllegalStateException ex) { session.invalidatePreview(); throw ex; }
			RunRecord candidate = session.prepareActivation(reviewToken, fresh, Instant.now());
			busy = true;
			publish(SetupView.Mode.SAVING, "Saving the confirmed snapshot. Wait for Active before closing the client.");
			submit(root -> repository.activate(root, candidate), saved ->
			{
				session.committed(saved);
				source.endInitialization();
				publish(SetupView.Mode.ACTIVE, "Legacy baseline saved.");
			});
		});
	}

	@Override
	public void cancel()
	{
		dispatch(() ->
		{
			if (!busy && session != null && session.isInitializing())
			{
				session.cancel();
				source.endInitialization();
				publish(SetupView.Mode.NOT_STARTED, "Setup cancelled. No snapshot was saved.");
			}
		});
	}

	@Override
	public void reset(UUID runId)
	{
		dispatch(() ->
		{
			if (busy || session == null || session.getSaved() == null
				|| !runId.equals(session.getSaved().getState().getRunId())) { return; }
			String selectedCharacter = character;
			busy = true;
			publish(SetupView.Mode.SAVING, "Backing up the active run before reset. Wait for confirmation.");
			submit(root -> repository.reset(root, selectedCharacter, runId), saved ->
			{
				session.committed(saved);
				publish(SetupView.Mode.NOT_STARTED, "Previous run archived. A new run requires a fresh snapshot.");
			});
		});
	}

	@Override
	public void retry()
	{
		dispatch(() ->
		{
			if (busy) { return; }
			context++;
			session = null;
			load();
		});
	}

	private void load()
	{
		busy = true;
		displayView(new SetupView(SetupView.Mode.LOADING, "Loading this character's saved run...", null, null, null));
		String selectedCharacter = character;
		submit(root -> repository.load(root, selectedCharacter), saved ->
		{
			session = new InitializationSession(selectedCharacter, saved.orElse(null));
			boolean active = saved.isPresent() && saved.get().getState().getStatus() == RunStatus.ACTIVE;
			publish(active ? SetupView.Mode.ACTIVE : SetupView.Mode.NOT_STARTED,
				active ? "Original legacy baseline restored." : "No saved active run.");
		});
	}

	private void dispatch(Runnable action)
	{
		long epoch = lifecycle.get();
		clientDispatch.accept(() ->
		{
			if (!enabled || lifecycle.get() != epoch) { return; }
			if (character == null || !character.equals(source.characterKey())) { tick(); return; }
			try { action.run(); }
			catch (IllegalStateException | IllegalArgumentException ex)
			{
				publish(session != null && session.isInitializing() ? SetupView.Mode.INITIALIZING : SetupView.Mode.ERROR, ex.getMessage());
			}
		});
	}

	private <T> void submit(StorageAction<T> action, Consumer<T> success)
	{
		long epoch = lifecycle.get();
		long generation = context;
		DirectoryProvider provider = directoryProvider;
		try
		{
			worker.execute(() ->
			{
				try
				{
					T result = action.execute(provider.get());
					clientDispatch.accept(() ->
					{
						if (!enabled || lifecycle.get() != epoch || context != generation) { return; }
						busy = false;
						if (!Objects.equals(character, source.characterKey())) { tick(); return; }
						success.accept(result);
					});
				}
				catch (Exception ex)
				{
					String message = ex instanceof SaveException ? ex.getMessage()
						: "Could not access run storage. No empty replacement was written; check permissions and retry.";
					clientDispatch.accept(() ->
					{
						if (!enabled || lifecycle.get() != epoch || context != generation) { return; }
						busy = false;
						if (!Objects.equals(character, source.characterKey())) { tick(); return; }
						publish(SetupView.Mode.ERROR, message);
					});
				}
			});
		}
		catch (RejectedExecutionException ex)
		{
			busy = false;
			publish(SetupView.Mode.ERROR, "Storage worker is unavailable. Reload or re-enable the plugin.");
		}
	}

	private void publish(SetupView.Mode mode, String message)
	{
		LegacySnapshot preview = session == null ? null : session.getPreview();
		CaptureProgress progress = source.progress();
		if (preview != null && progress.getContainers().isEmpty())
		{
			progress = CaptureProgress.from(preview);
		}
		SetupView view = new SetupView(mode, message, session == null ? null : session.getSaved(),
			preview, session == null ? null : session.getReviewToken(), progress);
		// Polling is needed to expire observations even without events. Unchanged
		// immutable views must not rebuild Swing on every game tick.
		displayView(view);
	}

	private void displayView(SetupView view)
	{
		if (!view.equals(lastView)) { display.accept(view); lastView = view; }
	}

	@FunctionalInterface
	public interface DirectoryProvider { Filepath get() throws IOException; }

	@FunctionalInterface
	private interface StorageAction<T> { T execute(Filepath root) throws IOException; }
}
