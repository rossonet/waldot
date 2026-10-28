package net.rossonet.waldot.client.api.subscription;

/**
 * Callback of a monitored event source. It runs on the delivery thread of
 * the subscription group: it must return quickly.
 */
@FunctionalInterface
public interface WaldotEventListener {

	/**
	 * An event was received.
	 *
	 * @param item  the monitored item
	 * @param event the selected fields of the event
	 */
	void onEvent(MonitoredEventItem item, WaldotEvent event);
}
