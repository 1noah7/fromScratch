package com.fromscratch;

import com.fromscratch.ui.FromScratchSidebar;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Wires RuneLite lifecycle events to the foundation UI. Game observation and
 * accounting deliberately enter through separate services in later milestones.
 */
@Slf4j
@PluginDescriptor(
	name = "From Scratch",
	internalName = "from_scratch",
	configName = FromScratchConfig.GROUP,
	description = "Start fresh with a separate virtual economy and progression",
	tags = {"bank", "progression", "challenge", "virtual", "ironman", "wealth"}
)
public class FromScratchPlugin extends Plugin
{
	@Inject
	private FromScratchSidebar sidebar;

	@Inject
	private FromScratchConfig config;

	@Override
	protected void startUp()
	{
		// Queue UI work without blocking the client thread. Lifecycle operations
		// share the EDT queue, so rapid enable/disable sequences retain their order.
		SwingUtilities.invokeLater(() -> sidebar.show(config.showFoundationHelp()));
		log.debug("From Scratch foundation started");
	}

	@Override
	protected void shutDown()
	{
		SwingUtilities.invokeLater(sidebar::hide);
		log.debug("From Scratch foundation stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (FromScratchConfig.GROUP.equals(event.getGroup()))
		{
			SwingUtilities.invokeLater(() -> sidebar.refresh(config.showFoundationHelp()));
		}
	}

	@Provides
	FromScratchConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FromScratchConfig.class);
	}
}
