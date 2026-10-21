package com.fromscratch.ui;

import com.fromscratch.model.SnapshotLocation;
import com.fromscratch.observation.CaptureProgress;
import java.util.Arrays;
import java.util.List;

/** The same reliable-observation checklist guides first setup and returning host sessions. */
public final class PreparationChecklist
{
	private PreparationChecklist() { }
	public static List<String> steps(CaptureProgress progress)
	{
		return Arrays.asList(step(progress.ready(SnapshotLocation.INVENTORY), "Open your inventory."),
			step(progress.ready(SnapshotLocation.EQUIPMENT), "Open your worn equipment tab."),
			step(progress.ready(SnapshotLocation.BANK), "Open your bank."));
	}
	public static String step(boolean complete, String text) { return (complete ? "\u2713 " : "\u25cb ") + text; }
	public static String instruction(CaptureProgress progress)
	{
		if (!progress.ready(SnapshotLocation.INVENTORY)) { return "Open your inventory so its contents can be checked. Empty is fine."; }
		if (!progress.ready(SnapshotLocation.EQUIPMENT)) { return "Open your worn equipment tab so its contents can be checked. Empty is fine."; }
		if (!progress.ready(SnapshotLocation.BANK)) { return "Open your normal bank and enter your PIN if needed. Special storage is excluded."; }
		return "Review the detected items and confirm when they are correct.";
	}
}
