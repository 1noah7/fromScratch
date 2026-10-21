package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.RunRecord;
import com.fromscratch.model.RunStatus;
import com.fromscratch.model.VirtualOwnershipTest;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.util.function.Consumer;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Filepath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class OwnershipPersistenceTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private Filepath root;
	private RunRepository repository;
	@Before public void setUp()
	{
		root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		repository = new RunRepository(SnapshotFixtures.codec());
	}
	private Filepath directory() { return root.join("characters", SnapshotFixtures.CHARACTER_A); }
	private Filepath current() { return directory().joinSegment("current.json"); }
	private RunRecord positive() { return SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 100).withOwnership(VirtualOwnershipTest.fixture()); }

	/** Exact M2 field shape, including the empty economy guard; all data is synthetic. */
	static String versionOne(RunRecord record)
	{
		// Frozen format-1 fixture writer: deliberately independent of the M3 encoder.
		JsonObject data = new JsonObject();
		data.addProperty("headerSchema", 1);
		data.addProperty("characterKey", record.getState().getCharacterKey());
		data.addProperty("revision", record.getRevision().toString());
		data.addProperty("status", record.getState().getStatus().name());
		data.addProperty("runId", record.getState().getRunId() == null ? null : record.getState().getRunId().toString());
		data.addProperty("previousRunId", record.getPreviousRunId() == null ? null : record.getPreviousRunId().toString());
		data.addProperty("startedAt", record.getState().getStartedAt() == null ? null : record.getState().getStartedAt().toString());
		JsonObject economy = new JsonObject();
		for (String field : new String[]{"spendableGp", "netWorth", "totalValueGained", "itemsUnlocked"}) { economy.addProperty(field, 0); }
		economy.add("items", new JsonObject()); data.add("virtualEconomy", economy);
		if (record.getSnapshot() == null) { data.add("snapshot", com.google.gson.JsonNull.INSTANCE); }
		else
		{
			JsonObject snapshot = new JsonObject(); snapshot.addProperty("capturedAt", record.getSnapshot().getCapturedAt().toString());
			for (com.fromscratch.model.SnapshotLocation location : com.fromscratch.model.SnapshotLocation.values())
			{
				com.fromscratch.model.ContainerSnapshot values = record.getSnapshot().at(location);
				JsonObject container = new JsonObject(); container.addProperty("occupiedSlots", values.getOccupiedSlots());
				JsonObject quantities = new JsonObject(); values.getQuantities().forEach((id, quantity) -> quantities.addProperty(id.toString(), quantity));
				container.add("quantities", quantities); snapshot.add(location.name(), container);
			}
			data.add("snapshot", snapshot);
		}
		JsonObject envelope = new JsonObject();
		envelope.addProperty("formatVersion", 1);
		envelope.addProperty("payload", data.toString());
		envelope.addProperty("sha256", SaveIntegrity.sha256(data.toString()));
		return envelope.toString();
	}
	private void writeOld(RunRecord record) throws Exception { directory().createDirectories(); current().write(versionOne(record)); }
	private static String contents(Filepath file) throws Exception
	{
		try (Reader reader = file.openReader())
		{
			StringBuilder result = new StringBuilder(); char[] buffer = new char[4096]; int read;
			while ((read = reader.read(buffer)) != -1) { result.append(buffer, 0, read); }
			return result.toString();
		}
	}

	@Test public void nonzeroVirtualAssetsRoundTripThroughRestart() throws Exception
	{
		RunRecord run = positive(); repository.activate(root, run);
		assertEquals(run, new RunRepository(SnapshotFixtures.codec()).load(root, SnapshotFixtures.CHARACTER_A).get());
		assertEquals(100, run.getSnapshot().at(com.fromscratch.model.SnapshotLocation.BANK).quantity(ItemID.COINS));
	}
	@Test public void virtualStateCannotCrossCharacterNamespaces() throws Exception
	{
		repository.activate(root, positive());
		assertFalse(repository.load(root, SnapshotFixtures.CHARACTER_B).isPresent());
	}
	@Test public void maximumLongVirtualStackSurvivesSaveAndReload() throws Exception
	{
		com.fromscratch.model.VirtualOwnership ownership = com.fromscratch.model.VirtualOwnership.empty().withItem(ItemID.COINS,
			new com.fromscratch.model.VirtualItemOwnership(Long.MAX_VALUE,
				java.util.Collections.singletonMap(com.fromscratch.model.VirtualLocation.BANK, Long.MAX_VALUE)));
		RunRecord run = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 1).withOwnership(ownership);
		repository.activate(root, run);
		assertEquals(Long.MAX_VALUE, repository.load(root, SnapshotFixtures.CHARACTER_A).get().getOwnership().total(ItemID.COINS));
	}
	@Test public void activeMilestoneTwoSaveMigratesWithoutChangingRunOrLegacyData() throws Exception
	{
		RunRecord old = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 100); writeOld(old);
		String before = contents(current());
		RunRecord migrated = repository.load(root, SnapshotFixtures.CHARACTER_A).get();
		assertEquals(old.getState(), migrated.getState());
		assertEquals(old.getSnapshot(), migrated.getSnapshot());
		assertTrue(migrated.getOwnership().getItems().isEmpty());
		assertNotEquals(old.getRevision(), migrated.getRevision());
		try (java.util.stream.Stream<Filepath> files = directory().walk(1))
		{
			Filepath backup = files.filter(path -> path.getFileName().startsWith("migration-v1-")).findFirst().get();
			assertEquals(before, contents(backup));
		}
		assertEquals(RunRecordCodec.FORMAT_VERSION, SnapshotFixtures.gson().fromJson(contents(current()), JsonObject.class).get("formatVersion").getAsInt());
	}
	@Test public void migratedSaveDoesNotMigrateAgainOnRestart() throws Exception
	{
		writeOld(SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 1));
		RunRecord first = repository.load(root, SnapshotFixtures.CHARACTER_A).get();
		String saved = contents(current());
		assertEquals(first, repository.load(root, SnapshotFixtures.CHARACTER_A).get());
		assertEquals(saved, contents(current()));
	}
	@Test public void resetTombstoneMigrationNeverResurrectsRun() throws Exception
	{
		writeOld(RunRecord.reset(SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 1)));
		assertEquals(RunStatus.NOT_STARTED, repository.load(root, SnapshotFixtures.CHARACTER_A).get().getState().getStatus());
	}
	@Test public void interruptedMigrationPreservesOldBytesAndBackup() throws Exception
	{
		writeOld(SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 1));
		String old = contents(current());
		RunRepository failing = new RunRepository(SnapshotFixtures.codec(), (pending, current) -> { throw new java.io.IOException("synthetic failure"); });
		try { failing.load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (SaveException expected) { }
		assertEquals(old, contents(current()));
		try (java.util.stream.Stream<Filepath> files = directory().walk(1))
		{
			assertTrue(files.anyMatch(path -> path.getFileName().startsWith("migration-v1-")));
		}
		assertEquals(RunStatus.ACTIVE, repository.load(root, SnapshotFixtures.CHARACTER_A).get().getState().getStatus());
	}
	@Test public void corruptOwnershipIsPreservedAndNeverReplacedWithZeros() throws Exception
	{
		directory().createDirectories();
		String corrupt = mutate(item -> item.addProperty("total", -1)); current().write(corrupt);
		try { repository.load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (SaveException expected) { }
		assertEquals(corrupt, contents(current()));
	}
	@Test public void unsupportedMilestoneTwoCurrencyIsNotMigratedToAnEmptyEconomy() throws Exception
	{
		JsonObject envelope = SnapshotFixtures.gson().fromJson(versionOne(positive()), JsonObject.class);
		JsonObject data = SnapshotFixtures.gson().fromJson(envelope.get("payload").getAsString(), JsonObject.class);
		data.getAsJsonObject("virtualEconomy").addProperty("spendableGp", 1);
		envelope.addProperty("payload", data.toString()); envelope.addProperty("sha256", SaveIntegrity.sha256(data.toString()));
		directory().createDirectories(); String original = envelope.toString(); current().write(original);
		try { repository.load(root, SnapshotFixtures.CHARACTER_A); fail(); } catch (SaveException expected) { }
		assertEquals(original, contents(current()));
		try (java.util.stream.Stream<Filepath> files = directory().walk(1))
		{
			assertFalse(files.anyMatch(path -> path.getFileName().startsWith("migration-")));
		}
	}
	@Test public void olderRecoveryCheckpointsRemainByteIdenticalAfterMigration() throws Exception
	{
		RunRecord old = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 100); writeOld(old);
		Filepath checkpoint = directory().joinSegment("checkpoint-" + old.getRevision() + ".json");
		checkpoint.write(contents(current())); String original = contents(checkpoint);
		repository.load(root, SnapshotFixtures.CHARACTER_A);
		assertEquals(original, contents(checkpoint));
	}
	@Test public void migrationBackupAlonePreventsBlankStartupAfterMissingCurrent() throws Exception
	{
		directory().createDirectories(); directory().joinSegment("migration-v1-recovery.json").write("recovery evidence");
		try { repository.load(root, SnapshotFixtures.CHARACTER_A); fail(); }
		catch (SaveException expected) { assertTrue(expected.getMessage().contains("Current save is missing")); }
		assertFalse(current().exists());
	}
	@Test public void incompleteLocationMapIsRejected() throws Exception { reject(mutate(item -> item.getAsJsonObject("locations").remove("BANK"))); }
	@Test public void allocationBeyondTotalIsRejected() throws Exception { reject(mutate(item -> item.addProperty("total", 1))); }
	@Test public void negativeLocationIsRejected() throws Exception { reject(mutate(item -> item.getAsJsonObject("locations").addProperty("BANK", -1))); }
	@Test public void fractionalOwnershipIsRejected() throws Exception { reject(mutate(item -> item.addProperty("total", 1.5))); }
	@Test public void missingOwnershipInNewFormatIsRejected() throws Exception
	{
		JsonObject envelope = SnapshotFixtures.gson().fromJson(versionOne(positive()), JsonObject.class);
		envelope.addProperty("formatVersion", RunRecordCodec.FORMAT_VERSION);
		reject(envelope.toString());
	}
	private String mutate(Consumer<JsonObject> mutation)
	{
		JsonObject envelope = SnapshotFixtures.gson().fromJson(SnapshotFixtures.codec().encode(positive()), JsonObject.class);
		JsonObject data = SnapshotFixtures.gson().fromJson(envelope.get("payload").getAsString(), JsonObject.class);
		mutation.accept(data.getAsJsonObject("ownership").getAsJsonObject(Integer.toString(ItemID.SHARK)));
		envelope.addProperty("payload", data.toString()); envelope.addProperty("sha256", SaveIntegrity.sha256(data.toString()));
		return envelope.toString();
	}
	private void reject(String text) throws Exception
	{
		try { SnapshotFixtures.codec().decode(text, SnapshotFixtures.CHARACTER_A); fail("Corrupt ownership accepted"); }
		catch (SaveException expected) { assertTrue(expected.getMessage().contains("incompatible")); }
	}
}
