package com.fromscratch.ge;

import com.fromscratch.bank.BankHistory;
import java.util.*;
import lombok.Value;

/** Read-only bounded journal page, separate from bank movement history. */
public final class GeHistory
{
	private GeHistory(){}
	@Value public static class Page
	{
		List<GeTrade> rows;
		BankHistory.Cursor next;
		String message;
		public Page(List<GeTrade> rows,BankHistory.Cursor next,String message){this.rows=Collections.unmodifiableList(new ArrayList<>(rows));this.next=next;this.message=message;}
	}
}
