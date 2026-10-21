package com.fromscratch;

import java.awt.Color;
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

	@ConfigItem(keyName = "bankVirtualQuantities", name = "Virtual bank quantities",
		description = "Show From Scratch bank-available quantities at bottom-right", position = 1)
	default boolean bankVirtualQuantities() { return true; }

	@ConfigItem(keyName = "lockedBankItems", name = "Dim locked bank items",
		description = "Dim items with zero virtual bank quantity while keeping ordinary quantity labels visible", position = 2)
	default boolean lockedBankItems() { return true; }

	@ConfigItem(keyName = "virtualQuantityColor", name = "Virtual quantity color",
		description = "Color of the virtual bank quantity", position = 3)
	default Color virtualQuantityColor() { return new Color(0xB450FF); }
}
