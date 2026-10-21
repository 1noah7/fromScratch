package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.*;
import com.fromscratch.observation.ItemObservation;
import com.fromscratch.tracking.*;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.stream.Stream;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static com.fromscratch.tracking.TrackingFixtures.*;

public class ItemJournalPersistenceTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private Filepath root;
	private RunRepository repository;
	private RunRecordCodec codec;
	@Before public void start()
	{
		root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath()); codec = SnapshotFixtures.codec(); repository = new RunRepository(codec);
	}
	private Filepath directory() { return root.join("characters", SnapshotFixtures.CHARACTER_A); }
	private Filepath current() { return directory().joinSegment("current.json"); }
	private RunRecord initial() throws Exception
	{
		RunRecord run = RunRecord.activate(SnapshotFixtures.CHARACTER_A, UUID.randomUUID(), snapshot(holdings(100, 0, 0)), TIME);
		repository.activate(root, run);
		AccountingPlan baseline = ENGINE.plan(run, observation(holdings(100, 0, 0), 1), id -> id, true, false, Collections.emptyMap(), "");
		return repository.saveItems(root, run, baseline);
	}
	@Test public void completeItemStateAndHistorySurviveRestart() throws Exception
	{
		RunRecord run = initial(); RunRecord earned = repository.saveItems(root, run, plan(run, holdings(100, 5, 0), 2));
		RunRecord loaded = new RunRepository(SnapshotFixtures.codec()).load(root, SnapshotFixtures.CHARACTER_A).get();
		assertEquals(earned, loaded); assertEquals(5, loaded.getOwnership().total(ItemID.SHARK)); assertEquals(1, loaded.getEconomy().getItemsUnlocked());
		assertEquals(1, loaded.getItemProgress().getAcquisitions().size());
	}
	@Test public void ledgerIsSeparateFromBoundedCheckpointAndRemainsComplete() throws Exception
	{
		RunRecord run = initial();
		for (int tick = 2; tick <= 55; tick++) { run = repository.saveItems(root, run, plan(run, holdings(100, tick, 0), tick)); }
		assertEquals(50, run.getItemProgress().getRecent().size()); assertEquals(50, run.getItemProgress().getAcquisitions().size());
		try (Stream<Filepath> files = directory().walk(1)) { assertEquals(55, files.filter(file -> file.getFileName().startsWith("ledger-")).count()); }
		assertEquals(run, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}
	@Test public void duplicateCommitIsIdempotent() throws Exception
	{
		RunRecord run = initial(); AccountingPlan candidate = plan(run, holdings(100, 5, 0), 2);
		RunRecord committed = repository.saveItems(root, run, candidate); assertEquals(committed, repository.saveItems(root, run, candidate));
		assertEquals(2, committed.getItemProgress().getSequence());
	}
	@Test public void staleRevisionCannotOverwriteProgression() throws Exception
	{
		RunRecord run = initial(); AccountingPlan first = plan(run, holdings(100, 5, 0), 2); repository.saveItems(root, run, first);
		String before = RunRepository.readText(current());
		try { repository.saveItems(root, run, plan(run, holdings(100, 6, 0), 3)); fail(); } catch (SaveException expected) { }
		assertEquals(before, RunRepository.readText(current()));
	}
	@Test public void interruptedPublicationKeepsOldCheckpointAndDoesNotReplayOrphan() throws Exception
	{
		RunRecord run = initial(); String original = RunRepository.readText(current());
		RunRepository failing = new RunRepository(codec, (pending, current) -> { throw new java.io.IOException("Simulated interruption"); });
		try { failing.saveItems(root, run, plan(run, holdings(100, 5, 0), 2)); fail(); } catch (SaveException expected) { }
		assertEquals(original, RunRepository.readText(current())); assertEquals(run, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
		assertEquals(0, repository.load(root, SnapshotFixtures.CHARACTER_A).get().getOwnership().total(ItemID.SHARK));
	}
	@Test public void missingJournalSegmentBlocksLoadingWithoutReplacingValidFiles() throws Exception
	{
		RunRecord run = initial(); String before = RunRepository.readText(current());
		directory().joinSegment("ledger-" + run.getState().getRunId() + "-" + run.getRevision() + ".json").delete();
		try { repository.load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (SaveException expected) { }
		assertEquals(before, RunRepository.readText(current()));
	}
	@Test public void corruptedJournalIsPreservedAndRejected() throws Exception
	{
		RunRecord run = initial(); Filepath journal = directory().joinSegment("ledger-" + run.getState().getRunId() + "-" + run.getRevision() + ".json");
		journal.write("{corrupt"); String current = RunRepository.readText(current());
		try { repository.load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (SaveException expected) { }
		assertEquals("{corrupt", RunRepository.readText(journal)); assertEquals(current, RunRepository.readText(current()));
	}
	@Test public void itemLedgerCannotCrossCharacters() throws Exception
	{
		RunRecord run = initial(); assertFalse(repository.load(root, SnapshotFixtures.CHARACTER_B).isPresent());
		RunRecord other = RunRecord.activate(SnapshotFixtures.CHARACTER_B, UUID.randomUUID(), snapshot(holdings(0, 0, 0)), TIME);
		repository.activate(root, other); assertEquals(run, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}
	@Test public void returnIdentitySurvivesRestartAndDoesNotBecomeNewAcquisition() throws Exception
	{
		RunRecord run = initial(); RunRecord earned = repository.saveItems(root, run, plan(run, holdings(100, 5, 0), 2));
		AccountingPlan drop = ENGINE.plan(earned, observation(holdings(100, 0, 0), 3, Collections.singletonMap(ItemID.SHARK, ItemObservation.Action.DROP), SESSION),
			id -> id, false, false, Collections.emptyMap(), "");
		repository.saveItems(root, earned, drop); RunRecord reloaded = repository.load(root, SnapshotFixtures.CHARACTER_A).get();
		AccountingPlan returned = ENGINE.plan(reloaded, observation(holdings(100, 5, 0), 1, Collections.emptyMap(), UUID.randomUUID()),
			id -> id, true, false, Collections.emptyMap(), "");
		RunRecord confirmed = repository.saveItems(root, reloaded, returned);
		assertEquals(5, confirmed.getOwnership().total(ItemID.SHARK)); assertEquals(1, confirmed.getItemProgress().getAcquisitions().size());
	}
	@Test public void resetArchivesRunAndRetainsItsJournal() throws Exception
	{
		RunRecord run = initial(); RunRecord reset = repository.reset(root, SnapshotFixtures.CHARACTER_A, run.getState().getRunId());
		assertNull(reset.getItemProgress()); assertEquals(RunStatus.NOT_STARTED, repository.load(root, SnapshotFixtures.CHARACTER_A).get().getState().getStatus());
		assertTrue(directory().joinSegment("ledger-" + run.getState().getRunId() + "-" + run.getRevision() + ".json").exists());
		try (Stream<Filepath> files = directory().walk(1)) { assertTrue(files.anyMatch(file -> file.getFileName().startsWith("archive-"))); }
	}
	@Test public void formatTwoMigrationKeepsNonzeroOwnershipAndExactBackup() throws Exception
	{
		RunRecord old = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 100).withOwnership(com.fromscratch.model.VirtualOwnershipTest.fixture());
		String original = versionTwo(old); directory().createDirectories(); current().write(original);
		RunRecord loaded = repository.load(root, SnapshotFixtures.CHARACTER_A).get();
		assertEquals(old.getOwnership(), loaded.getOwnership()); assertEquals(old.getSnapshot(), loaded.getSnapshot());
		assertEquals(old.getState(), loaded.getState()); assertFalse(loaded.getItemProgress().isEstablished());
		try (Stream<Filepath> files = directory().walk(1))
		{
			Filepath backup = files.filter(file -> file.getFileName().startsWith("migration-v2-")).findFirst().get();
			assertEquals(original, RunRepository.readText(backup));
		}
	}
	@Test public void missingProgressInFormatThreeNeverDefaultsToZeros() throws Exception
	{
		RunRecord run = initial(); JsonObject envelope = SnapshotFixtures.gson().fromJson(RunRepository.readText(current()), JsonObject.class);
		JsonObject data = SnapshotFixtures.gson().fromJson(envelope.get("payload").getAsString(), JsonObject.class); data.remove("itemProgress");
		envelope.addProperty("payload", data.toString()); envelope.addProperty("sha256", SaveIntegrity.sha256(data.toString())); current().write(envelope.toString());
		String damaged = RunRepository.readText(current());
		try { repository.load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (SaveException expected) { }
		assertEquals(damaged, RunRepository.readText(current()));
	}
	@Test public void unbackedJournalDeltasCannotBeCommitted() throws Exception
	{
		RunRecord run = initial(); AccountingPlan plan = plan(run, holdings(100, 5, 0), 2);
		AccountingPlan invalid = new AccountingPlan(plan.getOwnership(), plan.getProgress(), Collections.singletonList(run.getItemProgress().getRecent().get(0)), false, "Invalid fixture");
		try { repository.saveItems(root, run, invalid); fail(); } catch (SaveException expected) { }
		assertEquals(run, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}
	@Test public void rejectingAProposedGainDoesNotLeaveFalseAcquisitionOrUnlock() throws Exception
	{
		RunRecord run = initial();
		AccountingPlan corrected = ENGINE.plan(run, observation(holdings(100, 5, 0), 2, Collections.emptyMap(), UUID.randomUUID()), id -> id,
			true, false, Collections.singletonMap(ItemID.SHARK, new VirtualItemOwnership(0, Collections.emptyMap())), "These copies were legacy elsewhere.");
		RunRecord saved = repository.saveItems(root, run, corrected);
		assertEquals(0, saved.getOwnership().total(ItemID.SHARK)); assertEquals(0, saved.getEconomy().getItemsUnlocked());
		assertTrue(saved.getItemProgress().getAcquisitions().isEmpty());
		assertTrue(corrected.getTransactions().stream().noneMatch(entry -> entry.getCategory() == ItemTransaction.Category.ACQUISITION));
		assertEquals(saved, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}
	@Test public void partiallyRejectedGainRecordsOnlyTheApprovedAcquisition() throws Exception
	{
		RunRecord run = initial();
		AccountingPlan corrected = ENGINE.plan(run, observation(holdings(100, 5, 0), 2, Collections.emptyMap(), UUID.randomUUID()), id -> id,
			true, false, Collections.singletonMap(ItemID.SHARK, new VirtualItemOwnership(3, Collections.singletonMap(VirtualLocation.INVENTORY, 3L))), "Two copies came from legacy storage.");
		RunRecord saved = repository.saveItems(root, run, corrected);
		assertEquals(3, saved.getOwnership().total(ItemID.SHARK)); assertEquals(3, saved.getItemProgress().getAcquisitions().get(0).getQuantity());
		assertEquals(saved, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}
	@Test public void checkpointRevisionMustMatchTheJournalHead() throws Exception
	{
		initial(); JsonObject envelope = SnapshotFixtures.gson().fromJson(RunRepository.readText(current()), JsonObject.class);
		JsonObject data = SnapshotFixtures.gson().fromJson(envelope.get("payload").getAsString(), JsonObject.class);
		data.addProperty("revision", UUID.randomUUID().toString()); envelope.addProperty("payload", data.toString()); envelope.addProperty("sha256", SaveIntegrity.sha256(data.toString()));
		current().write(envelope.toString());
		String before = RunRepository.readText(current());
		try { repository.load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (SaveException expected) { }
		assertEquals(before, RunRepository.readText(current()));
	}
	@Test public void manualElsewherePreservationAndReturnCommitWithoutDoubleCounting() throws Exception
	{
		RunRecord run = initial(); RunRecord earned = repository.saveItems(root, run, plan(run, holdings(100, 5, 0), 2));
		AccountingPlan preserved = ENGINE.plan(earned, observation(holdings(100, 0, 0), 3), id -> id, true, false,
			Collections.singletonMap(ItemID.SHARK, new VirtualItemOwnership(5, Collections.emptyMap())), "Copies remain in unsupported storage.");
		RunRecord stored = repository.saveItems(root, earned, preserved);
		RunRecord reloaded = repository.load(root, SnapshotFixtures.CHARACTER_A).get(); assertEquals(stored, reloaded);
		AccountingPlan returned = plan(reloaded, holdings(100, 5, 0), 4);
		RunRecord saved = repository.saveItems(root, reloaded, returned);
		assertEquals(5, saved.getOwnership().total(ItemID.SHARK)); assertEquals(0, saved.getOwnership().item(ItemID.SHARK).unallocated());
		assertEquals(1, saved.getItemProgress().getAcquisitions().size());
		assertEquals(saved, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
	}
	static String versionTwo(RunRecord run)
	{
		JsonObject envelope = SnapshotFixtures.gson().fromJson(OwnershipPersistenceTest.versionOne(run), JsonObject.class);
		JsonObject data = SnapshotFixtures.gson().fromJson(envelope.get("payload").getAsString(), JsonObject.class);
		JsonObject current = SnapshotFixtures.gson().fromJson(SnapshotFixtures.codec().encode(run), JsonObject.class);
		JsonObject currentPayload = SnapshotFixtures.gson().fromJson(current.get("payload").getAsString(), JsonObject.class);
		data.add("ownership", currentPayload.get("ownership")); envelope.addProperty("formatVersion", 2);
		envelope.addProperty("payload", data.toString()); envelope.addProperty("sha256", SaveIntegrity.sha256(data.toString())); return envelope.toString();
	}
}
