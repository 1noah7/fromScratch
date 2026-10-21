package com.fromscratch.persistence;

import com.fromscratch.wealth.*;
import com.google.gson.*;
import java.time.Instant;
import java.util.*;
import static com.fromscratch.persistence.BankRunCodec.*;

/** Explicit financial JSON validation; historical quote absence is never defaulted to a verified zero. */
final class FinancialCodec
{
	private FinancialCodec(){}
	static JsonObject state(FinancialState s)
	{
		JsonObject p=new JsonObject();p.addProperty("from",s.getMeasuredFrom().toString());p.addProperty("imported",s.isImportedBaseline());p.add("highWater",map(s.getHighWater()));p.addProperty("gained",s.getGainedGp());p.add("unpriced",map(s.getUnpricedGains()));p.addProperty("expense",s.getExpenseGp());p.addProperty("expenseRecords",s.getExpenseRecords());p.addProperty("eventCount",s.getEventCount());p.addProperty("archive",s.getImportArchive());return p;
	}
	static FinancialState state(JsonObject p)
	{
		keys(p,"from","imported","highWater","gained","unpriced","expense","expenseRecords","eventCount","archive");
		if(!p.get("imported").isJsonPrimitive()||!p.getAsJsonPrimitive("imported").isBoolean()){throw new IllegalArgumentException("Invalid imported flag");}
		return new FinancialState(Instant.parse(str(p,"from")),p.get("imported").getAsBoolean(),readMap(p.getAsJsonObject("highWater")),number(p.get("gained")),readMap(p.getAsJsonObject("unpriced")),number(p.get("expense")),number(p.get("expenseRecords")),number(p.get("eventCount")),nullable(p,"archive"));
	}
	static JsonArray events(List<ValuationEvent> events)
	{
		JsonArray result=new JsonArray();for(ValuationEvent e:events){JsonObject p=new JsonObject();p.addProperty("id",e.getId().toString());p.addProperty("kind",e.getKind().name());p.addProperty("item",e.getItemId());p.addProperty("quantity",e.getQuantity());p.addProperty("reason",e.getReason());PriceQuote q=e.getQuote();JsonObject quote=new JsonObject();quote.addProperty("priceId",q.getPriceId());quote.addProperty("unit",q.getUnitGp());quote.addProperty("received",q.getReceivedAt()==null?null:q.getReceivedAt().toString());quote.addProperty("source",q.getSource().name());quote.addProperty("stale",q.isStale());p.add("quote",quote);result.add(p);}return result;
	}
	static List<ValuationEvent> events(JsonArray rows)
	{
		List<ValuationEvent> result=new ArrayList<>();Set<UUID> seen=new HashSet<>();for(JsonElement value:rows){JsonObject p=value.getAsJsonObject();keys(p,"id","kind","item","quantity","reason","quote");JsonObject q=p.getAsJsonObject("quote");keys(q,"priceId","unit","received","source","stale");if(!q.get("stale").isJsonPrimitive()||!q.getAsJsonPrimitive("stale").isBoolean()){throw new IllegalArgumentException("Invalid quote flag");}UUID id=UUID.fromString(str(p,"id"));if(!seen.add(id)){throw new IllegalArgumentException("Duplicate financial evidence");}String received=nullable(q,"received");PriceQuote quote=new PriceQuote(Math.toIntExact(number(q.get("priceId"))),q.get("unit").isJsonNull()?null:number(q.get("unit")),received==null?null:Instant.parse(received),PriceQuote.Source.valueOf(str(q,"source")),q.get("stale").getAsBoolean());result.add(new ValuationEvent(id,ValuationEvent.Kind.valueOf(str(p,"kind")),Math.toIntExact(number(p.get("item"))),number(p.get("quantity")),quote,str(p,"reason")));}return result;
	}
}
