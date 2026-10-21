package com.fromscratch.ge;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** A bounded possible bank conversion, never an owned asset or a second bank quantity. */
@Value
public class GeCredit
{
	public enum Kind { PURCHASE, SALE_PROCEEDS, BUY_REFUND, UNSOLD_RETURN }
	UUID id;
	UUID tradeId;
	int itemId;
	long remaining;
	Kind kind;
	Instant at;
	GeOffer.Origin origin;
	public GeCredit(UUID id,UUID tradeId,int itemId,long remaining,Kind kind,Instant at)
	{this(id,tradeId,itemId,remaining,kind,at,GeOffer.Origin.UNKNOWN);}
	public GeCredit(UUID id,UUID tradeId,int itemId,long remaining,Kind kind,Instant at,GeOffer.Origin origin)
	{if(id==null||tradeId==null||itemId<0||remaining<=0||kind==null||at==null||origin==null){throw new IllegalArgumentException("Invalid GE conversion budget");}this.id=id;this.tradeId=tradeId;this.itemId=itemId;this.remaining=remaining;this.kind=kind;this.at=at;this.origin=origin;}
	public GeCredit remaining(long n){return new GeCredit(id,tradeId,itemId,n,kind,at,origin);}
}
