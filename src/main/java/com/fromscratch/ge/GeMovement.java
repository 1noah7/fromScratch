package com.fromscratch.ge;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Bounded unmatched withdrawal evidence, for later association without a second ownership adjustment. */
@Value
public class GeMovement
{
	UUID bankEvent;
	int itemId;
	long remaining;
	Instant at;
	public GeMovement(UUID bankEvent,int itemId,long remaining,Instant at)
	{if(bankEvent==null||itemId<0||remaining<=0||at==null){throw new IllegalArgumentException("Invalid bank movement evidence");}this.bankEvent=bankEvent;this.itemId=itemId;this.remaining=remaining;this.at=at;}
}
