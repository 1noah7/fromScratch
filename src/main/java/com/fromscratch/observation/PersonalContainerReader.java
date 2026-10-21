package com.fromscratch.observation;

import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;
import net.runelite.api.gameval.InventoryID;

/** Keeps API observations authoritative; independently verifies only absent personal containers. */
public final class PersonalContainerReader
{
	private final IntFunction<List<ObservedItem>> containers;
	private final EmptyContainerVerifier verifier;

	public PersonalContainerReader(IntFunction<List<ObservedItem>> containers, EmptyContainerVerifier verifier)
	{
		this.containers = containers;
		this.verifier = verifier;
	}

	public List<ObservedItem> items(int id)
	{
		List<ObservedItem> observed = containers.apply(id);
		if (observed != null)
		{
			verifier.invalidate(id);
			return observed; // an existing zero-length or all-empty-slots container is complete
		}
		if (id != InventoryID.INV && id != InventoryID.WORN) { return null; }
		EmptyContainerVerifier.Result result = verifier.verify(id);
		if (result == EmptyContainerVerifier.Result.NON_EMPTY)
		{
			throw new IllegalStateException("A personal interface shows an item but its container is unavailable; wait for current data.");
		}
		if (result == EmptyContainerVerifier.Result.SETTLING)
		{
			throw new IllegalStateException("An empty personal interface is settling; wait for the next game tick.");
		}
		return result == EmptyContainerVerifier.Result.EMPTY ? Collections.emptyList() : null;
	}
}
