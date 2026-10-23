package com.fromscratch.bank;

import com.fromscratch.buyback.BuybackEngine;
import java.text.ParseException;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.Text;

/** Pure withdrawal limits: a bank click may take at most the unlocked (earned or bought back) quantity. */
public final class WithdrawRule
{
	/** The request needs a typed amount from the Withdraw-X prompt. */
	public static final long TYPED = -1;
	/** The menu option does not take items out of the bank. */
	public static final long NOT_WITHDRAWAL = -2;

	private WithdrawRule()
	{
	}

	/** Quantity requested by a bank item menu option, given the live stack size. */
	public static long requested(String option, long stack)
	{
		String text = option == null ? "" : Text.removeTags(option).trim();
		if (text.equals("Wear") || text.equals("Wield") || text.equals("Equip"))
		{
			return Math.min(1, stack);
		}
		if (!text.startsWith("Withdraw-"))
		{
			return NOT_WITHDRAWAL;
		}
		String amount = text.substring("Withdraw-".length());
		switch (amount)
		{
			case "All":
				return stack;
			case "All-but-1":
				return Math.max(0, stack - 1);
			case "X":
				return TYPED;
			default:
				try
				{
					return Math.min(Long.parseLong(amount), stack);
				}
				catch (NumberFormatException ex)
				{
					// Unknown wording: block rather than risk withdrawing legacy items.
					return stack;
				}
		}
	}

	/** Unlocked units of one item that may leave the bank, capped by the live stack. */
	public static long unlocked(BankRun run, int itemId, long stack)
	{
		long balance = BuybackEngine.balances(run).getOrDefault(itemId, 0L);
		return Math.max(0, Math.min(balance, stack));
	}

	/** A typed Withdraw-X amount, or -1 when it cannot be read. Accepts k/m/b suffixes like the game. */
	public static long typed(String input)
	{
		if (input == null || input.trim().isEmpty())
		{
			return -1;
		}
		try
		{
			return QuantityFormatter.parseQuantity(input.trim());
		}
		catch (ParseException ex)
		{
			return -1;
		}
	}

	public static boolean allowed(long requested, long unlocked)
	{
		return requested <= unlocked;
	}

	public static String message(long unlocked)
	{
		if (unlocked <= 0)
		{
			return "From Scratch: this item is locked legacy stock. Earn it or unlock it with a buyback first.";
		}
		return "From Scratch: only " + QuantityFormatter.formatNumber(unlocked) + " of this item " + (unlocked == 1 ? "is" : "are")
			+ " unlocked. Withdraw that many or fewer.";
	}
}
