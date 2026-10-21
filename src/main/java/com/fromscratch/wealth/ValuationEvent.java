package com.fromscratch.wealth;

import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.Value;

/** Immutable valuation evidence embedded in the same durable bank audit as its quantity change. */
@Value
public class ValuationEvent
{
	public enum Kind { GAIN, EXPENSE }
	UUID id;
	Kind kind;
	int itemId;
	long quantity;
	PriceQuote quote;
	String reason;
	public ValuationEvent(UUID id, Kind kind, int itemId, long quantity, PriceQuote quote, String reason)
	{
		if(id==null||kind==null||itemId<0||quantity<=0||quote==null||reason==null||reason.trim().isEmpty()||reason.length()>240){throw new IllegalArgumentException("Invalid financial event");}
		this.id=id;this.kind=kind;this.itemId=itemId;this.quantity=quantity;this.quote=quote;this.reason=reason;
	}
	public static UUID identity(UUID bankEvent, Kind kind, int itemId) { return UUID.nameUUIDFromBytes((bankEvent+":"+kind+":"+itemId).getBytes(StandardCharsets.UTF_8)); }
	public long knownGp() { return quote.verifiedNow()?Math.multiplyExact(quantity,quote.getUnitGp()):0; }
}
