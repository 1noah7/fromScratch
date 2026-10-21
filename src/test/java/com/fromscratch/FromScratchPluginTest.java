package com.fromscratch;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Development launcher; this main method is not an automated acceptance test. */
public class FromScratchPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FromScratchPlugin.class);
		RuneLite.main(args);
	}
}
