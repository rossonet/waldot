package net.rossonet.agent;

import java.util.ArrayList;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.agent.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.agent.client.v1.DefaultWaldOTAgentClientConfigurationV1;
import net.rossonet.waldot.agent.client.v1.WaldOTAgentClientImplV1;
import net.rossonet.waldot.api.models.WaldotGraph;

public class WaldotTestClientHandler implements AutoCloseable {

	private final WaldOTAgentClientImplV1 client;
	private final WaldotGraph graph;

	public WaldotTestClientHandler(WaldotGraph g) {
		final WaldOTAgentClientConfiguration configuration = new DefaultWaldOTAgentClientConfigurationV1();
		configuration.setTestAnonymousConnection(true);
		client = new WaldOTAgentClientImplV1(configuration);
		client.startConnectionProcedure();
		this.graph = g;
		final int retry = 0;
		while (retry < 10) {
			if (client.getStatus().equals(Status.CONNECTED)) {
				System.out.println("opc client connected");
				break;
			}
			try {
				Thread.sleep(500L);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean checkOpcUaVertexExists(String nodeId) {
		try {
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final UaObjectNode node = client.getOpcUaClient().getAddressSpace().getObjectNode(completedNodeId);
			return node != null;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public boolean checkOpcUaVertexValueBetween(String nodeId, String valueLabel, int minValue, int maxValue) {
		try {
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + "/" + valueLabel);
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final long value = (Long) results[0].value().getValue();
			System.out.println("Read value: " + value + " expected between: " + minValue + " and " + maxValue);
			return value >= minValue && value <= maxValue;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexValueEquals(String nodeId, String valueLabel, Object expectedValue) {
		try {
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + "/" + valueLabel);
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final Object value = results[0].value().getValue();
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return value.equals(expectedValue);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkVertexExists(String nodeId) {
		try {
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final Vertex v = graph.vertex(completedNodeId);
			return v != null;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkVertexValueBetween(String nodeId, String valueLabel, int minValue, int maxValue) {
		try {
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final Vertex v = graph.vertex(completedNodeId);
			final long value = (Long) v.property(valueLabel).value();
			System.out.println("Vertex value: " + value + " expected between: " + minValue + " and " + maxValue);
			return value >= minValue && value <= maxValue;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkVertexValueEquals(String nodeId, String valueLabel, Object expectedValue) {
		try {
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final Vertex v = graph.vertex(completedNodeId);
			final Object value = v.property(valueLabel).value();
			System.out.println("Vertex value: " + value + " expected: " + expectedValue);
			return value.equals(expectedValue);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void close() throws Exception {
		disconnect();

	}

	public void disconnect() {
		try {
			client.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	public long readIntOpcUaVertexValue(String nodeId, String valueLabel) throws UaException {
		final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + "/" + valueLabel);
		final List<ReadValueId> readValueIds = new ArrayList<>();
		readValueIds.add(new ReadValueId(completedNodeId, AttributeId.Value.uid(), null, // indexRange
				QualifiedName.NULL_VALUE));
		final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
				TimestampsToReturn.Both, readValueIds);
		final DataValue[] results = readResponse.getResults();
		final Object value = results[0].value().getValue();
		System.out.println("Read value: " + value);
		return (Long) value;
	}

	public List<String> runExpression(String expression) {
		try {
			return client.runExpression(expression);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void writeOpcUaVertexValue(String nodeId, String valueLabel, Object newValue) {
		final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + "/" + valueLabel);
		final List<WriteValue> writeValues = new ArrayList<>();
		writeValues.add(new WriteValue(completedNodeId, AttributeId.Value.uid(), null, // indexRange
				DataValue.valueOnly(new Variant(newValue))));
		try {
			final WriteResponse writeResponse = client.getOpcUaClient().write(writeValues);
			System.out.println("Write response: " + writeResponse);
		} catch (final UaException e) {
			e.printStackTrace();
		}
	}

}
