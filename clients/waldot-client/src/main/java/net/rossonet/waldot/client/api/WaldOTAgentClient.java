package net.rossonet.waldot.client.api;

import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.client.ServiceFaultListener;

import net.rossonet.waldot.client.v1.WaldOTAgentClientImplV1;

/**
 * WaldOT agent client: connection life cycle and basic operations. The
 * extended API (structured queries, commands, subscriptions, snapshots,
 * address space) is {@link WaldotClient}.
 */
public interface WaldOTAgentClient extends WaldotClientConnection, WaldotBaseOperations, ServiceFaultListener {

	/** Connection status of every client version. */
	public enum Status {
		/** closed for good: the client cannot be started again */
		CLOSED,
		/** the last connection attempt failed, a new one will follow */
		FAULTED,
		/** created or connecting */
		INIT,
		/** the session was lost and the client is reconnecting (v2) */
		RECONNECTING,
		/** connected, the session is active */
		RUNNING,
		/** stopped by the user, it can be started again */
		STOPPED,
		/** the client gave up (maximum number of attempts reached) */
		UNRECOVERABLE_FAULTED
	}

	public static int CONTROL_THREAD_PRIORITY = Thread.MAX_PRIORITY;
	public static long CONTROL_THREAD_SLEEP_TIME_MSEC = 5000L;

	public static int TIMEOUT_GET_SESSION_SEC = 2;

	/**
	 * Creates a client of the first version (control thread with periodic
	 * checks).
	 */
	public static WaldOTAgentClient withConfiguration(final WaldOTAgentClientConfiguration configuration) {
		return new WaldOTAgentClientImplV1(configuration);
	}

	/**
	 * Registers an observer of the status changes.
	 *
	 * @deprecated use {@link #addListener(WaldotClientListener)}
	 */
	@Deprecated
	default void setStatusObserver(final WaldotAgentClientObserver waldotAgentClientObserver) {
		addListener(waldotAgentClientObserver);
	}

	@Override
	CompletableFuture<WaldOTAgentClient> startConnectionProcedure();

	@Override
	CompletableFuture<WaldOTAgentClient> stopConnectionProcedure();

}
