package com.fromscratch.overlay;

import com.fromscratch.bank.*;
import com.fromscratch.model.*;
import com.fromscratch.ui.SetupView;
import java.util.*;

/** Historical M1–M4 fixtures adapted to the current bank-only renderer; never shipped in the plugin. */
public final class LegacyOverlayFixture
{
	private LegacyOverlayFixture() { }
	public static void update(BankOverlayState state, SetupView view)
	{
		RunRecord old = view.getSaved();
		if (view.getMode() != SetupView.Mode.ACTIVE || old == null || old.getState().getStatus() != RunStatus.ACTIVE) { state.clear(); return; }
		Map<Integer, Long> balances = new TreeMap<>(); Map<Integer, Long> physical = new TreeMap<>(old.getSnapshot().at(SnapshotLocation.BANK).getQuantities());
		Set<Integer> ids = new TreeSet<>(old.getOwnership().getItems().keySet()); if (old.getItemProgress() != null) { ids.addAll(old.getItemProgress().getLegacyDeficits().getItems().keySet()); }
		for (int id : ids) { long n = old.getOwnership().at(id, VirtualLocation.BANK) - (old.getItemProgress() == null ? 0 : old.getItemProgress().getLegacyDeficits().quantity(id)); balances.put(id, n); if (n > physical.getOrDefault(id, 0L)) { physical.put(id, n); } }
		BankAudit audit = new BankAudit(old.getRevision(), UUID.randomUUID(), old.getState().getStartedAt(), BankAudit.Kind.START, Collections.emptyMap(), Collections.emptyMap(), "Historical test fixture");
		BankRun run = new BankRun(old.getState().getCharacterKey(), old.getState().getRunId(), null, old.getState().getStartedAt(), old.getRevision(), 1, old.getSnapshot().at(SnapshotLocation.BANK), physical, balances, true, null, null, Collections.singletonList(audit), null);
		state.updateBank(new BankView(run, null, Collections.emptyMap(), false, false, "Historical fixture"));
	}
}
