package com.fromscratch.persistence;

import com.fromscratch.model.ContainerSnapshot;
import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.RunRecord;
import com.fromscratch.model.RunState;
import com.fromscratch.model.RunStatus;
import com.fromscratch.model.SnapshotLocation;
import com.fromscratch.model.VirtualOwnership;
import com.fromscratch.model.VirtualItemOwnership;
import com.fromscratch.model.VirtualLocation;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Explicit JSON-tree codec. Injected Gson handles JSON syntax; all domain fields
 * pass through validation rather than reflective deserialization of immutable models.
 */
public final class RunRecordCodec
{
	public static final int FORMAT_VERSION = 2;
	private static final String[] BALANCES = {"spendableGp", "netWorth", "totalValueGained", "itemsUnlocked"};
	private final Gson gson;

	@Inject
	public RunRecordCodec(Gson gson) { this.gson = gson.newBuilder().serializeNulls().create(); }

	public String encode(RunRecord record)
	{
		JsonObject payload = new JsonObject();
		RunState state = record.getState();
		payload.addProperty("headerSchema", state.getSchemaVersion());
		payload.addProperty("characterKey", state.getCharacterKey());
		payload.addProperty("revision", record.getRevision().toString());
		payload.addProperty("status", state.getStatus().name());
		payload.addProperty("runId", state.getRunId() == null ? null : state.getRunId().toString());
		payload.addProperty("previousRunId", record.getPreviousRunId() == null ? null : record.getPreviousRunId().toString());
		payload.addProperty("startedAt", state.getStartedAt() == null ? null : state.getStartedAt().toString());
		JsonObject economy = new JsonObject();
		for (String balance : BALANCES) { economy.addProperty(balance, 0L); }
		economy.add("items", new JsonObject());
		payload.add("virtualEconomy", economy);
		JsonObject ownership = new JsonObject();
		new TreeMap<>(record.getOwnership().getItems()).forEach((id, item) ->
		{
			JsonObject encoded = new JsonObject();
			encoded.addProperty("total", item.getTotal());
			JsonObject locations = new JsonObject();
			for (VirtualLocation location : VirtualLocation.values()) { locations.addProperty(location.name(), item.at(location)); }
			encoded.add("locations", locations);
			ownership.add(id.toString(), encoded);
		});
		payload.add("ownership", ownership);
		if (record.getSnapshot() == null)
		{
			payload.add("snapshot", com.google.gson.JsonNull.INSTANCE);
		}
		else
		{
			JsonObject snapshot = new JsonObject();
			snapshot.addProperty("capturedAt", record.getSnapshot().getCapturedAt().toString());
			for (SnapshotLocation location : SnapshotLocation.values())
			{
				ContainerSnapshot container = record.getSnapshot().at(location);
				JsonObject encoded = new JsonObject();
				encoded.addProperty("occupiedSlots", container.getOccupiedSlots());
				JsonObject quantities = new JsonObject();
				container.getQuantities().forEach((id, quantity) -> quantities.addProperty(id.toString(), quantity));
				encoded.add("quantities", quantities);
				snapshot.add(location.name(), encoded);
			}
			payload.add("snapshot", snapshot);
		}
		// Store the exact payload text so checksums do not depend on Gson's object
		// ordering or formatting when reading a future client version.
		String text = gson.toJson(payload);
		JsonObject envelope = new JsonObject();
		envelope.addProperty("formatVersion", FORMAT_VERSION);
		envelope.addProperty("sha256", SaveIntegrity.sha256(text));
		envelope.addProperty("payload", text);
		return gson.toJson(envelope);
	}

	public RunRecord decode(String text, String expectedCharacter) throws SaveException
	{
		return decodeSave(text, expectedCharacter).record;
	}

	/** Preserve source version so the repository can back up before durably migrating. */
	DecodedSave decodeSave(String text, String expectedCharacter) throws SaveException
	{
		try
		{
			JsonObject envelope = gson.fromJson(text, JsonObject.class);
			keys(envelope, "formatVersion", "sha256", "payload");
			long version = number(envelope.get("formatVersion"));
			if (version != 1 && version != FORMAT_VERSION)
			{
				throw new SaveException("This save version is not supported. Update the plugin; existing files were preserved.");
			}
			String payloadText = string(envelope.get("payload"));
			if (!SaveIntegrity.sha256(payloadText).equals(string(envelope.get("sha256"))))
			{
				throw new SaveException("Save checksum mismatch. The save and recovery checkpoints were preserved.");
			}
			JsonObject data = gson.fromJson(payloadText, JsonObject.class);
			if (version == 1)
			{
				keys(data, "headerSchema", "characterKey", "revision", "status", "runId", "previousRunId",
					"startedAt", "virtualEconomy", "snapshot");
			}
			else
			{
				keys(data, "headerSchema", "characterKey", "revision", "status", "runId", "previousRunId",
					"startedAt", "virtualEconomy", "snapshot", "ownership");
			}
			if (!expectedCharacter.equals(string(data.get("characterKey"))))
			{
				throw new SaveException("Save belongs to a different character. No data was changed.");
			}
			JsonObject economy = data.getAsJsonObject("virtualEconomy");
			keys(economy, "spendableGp", "netWorth", "totalValueGained", "itemsUnlocked", "items");
			for (String balance : BALANCES)
			{
				if (number(economy.get(balance)) != 0) { throw new IllegalArgumentException("Unsupported nonzero economy"); }
			}
			keys(economy.getAsJsonObject("items"));
			VirtualOwnership ownership = VirtualOwnership.empty();
			if (version == FORMAT_VERSION)
			{
				Map<Integer, VirtualItemOwnership> items = new HashMap<>();
				for (Map.Entry<String, JsonElement> entry : data.getAsJsonObject("ownership").entrySet())
				{
					int id = itemId(entry.getKey());
					JsonObject item = entry.getValue().getAsJsonObject();
					keys(item, "total", "locations");
					JsonObject encodedLocations = item.getAsJsonObject("locations");
					keys(encodedLocations, "BANK", "INVENTORY", "EQUIPMENT", "GRAND_EXCHANGE");
					Map<VirtualLocation, Long> locations = new EnumMap<>(VirtualLocation.class);
					for (VirtualLocation location : VirtualLocation.values()) { locations.put(location, number(encodedLocations.get(location.name()))); }
					items.put(id, new VirtualItemOwnership(number(item.get("total")), locations));
				}
				ownership = new VirtualOwnership(items);
			}
			LegacySnapshot snapshot = null;
			if (!data.get("snapshot").isJsonNull())
			{
				JsonObject encoded = data.getAsJsonObject("snapshot");
				keys(encoded, "capturedAt", "BANK", "INVENTORY", "EQUIPMENT");
				EnumMap<SnapshotLocation, ContainerSnapshot> containers = new EnumMap<>(SnapshotLocation.class);
				for (SnapshotLocation location : SnapshotLocation.values())
				{
					JsonObject container = encoded.getAsJsonObject(location.name());
					keys(container, "occupiedSlots", "quantities");
					Map<Integer, Long> quantities = new TreeMap<>();
					for (Map.Entry<String, JsonElement> item : container.getAsJsonObject("quantities").entrySet())
					{
						quantities.put(itemId(item.getKey()), number(item.getValue()));
					}
					containers.put(location, new ContainerSnapshot(quantities, Math.toIntExact(number(container.get("occupiedSlots")))));
				}
				snapshot = new LegacySnapshot(Instant.parse(string(encoded.get("capturedAt"))), containers);
			}
			RunState state = RunState.restore(Math.toIntExact(number(data.get("headerSchema"))), expectedCharacter,
				uuid(data.get("runId")), RunStatus.valueOf(string(data.get("status"))), instant(data.get("startedAt")), null);
			return new DecodedSave(new RunRecord(state, uuid(data.get("revision")), uuid(data.get("previousRunId")), snapshot, ownership), (int) version);
		}
		catch (SaveException ex) { throw ex; }
		catch (RuntimeException ex)
		{
			throw new SaveException("Save is corrupt, incomplete, or incompatible. Existing files were preserved; initialization is blocked.");
		}
	}

	private static int itemId(String text)
	{
		if (!text.matches("0|[1-9][0-9]*")) { throw new IllegalArgumentException("Invalid item ID"); }
		return Integer.parseInt(text);
	}

	static final class DecodedSave
	{
		final RunRecord record;
		final int version;
		DecodedSave(RunRecord record, int version) { this.record = record; this.version = version; }
	}

	private static void keys(JsonObject object, String... expected)
	{
		if (object == null || !object.keySet().equals(new HashSet<>(Arrays.asList(expected))))
		{
			throw new IllegalArgumentException("Unexpected save fields");
		}
	}

	private static String string(JsonElement value)
	{
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
		{
			throw new IllegalArgumentException("Expected text");
		}
		return value.getAsString();
	}

	private static long number(JsonElement value)
	{
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
			|| !value.getAsString().matches("0|[1-9][0-9]*"))
		{
			throw new IllegalArgumentException("Expected a nonnegative integer");
		}
		return Long.parseLong(value.getAsString());
	}

	private static UUID uuid(JsonElement value)
	{
		if (value.isJsonNull()) { return null; }
		String text = string(value);
		UUID id = UUID.fromString(text);
		if (!id.toString().equals(text)) { throw new IllegalArgumentException("Invalid UUID"); }
		return id;
	}

	private static Instant instant(JsonElement value)
	{
		return value.isJsonNull() ? null : Instant.parse(string(value));
	}
}
