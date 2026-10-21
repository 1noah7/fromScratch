package com.fromscratch.observation;

import com.fromscratch.model.LegacySnapshot;

/** Narrow observation boundary for deterministic controller tests without a game client. */
public interface SnapshotSource
{
	String characterKey();
	LegacySnapshot capture(String expectedCharacter);
	default void beginInitialization() { }
	default void endInitialization() { }
	default CaptureProgress progress() { return CaptureProgress.from(null); }
	default boolean supportsTracking() { return false; }
	default void beginTracking() { }
	default void confirmedSnapshot(LegacySnapshot snapshot) { }
	default ItemObservation observeItems(boolean preparing) { return null; }
	default void acknowledgeItems(long tick) { }
	default void acknowledgeItems(ItemObservation observation) { acknowledgeItems(observation.getEventVersion()); }
	default String itemName(int id) { return "Item #" + id; }
	default int noteBase(int id) { return id; }
}
