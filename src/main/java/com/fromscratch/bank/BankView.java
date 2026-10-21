package com.fromscratch.bank;

import java.util.*;
import lombok.Value;

/** Immutable Swing/render projection; only committed runs cross the storage boundary. */
@Value
public class BankView
{
	BankRun run;
	BankObservation observation;
	Map<Integer, String> names;
	boolean busy;
	boolean error;
	String message;
	BankTimings.Trace trace;
	com.fromscratch.wealth.BankValuation valuation;
	public BankView(BankRun run, BankObservation observation, Map<Integer, String> names, boolean busy, boolean error, String message)
	{ this(run, observation, names, busy, error, message, null); }
	public BankView(BankRun run, BankObservation observation, Map<Integer, String> names, boolean busy, boolean error, String message, BankTimings.Trace trace)
	{ this(run,observation,names,busy,error,message,trace,null); }
	public BankView(BankRun run, BankObservation observation, Map<Integer, String> names, boolean busy, boolean error, String message, BankTimings.Trace trace,com.fromscratch.wealth.BankValuation valuation)
	{ this.run = run; this.observation = observation; this.names = Collections.unmodifiableMap(new TreeMap<>(names)); this.busy = busy; this.error = error; this.message = message; this.trace = trace; this.valuation=valuation; }
	public boolean ready() { return !busy && !error && observation != null && observation.isReady(); }
	public String name(int id) { return names.getOrDefault(id, "Item #" + id); }
}
