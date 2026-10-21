package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.*;
import com.fromscratch.tracking.*;
import com.google.gson.*;
import java.util.*;
import java.util.stream.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.tracking.TrackingFixtures.*;

public class LegacyDeficitPersistenceTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private Filepath root;
	private RunRepository repository;
	private RunRecordCodec codec;
	@Before public void start()
	{ root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath()); codec = SnapshotFixtures.codec(); repository = new RunRepository(codec); }
	private Filepath directory() { return root.join("characters", SnapshotFixtures.CHARACTER_A); }
	private Filepath current() { return directory().joinSegment("current.json"); }
	private RunRecord initial() throws Exception
	{
		RunRecord run = RunRecord.activate(SnapshotFixtures.CHARACTER_A, UUID.randomUUID(), snapshot(holdings(100, 0, 0)), TIME);
		repository.activate(root, run);
		return repository.saveItems(root, run, ENGINE.plan(run, observation(holdings(100, 0, 0), 1), id -> id, true, false, Collections.emptyMap(), ""));
	}
	@Test public void deficitsPersistWithoutNegativeOwnershipAndStayCharacterIsolated() throws Exception
	{
		RunRecord before = initial(); RunRecord withdrawn = repository.saveItems(root, before, plan(before, holdings(98, 2, 0), 2));
		RunRecord loaded = repository.load(root, SnapshotFixtures.CHARACTER_A).get();
		assertEquals(withdrawn, loaded); assertEquals(2, loaded.getItemProgress().getLegacyDeficits().quantity(ItemID.SHARK));
		assertEquals(0, loaded.getOwnership().total(ItemID.SHARK)); assertEquals(before.getSnapshot(), loaded.getSnapshot());
		assertFalse(repository.load(root, SnapshotFixtures.CHARACTER_B).isPresent());
	}
	@Test public void resolveDeficitIsAnAuditedDecisionWithoutLifetimeAcquisition() throws Exception
	{
		RunRecord before = initial(); RunRecord withdrawn = repository.saveItems(root, before, plan(before, holdings(98, 2, 0), 2));
		ItemReviewEdits edits = new ItemReviewEdits(Collections.emptyMap(), Collections.singletonMap(ItemID.SHARK, 0L), Collections.emptySet(),
			Collections.emptySet(), "Confirmed original copies permanently consumed.");
		AccountingPlan corrected = ENGINE.plan(withdrawn, observation(holdings(98, 2, 0), 3), id -> id, true, false, edits.getOwnership(), edits.getReason(), edits);
		RunRecord saved = repository.saveItems(root, withdrawn, corrected);
		assertEquals(0, saved.getItemProgress().getLegacyDeficits().quantity(ItemID.SHARK)); assertEquals(0, saved.getEconomy().getItemsUnlocked());
		assertTrue(corrected.getTransactions().stream().anyMatch(entry -> entry.getLegacyWithdrawalChange() == -2 && entry.getConfidence() == ItemTransaction.Confidence.MANUAL));
		assertEquals(saved, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}
	@Test public void unbackedDeficitChangeCannotBypassJournalValidation() throws Exception
	{
		RunRecord before = initial(); AccountingPlan valid = plan(before, holdings(99, 1, 0), 2);
		List<ItemTransaction> omitted = valid.getTransactions().stream().filter(entry -> entry.getLegacyWithdrawalChange() == 0).collect(Collectors.toList());
		try { repository.saveItems(root, before, new AccountingPlan(valid.getOwnership(), valid.getProgress(), omitted, false, "Invalid test fixture.")); fail(); }
		catch (SaveException expected) { }
		assertEquals(before, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}
	@Test public void establishedFormatThreeSaveMigratesWithHistoryAndExactBytesPreserved() throws Exception
	{
		RunRecord before = initial(); before = repository.saveItems(root, before, plan(before, holdings(100, 3, 0), 2));
		String legacy = installFormatThreeFixture();
		RunRecord loaded = repository.load(root, SnapshotFixtures.CHARACTER_A).get();
		assertEquals(before.getState(), loaded.getState()); assertEquals(before.getSnapshot(), loaded.getSnapshot());
		assertEquals(before.getOwnership(), loaded.getOwnership()); assertEquals(before.getItemProgress().getAcquisitions(), loaded.getItemProgress().getAcquisitions());
		assertEquals(before.getItemProgress().getUnlocked(), loaded.getItemProgress().getUnlocked());
		assertEquals(before.getItemProgress().getSequence() + 1, loaded.getItemProgress().getSequence());
		assertTrue(loaded.getItemProgress().getLegacyDeficits().getItems().isEmpty());
		try (Stream<Filepath> files = directory().walk(1))
		{ assertEquals(legacy, RunRepository.readText(files.filter(file -> file.getFileName().startsWith("migration-v3-")).findFirst().get())); }
		assertEquals(loaded, new RunRepository(SnapshotFixtures.codec()).load(root, SnapshotFixtures.CHARACTER_A).get());
	}
	@Test public void corruptOldJournalBlocksMigrationWithoutOverwritingOrBackupOfInvalidData() throws Exception
	{
		RunRecord before = initial(); String legacy = installFormatThreeFixture();
		Filepath segment = directory().joinSegment("ledger-" + before.getState().getRunId() + "-" + before.getRevision() + ".json"); segment.write("{corrupt");
		try { repository.load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (SaveException expected) { }
		assertEquals(legacy, RunRepository.readText(current())); assertEquals("{corrupt", RunRepository.readText(segment));
		try (Stream<Filepath> files = directory().walk(1)) { assertFalse(files.anyMatch(file -> file.getFileName().startsWith("migration-v3-"))); }
	}
	@Test public void interruptedFormatThreeMigrationRetainsOldChainAndCanRetrySafely() throws Exception
	{
		initial(); String legacy = installFormatThreeFixture();
		RunRepository interrupted = new RunRepository(codec, (pending, current) -> { throw new java.io.IOException("Test interruption"); });
		try { interrupted.load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (java.io.IOException expected) { }
		assertEquals(legacy, RunRepository.readText(current()));
		RunRecord loaded = repository.load(root, SnapshotFixtures.CHARACTER_A).get(); assertEquals(2, loaded.getItemProgress().getSequence());
		assertEquals(loaded, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}
	@Test public void missingFormatFourDeficitsAreCorruptionNotImplicitZeros() throws Exception
	{
		initial(); JsonObject envelope = json(RunRepository.readText(current())); JsonObject data = json(envelope.get("payload").getAsString());
		data.getAsJsonObject("itemProgress").remove("legacyWithdrawals"); envelope.addProperty("payload", data.toString());
		envelope.addProperty("sha256", SaveIntegrity.sha256(data.toString())); current().write(envelope.toString()); String original = RunRepository.readText(current());
		try { repository.load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (SaveException expected) { }
		assertEquals(original, RunRepository.readText(current()));
	}
	/** Construct old wire trees independently of encodeVersion: remove only fields absent in the accepted M4 format. */
	private String installFormatThreeFixture() throws Exception
	{
		Map<String, String> digests = new HashMap<>(); List<Filepath> files;
		try (Stream<Filepath> stream = directory().walk(1)) { files = stream.collect(Collectors.toList()); }
		for (Filepath file : files)
		{
			if (!file.getFileName().startsWith("checkpoint-")) { continue; }
			String old = oldCheckpoint(RunRepository.readText(file)); JsonObject payload = json(json(old).get("payload").getAsString());
			digests.put(payload.get("revision").getAsString(), SaveIntegrity.sha256(old));
		}
		for (Filepath file : files)
		{
			if (!file.getFileName().startsWith("ledger-")) { continue; }
			JsonObject envelope = json(RunRepository.readText(file)); JsonObject data = json(envelope.get("payload").getAsString());
			stripEntries(data.getAsJsonArray("entries")); data.addProperty("resultSha256", digests.get(data.get("id").getAsString()));
			envelope.addProperty("version", 1); envelope.addProperty("payload", data.toString()); envelope.addProperty("sha256", SaveIntegrity.sha256(data.toString())); file.write(envelope.toString());
		}
		String old = oldCheckpoint(RunRepository.readText(current())); current().write(old); return old;
	}
	private String oldCheckpoint(String text)
	{
		JsonObject envelope = json(text); JsonObject data = json(envelope.get("payload").getAsString()); JsonObject progress = data.getAsJsonObject("itemProgress");
		progress.remove("legacyWithdrawals"); stripEntries(progress.getAsJsonArray("recent")); stripEntries(progress.getAsJsonArray("acquisitions"));
		envelope.addProperty("formatVersion", 3); envelope.addProperty("payload", data.toString()); envelope.addProperty("sha256", SaveIntegrity.sha256(data.toString())); return envelope.toString();
	}
	private static void stripEntries(JsonArray array) { for (JsonElement value : array) { value.getAsJsonObject().remove("legacyWithdrawalChange"); } }
	private static JsonObject json(String text) { return SnapshotFixtures.gson().fromJson(text, JsonObject.class); }
}
