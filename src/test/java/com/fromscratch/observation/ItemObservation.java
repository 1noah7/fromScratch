package com.fromscratch.observation;

import com.fromscratch.tracking.Holdings;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Value;

/** Copied client-thread facts. An absent complete holdings object never means empty holdings. */
@Value
public class ItemObservation
{
	public enum Action { DROP, CONSUME, ACQUIRE }
	UUID sessionId;
	long tick;
	Instant time;
	CaptureProgress progress;
	Holdings holdings;
	boolean stable;
	boolean bankOpen;
	boolean uncertainBankChange;
	Map<Integer, Action> actions;
	long eventVersion;
	boolean bankInteraction;
	boolean unsupportedAction;
	Map<Integer, Long> ordinaryReceipts;
	public ItemObservation(UUID sessionId, long tick, Instant time, CaptureProgress progress, Holdings holdings,
		boolean stable, boolean bankOpen, boolean uncertainBankChange, Map<Integer, Action> actions)
	{
		this(sessionId, tick, time, progress, holdings, stable, bankOpen, uncertainBankChange, actions, tick, false, false);
	}
	public ItemObservation(UUID sessionId, long tick, Instant time, CaptureProgress progress, Holdings holdings,
		boolean stable, boolean bankOpen, boolean uncertainBankChange, Map<Integer, Action> actions,
		long eventVersion, boolean bankInteraction, boolean unsupportedAction)
	{
		this(sessionId, tick, time, progress, holdings, stable, bankOpen, uncertainBankChange, actions,
			eventVersion, bankInteraction, unsupportedAction, Collections.emptyMap());
	}
	public ItemObservation(UUID sessionId, long tick, Instant time, CaptureProgress progress, Holdings holdings,
		boolean stable, boolean bankOpen, boolean uncertainBankChange, Map<Integer, Action> actions,
		long eventVersion, boolean bankInteraction, boolean unsupportedAction, Map<Integer, Long> ordinaryReceipts)
	{
		this.sessionId = sessionId; this.tick = tick; this.time = time; this.progress = progress;
		this.holdings = holdings; this.stable = stable; this.bankOpen = bankOpen; this.uncertainBankChange = uncertainBankChange;
		this.actions = Collections.unmodifiableMap(new HashMap<>(actions));
		this.eventVersion = eventVersion; this.bankInteraction = bankInteraction; this.unsupportedAction = unsupportedAction;
		this.ordinaryReceipts = Collections.unmodifiableMap(new HashMap<>(ordinaryReceipts));
	}
}
