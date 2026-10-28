package net.rossonet.waldot.client.api.subscription;

import java.util.List;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;

/**
 * A group of monitored items sharing an OPC UA subscription (publishing
 * interval, priority, keep alive). The group survives the reconnections: if
 * the server cannot transfer it to the new session, or the client opens a
 * new connection, it is created again with all its items.
 */
public interface SubscriptionGroup extends AutoCloseable {

	/** Registers a listener of the group events. */
	void addListener(SubscriptionGroupListener listener);

	/** Deletes the subscription on the server and removes the group. */
	@Override
	void close();

	/** @return the monitored values of the group */
	List<MonitoredDataItem> dataItems();

	/** @return the monitored event sources of the group */
	List<MonitoredEventItem> eventItems();

	/** @return true while the group is not closed */
	boolean isActive();

	/** @return true if the publishing is enabled on the server */
	boolean isPublishingEnabled();

	/**
	 * Changes publishing interval, keep alive, lifetime, notifications per
	 * publish and priority at runtime (ModifySubscription); the publishing
	 * mode follows {@code publishingEnabled}.
	 */
	void modify(SubscriptionGroupOptions options);

	/** @return name of the group (unique in the client) */
	String name();

	/** @return the current parameters */
	SubscriptionGroupOptions options();

	/** Removes a listener. */
	void removeListener(SubscriptionGroupListener listener);

	/** @return publishing interval revised by the server */
	Optional<Double> revisedPublishingInterval();

	/** Changes the monitoring mode of some items in a single request. */
	void setMonitoringMode(MonitoringMode mode, List<? extends MonitoredItemHandle> items);

	/** Enables or disables the publishing (SetPublishingMode). */
	void setPublishingEnabled(boolean enabled);

	/**
	 * Monitors the value of a node.
	 *
	 * @param nodeId   variable node
	 * @param options  parameters of the item
	 * @param listener callback of the values
	 */
	MonitoredDataItem subscribeData(NodeId nodeId, DataItemOptions options, DataChangeListener listener);

	/**
	 * Monitors the events of a node (EventNotifier).
	 *
	 * @param nodeId   object node (e.g. a vertex or the Server object)
	 * @param options  parameters of the item
	 * @param listener callback of the events
	 */
	MonitoredEventItem subscribeEvents(NodeId nodeId, EventItemOptions options, WaldotEventListener listener);

	/** @return id of the subscription on the server, empty if not created */
	Optional<UInteger> subscriptionId();
}
