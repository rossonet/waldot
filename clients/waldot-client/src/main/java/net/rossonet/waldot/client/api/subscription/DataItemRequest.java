package net.rossonet.waldot.client.api.subscription;

import java.util.Objects;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * One value to monitor in {@link SubscriptionGroup#subscribeData(java.util.List)}.
 *
 * @param nodeId   variable node
 * @param options  parameters of the item; null for the default sampling of
 *                 the client configuration
 * @param listener callback of the values
 */
public record DataItemRequest(NodeId nodeId, DataItemOptions options, DataChangeListener listener) {

	public DataItemRequest {
		Objects.requireNonNull(nodeId, "nodeId");
		Objects.requireNonNull(listener, "listener");
	}

}
