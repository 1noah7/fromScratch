package com.fromscratch.bank;

/** Client-thread boundary. Personal inventory/equipment are deliberately absent. */
public interface BankSource
{
	String characterKey();
	BankObservation observe();
	String itemName(int id);
	/** Price identity only: accounting still keeps exact IDs. Implementations resolve this on the client thread. */
	default int priceId(int id) { return id; }
	/** Fail closed unless the live definition verifies genuine untradeable equipment, never a note/placeholder. */
	default boolean buybackDefinition(int id) { return false; }
	default com.fromscratch.pouch.PouchObservation pouch(){return com.fromscratch.pouch.PouchObservation.unknown();}
}
