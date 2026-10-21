package com.fromscratch.persistence;

import com.fromscratch.model.SnapshotLocation;
import com.fromscratch.tracking.*;
import com.google.gson.*;
import java.util.*;
import static com.fromscratch.persistence.RunRecordCodec.*;

/** Explicit versioned trees; no reflective domain deserialization or executable content. */
final class ItemJournalCodec
{
	private final Gson gson;
	ItemJournalCodec(Gson gson) { this.gson = gson; }
	JsonElement progress(ItemProgress progress)
	{
		return progress(progress, true);
	}
	JsonElement progress(ItemProgress progress, boolean deficits)
	{
		if (progress == null) { return JsonNull.INSTANCE; }
		JsonObject data = new JsonObject();
		JsonObject physical = new JsonObject();
		progress.getPhysical().getLocations().forEach((location, items) ->
		{
			JsonObject counts = new JsonObject(); items.forEach((id, quantity) -> counts.addProperty(id.toString(), quantity)); physical.add(location.name(), counts);
		});
		data.add("physical", physical);
		JsonObject returns = new JsonObject();
		new TreeMap<>(progress.getReturns()).forEach((id, balance) ->
		{
			JsonObject value = new JsonObject(); value.addProperty("legacy", balance.getLegacy()); value.addProperty("virtual", balance.getVirtual()); returns.add(id.toString(), value);
		});
		data.add("returns", returns);
		JsonArray unlocked = new JsonArray(); new TreeSet<>(progress.getUnlocked()).forEach(unlocked::add); data.add("unlocked", unlocked);
		data.add("recent", entries(progress.getRecent(), deficits)); data.add("acquisitions", entries(progress.getAcquisitions(), deficits));
		if (deficits)
		{
			JsonObject withdrawals = new JsonObject();
			progress.getLegacyDeficits().getItems().forEach((id, quantity) -> withdrawals.addProperty(id.toString(), quantity));
			data.add("legacyWithdrawals", withdrawals);
		}
		data.addProperty("session", progress.getSessionId().toString()); data.addProperty("tick", progress.getTick());
		data.addProperty("observedAt", progress.getObservedAt().toString());
		data.addProperty("head", progress.getLedgerHead() == null ? null : progress.getLedgerHead().toString());
		data.addProperty("sequence", progress.getSequence()); data.addProperty("established", progress.isEstablished());
		return data;
	}
	ItemProgress progress(JsonElement value)
	{
		return progress(value, true);
	}
	ItemProgress progress(JsonElement value, boolean deficits)
	{
		if (value.isJsonNull()) { return null; }
		JsonObject data = value.getAsJsonObject();
		if (deficits) { keys(data, "physical", "returns", "unlocked", "recent", "acquisitions", "session", "tick", "observedAt", "head", "sequence", "established", "legacyWithdrawals"); }
		else { keys(data, "physical", "returns", "unlocked", "recent", "acquisitions", "session", "tick", "observedAt", "head", "sequence", "established"); }
		JsonObject physical = data.getAsJsonObject("physical"); keys(physical, "BANK", "INVENTORY", "EQUIPMENT");
		Map<SnapshotLocation, Map<Integer, Long>> locations = new EnumMap<>(SnapshotLocation.class);
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			Map<Integer, Long> counts = new TreeMap<>();
			physical.getAsJsonObject(location.name()).entrySet().forEach(entry -> counts.put(itemId(entry.getKey()), number(entry.getValue())));
			locations.put(location, counts);
		}
		Map<Integer, ReturnBalance> returns = new HashMap<>();
		data.getAsJsonObject("returns").entrySet().forEach(entry ->
		{
			JsonObject balance = entry.getValue().getAsJsonObject(); keys(balance, "legacy", "virtual");
			returns.put(itemId(entry.getKey()), new ReturnBalance(number(balance.get("legacy")), number(balance.get("virtual"))));
		});
		Set<Integer> unlocked = new HashSet<>();
		for (JsonElement id : data.getAsJsonArray("unlocked"))
		{
			if (!unlocked.add(Math.toIntExact(number(id)))) { throw new IllegalArgumentException("Duplicate lifetime item ID"); }
		}
		JsonElement established = data.get("established");
		if (!established.isJsonPrimitive() || !established.getAsJsonPrimitive().isBoolean()) { throw new IllegalArgumentException("Invalid observation authority"); }
		Map<Integer, Long> withdrawals = new TreeMap<>();
		if (deficits) { data.getAsJsonObject("legacyWithdrawals").entrySet().forEach(entry -> withdrawals.put(itemId(entry.getKey()), number(entry.getValue()))); }
		return new ItemProgress(new Holdings(locations), returns, unlocked, entries(data.getAsJsonArray("recent"), deficits),
			entries(data.getAsJsonArray("acquisitions"), deficits), uuid(data.get("session")), number(data.get("tick")), instant(data.get("observedAt")),
			uuid(data.get("head")), number(data.get("sequence")), established.getAsBoolean(), new LegacyBankDeficits(withdrawals));
	}
	String encode(ItemLedgerBatch batch)
	{
		JsonObject data = new JsonObject();
		data.addProperty("id", batch.getId().toString()); data.addProperty("previousHead", batch.getPreviousHead() == null ? null : batch.getPreviousHead().toString());
		data.addProperty("previousRevision", batch.getPreviousRevision().toString()); data.addProperty("character", batch.getCharacter());
		data.addProperty("run", batch.getRunId().toString()); data.addProperty("session", batch.getSessionId().toString());
		data.addProperty("sequence", batch.getSequence()); data.addProperty("time", batch.getTime().toString());
		data.addProperty("resultSha256", batch.getResultSha256()); data.add("entries", entries(batch.getEntries(), true));
		String text = gson.toJson(data); JsonObject envelope = new JsonObject();
		envelope.addProperty("version", 2); envelope.addProperty("sha256", SaveIntegrity.sha256(text)); envelope.addProperty("payload", text);
		return gson.toJson(envelope);
	}
	ItemLedgerBatch decode(String text) throws SaveException
	{
		try
		{
			JsonObject envelope = gson.fromJson(text, JsonObject.class); keys(envelope, "version", "sha256", "payload");
			String payload = string(envelope.get("payload"));
			long version = number(envelope.get("version"));
			if ((version != 1 && version != 2) || !SaveIntegrity.sha256(payload).equals(string(envelope.get("sha256"))))
			{ throw new IllegalArgumentException("Ledger version or checksum mismatch"); }
			JsonObject data = gson.fromJson(payload, JsonObject.class);
			keys(data, "id", "previousHead", "previousRevision", "character", "run", "session", "sequence", "time", "resultSha256", "entries");
			return new ItemLedgerBatch(uuid(data.get("id")), uuid(data.get("previousHead")), uuid(data.get("previousRevision")), string(data.get("character")),
				uuid(data.get("run")), uuid(data.get("session")), number(data.get("sequence")), instant(data.get("time")),
				string(data.get("resultSha256")), entries(data.getAsJsonArray("entries"), version >= 2));
		}
		catch (RuntimeException ex) { throw new SaveException("Item ledger is corrupt or incompatible. All files were preserved; reload is blocked."); }
	}
	private JsonArray entries(List<ItemTransaction> entries, boolean deficits)
	{
		JsonArray array = new JsonArray();
		for (ItemTransaction entry : entries)
		{
			JsonObject data = new JsonObject(); data.addProperty("id", entry.getId().toString()); data.addProperty("time", entry.getTime().toString());
			data.addProperty("item", entry.getItemId()); data.addProperty("quantity", entry.getQuantity()); data.addProperty("virtualChange", entry.getVirtualChange());
			JsonObject locations = new JsonObject(); entry.getLocationChanges().forEach((location, delta) -> locations.addProperty(location.name(), delta)); data.add("locations", locations);
			data.addProperty("source", entry.getSource() == null ? null : entry.getSource().name()); data.addProperty("destination", entry.getDestination() == null ? null : entry.getDestination().name());
			data.addProperty("category", entry.getCategory().name()); data.addProperty("confidence", entry.getConfidence().name()); data.addProperty("reason", entry.getReason()); array.add(data);
			if (deficits) { data.addProperty("legacyWithdrawalChange", entry.getLegacyWithdrawalChange()); }
		}
		return array;
	}
	private List<ItemTransaction> entries(JsonArray array, boolean deficits)
	{
		List<ItemTransaction> entries = new ArrayList<>(); Set<UUID> ids = new HashSet<>();
		for (JsonElement value : array)
		{
			JsonObject data = value.getAsJsonObject();
			if (deficits) { keys(data, "id", "time", "item", "quantity", "virtualChange", "locations", "source", "destination", "category", "confidence", "reason", "legacyWithdrawalChange"); }
			else { keys(data, "id", "time", "item", "quantity", "virtualChange", "locations", "source", "destination", "category", "confidence", "reason"); }
			Map<SnapshotLocation, Long> locations = new EnumMap<>(SnapshotLocation.class);
			data.getAsJsonObject("locations").entrySet().forEach(entry -> locations.put(SnapshotLocation.valueOf(entry.getKey()), signed(entry.getValue())));
			UUID id = uuid(data.get("id")); if (!ids.add(id)) { throw new IllegalArgumentException("Duplicate transaction ID"); }
			entries.add(new ItemTransaction(id, instant(data.get("time")), Math.toIntExact(signed(data.get("item"))), number(data.get("quantity")),
				signed(data.get("virtualChange")), locations, location(data.get("source")), location(data.get("destination")),
				ItemTransaction.Category.valueOf(string(data.get("category"))), ItemTransaction.Confidence.valueOf(string(data.get("confidence"))), string(data.get("reason")),
				deficits ? signed(data.get("legacyWithdrawalChange")) : 0));
		}
		return entries;
	}
	private static SnapshotLocation location(JsonElement value) { return value.isJsonNull() ? null : SnapshotLocation.valueOf(string(value)); }
	private static long signed(JsonElement value)
	{
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber() || !value.getAsString().matches("0|-?[1-9][0-9]*"))
		{ throw new IllegalArgumentException("Invalid signed integer"); }
		long result = Long.parseLong(value.getAsString());
		if (result == Long.MIN_VALUE) { throw new IllegalArgumentException("Unsupported signed endpoint"); }
		return result;
	}
}
