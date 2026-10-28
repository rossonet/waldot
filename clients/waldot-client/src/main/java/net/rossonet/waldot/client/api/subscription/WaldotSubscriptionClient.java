package net.rossonet.waldot.client.api.subscription;

import java.util.List;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * Subscriptions to values and events. The items are grouped in
 * {@link SubscriptionGroup}s (one OPC UA subscription each); the shortcut
 * methods use the {@value #DEFAULT_GROUP} group, created with the default
 * publishing interval of the configuration.
 */
public interface WaldotSubscriptionClient {

	/** Name of the group used by the shortcut methods. */
	String DEFAULT_GROUP = "default";

	/**
	 * Creates a group.
	 *
	 * @throws IllegalArgumentException if a group with the same name exists
	 */
	SubscriptionGroup createGroup(String name, SubscriptionGroupOptions options);

	/** @return the default group (created on first use) */
	SubscriptionGroup defaultGroup();

	/** @return a group by name */
	Optional<SubscriptionGroup> group(String name);

	/** @return the open groups */
	List<SubscriptionGroup> groups();

	/**
	 * Monitors the events of a vertex (or any node) in the default group.
	 */
	MonitoredEventItem subscribeEvents(NodeId nodeId, EventItemOptions options, WaldotEventListener listener);

	/**
	 * Monitors a property of a vertex or an edge in the default group (the
	 * variable {@code <element>/<key>}).
	 */
	MonitoredDataItem subscribeProperty(NodeId element, String key, DataItemOptions options,
			DataChangeListener listener);

	/** Monitors the value of any variable in the default group. */
	MonitoredDataItem subscribeValue(NodeId nodeId, DataItemOptions options, DataChangeListener listener);
}
