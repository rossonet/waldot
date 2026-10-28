package net.rossonet.waldot.client.utils;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.List;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.client.AddressSpace.BrowseOptions;
import org.eclipse.milo.opcua.sdk.client.NodeCache;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.SimpleAttributeOperand;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;

import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.v1.DefaultWaldOTAgentClientConfigurationV1;
import net.rossonet.waldot.client.v1.WaldOTAgentClientImplV1;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;

/**
 * Test helper: an OPC UA client (first version) connected to a WaldOT server
 * running in the same JVM, with checks that compare the OPC UA address space
 * with the Gremlin graph.
 * <p>
 * Node naming used by the checks (string identifiers in the WaldOT
 * namespace): vertex {@code <id>}, vertex property {@code <id>/<key>},
 * standard attributes {@code <id>.Label}, {@code <id>.Type},
 * {@code <id>.Port}, {@code <id>.Bind}.
 * </p>
 */
public class WaldotTestClientHandler implements AutoCloseable {

	public interface EventObserver {
		default void onEvent(final String nodeId, final String eventType, final Object value) {
			System.out.println("Received event for node: " + nodeId + " event type: " + eventType + " value: " + value);
		}
	}

	private static final long CONNECTION_POLL_MSEC = 50L;

	private static final long CONNECTION_TIMEOUT_MSEC = Long.getLong("waldot.test.client.timeout.msec", 15_000L);

	private static final String[] EVENT_FIELDS = { "EventId", "EventType", "Severity", "Time", "Message" };

	private static String attributeText(final Object value) {
		if (value instanceof QualifiedName) {
			return ((QualifiedName) value).getName();
		}
		if (value instanceof LocalizedText) {
			return ((LocalizedText) value).getText();
		}
		return String.valueOf(value);
	}

	private static boolean valuesEqual(final Object value, final Object expected) {
		if (value instanceof Number && expected instanceof Number) {
			return Double.compare(((Number) value).doubleValue(), ((Number) expected).doubleValue()) == 0;
		}
		return String.valueOf(value).equals(String.valueOf(expected));
	}

	private final WaldOTAgentClientImplV1 client;

	private final WaldotGraph graph;

	public WaldotTestClientHandler(final WaldotGraph g) {
		this(g, new DefaultWaldOTAgentClientConfigurationV1());
	}

	public WaldotTestClientHandler(final WaldotGraph g, final WaldOTAgentClientConfiguration configuration) {
		client = new WaldOTAgentClientImplV1(configuration);
		this.graph = g;
		client.startConnectionProcedure();
		final long deadline = System.currentTimeMillis() + CONNECTION_TIMEOUT_MSEC;
		while (!client.getStatus().equals(Status.RUNNING)) {
			if (System.currentTimeMillis() > deadline) {
				disconnect();
				throw new IllegalStateException("OPC UA test client not connected after " + CONNECTION_TIMEOUT_MSEC
						+ " ms (status " + client.getStatus() + ", endpoint " + configuration.getDiscoveryEndpoint()
						+ ")");
			}
			try {
				Thread.sleep(CONNECTION_POLL_MSEC);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				disconnect();
				throw new IllegalStateException("interrupted while waiting for OPC UA test client connection", e);
			}
		}
		System.out.println("opc client connected");
	}

	/** Compares a string read from the address space with the expected value. */
	private boolean checkOpcUaAttribute(final NodeId nodeId, final AttributeId attribute, final String expected) {
		try {
			final String value = attributeText(readAttribute(nodeId, attribute));
			System.out.println("Read value: " + value + " expected: " + expected);
			return expected.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaEdgeBrowserNameNameValueEquals(final WaldotEdge edge, final String expectedValue) {
		return checkOpcUaAttribute(edge.getNodeId(), AttributeId.BrowseName, expectedValue);
	}

	public boolean checkOpcUaEdgeDescriptionValueEquals(final WaldotEdge edge, final String expectedValue) {
		return checkOpcUaAttribute(edge.getNodeId(), AttributeId.Description, expectedValue);
	}

	public boolean checkOpcUaEdgeDisplayNameValueEquals(final WaldotEdge edge, final String expectedValue) {
		return checkOpcUaAttribute(edge.getNodeId(), AttributeId.DisplayName, expectedValue);
	}

	public boolean checkOpcUaEdgeLabelReadOnly(final WaldotEdge edge) {
		return isReadOnly(nodeId(edge.getNodeId().getIdentifier().toString() + ".Label"));
	}

	public boolean checkOpcUaEdgeLabelValueEquals(final WaldotEdge edge, final String expectedValue) {
		return checkOpcUaAttribute(nodeId(edge.getNodeId().getIdentifier().toString() + ".Label"), AttributeId.Value,
				expectedValue);
	}

	public boolean checkOpcUaVertexBindReadOnly(final String nodeId) {
		return isReadOnly(nodeId(nodeId + ".Bind"));
	}

	public boolean checkOpcUaVertexBindValueEquals(final String nodeId, final String expectedValue) {
		return checkOpcUaAttribute(nodeId(nodeId + ".Bind"), AttributeId.Value, expectedValue);
	}

	public boolean checkOpcUaVertexBrowserNameNameValueEquals(final String nodeId, final String expectedValue) {
		return checkOpcUaAttribute(nodeId(nodeId), AttributeId.BrowseName, expectedValue);
	}

	public boolean checkOpcUaVertexDescriptionValueEquals(final String nodeId, final String expectedValue) {
		return checkOpcUaAttribute(nodeId(nodeId), AttributeId.Description, expectedValue);
	}

	public boolean checkOpcUaVertexDisplayNameValueEquals(final String nodeId, final String expectedValue) {
		return checkOpcUaAttribute(nodeId(nodeId), AttributeId.DisplayName, expectedValue);
	}

	/**
	 * Checks that the edge {@code typeEdge} between two vertices is published
	 * in the address space: main reference (forward from the source, inverse
	 * on the target) and reference descriptions on both vertices.
	 */
	public boolean checkOpcUaVertexEdge(final long nodeOut, final long nodeIn, final String typeEdge) {
		try {
			resetCache();
			final UaNode outObject = client.getOpcUaClient().getAddressSpace()
					.getNode(graph.getWaldotNamespace().generateNodeId(nodeOut));
			final UaNode inObject = client.getOpcUaClient().getAddressSpace()
					.getNode(graph.getWaldotNamespace().generateNodeId(nodeIn));
			final List<ReferenceDescription> outReferences = outObject
					.browse(nonHierarchical(BrowseDirection.Forward, NodeIds.NonHierarchicalReferences));
			final boolean foundOut = outReferences.stream()
					.anyMatch(r -> typeEdge.equals(r.getReferenceTypeId().getIdentifier()));
			boolean descriptionOut = false;
			for (final ReferenceDescription r : outReferences) {
				if (isReferenceDescription(r)) {
					for (final UaNode nodeEdge : outObject
							.browseNodes(nonHierarchical(BrowseDirection.Forward, r.getReferenceTypeId()))) {
						descriptionOut |= edgeLabelIs(nodeEdge.getNodeId(), typeEdge);
					}
				}
			}
			if (!foundOut || !descriptionOut) {
				return false;
			}
			final boolean foundIn = inObject
					.browse(nonHierarchical(BrowseDirection.Inverse, NodeIds.NonHierarchicalReferences)).stream()
					.anyMatch(r -> typeEdge.equals(r.getReferenceTypeId().getIdentifier()));
			if (!foundIn) {
				return false;
			}
			boolean descriptionIn = false;
			for (final ReferenceDescription r : inObject
					.browse(nonHierarchical(BrowseDirection.Forward, NodeIds.NonHierarchicalReferences))) {
				if (isReferenceDescription(r)) {
					final List<? extends UaNode> edges = outObject
							.browseNodes(nonHierarchical(BrowseDirection.Forward, r.getReferenceTypeId()));
					if (edges.isEmpty() || edges.get(0) == null) {
						return false;
					}
					descriptionIn |= edgeLabelIs(edges.get(0).getNodeId(), typeEdge);
				}
			}
			return descriptionIn;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexExists(final long nodeId) {
		return opcUaNodeExists(graph.getWaldotNamespace().generateNodeId(nodeId));
	}

	public boolean checkOpcUaVertexExists(final String nodeId) {
		return opcUaNodeExists(nodeId(nodeId));
	}

	public boolean checkOpcUaVertexLabelReadOnly(final String nodeId) {
		return isReadOnly(nodeId(nodeId + ".Label"));
	}

	public boolean checkOpcUaVertexLabelValueEquals(final String nodeId, final String expectedValue) {
		return checkOpcUaAttribute(nodeId(nodeId + ".Label"), AttributeId.Value, expectedValue);
	}

	public boolean checkOpcUaVertexPortReadOnly(final String nodeId) {
		return isReadOnly(nodeId(nodeId + ".Port"));
	}

	public boolean checkOpcUaVertexPortValueEquals(final String nodeId, final String expectedValue) {
		return checkOpcUaAttribute(nodeId(nodeId + ".Port"), AttributeId.Value, expectedValue);
	}

	public boolean checkOpcUaVertexTypeValueEquals(final String nodeId, final String expectedValue) {
		return checkOpcUaAttribute(nodeId(nodeId + ".Type"), AttributeId.Value, expectedValue);
	}

	public boolean checkOpcUaVertexValueBetween(final String nodeId, final String valueLabel, final int minValue,
			final int maxValue) {
		try {
			final double value = ((Number) readAttribute(propertyNodeId(nodeId, valueLabel), AttributeId.Value))
					.doubleValue();
			System.out.println("Read value: " + value + " expected between: " + minValue + " and " + maxValue);
			return value >= minValue && value <= maxValue;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexValueEquals(final long nodeId, final String valueLabel, final Object expectedValue) {
		return checkOpcUaVertexValueEquals(String.valueOf(nodeId), valueLabel, expectedValue);
	}

	public boolean checkOpcUaVertexValueEquals(final String nodeId, final String valueLabel,
			final Object expectedValue) {
		try {
			final Object value = readAttribute(propertyNodeId(nodeId, valueLabel), AttributeId.Value);
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return valuesEqual(value, expectedValue);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkVertexExists(final long nodeId) {
		return graphVertex(graph.getWaldotNamespace().generateNodeId(nodeId)) != null;
	}

	public boolean checkVertexExists(final String nodeId) {
		return graphVertex(nodeId(nodeId)) != null;
	}

	public boolean checkVertexValueBetween(final String nodeId, final String valueLabel, final int minValue,
			final int maxValue) {
		try {
			final double value = ((Number) graphVertex(nodeId(nodeId)).property(valueLabel).value()).doubleValue();
			System.out.println("Vertex value: " + value + " expected between: " + minValue + " and " + maxValue);
			return value >= minValue && value <= maxValue;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkVertexValueEquals(final long nodeId, final String valueLabel, final Object expectedValue) {
		return checkVertexValueEquals(graph.getWaldotNamespace().generateNodeId(nodeId), valueLabel, expectedValue);
	}

	private boolean checkVertexValueEquals(final NodeId nodeId, final String valueLabel, final Object expectedValue) {
		try {
			final Object value = graphVertex(nodeId).property(valueLabel).value();
			System.out.println("Vertex value: " + value + " expected: " + expectedValue);
			return valuesEqual(value, expectedValue);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkVertexValueEquals(final String nodeId, final String valueLabel, final Object expectedValue) {
		return checkVertexValueEquals(nodeId(nodeId), valueLabel, expectedValue);
	}

	@Override
	public void close() throws Exception {
		disconnect();
	}

	public String[] createEdgeWithOpcUa(final String type, final String sourceNodeId, final String destinationNodeId,
			final String[] keyValues) {
		try {
			return client.createEdge(type, sourceNodeId, destinationNodeId, keyValues);
		} catch (final Exception e) {
			e.printStackTrace();
			return new String[] { "", e.getMessage() };
		}
	}

	public String[] createVertexWithOpcUa(final String id, final String label, final String type,
			final String[] keyValues) {
		try {
			return client.createVertex(id, label, type, keyValues);
		} catch (final Exception e) {
			e.printStackTrace();
			return new String[] { "", e.getMessage() };
		}
	}

	public String[] deleteVertexWithOpcUa(final int id) {
		try {
			return client.deleteVertex(id);
		} catch (final Exception e) {
			e.printStackTrace();
			return new String[] { "", e.getMessage() };
		}
	}

	public String[] deleteVertexWithOpcUa(final String id) {
		try {
			return client.deleteVertex(id);
		} catch (final Exception e) {
			e.printStackTrace();
			return new String[] { "", e.getMessage() };
		}
	}

	public void disconnect() {
		try {
			client.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private boolean edgeLabelIs(final NodeId edgeNodeId, final String label) {
		final WaldotEdge edge = graph.getWaldotNamespace().getEdgeNode(edgeNodeId);
		return edge != null && label.equals(edge.label());
	}

	/** @return the underlying client */
	public WaldOTAgentClientImplV1 getClient() {
		return client;
	}

	public List<String> getServerInfo() throws UaException {
		return client.getServerInfo();
	}

	private Vertex graphVertex(final NodeId nodeId) {
		try {
			return graph.vertex(nodeId);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static boolean isReferenceDescription(final ReferenceDescription reference) {
		return MiloSingleServerBaseReferenceNodeBuilder.HAS_REFERENCE_DESCRIPTION_REFERENCE
				.equals(reference.getReferenceTypeId().getIdentifier());
	}

	/**
	 * Tries to write a random string in the node value.
	 *
	 * @return true if the write is refused (or fails)
	 */
	private boolean isReadOnly(final NodeId nodeId) {
		try {
			final StatusCode result = client.getOpcUaClient()
					.write(List.of(new WriteValue(nodeId, AttributeId.Value.uid(), null,
							DataValue.valueOnly(new Variant(UUID.randomUUID().toString())))))
					.getResults()[0];
			System.out.println("Write result on " + nodeId.toParseableString() + ": " + result);
			return !result.isGood();
		} catch (final Exception e) {
			e.printStackTrace();
			return true;
		}
	}

	private static BrowseOptions nonHierarchical(final BrowseDirection direction, final NodeId referenceType) {
		return new BrowseOptions(direction, referenceType, true, uint(0xFF), uint(0x3F), uint(0));
	}

	private NodeId nodeId(final String identifier) {
		return graph.getWaldotNamespace().generateNodeId(identifier);
	}

	private boolean opcUaNodeExists(final NodeId nodeId) {
		try {
			resetCache();
			return client.getOpcUaClient().getAddressSpace().getNode(nodeId) != null;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private NodeId propertyNodeId(final String vertexId, final String key) {
		return nodeId(vertexId + "/" + key);
	}

	/** Reads an attribute without any cache (maxAge 0). */
	private Object readAttribute(final NodeId nodeId, final AttributeId attribute) throws UaException {
		final DataValue[] results = client.getOpcUaClient().read(0.0, TimestampsToReturn.Both,
				List.of(new ReadValueId(nodeId, attribute.uid(), null, QualifiedName.NULL_VALUE))).getResults();
		return results[0].value().getValue();
	}

	public long readIntOpcUaVertexValue(final String nodeId, final String valueLabel) throws UaException {
		final Object value = readAttribute(propertyNodeId(nodeId, valueLabel), AttributeId.Value);
		System.out.println("Read value: " + value);
		return ((Number) value).longValue();
	}

	private void resetCache() {
		client.getOpcUaClient().getAddressSpace().setNodeCache(new NodeCache());
	}

	public List<String> runExpression(final String expression) {
		try {
			return client.runExpression(expression);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public OpcUaSubscription subscribeToOpcUaVertexEventsMessage(final String nodeId, final EventObserver observer) {
		final var subscription = new OpcUaSubscription(client.getOpcUaClient());
		try {
			subscription.create();
		} catch (final UaException e) {
			e.printStackTrace();
		}
		final SimpleAttributeOperand[] select = new SimpleAttributeOperand[EVENT_FIELDS.length];
		for (int i = 0; i < EVENT_FIELDS.length; i++) {
			select[i] = new SimpleAttributeOperand(NodeIds.BaseEventType,
					new QualifiedName[] { new QualifiedName(0, EVENT_FIELDS[i]) }, AttributeId.Value.uid(), null);
		}
		final var monitoredItem = OpcUaMonitoredItem.newEventItem(nodeId(nodeId),
				new EventFilter(select, new ContentFilter(null)));
		monitoredItem.setEventValueListener(
				(item, vs) -> observer.onEvent(nodeId, vs[1].getValue().toString(), vs[4].getValue()));
		subscription.addMonitoredItem(monitoredItem);
		try {
			subscription.synchronizeMonitoredItems();
		} catch (final MonitoredItemSynchronizationException e) {
			e.printStackTrace();
		}
		return subscription;
	}

	public void writeOpcUaVertexValue(final String nodeId, final String valueLabel, final Object newValue) {
		try {
			final StatusCode result = client.getOpcUaClient()
					.write(List.of(new WriteValue(propertyNodeId(nodeId, valueLabel), AttributeId.Value.uid(), null,
							DataValue.valueOnly(new Variant(newValue)))))
					.getResults()[0];
			System.out.println("Write result: " + result);
		} catch (final UaException e) {
			e.printStackTrace();
		}
	}

}
