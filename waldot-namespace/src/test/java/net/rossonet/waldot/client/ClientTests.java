package net.rossonet.waldot.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.client.NodeCache;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.api.WaldOTAgentClient;
import net.rossonet.waldot.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.api.WaldotAgentClientObserver;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;

public class ClientTests {
	private WaldotGraph d;
	private WaldOTAgentClient client;

	private boolean checkOpcUaVertexExists(final String nodeId) {
		try {
			resetCache();
			final NodeId completedNodeId = d.getWaldotNamespace().generateNodeId(nodeId);
			final UaNode node = client.getOpcUaClient().getAddressSpace().getNode(completedNodeId);
			return node != null;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean checkOpcUaVertexValueEquals(final String nodeId, final String valueLabel,
			final Object expectedValue) {
		try {
			resetCache();
			final NodeId completedNodeId = d.getWaldotNamespace().generateNodeId(nodeId + "/" + valueLabel);
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final Object value = results[0].value().getValue();
			return value.toString().equals(expectedValue.toString());
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean checkVertexExists(final String nodeId) {
		try {
			final NodeId completedNodeId = d.getWaldotNamespace().generateNodeId(nodeId);
			final Vertex v = d.vertex(completedNodeId);
			return v != null;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean checkVertexValueEquals(final String nodeId, final String valueLabel, final Object expectedValue) {
		try {
			final NodeId completedNodeId = d.getWaldotNamespace().generateNodeId(nodeId);
			final Vertex v = d.vertex(completedNodeId);
			final Object value = v.property(valueLabel).value();
			return value.toString().equals(expectedValue.toString());
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void resetCache() {
		client.getOpcUaClient().getAddressSpace().setNodeCache(new NodeCache());

	}

	@Test
	public void runClientOneMinutes() throws Exception {
		d = OpcFactory.getOpcGraph();
		final WaldOTAgentClientConfiguration configuration = WaldOTAgentClientConfiguration.getDefaultConfiguration();
		client = WaldOTAgentClient.withConfiguration(configuration);
		client.setStatusObserver(new WaldotAgentClientObserver() {

			@Override
			public void onStatusChanged(final Status status) {
				System.out.println("Status changed: " + status);
			}
		});
		client.startConnectionProcedure();
		final int limit = 60 / 5;
		for (int i = 0; i < limit; i++) {
			final WaldOTAgentClient.Status status = client.getStatus();
			Thread.sleep(5000);
		}
		final Vertex source = d.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = d.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		assert checkVertexExists("source");
		assert checkVertexExists("destination");
		assert checkOpcUaVertexExists("source");
		assert checkOpcUaVertexExists("destination");
		assert checkOpcUaVertexValueEquals("source", "a", 1);
		assert checkVertexValueEquals("source", "a", 1);
		assert checkOpcUaVertexValueEquals("source", "b", 2);
		assert checkVertexValueEquals("source", "b", 2);
		assert checkOpcUaVertexValueEquals("destination", "a", 3);
		assert checkVertexValueEquals("destination", "a", 3);
		assert checkOpcUaVertexValueEquals("destination", "b", 4);
		assert checkVertexValueEquals("destination", "b", 4);

		client.stopConnectionProcedure();
		d.getWaldotNamespace().close();
	}

}
