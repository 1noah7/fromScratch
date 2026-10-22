package com.fromscratch.ge;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Observed execution evidence; reported GP is not asserted to be a verified after-tax settlement or profit. */
@Value
public class GeTrade
{
	public enum Evidence { FILL, COMPATIBLE_NET_FILL, EXISTING_SNAPSHOT, STATUS, UNCERTAIN }
	UUID id;
	UUID generation;
	int slot;
	int itemId;
	String name;
	boolean buy;
	long quantity;
	long reportedGp;
	Long estimatedTax;
	String taxMethod;
	String status;
	GeOffer.Origin origin;
	Evidence evidence;
	Instant at;
	public GeTrade(UUID id,UUID generation,int slot,int itemId,String name,boolean buy,long quantity,long reportedGp,
		Long estimatedTax,String taxMethod,String status,GeOffer.Origin origin,Evidence evidence,Instant at)
	{
		if(id==null||generation==null||slot<0||slot>=8||itemId<0||name==null||name.length()>200||quantity<0||reportedGp<0
			||estimatedTax!=null&&(estimatedTax<0||estimatedTax>reportedGp)||taxMethod==null||taxMethod.length()>200||status==null||status.length()>240||origin==null||evidence==null||at==null){throw new IllegalArgumentException("Invalid GE trade evidence");}
		this.id=id;this.generation=generation;this.slot=slot;this.itemId=itemId;this.name=name;this.buy=buy;this.quantity=quantity;this.reportedGp=reportedGp;
		this.estimatedTax=estimatedTax;this.taxMethod=taxMethod;this.status=status;this.origin=origin;this.evidence=evidence;this.at=at;
	}
	public boolean execution(){return evidence==Evidence.FILL||evidence==Evidence.COMPATIBLE_NET_FILL;}
	/** Only derived completed offers belong in the public history, never raw lifecycle/fill evidence. */
	public boolean visibleInHistory(){return quantity>0&&(status.equals("Bought")||status.equals("Bought (Partial)")||status.equals("Sold")||status.equals("Sold (Partial)"));}
}
