package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import com.fromscratch.model.ContainerSnapshot;
import java.lang.management.ManagementFactory;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.Test;

/** Diagnostic benchmark, not an FPS/real-client acceptance claim. No game client is launched. */
public class BankUiBenchmarkTest
{
	@Test public void oneThousandItemBankSidebarRefreshCpuAndWallTime() throws Exception
	{
		Map<Integer, Long> counts = new TreeMap<>(); Map<Integer, String> names = new TreeMap<>();
		for (int i = 0; i < 1200; i++) { counts.put(100000 + i, 1L); names.put(100000 + i, "Synthetic item " + i); }
		UUID session = UUID.randomUUID(); BankRun run = BankEngine.start(SnapshotFixtures.CHARACTER_A, null, new ContainerSnapshot(counts, 1200), true, session, SnapshotFixtures.TIME);
		List<BankView> views = new ArrayList<>();
		for (int i = 0; i < 120; i++) { counts.put(100000, 2L + i); run = BankEngine.observe(run, new ContainerSnapshot(counts, 1200), session, SnapshotFixtures.TIME.plusSeconds(i)); views.add(new BankView(run, null, names, false, false, "Bank progress saved.")); }
		SwingUtilities.invokeAndWait(() ->
		{
			BankPanel panel = new BankPanel();
			for (int i = 0; i < 20; i++) { panel.render(views.get(i), null); }
			long cpu = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime(); long start = System.nanoTime();
			for (int i = 20; i < 120; i++) { panel.render(views.get(i), null); }
			System.out.printf(Locale.ROOT, "BANK_UI_1200_ITEMS updates=100 wall_ms=%.3f edt_cpu_ms=%.3f%n", (System.nanoTime() - start) / 1e6, (ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - cpu) / 1e6);
		});
	}
}
