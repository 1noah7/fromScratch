package com.fromscratch.bank;

import com.fromscratch.FromScratchConfig;
import java.awt.event.KeyEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;

/**
 * Blocks bank withdrawals that would take locked legacy stock. It only consumes the player's own
 * clicks and Enter presses; it never sends actions, edits menus or types into the chatbox.
 */
@Singleton
public final class WithdrawGuard implements KeyListener
{
	private final Client client;
	private final FromScratchConfig config;
	private final BankController controller;
	private final ChatMessageManager chat;
	private final KeyManager keys;

	/** Unlocked limit for an open Withdraw-X prompt, or -1 when no guarded prompt is open. */
	private volatile long typedLimit = -1;

	@Inject
	public WithdrawGuard(Client client, FromScratchConfig config, BankController controller, ChatMessageManager chat, KeyManager keys)
	{
		this.client = client;
		this.config = config;
		this.controller = controller;
		this.chat = chat;
		this.keys = keys;
	}

	public void start()
	{
		keys.registerKeyListener(this);
	}

	public void stop()
	{
		keys.unregisterKeyListener(this);
		typedLimit = -1;
	}

	public void bankClosed()
	{
		typedLimit = -1;
	}

	/** Client thread. Consumes a bank click that would withdraw more than the unlocked quantity. */
	public void clicked(MenuOptionClicked event)
	{
		if (event.isConsumed() || !config.lockLegacyWithdrawals() || event.getParam1() != InterfaceID.Bankmain.ITEMS)
		{
			return;
		}
		if (event.getMenuAction() != MenuAction.CC_OP && event.getMenuAction() != MenuAction.CC_OP_LOW_PRIORITY)
		{
			return;
		}
		BankRun run = controller.currentRun();
		int itemId = itemId(event);
		if (run == null || itemId < 0)
		{
			return;
		}
		long stack = stack(itemId);
		long requested = WithdrawRule.requested(event.getMenuOption(), stack);
		if (requested == WithdrawRule.NOT_WITHDRAWAL || stack <= 0)
		{
			return;
		}
		long unlocked = WithdrawRule.unlocked(run, itemId, stack);
		if (requested == WithdrawRule.TYPED)
		{
			if (unlocked <= 0)
			{
				block(event, unlocked);
			}
			else
			{
				// The amount is checked when the player presses Enter in the prompt.
				typedLimit = unlocked < stack ? unlocked : -1;
			}
			return;
		}
		typedLimit = -1;
		if (!WithdrawRule.allowed(requested, unlocked))
		{
			block(event, unlocked);
		}
	}

	private void block(MenuOptionClicked event, long unlocked)
	{
		event.consume();
		chat.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).runeLiteFormattedMessage(WithdrawRule.message(unlocked)).build());
	}

	private int itemId(MenuOptionClicked event)
	{
		int id = event.getItemId();
		if (id >= 0)
		{
			return id;
		}
		Widget parent = client.getWidget(event.getParam1());
		Widget item = parent == null || event.getParam0() < 0 ? null : parent.getChild(event.getParam0());
		return item == null ? -1 : item.getItemId();
	}

	private long stack(int itemId)
	{
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		return bank == null ? 0 : bank.count(itemId);
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	/** AWT thread. Holds back Enter on a guarded Withdraw-X prompt when the typed amount is too high. */
	@Override
	public void keyPressed(KeyEvent e)
	{
		long limit = typedLimit;
		if (limit < 0 || e.getKeyCode() != KeyEvent.VK_ENTER)
		{
			return;
		}
		if (client.getVarcIntValue(VarClientID.MESLAYERMODE) == 0)
		{
			// The prompt already closed; this Enter belongs to something else.
			typedLimit = -1;
			return;
		}
		long amount = WithdrawRule.typed(client.getVarcStrValue(VarClientID.MESLAYERINPUT));
		if (amount > limit)
		{
			e.consume();
			chat.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).runeLiteFormattedMessage(WithdrawRule.message(limit)).build());
			return;
		}
		typedLimit = -1;
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}
}
