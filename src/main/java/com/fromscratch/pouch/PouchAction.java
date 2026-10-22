package com.fromscratch.pouch;

import java.util.*;
import lombok.Value;

/** Explicit player evidence: register before moving, restore unrelated earnings, or attest empty/finish. */
@Value
public class PouchAction
{
	public enum Kind { REGISTER, RESTORE_EARNED, FINISH }
	Kind kind;
	Map<Integer,Long> quantities;
	boolean emptyConfirmed;
	boolean retireMissing;
	String reason;
	public PouchAction(Kind kind,Map<Integer,Long> quantities,boolean emptyConfirmed,boolean retireMissing,String reason)
	{
		this.kind=Objects.requireNonNull(kind);this.quantities=PouchState.quantities(quantities);this.emptyConfirmed=emptyConfirmed;this.retireMissing=retireMissing;
		if(reason==null||reason.trim().isEmpty()||reason.length()>500){throw new IllegalArgumentException("Explain the pouch preparation confirmation");}this.reason=reason;
		if(kind==Kind.REGISTER&&this.quantities.isEmpty()||kind==Kind.RESTORE_EARNED&&this.quantities.isEmpty()||kind==Kind.FINISH&&(!emptyConfirmed||!this.quantities.isEmpty())){throw new IllegalArgumentException("Confirm the pouch is empty, or record its contents first.");}
	}
}
