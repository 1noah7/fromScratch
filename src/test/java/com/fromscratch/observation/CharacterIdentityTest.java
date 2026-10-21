package com.fromscratch.observation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import net.runelite.client.config.RuneScapeProfile;
import net.runelite.client.config.RuneScapeProfileType;
import org.junit.Test;

import static org.junit.Assert.*;

public class CharacterIdentityTest
{
	private static final long HASH = 12345;
	private static final String PROFILE = "rsprofile.synthetic";

	private Optional<String> verify(boolean loggedIn, long hash, RuneScapeProfileType type, String selected)
	{
		return CharacterIdentity.verify(loggedIn, hash, type, selected,
			Collections.singletonList(new RuneScapeProfile("Synthetic", RuneScapeProfileType.STANDARD, HASH, PROFILE)));
	}

	@Test
	public void loggedOutIdentityIsUnavailable() { assertFalse(verify(false, HASH, RuneScapeProfileType.STANDARD, PROFILE).isPresent()); }

	@Test
	public void unknownAccountHashIsUnavailable() { assertFalse(verify(true, RuneScapeProfile.ACCOUNT_HASH_INVALID, RuneScapeProfileType.STANDARD, PROFILE).isPresent()); }

	@Test
	public void staleSelectedCharacterProfileIsRejected() { assertFalse(verify(true, HASH + 1, RuneScapeProfileType.STANDARD, PROFILE).isPresent()); }

	@Test
	public void staleWorldProfileTypeIsRejected() { assertFalse(verify(true, HASH, RuneScapeProfileType.DEADMAN, PROFILE).isPresent()); }

	@Test
	public void absentSelectedProfileIsRejected() { assertFalse(verify(true, HASH, RuneScapeProfileType.STANDARD, null).isPresent()); }

	@Test
	public void duplicateMatchingProfilesAreAmbiguous()
	{
		RuneScapeProfile profile = new RuneScapeProfile("Synthetic", RuneScapeProfileType.STANDARD, HASH, PROFILE);
		assertFalse(CharacterIdentity.verify(true, HASH, RuneScapeProfileType.STANDARD, PROFILE, Arrays.asList(profile, profile)).isPresent());
	}

	@Test
	public void displayNameAndConfigurationProfileKeyChangesPreserveIdentity()
	{
		String original = verify(true, HASH, RuneScapeProfileType.STANDARD, PROFILE).get();
		String changed = CharacterIdentity.verify(true, HASH, RuneScapeProfileType.STANDARD, "rsprofile.new",
			Collections.singletonList(new RuneScapeProfile("Renamed", RuneScapeProfileType.STANDARD, HASH, "rsprofile.new"))).get();
		assertEquals(original, changed);
		assertTrue(original.matches("[a-f0-9]{64}"));
	}

	@Test
	public void accountAndWorldProfileTypeProduceSeparateNamespaces()
	{
		String original = verify(true, HASH, RuneScapeProfileType.STANDARD, PROFILE).get();
		for (RuneScapeProfile profile : Arrays.asList(
			new RuneScapeProfile("Other", RuneScapeProfileType.STANDARD, HASH + 1, "rsprofile.other"),
			new RuneScapeProfile("Synthetic", RuneScapeProfileType.DEADMAN, HASH, "rsprofile.deadman")))
		{
			assertNotEquals(original, CharacterIdentity.verify(true, profile.getAccountHash(), profile.getType(), profile.getKey(), Collections.singletonList(profile)).get());
		}
	}
}
