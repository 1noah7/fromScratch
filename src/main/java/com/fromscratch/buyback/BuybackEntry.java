package com.fromscratch.buyback;

import java.util.*;
import lombok.Value;

/** Curated purchase; each required role grants exactly one physical-copy entitlement. */
@Value
public class BuybackEntry
{
	String id;
	String name;
	String category;
	int icon;
	long price;
	Map<String,Integer> roles;
	public BuybackEntry(String id,String name,String category,int icon,long price,Map<String,Integer> roles)
	{
		this.id=id;this.name=name;this.category=category;this.icon=icon;this.price=price;
		this.roles=Collections.unmodifiableMap(new LinkedHashMap<>(roles));
	}
}
