package com.fromscratch.model;

/** Persistent lifecycle names. Renaming a value requires a save-schema migration. */
public enum RunStatus
{
	NOT_STARTED,
	INITIALIZING,
	ACTIVE,
	NEEDS_RECONCILIATION,
	COMPLETED
}
