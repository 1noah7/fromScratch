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
}
