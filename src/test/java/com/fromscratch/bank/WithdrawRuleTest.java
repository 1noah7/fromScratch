package com.fromscratch.bank;

import com.fromscratch.SnapshotFixtures;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class WithdrawRuleTest
{
	@Test
	public void fixedOptionsRequestTheirAmountCappedByTheStack()
	{
		assertEquals(1, WithdrawRule.requested("Withdraw-1", 110));
		assertEquals(5, WithdrawRule.requested("Withdraw-5", 110));
		assertEquals(10, WithdrawRule.requested("Withdraw-10", 110));
		assertEquals(25, WithdrawRule.requested("Withdraw-25", 110));
		assertEquals(3, WithdrawRule.requested("Withdraw-10", 3));
	}

	@Test
	public void allOptionsUseTheLiveStack()
	{
		assertEquals(110, WithdrawRule.requested("Withdraw-All", 110));
		assertEquals(109, WithdrawRule.requested("Withdraw-All-but-1", 110));
		assertEquals(0, WithdrawRule.requested("Withdraw-All-but-1", 1));
	}

	@Test
	public void typedAndEquipOptions()
	{
		assertEquals(WithdrawRule.TYPED, WithdrawRule.requested("Withdraw-X", 110));
		assertEquals(1, WithdrawRule.requested("Wear", 1));
		assertEquals(1, WithdrawRule.requested("Wield", 4));
	}

	@Test
	public void otherOptionsAreNotWithdrawals()
	{
		assertEquals(WithdrawRule.NOT_WITHDRAWAL, WithdrawRule.requested("Examine", 110));
		assertEquals(WithdrawRule.NOT_WITHDRAWAL, WithdrawRule.requested("Placeholder", 110));
		assertEquals(WithdrawRule.NOT_WITHDRAWAL, WithdrawRule.requested(null, 110));
	}

	@Test
	public void unknownWithdrawWordingIsTreatedAsTheWholeStack()
	{
		assertEquals(110, WithdrawRule.requested("Withdraw-Some", 110));
	}

	@Test
	public void typedAmountsAcceptGameSuffixes()
	{
		assertEquals(10, WithdrawRule.typed("10"));
		assertEquals(10_000, WithdrawRule.typed("10k"));
		assertEquals(2_000_000, WithdrawRule.typed("2m"));
		assertEquals(-1, WithdrawRule.typed(""));
		assertEquals(-1, WithdrawRule.typed("abc"));
	}

	@Test
	public void legacyStockIsLockedAndEarnedStockIsUnlocked()
	{
		BankRun run = BankFixtures.run(100);
		assertEquals(0, WithdrawRule.unlocked(run, BankFixtures.SHIELD, 100));
		assertFalse(WithdrawRule.allowed(1, WithdrawRule.unlocked(run, BankFixtures.SHIELD, 100)));

		BankRun earned = BankEngine.observe(run, BankFixtures.bank(110), UUID.randomUUID(), SnapshotFixtures.TIME);
		long unlocked = WithdrawRule.unlocked(earned, BankFixtures.SHIELD, 110);
		assertEquals(10, unlocked);
		assertTrue(WithdrawRule.allowed(WithdrawRule.requested("Withdraw-10", 110), unlocked));
		assertFalse(WithdrawRule.allowed(WithdrawRule.requested("Withdraw-All", 110), unlocked));
		assertFalse(WithdrawRule.allowed(WithdrawRule.requested("Withdraw-All-but-1", 110), unlocked));
	}

	@Test
	public void unlockedIsCappedByTheLiveStack()
	{
		BankRun run = BankEngine.observe(BankFixtures.run(0), BankFixtures.bank(10), UUID.randomUUID(), SnapshotFixtures.TIME);
		assertEquals(10, WithdrawRule.unlocked(run, BankFixtures.SHIELD, 10));
		assertEquals(4, WithdrawRule.unlocked(run, BankFixtures.SHIELD, 4));
		assertTrue(WithdrawRule.allowed(WithdrawRule.requested("Withdraw-All", 10), 10));
	}

	@Test
	public void messagesExplainTheLimit()
	{
		assertTrue(WithdrawRule.message(0).contains("locked legacy stock"));
		assertTrue(WithdrawRule.message(1).contains("only 1 of this item is unlocked"));
		assertTrue(WithdrawRule.message(1500).contains("only 1,500 of this item are unlocked"));
	}
}
