package com.fromscratch.playtime;

import java.time.Instant;
import java.util.*;
import lombok.Value;

/** Host-observed game ticks, independent of run age and economic transactions. */
@Value
public class Playtime
{
	String character;
	UUID run;
	UUID revision;
	Instant measuredFrom;
	long ticks;
	public Playtime(String character,UUID run,UUID revision,Instant measuredFrom,long ticks)
	{
		if(character==null||!character.matches("[a-f0-9]{64}")||ticks<0) { throw new IllegalArgumentException("Invalid playtime"); }
		this.character=character;this.run=Objects.requireNonNull(run);this.revision=Objects.requireNonNull(revision);
		this.measuredFrom=Objects.requireNonNull(measuredFrom);this.ticks=ticks;
	}
	public static String format(long ticks)
	{
		long minutes=Math.max(0,ticks)/100; // 100 server ticks at 600 ms each; no wall-clock intervals.
		long number=minutes<60?minutes:minutes<1440?minutes/60:minutes/1440;
		String unit=minutes<60?"minute":minutes<1440?"hour":"day";
		return number+" "+unit+(number==1?"":"s");
	}
}
