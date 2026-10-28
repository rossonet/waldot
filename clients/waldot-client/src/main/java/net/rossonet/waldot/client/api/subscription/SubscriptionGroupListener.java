package net.rossonet.waldot.client.api.subscription;

import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

/**
 * Events of a subscription group (OPC UA subscription). Every method has an
 * empty default implementation; the callbacks run on the delivery thread of
 * the group.
 */
public interface SubscriptionGroupListener {

	/** A keep alive was received (no notifications in the period). */
	default void onKeepAlive(final SubscriptionGroup group) {
		// nothing by default
	}

	/** Some notifications were lost (sequence numbers not contiguous). */
	default void onNotificationDataLost(final SubscriptionGroup group) {
		// nothing by default
	}

	/**
	 * The group was created again on the server (after a failed transfer or a
	 * new connection); the monitored items were created again too.
	 */
	default void onRecreated(final SubscriptionGroup group) {
		// nothing by default
	}

	/** The server changed the status of the subscription (e.g. timeout). */
	default void onStatusChanged(final SubscriptionGroup group, final StatusCode status) {
		// nothing by default
	}

	/**
	 * The subscription could not be transferred to the new session after a
	 * reconnection: the client creates it again (see {@link #onRecreated}).
	 */
	default void onTransferFailed(final SubscriptionGroup group, final StatusCode status) {
		// nothing by default
	}

	/** No publish response nor keep alive within the expected time. */
	default void onWatchdogTimerElapsed(final SubscriptionGroup group) {
		// nothing by default
	}
}
