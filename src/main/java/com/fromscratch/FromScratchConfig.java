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
		name = "Detailed setup tooltip",
		description = "Show extra snapshot details in a tooltip before initialization",
		position = 0
	)
	default boolean showFoundationHelp()
	{
		return true;
	}
}
