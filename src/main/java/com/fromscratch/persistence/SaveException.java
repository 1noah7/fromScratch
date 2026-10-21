package com.fromscratch.persistence;

import java.io.IOException;

/** Safe user-facing storage error. Messages must not contain paths, inventory, or raw identity. */
public class SaveException extends IOException
{
	public SaveException(String message) { super(message); }
}
