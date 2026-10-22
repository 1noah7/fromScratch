package com.fromscratch.beta;

import com.fromscratch.FromScratchPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Owner-distributed bootstrap, excluded from the Plugin Hub plugin JAR. No authentication code. */
public final class BetaLauncher
{
	private BetaLauncher() { }

	public static void main(String[] args) throws Exception
	{
		if (args.length == 1 && "--help".equals(args[0]))
		{
			RuneLite.main(new String[]{"--help"});
			return;
		}
		if (args.length != 0)
		{
			throw new IllegalArgumentException("Use the supplied launcher, or --help. Authentication is handled by RuneLite's Jagex Accounts panel.");
		}
		ExternalPluginManager.loadBuiltin(FromScratchPlugin.class);
		RuneLite.main(new String[]{"--developer-mode", "--disable-telemetry"});
	}
}
