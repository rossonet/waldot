package net.rossonet.waldot.cli;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.address.AddressSpaceBrowser;
import net.rossonet.waldot.client.api.command.WaldotCommandClient;
import net.rossonet.waldot.client.api.graph.GraphSnapshotClient;
import net.rossonet.waldot.client.api.graph.WaldotGraphClient;
import net.rossonet.waldot.client.api.subscription.WaldotSubscriptionClient;

/**
 * Mockito client with its sub-clients already wired; the node ids are parsed
 * like the real client (OPC UA form or string identifier in namespace 2).
 */
public final class MockClient {

	/** Namespace of WaldOT in the tests. */
	public static final int NAMESPACE = 2;

	/** Parses a node id like {@code AddressSpaceBrowser#parseNodeId}. */
	public static NodeId nodeId(final String value) {
		if (value.contains("=")) {
			final NodeId parsed = NodeId.parseOrNull(value);
			if (parsed == null) {
				throw new IllegalArgumentException("invalid node id " + value);
			}
			return parsed;
		}
		if (value.isBlank()) {
			throw new IllegalArgumentException("empty node id");
		}
		return new NodeId(NAMESPACE, value);
	}

	public final AddressSpaceBrowser address = mock(AddressSpaceBrowser.class);
	public final WaldotClient client = mock(WaldotClient.class);
	public final WaldotCommandClient commands = mock(
			WaldotCommandClient.class);
	/** Configuration received by the factory of the last command. */
	public volatile WaldOTAgentClientConfiguration configuration;
	public final WaldotGraphClient graph = mock(WaldotGraphClient.class);
	public final GraphSnapshotClient snapshots = mock(
			GraphSnapshotClient.class);
	public final WaldotSubscriptionClient subscriptions = mock(
			WaldotSubscriptionClient.class);

	public MockClient() {
		when(client.addressSpace()).thenReturn(address);
		when(client.commands()).thenReturn(commands);
		when(client.graph()).thenReturn(graph);
		when(client.snapshots()).thenReturn(snapshots);
		when(client.subscriptions()).thenReturn(subscriptions);
		when(client.namespaceIndex()).thenReturn(NAMESPACE);
		when(address.parseNodeId(anyString()))
				.thenAnswer(invocation -> nodeId(invocation.getArgument(0)));
	}
}
