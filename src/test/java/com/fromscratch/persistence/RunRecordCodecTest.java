package com.fromscratch.persistence;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.RunRecord;
import com.google.gson.JsonObject;
import java.util.function.Consumer;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.*;

public class RunRecordCodecTest
{
	private static final String COINS = Integer.toString(ItemID.COINS);
	@Test
	public void activeSnapshotAndLargeLegacyCoinsRoundTrip() throws Exception
	{
		RunRecord run = SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, Long.MAX_VALUE);
		assertEquals(run, SnapshotFixtures.codec().decode(SnapshotFixtures.codec().encode(run), SnapshotFixtures.CHARACTER_A));
	}

	@Test
	public void resetTombstoneRoundTripsWithoutResurrectingTheOldRun() throws Exception
	{
		RunRecord run = RunRecord.reset(SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 1));
		assertEquals(run, SnapshotFixtures.codec().decode(SnapshotFixtures.codec().encode(run), SnapshotFixtures.CHARACTER_A));
	}

	@Test
	public void checksumMismatchIsAnExplicitError() throws Exception
	{
		JsonObject envelope = envelope();
		envelope.addProperty("sha256", "damaged");
		reject(envelope.toString(), "checksum mismatch");
	}

	@Test
	public void unknownSaveFormatCannotBeTreatedAsEmpty() throws Exception
	{
		JsonObject envelope = envelope();
		envelope.addProperty("formatVersion", RunRecordCodec.FORMAT_VERSION + 1);
		reject(envelope.toString(), "version is not supported");
	}

	@Test
	public void unknownHeaderSchemaRequiresAMigration() throws Exception
	{
		reject(modify(data -> data.addProperty("headerSchema", 2)), "incompatible");
	}

	@Test
	public void missingContainerCannotPassBoundaryValidation() throws Exception
	{
		reject(modify(data -> data.getAsJsonObject("snapshot").remove("EQUIPMENT")), "incomplete");
	}

	@Test
	public void fractionalQuantitiesCannotBeTruncatedIntoIntegers() throws Exception
	{
		reject(modify(data -> quantities(data).addProperty(COINS, 1.5)), "incompatible");
	}

	@Test
	public void quotedQuantitiesAreNotSilentlyCoerced() throws Exception
	{
		reject(modify(data -> quantities(data).addProperty(COINS, "10")), "incompatible");
	}

	@Test
	public void negativeQuantitiesAreRejected() throws Exception
	{
		reject(modify(data -> quantities(data).addProperty(COINS, -1)), "incompatible");
	}

	@Test
	public void nonzeroFutureEconomyCannotBeSilentlyDiscardedByM2() throws Exception
	{
		reject(modify(data -> data.getAsJsonObject("virtualEconomy").addProperty("spendableGp", 100)), "incompatible");
	}

	@Test
	public void overflowingQuantitiesAreRejected() throws Exception
	{
		reject(modify(data -> quantities(data).add(COINS, new com.google.gson.JsonPrimitive(new java.math.BigInteger("9223372036854775808")))), "incompatible");
	}

	@Test
	public void unknownFieldsRequireExplicitCompatibilityHandling() throws Exception
	{
		reject(modify(data -> data.addProperty("futureFeature", true)), "incompatible");
	}

	private static JsonObject quantities(JsonObject data) { return data.getAsJsonObject("snapshot").getAsJsonObject("BANK").getAsJsonObject("quantities"); }
	private static JsonObject envelope()
	{
		return SnapshotFixtures.gson().fromJson(SnapshotFixtures.codec().encode(SnapshotFixtures.active(SnapshotFixtures.CHARACTER_A, 100)), JsonObject.class);
	}

	private static String modify(Consumer<JsonObject> mutation)
	{
		JsonObject envelope = envelope();
		JsonObject payload = SnapshotFixtures.gson().fromJson(envelope.get("payload").getAsString(), JsonObject.class);
		mutation.accept(payload);
		String text = payload.toString();
		envelope.addProperty("payload", text);
		envelope.addProperty("sha256", SaveIntegrity.sha256(text));
		return envelope.toString();
	}

	private static void reject(String data, String fragment) throws Exception
	{
		try { SnapshotFixtures.codec().decode(data, SnapshotFixtures.CHARACTER_A); fail("Invalid save accepted"); }
		catch (SaveException expected) { assertTrue(expected.getMessage(), expected.getMessage().contains(fragment)); }
	}
}
