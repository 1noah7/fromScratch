package com.fromscratch.tracking;

import lombok.Value;

/** Removed copies which may return. They are not current ownership or new lifetime earnings. */
@Value
public class ReturnBalance
{
	long legacy;
	long virtual;
	public ReturnBalance(long legacy, long virtual)
	{
		if (legacy < 0 || virtual < 0) { throw new IllegalArgumentException("Negative return balance"); }
		Math.addExact(legacy, virtual);
		this.legacy = legacy; this.virtual = virtual;
	}
}
