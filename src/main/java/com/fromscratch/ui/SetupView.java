package com.fromscratch.ui;

import com.fromscratch.model.LegacySnapshot;
import com.fromscratch.model.RunRecord;
import com.fromscratch.observation.CaptureProgress;
import java.util.UUID;
import lombok.Value;

/** Immutable presentation snapshot; no client objects or mutable session cross onto Swing. */
@Value
public class SetupView
{
	public enum Mode { SIGNED_OUT, LOADING, NOT_STARTED, INITIALIZING, SAVING, ACTIVE, ERROR }

	Mode mode;
	String message;
	RunRecord saved;
	LegacySnapshot preview;
	UUID reviewToken;
	CaptureProgress progress;

	public SetupView(Mode mode, String message, RunRecord saved, LegacySnapshot preview, UUID reviewToken)
	{
		this(mode, message, saved, preview, reviewToken, CaptureProgress.from(preview));
	}

	public SetupView(Mode mode, String message, RunRecord saved, LegacySnapshot preview, UUID reviewToken, CaptureProgress progress)
	{
		this.mode = mode;
		this.message = message;
		this.saved = saved;
		this.preview = preview;
		this.reviewToken = reviewToken;
		this.progress = progress;
	}

	public static SetupView signedOut()
	{
		return new SetupView(Mode.SIGNED_OUT, "Log in and wait for a verified character identity.", null, null, null);
	}
}
