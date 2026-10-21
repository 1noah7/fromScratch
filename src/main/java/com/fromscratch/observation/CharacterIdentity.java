package com.fromscratch.observation;

import com.fromscratch.persistence.SaveIntegrity;
import java.util.List;
import java.util.Optional;
import net.runelite.client.config.RuneScapeProfile;
import net.runelite.client.config.RuneScapeProfileType;

/** Verifies the selected RuneLite RS profile against the currently logged-in character. */
public final class CharacterIdentity
{
	private CharacterIdentity() { }

	public static Optional<String> verify(boolean loggedIn, long accountHash, RuneScapeProfileType type,
		String selectedProfile, List<RuneScapeProfile> profiles)
	{
		if (!loggedIn || accountHash == RuneScapeProfile.ACCOUNT_HASH_INVALID || selectedProfile == null || type == null)
		{
			return Optional.empty();
		}
		long matches = profiles.stream().filter(profile -> selectedProfile.equals(profile.getKey())
			&& profile.getAccountHash() == accountHash && profile.getType() == type).count();
		if (matches != 1)
		{
			return Optional.empty();
		}
		// Hash+profile type is stable across configuration profiles and display-name
		// changes. The random RuneLite config key alone is not a portable identity.
		return Optional.of(SaveIntegrity.sha256("fromscratch:character:v1:" + Long.toUnsignedString(accountHash) + ":" + type.name()));
	}
}
