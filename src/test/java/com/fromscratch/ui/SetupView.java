package com.fromscratch.ui;

import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.RunRecord;
import com.fromscratch.observation.CaptureProgress;
import java.util.UUID;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import lombok.Value;

/** Immutable presentation snapshot; no client objects or mutable session cross onto Swing. */
@Value
public class SetupView
{
	public enum Mode { SIGNED_OUT, LOADING, NOT_STARTED, INITIALIZING, SAVING, ACTIVE, RECONCILING, ERROR }

	Mode mode;
	String message;
	RunRecord saved;
	LegacySnapshot preview;
	UUID reviewToken;
	CaptureProgress progress;
	ItemReview itemReview;
	Map<Integer, String> itemNames;
	long elapsedMinutes;
	boolean writing;

	public SetupView(Mode mode, String message, RunRecord saved, LegacySnapshot preview, UUID reviewToken)
	{
		this(mode, message, saved, preview, reviewToken, CaptureProgress.from(preview));
	}

	public SetupView(Mode mode, String message, RunRecord saved, LegacySnapshot preview, UUID reviewToken, CaptureProgress progress)
	{
		this(mode, message, saved, preview, reviewToken, progress, null, Collections.emptyMap());
	}
	public SetupView(Mode mode, String message, RunRecord saved, LegacySnapshot preview, UUID reviewToken, CaptureProgress progress,
		ItemReview itemReview, Map<Integer, String> itemNames)
	{
		this(mode, message, saved, preview, reviewToken, progress, itemReview, itemNames, false);
	}
	public SetupView(Mode mode, String message, RunRecord saved, LegacySnapshot preview, UUID reviewToken, CaptureProgress progress,
		ItemReview itemReview, Map<Integer, String> itemNames, boolean writing)
	{
		this.mode = mode;
		this.message = message;
		this.saved = saved;
		this.preview = preview;
		this.reviewToken = reviewToken;
		this.progress = progress;
		this.itemReview = itemReview;
		this.itemNames = Collections.unmodifiableMap(new HashMap<>(itemNames));
		this.elapsedMinutes = RunDuration.minutes(saved == null ? null : saved.getState().getStartedAt(), java.time.Instant.now());
		this.writing = writing;
	}

	public static SetupView signedOut()
	{
		return new SetupView(Mode.SIGNED_OUT, "Log in and wait for a verified character identity.", null, null, null);
	}
}
