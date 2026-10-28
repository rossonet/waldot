package net.rossonet.waldot.client.api.subscription;

/** A monitored event source (e.g. a vertex). */
public interface MonitoredEventItem extends MonitoredItemHandle {

	/**
	 * Changes the parameters at runtime (fields, types, queue, monitoring
	 * mode) with ModifyMonitoredItems.
	 */
	void modify(EventItemOptions options);

	/** @return the current parameters */
	EventItemOptions options();
}
