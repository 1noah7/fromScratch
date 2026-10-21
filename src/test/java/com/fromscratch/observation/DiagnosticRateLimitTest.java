package com.fromscratch.observation;

import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class DiagnosticRateLimitTest
{
	@Test
	public void repeatedTicksAndStateChangesShareOneTenSecondLimit()
	{
		DiagnosticRateLimit limit = new DiagnosticRateLimit();
		assertTrue(limit.allow(0));
		for (int second = 0; second < 10; second++) { assertFalse(limit.allow(TimeUnit.SECONDS.toNanos(second))); }
		assertTrue(limit.allow(TimeUnit.SECONDS.toNanos(10)));
		assertFalse(limit.allow(TimeUnit.SECONDS.toNanos(10)));
	}

	@Test
	public void monotonicCounterMayStartAtAnArbitraryNegativeValue()
	{
		DiagnosticRateLimit limit = new DiagnosticRateLimit();
		assertTrue(limit.allow(-100));
		assertFalse(limit.allow(-99));
		assertTrue(limit.allow(TimeUnit.SECONDS.toNanos(10) - 100));
	}
}
