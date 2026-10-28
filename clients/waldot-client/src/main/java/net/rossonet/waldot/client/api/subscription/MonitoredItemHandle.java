package net.rossonet.waldot.client.api.subscription;

import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;

/** Common part of a monitored value and a monitored event source. */
public interface MonitoredItemHandle extends AutoCloseable {

	/** Removes the item from its group (and from the server). */
	@Override
	void close();

	/** @return the group of the item */
	SubscriptionGroup group();

	/** @return true while the item belongs to its group */
	boolean isActive();

	/** @return current monitoring mode */
	MonitoringMode monitoringMode();

	/** @return the monitored node */
	NodeId nodeId();

	/** @return queue size revised by the server, empty before the creation */
	Optional<UInteger> revisedQueueSize();

	/** @return sampling interval revised by the server */
	Optional<Double> revisedSamplingInterval();

	/** Changes the monitoring mode (reporting, sampling, disabled). */
	void setMonitoringMode(MonitoringMode mode);

	/** @return result of the last create/modify operation on the server */
	Optional<StatusCode> status();
}
