package com.fromscratch.pouch;

import java.util.*;
import net.runelite.api.gameval.ItemID;

/** Real rune IDs, verified against the 1.13.3 enum/item definitions; no fake/minigame runes. */
public final class PouchRunes
{
	private static final Map<Integer,String> IDS=new LinkedHashMap<>();
	static
	{
		add(ItemID.AIRRUNE,"Air rune");add(ItemID.WATERRUNE,"Water rune");add(ItemID.EARTHRUNE,"Earth rune");add(ItemID.FIRERUNE,"Fire rune");add(ItemID.MINDRUNE,"Mind rune");add(ItemID.CHAOSRUNE,"Chaos rune");add(ItemID.DEATHRUNE,"Death rune");add(ItemID.BLOODRUNE,"Blood rune");add(ItemID.COSMICRUNE,"Cosmic rune");add(ItemID.NATURERUNE,"Nature rune");add(ItemID.LAWRUNE,"Law rune");add(ItemID.BODYRUNE,"Body rune");add(ItemID.SOULRUNE,"Soul rune");add(ItemID.ASTRALRUNE,"Astral rune");add(ItemID.MISTRUNE,"Mist rune");add(ItemID.MUDRUNE,"Mud rune");add(ItemID.DUSTRUNE,"Dust rune");add(ItemID.LAVARUNE,"Lava rune");add(ItemID.STEAMRUNE,"Steam rune");add(ItemID.SMOKERUNE,"Smoke rune");add(ItemID.WRATHRUNE,"Wrath rune");add(ItemID.SUNFIRERUNE,"Sunfire rune");add(ItemID.AETHERRUNE,"Aether rune");
	}
	private PouchRunes(){}private static void add(int id,String name){IDS.put(id,name);}
	public static Map<Integer,String> ids(){return Collections.unmodifiableMap(IDS);}
}
