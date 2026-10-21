package com.fromscratch.bank;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Bounded read-only history pages. Cursors are internal and never displayed to players. */
public final class BankHistory
{
	private BankHistory() { }
	@Value public static class Cursor { UUID revision; int offset; }
	@Value public static class Row { Instant at; int itemId; String name; long change; }
	@Value public static class Page
	{
		List<Row> rows;
		Cursor next;
		String message;
		public Page(List<Row> rows, Cursor next, String message) { this.rows = Collections.unmodifiableList(new ArrayList<>(rows)); this.next = next; this.message = message; }
	}
}
