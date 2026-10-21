package com.fromscratch.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Stable hashes for private filenames and accidental-corruption detection, not anti-cheat. */
public final class SaveIntegrity
{
	private SaveIntegrity() { }

	public static String sha256(String value)
	{
		try
		{
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder(digest.length * 2);
			for (byte part : digest)
			{
				result.append(Character.forDigit((part >>> 4) & 15, 16));
				result.append(Character.forDigit(part & 15, 16));
			}
			return result.toString();
		}
		catch (NoSuchAlgorithmException ex)
		{
			throw new IllegalStateException("The JVM does not provide SHA-256", ex);
		}
	}
}
