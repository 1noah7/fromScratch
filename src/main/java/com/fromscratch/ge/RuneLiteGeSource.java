package com.fromscratch.ge;

import java.time.Instant;
import java.util.*;
import javax.inject.*;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;

/** Client-thread passive offer observation. No GE UI, inventory, menu or action manipulation. */
@Singleton
public final class RuneLiteGeSource
{
	interface Access { boolean loggedIn(); GrandExchangeOffer[] offers(); String itemName(int id); }
	private final Access access;
	private UUID session=UUID.randomUUID();
	private final boolean[] seen=new boolean[8];
	private final Deque<GeObservation> pending=new ArrayDeque<>();
	private boolean seeded;
	private boolean overflow;
	@Inject public RuneLiteGeSource(Client client)
	{
		this(new Access(){public boolean loggedIn(){return client.getGameState()==GameState.LOGGED_IN&&client.getEnvironment()==0;}public GrandExchangeOffer[] offers(){return client.getGrandExchangeOffers();}public String itemName(int id){return client.getItemDefinition(id).getName();}});
	}
	RuneLiteGeSource(Access access){this.access=access;}
	public void resetSession(){session=UUID.randomUUID();Arrays.fill(seen,false);pending.clear();seeded=false;overflow=false;}
	public void changed(int slot,GrandExchangeOffer offer)
	{
		if(!access.loggedIn()||offer==null||slot<0||slot>=8){return;}
		boolean initial=!seen[slot];
		if(offer.getState()==GrandExchangeOfferState.EMPTY&&initial){return;}
		try
		{
			GeObservation copy=new GeObservation(UUID.randomUUID(),session,Instant.now(),slot,offer.getState()==GrandExchangeOfferState.EMPTY?0:offer.getItemId(),
				offer.getState()==GrandExchangeOfferState.EMPTY?"":access.itemName(offer.getItemId()),offer.getState(),offer.getTotalQuantity(),offer.getQuantitySold(),offer.getPrice(),offer.getSpent(),initial);
			if(pending.size()>=256){overflow=true;return;}pending.addLast(copy);seen[slot]=true;
		}
		catch(IllegalArgumentException ex){overflow=true; /* Report a gap; never turn invalid packets into fills. */}
	}
	/** Seed only nonempty current snapshots, then drain copied events before the same tick's bank comparison. */
	public List<GeObservation> drain()
	{
		if(!access.loggedIn()){return Collections.emptyList();}
		if(!seeded)
		{
			GrandExchangeOffer[] offers=access.offers();
			if(offers!=null&&offers.length==8){for(int slot=0;slot<offers.length;slot++){if(!seen[slot]){changed(slot,offers[slot]);}}seeded=true;}
		}
		List<GeObservation> result=new ArrayList<>(pending);pending.clear();return result;
	}
	public boolean takeObservationGap(){boolean gap=overflow;overflow=false;return gap;}
}
