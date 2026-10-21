package com.fromscratch.wealth;

import com.fromscratch.overlay.VirtualQuantityFormatter;
import org.junit.Test;
import static org.junit.Assert.*;

/** Shared integer display policy only: financial amounts and historical quotes remain untouched. */
public class GpFormatterTest
{
	@Test public void requestedWealthExamplesUseIntegerOsrsLabels()
	{
		long[] values = {0, 1740, 9999, 10000, 50840, 980640, 1000000, 3963708, 9999999, 10000000, 25600000};
		String[] labels = {"0", "1740", "9999", "10K", "50K", "980K", "1000K", "3963K", "9999K", "10M", "25M"};
		for (int i = 0; i < values.length; i++) { assertEquals(labels[i] + " GP", GpFormatter.compact(values[i])); }
	}
	@Test public void negativeWealthUsesTheSameMagnitudeThresholds()
	{
		assertEquals("-1740 GP", GpFormatter.compact(-1740)); assertEquals("-10K GP", GpFormatter.compact(-10000));
		assertEquals("-1000K GP", GpFormatter.compact(-1000000)); assertEquals("-9999K GP", GpFormatter.compact(-9999999));
		assertEquals("-10M GP", GpFormatter.compact(-10000000));
	}
	@Test public void suffixBoundariesTruncateWithoutDecimalsOrRounding()
	{
		for (long n : new long[]{9999, 10000, 10999, 999999, 1000000, 9999999, 10000000, 10999999, 25600000})
		{
			for (long signed : new long[]{n, -n})
			{
				String money = GpFormatter.compact(signed); assertFalse(money.contains(".")); assertFalse(money.contains(","));
				assertEquals(VirtualQuantityFormatter.signed(signed) + " GP", money);
			}
		}
		assertEquals("10M GP", GpFormatter.compact(10999999));
	}
	@Test public void allSmallGpValuesRemainExact()
	{
		for (long n = -9999; n <= 9999; n++) { assertEquals(Long.toString(n) + " GP", GpFormatter.compact(n)); }
	}
	@Test public void longExtremesDoNotOverflowOrLoseTheirSigns()
	{
		assertEquals("9223372036854M GP", GpFormatter.compact(Long.MAX_VALUE));
		assertEquals("-9223372036854M GP", GpFormatter.compact(Long.MIN_VALUE));
	}
	@Test public void exactTooltipDetailsKeepEveryGpAndGroupingSeparator()
	{
		assertEquals("3,963,708 GP", GpFormatter.exact(3963708)); assertEquals("-1,000,000 GP", GpFormatter.exact(-1000000));
		assertEquals("-9,223,372,036,854,775,808 GP", GpFormatter.exact(Long.MIN_VALUE));
	}
}
