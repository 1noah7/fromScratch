package com.fromscratch.persistence;

import com.fromscratch.model.RunRecord;
import com.fromscratch.model.RunStatus;
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

	@Inject
	public RunRepository(RunRecordCodec codec) { this(codec, RunRepository::publish); }

	// Publication is the filesystem failure boundary used by deterministic recovery tests.
	RunRepository(RunRecordCodec codec, Publication publication)
	{
		this.codec = codec;
		this.publication = publication;
	}

	public Optional<RunRecord> load(Filepath root, String character) throws IOException
	{
		return locked(root, character, directory ->
		{
			Optional<RunRecord> existing = readCurrent(directory, character);
			if (!existing.isPresent()) { return existing; }
			Filepath current = directory.joinSegment("current.json");
			if (readDocument(current, character).version == RunRecordCodec.FORMAT_VERSION) { return existing; }
			// A known M2 save has no virtual assets. Archive the exact validated
			// bytes before adding empty ownership; never repair an invalid save.
			Filepath backup = directory.joinSegment("migration-v1-" + UUID.randomUUID() + ".json");
			current.copyTo(backup);
			force(backup);
			if (!read(backup, character).equals(existing.get())) { throw new SaveException("Migration backup verification failed. Existing files were preserved."); }
			RunRecord old = existing.get();
			RunRecord migrated = new RunRecord(old.getState(), UUID.randomUUID(), old.getPreviousRunId(), old.getSnapshot(), old.getOwnership());
			commit(directory, migrated);
			return Optional.of(migrated);
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
				|| path.getFileName().startsWith("migration-")))
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
		return codec.decodeSave(data.toString(), character);
	}

	private static void writeNew(Filepath file, String data) throws IOException
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
