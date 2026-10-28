package net.rossonet.waldot.client.api.subscription;

import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

/** A monitored value (e.g. a property of a vertex). */
public interface MonitoredDataItem extends MonitoredItemHandle {

	/** @return the last value received, empty before the first notification */
	Optional<DataValue> lastValue();

	/**
	 * Changes the parameters at runtime (sampling interval, queue, deadband,
	 * trigger, monitoring mode) with ModifyMonitoredItems.
	 */
	void modify(DataItemOptions options);

	/** @return the current parameters */
	DataItemOptions options();
}
