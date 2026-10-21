package com.fromscratch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/** Local presentation preferences; progression will be stored separately per character. */
@ConfigGroup(FromScratchConfig.GROUP)
public interface FromScratchConfig extends Config
{
	String GROUP = "fromscratch";

	@ConfigItem(
		keyName = "showFoundationHelp",
		name = "Show foundation help",
		description = "Show the explanation of features planned after the foundation milestone",
		position = 0
	)
	default boolean showFoundationHelp()
	{
		return true;
	}
}
