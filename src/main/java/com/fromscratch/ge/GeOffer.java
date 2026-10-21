package com.fromscratch.ge;

import java.util.*;
import lombok.Value;

/** Local generation identity. RuneLite supplies no server offer UUID. */
@Value
public class GeOffer
{
	public enum Origin { OBSERVED_NEW, PREEXISTING, UNKNOWN }
	UUID generation;
	GeObservation last;
	Origin origin;
	boolean closed;
	boolean uncertain;
	public GeOffer(UUID generation,GeObservation last,Origin origin,boolean closed,boolean uncertain)
	{this.generation=Objects.requireNonNull(generation);this.last=Objects.requireNonNull(last);this.origin=Objects.requireNonNull(origin);this.closed=closed;this.uncertain=uncertain;}
}
