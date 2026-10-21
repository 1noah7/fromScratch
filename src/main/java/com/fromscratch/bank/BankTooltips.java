package com.fromscratch.bank;

import com.fromscratch.wealth.BankValuation;
import com.fromscratch.wealth.FinancialState;
import com.fromscratch.wealth.GpFormatter;

/** Presentation only: bounded Swing HTML, escaped text and no technical accounting timestamps. */
final class BankTooltips
{
	static final int WIDTH = 250;
	private BankTooltips() { }

	static String html(String title, String... lines)
	{
		StringBuilder result = new StringBuilder("<html><div style='width:").append(WIDTH)
			// Swing's legacy HTML renderer enlarges CSS px by 4/3; pt matches its logical layout units.
			.append("pt;font-family:Dialog;font-size:11pt'><b>").append(escape(title)).append("</b>");
		for (String line : lines) { if (line != null && !line.isEmpty()) { result.append("<br>").append(escape(line)); } }
		return result.append("</div></html>").toString();
	}

	static String gained(FinancialState state)
	{
		if (state == null) { return html("Estimated Value Gained", "Estimated bank gains are unavailable."); }
		int unknown = state.getUnpricedGains().size();
		return html("Estimated Value Gained", "Estimated bank gains: " + GpFormatter.exact(state.getGainedGp()) + ".",
			"Tracks bank progress, not every acquisition.", unknown == 0 ? null : unknown == 1
				? "1 item type has an unknown historical price." : unknown + " item types have unknown historical prices.");
	}

	static String netWorth(BankValuation market)
	{
		if (market == null) { return html("Current Bank Net Worth", "Bank value is currently unavailable."); }
		String amount = market.getSubtotalGp() == null ? "Value is too large to display."
			: market.getPricedTypes() == 0 && market.getMissingTypes() > 0 ? "Prices are currently unavailable."
			: (market.getMissingTypes() > 0 ? "Priced subtotal: " : "Estimated bank value: ") + GpFormatter.exact(market.getSubtotalGp()) + ".";
		int missing = market.getMissingTypes(), stale = market.getStaleTypes();
		return html("Current Bank Net Worth", amount, "GE estimates; coins counted once.",
			missing == 0 ? null : missing == 1 ? "1 item type has no available price." : missing + " item types have no available price.",
			stale == 0 ? null : stale == 1 ? "1 item type uses an older price." : stale + " item types use older prices.");
	}

	static String bankGp(long gp)
	{
		return html("Bank GP", "Bank coin balance: " + GpFormatter.exact(gp) + ".", "Signed bank coins, not a spendable wallet.");
	}

	static String expenses(FinancialState state)
	{
		if (state == null || state.getExpenseRecords() == 0)
		{
			return html("Recorded Losses / Expenses", "Losses and spending are not tracked automatically.", "Click to record a verified expense.");
		}
		long count = state.getExpenseRecords();
		return html("Recorded Losses / Expenses", "Recorded amount: " + GpFormatter.exact(state.getExpenseGp()) + ".",
			count + (count == 1 ? " recorded expense." : " recorded expenses."), "Click to record another expense.");
	}

	static String escape(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}
}
