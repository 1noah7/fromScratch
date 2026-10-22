package com.fromscratch.persistence;

import com.fromscratch.pouch.*;
import com.google.gson.*;
import java.util.*;
import static com.fromscratch.persistence.BankRunCodec.*;

/** Required format-9 state; historical files acquire review state, never invented rune quantities. */
final class PouchCodec
{
	static JsonObject state(PouchState s){JsonObject j=new JsonObject();j.addProperty("phase",s.getPhase().name());j.addProperty("preparation",s.getPreparation()==null?null:s.getPreparation().toString());j.add("recorded",map(s.getRecorded()));j.add("remaining",map(s.getRemaining()));j.add("held",map(s.getHeld()));j.add("retired",map(s.getRetired()));j.addProperty("archive",s.getArchive());return j;}
	static PouchState state(JsonObject j){keys(j,"phase","preparation","recorded","remaining","held","retired","archive");String id=nullable(j,"preparation");return new PouchState(PouchState.Phase.valueOf(str(j,"phase")),id==null?null:UUID.fromString(id),readMap(j.getAsJsonObject("recorded")),readMap(j.getAsJsonObject("remaining")),readMap(j.getAsJsonObject("held")),readMap(j.getAsJsonObject("retired")),nullable(j,"archive"));}
	static JsonObject action(PouchAction a){JsonObject j=new JsonObject();j.addProperty("kind",a.getKind().name());j.add("quantities",map(a.getQuantities()));j.addProperty("emptyConfirmed",a.isEmptyConfirmed());j.addProperty("retireMissing",a.isRetireMissing());j.addProperty("reason",a.getReason());return j;}
	static PouchAction action(JsonObject j){keys(j,"kind","quantities","emptyConfirmed","retireMissing","reason");for(String key:new String[]{"emptyConfirmed","retireMissing"}){if(!j.get(key).isJsonPrimitive()||!j.getAsJsonPrimitive(key).isBoolean()){throw new IllegalArgumentException("Invalid pouch confirmation");}}return new PouchAction(PouchAction.Kind.valueOf(str(j,"kind")),readMap(j.getAsJsonObject("quantities")),j.get("emptyConfirmed").getAsBoolean(),j.get("retireMissing").getAsBoolean(),str(j,"reason"));}
}
