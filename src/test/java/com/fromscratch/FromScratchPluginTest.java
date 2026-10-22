package com.fromscratch;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.eventbus.EventBus;
import org.junit.Test;

/** Development launcher; this main method is not an automated acceptance test. */
public class FromScratchPluginTest
{
	@Test public void pluginCanBeConstructedAndRegisteredAcrossEnableCycles()
	{
		// PluginManager registers this exact object after startUp. Real EventBus validation
		// catches invalid subscriber names that compilation and filter fixtures cannot detect.
		FromScratchPlugin plugin=new FromScratchPlugin();EventBus events=new EventBus();
		events.register(plugin);events.unregister(plugin);events.register(plugin);events.unregister(plugin);
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FromScratchPlugin.class);
		RuneLite.main(args);
	}
}
