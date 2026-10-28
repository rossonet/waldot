package net.rossonet.waldot.client.api.subscription;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

/**
 * Callback of a monitored value. It runs on the delivery thread of the
 * subscription group: it must return quickly.
 */
@FunctionalInterface
public interface DataChangeListener {

	/**
	 * A new value was received.
	 *
	 * @param item  the monitored item
	 * @param value value, status and timestamps
	 */
	void onDataChange(MonitoredDataItem item, DataValue value);
}
