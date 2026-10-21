package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.RunRecord;
import com.fromscratch.model.RunStatus;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.runelite.client.util.Filepath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class RunRepositoryTest
{
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();
	private Filepath root;
	private RunRepository repository;

	@Before
	public void setUp()
	{
		// Test-only root establishment. Every operation below uses Filepath; this
		// escape hatch is never used in production or pointed at real account data.
		root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		repository = new RunRepository(SnapshotFixtures.codec());
	}

	private Filepath directory(String character) { return root.join("characters", character); }
	private Filepath current() { return directory(SnapshotFixtures.CHARACTER_A).join("current.json"); }
	private RunRecord active() { return SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 2000000); }
	private List<Filepath> files(String prefix) throws IOException
	{
		try (Stream<Filepath> files = directory(SnapshotFixtures.CHARACTER_A).walk(1))
		{
			return files.filter(file -> file.getFileName().startsWith(prefix)).collect(Collectors.toList());
		}
	}

	@Test
	public void missingSaveReturnsNotStartedWithoutWritingBlankCurrentData() throws Exception
	{
		assertFalse(repository.load(root, SnapshotFixtures.CHARACTER_A).isPresent());
		assertFalse(current().exists());
	}

	@Test
	public void saveAndReloadPreserveAllBaselineAndIdentityFields() throws Exception
	{
		RunRecord run = active();
		repository.activate(root, run);
		assertEquals(run, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
		assertEquals(1, files("checkpoint-").size());
	}

	@Test
	public void newRepositoryAfterRestartLoadsEquivalentRun() throws Exception
	{
		RunRecord run = active();
		repository.activate(root, run);
		assertEquals(run, new RunRepository(SnapshotFixtures.codec()).load(root, SnapshotFixtures.CHARACTER_A).get());
	}

	@Test
	public void separateCharactersKeepIndependentSnapshots() throws Exception
	{
		RunRecord a = active();
		RunRecord b = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_B, 7);
		repository.activate(root, a);
		repository.activate(root, b);
		assertEquals(a, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
		assertEquals(b, repository.load(root, SnapshotFixtures.CHARACTER_B).get());
	}

	@Test
	public void duplicateActivationCannotOverwriteAnActiveBaseline() throws Exception
	{
		RunRecord original = active();
		repository.activate(root, original);
		expectFailure(() -> repository.activate(root, active()), "already exists");
		assertEquals(original, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}

	@Test
	public void retryingTheExactCommittedActivationIsIdempotent() throws Exception
	{
		RunRecord original = active();
		repository.activate(root, original);
		assertEquals(original, repository.activate(root, original));
		assertEquals(1, files("checkpoint-").size());
	}

	@Test
	public void corruptCurrentSaveIsPreservedAndCannotBeOverwritten() throws Exception
	{
		repository.activate(root, active());
		current().write("{broken", StandardOpenOption.TRUNCATE_EXISTING);
		long damagedSize = current().size();
		expectFailure(() -> repository.load(root, SnapshotFixtures.CHARACTER_A), "corrupt");
		expectFailure(() -> repository.activate(root, active()), "corrupt");
		assertEquals(damagedSize, current().size());
		assertEquals(1, files("checkpoint-").size());
	}

	@Test
	public void missingCurrentWithRecoveryEvidenceIsNotTreatedAsANewCharacter() throws Exception
	{
		repository.activate(root, active());
		current().delete();
		expectFailure(() -> repository.load(root, SnapshotFixtures.CHARACTER_A), "recovery files exist");
		assertFalse(current().exists());
	}

	@Test
	public void copiedSaveFromAnotherCharacterIsRejectedWithoutChanges() throws Exception
	{
		repository.activate(root, active());
		repository.load(root, SnapshotFixtures.CHARACTER_B);
		current().copyTo(directory(SnapshotFixtures.CHARACTER_B).join("current.json"));
		expectFailure(() -> repository.load(root, SnapshotFixtures.CHARACTER_B), "different character");
	}

	@Test
	public void resetArchivesTheOriginalBeforeReturningNotStarted() throws Exception
	{
		RunRecord original = active();
		repository.activate(root, original);
		RunRecord reset = repository.reset(root, SnapshotFixtures.CHARACTER_A, original.getState().getRunId());
		assertEquals(RunStatus.NOT_STARTED, reset.getState().getStatus());
		assertNull(reset.getState().getRunId());
		assertNull(reset.getSnapshot());
		assertEquals(reset, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
		List<Filepath> archives = files("archive-");
		assertEquals(1, archives.size());
		try (java.io.Reader reader = archives.get(0).openReader())
		{
			StringBuilder data = new StringBuilder();
			char[] buffer = new char[1024];
			int length;
			while ((length = reader.read(buffer)) != -1) { data.append(buffer, 0, length); }
			assertEquals(original, SnapshotFixtures.codec().decode(data.toString(), SnapshotFixtures.CHARACTER_A));
		}
	}

	@Test
	public void repeatedResetDoesNotCreateAnotherRunOrArchive() throws Exception
	{
		RunRecord original = active();
		repository.activate(root, original);
		RunRecord reset = repository.reset(root, SnapshotFixtures.CHARACTER_A, original.getState().getRunId());
		assertEquals(reset, repository.reset(root, SnapshotFixtures.CHARACTER_A, original.getState().getRunId()));
		assertEquals(1, files("archive-").size());
	}

	@Test
	public void resetRequiresTheCurrentlyReviewedRunId() throws Exception
	{
		RunRecord original = active();
		repository.activate(root, original);
		expectFailure(() -> repository.reset(root, SnapshotFixtures.CHARACTER_A, UUID.randomUUID()), "changed");
		assertEquals(original, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
		assertTrue(files("archive-").isEmpty());
	}

	@Test
	public void startingAfterResetRequiresAFreshExplicitActivation() throws Exception
	{
		RunRecord original = active();
		repository.activate(root, original);
		repository.reset(root, SnapshotFixtures.CHARACTER_A, original.getState().getRunId());
		RunRecord next = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 99);
		repository.activate(root, next);
		assertEquals(next, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
		assertEquals(1, files("archive-").size());
	}

	@Test
	public void interruptedFirstPublicationRetainsCheckpointAndBlocksBlankRestart() throws Exception
	{
		RunRepository failing = new RunRepository(SnapshotFixtures.codec(), (pending, current) -> { throw new IOException("Synthetic interruption"); });
		expectFailure(() -> failing.activate(root, active()), "recovery files were preserved");
		assertFalse(current().exists());
		assertEquals(1, files("checkpoint-").size());
		assertEquals(1, files("pending-").size());
		expectFailure(() -> repository.load(root, SnapshotFixtures.CHARACTER_A), "recovery files exist");
	}

	@Test
	public void interruptedResetPublicationPreservesActiveRunAndVerifiedArchive() throws Exception
	{
		RunRecord original = active();
		repository.activate(root, original);
		RunRepository failing = new RunRepository(SnapshotFixtures.codec(), (pending, current) -> { throw new IOException("Synthetic interruption"); });
		expectFailure(() -> failing.reset(root, SnapshotFixtures.CHARACTER_A, original.getState().getRunId()), "recovery files were preserved");
		assertEquals(original, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
		assertEquals(1, files("archive-").size());
	}

	@Test
	public void anotherClientLockPreventsResetWithoutModifyingTheRun() throws Exception
	{
		RunRecord original = active();
		repository.activate(root, original);
		try (FileChannel channel = directory(SnapshotFixtures.CHARACTER_A).join("run.lock").openFileChannel(StandardOpenOption.WRITE);
			FileLock ignored = channel.lock())
		{
			expectFailure(() -> repository.reset(root, SnapshotFixtures.CHARACTER_A, original.getState().getRunId()), "busy");
		}
		assertEquals(original, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}

	@Test
	public void unsafeCharacterFilenameIsRejectedBeforeCreatingDirectories() throws Exception
	{
		expectFailure(() -> repository.load(root, "../other"), "identity is invalid");
		assertFalse(root.join("characters").exists());
	}

	private static void expectFailure(IoAction action, String fragment) throws Exception
	{
		try { action.run(); fail("Expected safe persistence failure"); }
		catch (SaveException expected) { assertTrue(expected.getMessage(), expected.getMessage().contains(fragment)); }
	}

	@FunctionalInterface
	private interface IoAction { void run() throws Exception; }
}
