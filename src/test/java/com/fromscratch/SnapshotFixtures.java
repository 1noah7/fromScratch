package com.fromscratch;

import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.RunRecord;
import com.fromscratch.model.SnapshotLocation;
import com.fromscratch.observation.CharacterIdentity;
import com.fromscratch.observation.ObservedItem;
import com.fromscratch.observation.SnapshotAssembler;
import com.fromscratch.persistence.RunRecordCodec;
import com.google.gson.Gson;
import com.google.inject.Guice;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.RuneScapeProfile;
import net.runelite.client.config.RuneScapeProfileType;

/** Synthetic data only. The test DI container supplies Gson; no account/client is required. */
public final class SnapshotFixtures
{
	public static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z");
	public static final String CHARACTER_A = character(12345);
	public static final String CHARACTER_B = character(67890);

	private SnapshotFixtures() { }

	public static Gson gson() { return Guice.createInjector().getInstance(Gson.class); }
	public static RunRecordCodec codec() { return new RunRecordCodec(gson()); }

	public static String character(long hash)
	{
		return CharacterIdentity.verify(true, hash, RuneScapeProfileType.STANDARD, "rsprofile.test",
			Collections.singletonList(new RuneScapeProfile("Synthetic", RuneScapeProfileType.STANDARD, hash, "rsprofile.test"))).get();
	}

	public static EnumMap<SnapshotLocation, List<ObservedItem>> observations()
	{
		EnumMap<SnapshotLocation, List<ObservedItem>> result = new EnumMap<>(SnapshotLocation.class);
		for (SnapshotLocation location : SnapshotLocation.values()) { result.put(location, new ArrayList<>()); }
		return result;
	}

	public static LegacySnapshot snapshot(long coins)
	{
		EnumMap<SnapshotLocation, List<ObservedItem>> observations = observations();
		if (coins > 0) { observations.get(SnapshotLocation.BANK).add(new ObservedItem(ItemID.COINS, coins, false)); }
		return SnapshotAssembler.capture(true, true, observations, TIME);
	}

	public static RunRecord active(String character, long coins)
	{
		return RunRecord.activate(character, UUID.randomUUID(), snapshot(coins), TIME.plusSeconds(1));
	}
}
