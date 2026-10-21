package com.fromscratch.persistence;

import com.fromscratch.model.RunRecord;
import com.fromscratch.model.RunStatus;
import com.fromscratch.tracking.AccountingPlan;
import com.fromscratch.tracking.ItemProgress;
import com.fromscratch.tracking.ItemTransaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.inject.Inject;
import net.runelite.client.util.Filepath;

/**
 * Background-only checkpoint repository. A forced immutable checkpoint precedes
 * replacement of current.json. Interrupted replacement preserves recovery evidence
 * and never turns an unreadable save into a new empty run.
 */
public final class RunRepository
{
	private static final long MAX_SAVE_BYTES = 4 * 1024 * 1024;
	private final RunRecordCodec codec;
	private final Publication publication;
	private final ItemJournalStore journal;

	@Inject
	public RunRepository(RunRecordCodec codec) { this(codec, RunRepository::publish); }

	// Publication is the filesystem failure boundary used by deterministic recovery tests.
	RunRepository(RunRecordCodec codec, Publication publication)
	{
		this.codec = codec;
		this.publication = publication;
		this.journal = new ItemJournalStore(codec);
	}

	public Optional<RunRecord> load(Filepath root, String character) throws IOException
	{
		return locked(root, character, directory ->
		{
			Optional<RunRecord> existing = readCurrent(directory, character);
			if (!existing.isPresent()) { return existing; }
			Filepath current = directory.joinSegment("current.json");
			int version = readDocument(current, character).version;
			if (version == RunRecordCodec.FORMAT_VERSION) { journal.verifyChain(directory, existing.get()); return existing; }
			// Existing journal result digests use their original format-3 encoding.
			// Verify that chain before migration, then append a linked migration marker
			// instead of rewriting old segments or breaking their head/revision pair.
			if (version == 3) { journal.verifyChain(directory, existing.get(), 3); }
			// Archive exact verified M2/M3 bytes before changing the envelope. Only
			// M2 may introduce zero ownership; the codec preserves M3 allocations.
			Filepath backup = directory.joinSegment("migration-v" + version + "-" + UUID.randomUUID() + ".json");
			current.copyTo(backup);
			force(backup);
			if (!read(backup, character).equals(existing.get())) { throw new SaveException("Migration backup verification failed. Existing files were preserved."); }
			RunRecord old = existing.get();
			RunRecord migrated;
			if (old.getItemProgress() != null && old.getItemProgress().isEstablished())
			{
				ItemProgress prior = old.getItemProgress();
				ItemTransaction marker = new ItemTransaction(UUID.randomUUID(), prior.getObservedAt(), -1, 0, 0, Collections.emptyMap(),
					null, null, ItemTransaction.Category.RECONCILIATION_ADJUSTMENT, ItemTransaction.Confidence.OBSERVED,
					"Save upgraded for legacy withdrawal counters; previous ownership and history preserved.");
				List<ItemTransaction> recent = new ArrayList<>(prior.getRecent()); recent.add(0, marker);
				if (recent.size() > ItemProgress.RECENT_LIMIT) { recent.remove(recent.size() - 1); }
				ItemProgress progress = new ItemProgress(prior.getPhysical(), prior.getReturns(), prior.getUnlocked(), recent, prior.getAcquisitions(),
					prior.getSessionId(), prior.getTick(), prior.getObservedAt(), UUID.randomUUID(), Math.incrementExact(prior.getSequence()), true,
					prior.getLegacyDeficits());
				migrated = old.withItems(new AccountingPlan(old.getOwnership(), progress, Collections.singletonList(marker), false, "Save upgraded."));
				journal.append(directory, old, migrated, Collections.singletonList(marker));
			}
			else { migrated = new RunRecord(old.getState(), UUID.randomUUID(), old.getPreviousRunId(), old.getSnapshot(), old.getOwnership(), old.getItemProgress()); }
			commit(directory, migrated);
			return Optional.of(migrated);
		});
	}

	/** Compare-and-swap a complete item batch. A crash can leave an orphan journal file, never half an unlock. */
	public RunRecord saveItems(Filepath root, RunRecord expected, AccountingPlan plan) throws IOException
	{
		RunRecord next = expected.withItems(plan);
		return locked(root, expected.getState().getCharacterKey(), directory ->
		{
			RunRecord current = readCurrent(directory, expected.getState().getCharacterKey()).orElseThrow(() -> new SaveException("No active run was found."));
			if (current.equals(next)) { journal.verifyHead(directory, current); return current; }
			if (current.getState().getStatus() != RunStatus.ACTIVE || !current.equals(expected)
				|| !current.getState().getRunId().equals(expected.getState().getRunId())) { throw new SaveException("Saved progression changed; reload and reconcile before retrying."); }
			journal.verifyHead(directory, current);
			journal.append(directory, expected, next, plan.getTransactions());
			commit(directory, next);
			return next;
		});
	}

	public RunRecord activate(Filepath root, RunRecord candidate) throws IOException
	{
		String character = candidate.getState().getCharacterKey();
		return locked(root, character, directory ->
		{
			if (candidate.getState().getStatus() != RunStatus.ACTIVE) { throw new SaveException("Activation requires a complete snapshot."); }
			Optional<RunRecord> saved = readCurrent(directory, character);
			if (saved.isPresent() && saved.get().getState().getStatus() == RunStatus.ACTIVE)
			{
				if (saved.get().equals(candidate)) { return saved.get(); }
				throw new SaveException("An active run already exists. It was not overwritten.");
			}
			commit(directory, candidate);
			return candidate;
		});
	}

	public RunRecord reset(Filepath root, String character, UUID expectedRun) throws IOException
	{
		return locked(root, character, directory ->
		{
			RunRecord active = readCurrent(directory, character).orElseThrow(() -> new SaveException("There is no saved run to reset."));
			if (active.getState().getStatus() == RunStatus.NOT_STARTED && expectedRun.equals(active.getPreviousRunId())) { return active; }
			if (!expectedRun.equals(active.getState().getRunId())) { throw new SaveException("The saved run changed. Reload before resetting."); }
			Filepath archive = directory.joinSegment("archive-" + expectedRun + "-" + UUID.randomUUID() + ".json");
			// Read/validate/force the archival copy before any replacement is attempted.
			directory.joinSegment("current.json").copyTo(archive);
			force(archive);
			if (!read(archive, character).equals(active)) { throw new SaveException("Backup verification failed. The active run was preserved."); }
			RunRecord reset = RunRecord.reset(active);
			commit(directory, reset);
			return reset;
		});
	}

	private Optional<RunRecord> readCurrent(Filepath directory, String character) throws IOException
	{
		Filepath current = directory.joinSegment("current.json");
		if (current.exists()) { return Optional.of(read(current, character)); }
		try (Stream<Filepath> files = directory.walk(1))
		{
			if (files.anyMatch(path -> path.getFileName().startsWith("checkpoint-")
				|| path.getFileName().startsWith("pending-") || path.getFileName().startsWith("archive-")
				|| path.getFileName().startsWith("migration-") || path.getFileName().startsWith("ledger-")))
			{
				throw new SaveException("Current save is missing, but recovery files exist. Files were preserved; restore a checkpoint before starting.");
			}
		}
		return Optional.empty();
	}

	private void commit(Filepath directory, RunRecord record) throws IOException
	{
		String encoded = codec.encode(record);
		Filepath checkpoint = directory.joinSegment("checkpoint-" + record.getRevision() + ".json");
		writeNew(checkpoint, encoded);
		if (!read(checkpoint, record.getState().getCharacterKey()).equals(record)) { throw new SaveException("Checkpoint verification failed."); }
		Filepath pending = directory.joinSegment("pending-" + record.getRevision() + ".json");
		writeNew(pending, encoded);
		Filepath current = directory.joinSegment("current.json");
		publication.replace(pending, current);
		if (!read(current, record.getState().getCharacterKey()).equals(record)) { throw new SaveException("Save verification failed. Recovery checkpoints were retained."); }
	}

	private static void publish(Filepath pending, Filepath current) throws IOException
	{
		try
		{
			pending.moveTo(current, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (AtomicMoveNotSupportedException ex)
		{
			// The forced checkpoint remains recoverable even if this fallback copy
			// is interrupted. An invalid current file is surfaced as an error on load.
			pending.copyTo(current, StandardCopyOption.REPLACE_EXISTING);
			force(current);
			pending.delete();
		}
	}

	private RunRecord read(Filepath file, String character) throws IOException
	{
		return readDocument(file, character).record;
	}

	private RunRecordCodec.DecodedSave readDocument(Filepath file, String character) throws IOException
	{
		return codec.decodeSave(readText(file), character);
	}

	static String readText(Filepath file) throws IOException
	{
		if (file.size() > MAX_SAVE_BYTES) { throw new SaveException("Save exceeds the supported size. It was not modified."); }
		StringBuilder data = new StringBuilder();
		try (Reader reader = file.openReader())
		{
			char[] buffer = new char[4096];
			int length;
			while ((length = reader.read(buffer)) != -1)
			{
				if (data.length() + length > MAX_SAVE_BYTES) { throw new SaveException("Save exceeds the supported size."); }
				data.append(buffer, 0, length);
			}
		}
		return data.toString();
	}

	static void writeNew(Filepath file, String data) throws IOException
	{
		if (Thread.currentThread().isInterrupted()) { throw new SaveException("Save interrupted. Existing recovery files were retained."); }
		byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
		if (bytes.length > MAX_SAVE_BYTES) { throw new SaveException("Snapshot exceeds the supported save size."); }
		try (FileChannel channel = file.openFileChannel(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))
		{
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			while (buffer.hasRemaining()) { channel.write(buffer); }
			channel.force(true);
		}
	}

	private static void force(Filepath file) throws IOException
	{
		try (FileChannel channel = file.openFileChannel(StandardOpenOption.WRITE)) { channel.force(true); }
	}

	private <T> T locked(Filepath root, String character, DirectoryTask<T> task) throws IOException
	{
		if (character == null || !character.matches("[a-f0-9]{64}")) { throw new SaveException("Character identity is invalid; storage is blocked."); }
		try
		{
			Filepath directory = root.joinSegment("characters").joinSegment(character);
			directory.createDirectories();
			try (FileChannel channel = directory.joinSegment("run.lock").openFileChannel(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
				FileLock lock = channel.tryLock())
			{
				if (lock == null) { throw new SaveException("Run data is busy in another client. Close that client and retry."); }
				return task.execute(directory);
			}
		}
		catch (SaveException ex) { throw ex; }
		catch (OverlappingFileLockException ex) { throw new SaveException("Run data is busy. Retry after the previous operation finishes."); }
		catch (IOException ex) { throw new SaveException("Could not access or save run data. Check storage permissions and retry; recovery files were preserved."); }
	}

	@FunctionalInterface
	private interface DirectoryTask<T> { T execute(Filepath directory) throws IOException; }

	@FunctionalInterface
	interface Publication { void replace(Filepath pending, Filepath current) throws IOException; }
}
