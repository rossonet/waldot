package net.rossonet.waldot.rules.events;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base class for executable rule actions.
 * <p>
 * FireableAction rappresenta un'azione eseguibile generata da un RuleVertex.
 * Estende Runnable per essere eseguita in un thread pool gestito da ComputeVertex.
 * Tiene traccia del tempo di inizio per permettere il timeout delle azioni.
 * </p>
 * 
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Created by RuleVertex.getRunnableEvent() or getRunnablePropertyEvent()</li>
 *   <li>Wrapped in RunnableEvent and enqueued in priority queue</li>
 *   <li>Retrieved by ComputeVertex with getAction(startingTime)</li>
 *   <li>Starting time set for timeout tracking</li>
 *   <li>Submitted to virtual thread pool for execution</li>
 *   <li>run() method executes rule action (condition + action evaluation)</li>
 * </ol>
 * 
 * <h2>Timeout Mechanism</h2>
 * <p>
 * ComputeVertex tracks starting time and cancels actions that exceed the
 * configured execution timeout (default: 2 minutes). This prevents hanging
 * actions from blocking the thread pool.
 * </p>
 * 
 * @see RunnableEvent
 * 
 * @author Andrea Ambrosini - Zona Autonoma srl
 * @since 0.4.0
 */
public abstract class FireableAction implements Runnable {

	// Timestamp di inizio esecuzione (per timeout tracking), letto dal thread
	// manager e scritto da chi estrae l'evento dalla coda
	private volatile long startingTime;

	private final AtomicBoolean timedOut = new AtomicBoolean(false);

	/**
	 * Returns the execution starting time in milliseconds.
	 * 
	 * @return starting time (System.currentTimeMillis())
	 */
	public long getStartingTime() {
		return startingTime;
	}

	/**
	 * Sets the execution starting time.
	 * <p>
	 * Chiamato da ComputeVertex quando l'azione viene presa dalla coda
	 * per essere eseguita.
	 * </p>
	 * 
	 * @param timeMillis starting time in milliseconds
	 */
	public void setStartingTime(long timeMillis) {
		startingTime = timeMillis;
	}

	/**
	 * Marks the action as timed out once.
	 *
	 * @return true only on the first timeout mark
	 */
	public boolean markTimedOut() {
		return timedOut.compareAndSet(false, true);
	}

	/**
	 * @return true when the action has been marked as timed out
	 */
	public boolean isTimedOut() {
		return timedOut.get();
	}

	/**
	 * Hook invoked by ComputeVertex when timeout cancellation happens.
	 */
	public void onTimeout() {
		// default no-op
	}

}
