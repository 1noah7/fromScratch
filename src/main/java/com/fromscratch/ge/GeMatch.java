package com.fromscratch.ge;

import java.util.*;
import lombok.Value;

/** Possible attribution only: observable compatibility cannot identify indistinguishable physical copies. */
@Value
public class GeMatch
{
	UUID tradeId;
	UUID bankEvent;
	int itemId;
	long quantity;
	boolean deposit;
	GeOffer.Origin origin;
	public GeMatch(UUID tradeId,UUID bankEvent,int itemId,long quantity,boolean deposit)
	{this(tradeId,bankEvent,itemId,quantity,deposit,GeOffer.Origin.UNKNOWN);}
	public GeMatch(UUID tradeId,UUID bankEvent,int itemId,long quantity,boolean deposit,GeOffer.Origin origin)
	{if(tradeId==null||bankEvent==null||itemId<0||quantity<=0||origin==null){throw new IllegalArgumentException("Invalid GE bank attribution");}this.tradeId=tradeId;this.bankEvent=bankEvent;this.itemId=itemId;this.quantity=quantity;this.deposit=deposit;this.origin=origin;}
}
