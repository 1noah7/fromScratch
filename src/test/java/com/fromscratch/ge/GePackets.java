package com.fromscratch.ge;

import java.util.function.Consumer;
import net.runelite.api.*;

/** Test-only real API packet adapter feeding the same live source as plugin events. */
public final class GePackets
{
	private final RuneLiteGeSource source=new RuneLiteGeSource(new RuneLiteGeSource.Access()
	{public boolean loggedIn(){return true;}public GrandExchangeOffer[] offers(){return null;}public String itemName(int id){return "Fixture item "+id;}});
	public void reset(){source.resetSession();}
	public void send(Consumer<GeObservation> sink,int slot,int item,GrandExchangeOfferState state,int total,int filled,long limit,long gp)
	{
		GrandExchangeOfferChangedPacket packet=new GrandExchangeOfferChangedPacket(item,state,total,filled,limit,gp);
		// Same boundary as FromScratchPlugin.onGrandExchangeOfferChanged -> source -> GameTick drain.
		source.changed(slot,packet);source.drain().forEach(sink);
	}
	private static final class GrandExchangeOfferChangedPacket implements GrandExchangeOffer
	{
		private final int item,total,filled;private final long limit,gp;private final GrandExchangeOfferState state;
		private GrandExchangeOfferChangedPacket(int item,GrandExchangeOfferState state,int total,int filled,long limit,long gp){this.item=item;this.state=state;this.total=total;this.filled=filled;this.limit=limit;this.gp=gp;}
		public int getItemId(){return item;}public int getTotalQuantity(){return total;}public int getQuantitySold(){return filled;}public long getPrice(){return limit;}public long getSpent(){return gp;}public GrandExchangeOfferState getState(){return state;}
	}
}
