package com.fromscratch.observation;

import lombok.Value;

/** A copied slot; no client objects cross the observation boundary. */
@Value
public class ObservedItem
{
	int itemId;
	long quantity;
	boolean placeholder;
}
