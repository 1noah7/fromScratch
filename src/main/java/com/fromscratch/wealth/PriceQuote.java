package com.fromscratch.wealth;

import java.time.Instant;
import lombok.Value;

/** A received price record, not a claimed exchange execution price. Null unit value means unknown. */
@Value
public class PriceQuote
{
	public enum Source { RUNELITE_GE_GUIDE, COINS, UNKNOWN }
	int priceId;
	Long unitGp;
	Instant receivedAt;
	Source source;
	boolean stale;
	public PriceQuote(int priceId, Long unitGp, Instant receivedAt, Source source, boolean stale)
	{
		if (priceId < 0 || source == null || (unitGp != null && (unitGp < 0 || receivedAt == null || source == Source.UNKNOWN)) || (unitGp == null && source != Source.UNKNOWN)) { throw new IllegalArgumentException("Invalid price metadata"); }
		if(source==Source.COINS&&(priceId!=net.runelite.api.gameval.ItemID.COINS||unitGp!=1L||stale)){throw new IllegalArgumentException("Invalid exact coin quote");}
		this.priceId=priceId; this.unitGp=unitGp; this.receivedAt=receivedAt; this.source=source; this.stale=stale;
	}
	public static PriceQuote unknown(int id) { return new PriceQuote(id,null,null,Source.UNKNOWN,false); }
	public static PriceQuote coins(Instant at) { return new PriceQuote(net.runelite.api.gameval.ItemID.COINS,1L,at,Source.COINS,false); }
	public boolean verifiedNow() { return unitGp != null && !stale; }
}
