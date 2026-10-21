package com.fromscratch.persistence;

import com.fromscratch.ge.*;
import com.google.gson.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.GrandExchangeOfferState;
import static com.fromscratch.persistence.BankRunCodec.*;

/** Explicit schema-7 GE trees. No reflective domain deserialization or missing-field defaults. */
final class GeCodec
{
	private GeCodec(){}
	static JsonObject state(GeState s)
	{
		JsonObject p=new JsonObject(),offers=new JsonObject();s.getOffers().forEach((slot,offer)->{JsonObject o=new JsonObject();put(o,"generation",offer.getGeneration());o.add("last",observation(offer.getLast()));put(o,"origin",offer.getOrigin());o.addProperty("closed",offer.isClosed());o.addProperty("uncertain",offer.isUncertain());offers.add(slot.toString(),o);});p.add("offers",offers);
		JsonArray credits=new JsonArray();for(GeCredit c:s.getCredits()){JsonObject o=new JsonObject();put(o,"id",c.getId());put(o,"trade",c.getTradeId());o.addProperty("item",c.getItemId());o.addProperty("remaining",c.getRemaining());put(o,"kind",c.getKind());put(o,"at",c.getAt());put(o,"origin",c.getOrigin());credits.add(o);}p.add("credits",credits);
		JsonArray movements=new JsonArray();for(GeMovement m:s.getMovements()){JsonObject o=new JsonObject();put(o,"bank",m.getBankEvent());o.addProperty("item",m.getItemId());o.addProperty("remaining",m.getRemaining());put(o,"at",m.getAt());movements.add(o);}p.add("movements",movements);
		p.add("recent",trades(s.getRecent()));p.addProperty("purchases",s.getPurchases());p.addProperty("sales",s.getSales());p.addProperty("estimatedTax",s.getEstimatedTaxGp());p.addProperty("taxRecords",s.getTaxRecords());put(p,"archive",s.getArchive());return p;
	}
	static GeState state(JsonObject p)
	{
		keys(p,"offers","credits","movements","recent","purchases","sales","estimatedTax","taxRecords","archive");Map<Integer,GeOffer> offers=new TreeMap<>();
		for(Map.Entry<String,JsonElement> entry:p.getAsJsonObject("offers").entrySet())
		{
			if(!entry.getKey().matches("[0-7]")){throw new IllegalArgumentException("Invalid GE slot");}JsonObject o=entry.getValue().getAsJsonObject();keys(o,"generation","last","origin","closed","uncertain");offers.put(Integer.parseInt(entry.getKey()),new GeOffer(uuid(o,"generation"),observation(o.getAsJsonObject("last")),GeOffer.Origin.valueOf(str(o,"origin")),bool(o,"closed"),bool(o,"uncertain")));
		}
		List<GeCredit> credits=new ArrayList<>();for(JsonElement e:p.getAsJsonArray("credits")){JsonObject o=e.getAsJsonObject();keys(o,"id","trade","item","remaining","kind","at","origin");credits.add(new GeCredit(uuid(o,"id"),uuid(o,"trade"),integer(o,"item"),number(o.get("remaining")),GeCredit.Kind.valueOf(str(o,"kind")),at(o),GeOffer.Origin.valueOf(str(o,"origin"))));}
		List<GeMovement> movements=new ArrayList<>();for(JsonElement e:p.getAsJsonArray("movements")){JsonObject o=e.getAsJsonObject();keys(o,"bank","item","remaining","at");movements.add(new GeMovement(uuid(o,"bank"),integer(o,"item"),number(o.get("remaining")),at(o)));}
		return new GeState(offers,credits,movements,trades(p.getAsJsonArray("recent")),number(p.get("purchases")),number(p.get("sales")),number(p.get("estimatedTax")),number(p.get("taxRecords")),nullable(p,"archive"));
	}
	static JsonObject update(GeUpdate u)
	{
		JsonObject p=new JsonObject();p.add("observation",u.getObservation()==null?JsonNull.INSTANCE:observation(u.getObservation()));p.add("trades",trades(u.getTrades()));
		JsonArray matches=new JsonArray();for(GeMatch m:u.getMatches()){JsonObject o=new JsonObject();put(o,"trade",m.getTradeId());put(o,"bank",m.getBankEvent());o.addProperty("item",m.getItemId());o.addProperty("quantity",m.getQuantity());o.addProperty("deposit",m.isDeposit());put(o,"origin",m.getOrigin());matches.add(o);}p.add("matches",matches);p.add("exclusions",map(u.getGainExclusions()));return p;
	}
	static GeUpdate update(JsonObject p)
	{
		keys(p,"observation","trades","matches","exclusions");List<GeMatch> matches=new ArrayList<>();for(JsonElement e:p.getAsJsonArray("matches")){JsonObject o=e.getAsJsonObject();keys(o,"trade","bank","item","quantity","deposit","origin");matches.add(new GeMatch(uuid(o,"trade"),uuid(o,"bank"),integer(o,"item"),number(o.get("quantity")),bool(o,"deposit"),GeOffer.Origin.valueOf(str(o,"origin"))));}
		return new GeUpdate(p.get("observation").isJsonNull()?null:observation(p.getAsJsonObject("observation")),trades(p.getAsJsonArray("trades")),matches,readMap(p.getAsJsonObject("exclusions")));
	}
	private static JsonObject observation(GeObservation v)
	{
		JsonObject p=new JsonObject();put(p,"id",v.getEventId());put(p,"session",v.getSession());put(p,"at",v.getAt());p.addProperty("slot",v.getSlot());p.addProperty("item",v.getItemId());put(p,"name",v.getName());put(p,"state",v.getState());p.addProperty("total",v.getTotal());p.addProperty("filled",v.getFilled());p.addProperty("limit",v.getLimitPrice());p.addProperty("reported",v.getReportedGp());p.addProperty("initial",v.isInitial());p.addProperty("beforeStart",v.isBeforeStart());return p;
	}
	private static GeObservation observation(JsonObject p)
	{
		keys(p,"id","session","at","slot","item","name","state","total","filled","limit","reported","initial","beforeStart");return new GeObservation(uuid(p,"id"),uuid(p,"session"),at(p),integer(p,"slot"),integer(p,"item"),str(p,"name"),GrandExchangeOfferState.valueOf(str(p,"state")),number(p.get("total")),number(p.get("filled")),number(p.get("limit")),number(p.get("reported")),bool(p,"initial"),bool(p,"beforeStart"));
	}
	private static JsonArray trades(List<GeTrade> rows)
	{
		JsonArray result=new JsonArray();for(GeTrade t:rows){JsonObject o=new JsonObject();put(o,"id",t.getId());put(o,"generation",t.getGeneration());o.addProperty("slot",t.getSlot());o.addProperty("item",t.getItemId());put(o,"name",t.getName());o.addProperty("buy",t.isBuy());o.addProperty("quantity",t.getQuantity());o.addProperty("reported",t.getReportedGp());o.addProperty("tax",t.getEstimatedTax());put(o,"method",t.getTaxMethod());put(o,"status",t.getStatus());put(o,"origin",t.getOrigin());put(o,"evidence",t.getEvidence());put(o,"at",t.getAt());result.add(o);}return result;
	}
	private static List<GeTrade> trades(JsonArray array)
	{
		List<GeTrade> result=new ArrayList<>();for(JsonElement e:array){JsonObject o=e.getAsJsonObject();keys(o,"id","generation","slot","item","name","buy","quantity","reported","tax","method","status","origin","evidence","at");result.add(new GeTrade(uuid(o,"id"),uuid(o,"generation"),integer(o,"slot"),integer(o,"item"),str(o,"name"),bool(o,"buy"),number(o.get("quantity")),number(o.get("reported")),o.get("tax").isJsonNull()?null:number(o.get("tax")),str(o,"method"),str(o,"status"),GeOffer.Origin.valueOf(str(o,"origin")),GeTrade.Evidence.valueOf(str(o,"evidence")),at(o)));}return result;
	}
	private static void put(JsonObject p,String key,Object value){p.addProperty(key,value==null?null:value.toString());}
	private static UUID uuid(JsonObject p,String key){return UUID.fromString(str(p,key));}
	private static Instant at(JsonObject p){return Instant.parse(str(p,"at"));}
	private static int integer(JsonObject p,String key){return Math.toIntExact(number(p.get(key)));}
	private static boolean bool(JsonObject p,String key){JsonPrimitive v=p.getAsJsonPrimitive(key);if(!v.isBoolean()){throw new IllegalArgumentException("Invalid GE flag");}return v.getAsBoolean();}
}
