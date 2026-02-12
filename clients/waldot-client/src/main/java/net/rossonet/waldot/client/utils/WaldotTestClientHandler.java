package net.rossonet.waldot.client.utils;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.client.AddressSpace.BrowseOptions;
import org.eclipse.milo.opcua.sdk.client.NodeCache;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
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
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.SimpleAttributeOperand;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;

import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.v1.DefaultWaldOTAgentClientConfigurationV1;
import net.rossonet.waldot.client.v1.WaldOTAgentClientImplV1;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;

public class WaldotTestClientHandler implements AutoCloseable {

	public interface EventObserver {
		default void onEvent(String nodeId, String eventType, Object value) {
			System.out.println("Received event for node: " + nodeId + " event type: " + eventType + " value: " + value);
		}
	}

	private final WaldOTAgentClientImplV1 client;

	private final WaldotGraph graph;

	public WaldotTestClientHandler(WaldotGraph g) {
		final WaldOTAgentClientConfiguration configuration = new DefaultWaldOTAgentClientConfigurationV1();
		client = new WaldOTAgentClientImplV1(configuration);
		client.startConnectionProcedure();
		this.graph = g;
		final int retry = 0;
		while (retry < 10) {
			if (client.getStatus().equals(Status.RUNNING)) {
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

	public boolean checkOpcUaEdgeBrowserNameNameValueEquals(WaldotEdge edge, String expectedValue) {
		try {
			resetCache();
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(edge.getNodeId(), AttributeId.BrowseName.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final String value = ((QualifiedName) results[0].value().getValue()).getName();
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return expectedValue.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaEdgeDescriptionValueEquals(WaldotEdge edge, String expectedValue) {
		try {
			resetCache();
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(edge.getNodeId(), AttributeId.Description.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final String value = ((LocalizedText) results[0].value().getValue()).getText();
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return expectedValue.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaEdgeDisplayNameValueEquals(WaldotEdge edge, String expectedValue) {
		try {
			resetCache();
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(edge.getNodeId(), AttributeId.DisplayName.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final String value = ((LocalizedText) results[0].value().getValue()).getText();
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return expectedValue.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaEdgeLabelReadOnly(WaldotEdge edge) {

		final NodeId completedNodeId = graph.getWaldotNamespace()
				.generateNodeId(edge.getNodeId().getIdentifier().toString() + ".Label");
		final OpcUaClient clientOpc = client.getOpcUaClient();
		try {
			resetCache();
			final List<WriteValue> writeValues = new ArrayList<>();
			writeValues.add(new WriteValue(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					DataValue.valueOnly(new Variant(UUID.randomUUID().toString()))));
			final WriteResponse writeResponse = clientOpc.write(writeValues);
			System.out.println("Write response: " + writeResponse);
			if (writeResponse.getResults()[0].isGood()) {
				System.out.println("Label is not read-only");
				return false;
			} else {
				System.out.println("Label is read-only");
				return true;
			}
		} catch (final Exception e) {
			e.printStackTrace();
			return true;
		}
	}

	public boolean checkOpcUaEdgeLabelValueEquals(WaldotEdge edge, String expectedValue) {
		try {
			resetCache();
			final NodeId completedNodeId = graph.getWaldotNamespace()
					.generateNodeId(edge.getNodeId().getIdentifier().toString() + ".Label");
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final String value = (String) results[0].value().getValue();
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return expectedValue.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexBindReadOnly(String nodeId) {
		final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + ".Bind");
		final OpcUaClient clientOpc = client.getOpcUaClient();
		try {
			resetCache();
			final List<WriteValue> writeValues = new ArrayList<>();
			writeValues.add(new WriteValue(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					DataValue.valueOnly(new Variant(UUID.randomUUID().toString()))));
			final WriteResponse writeResponse = clientOpc.write(writeValues);
			System.out.println("Write response: " + writeResponse);
			if (writeResponse.getResults()[0].isGood()) {
				System.out.println("Port is not read-only");
				return false;
			} else {
				System.out.println("Port is read-only");
				return true;
			}
		} catch (final Exception e) {
			e.printStackTrace();
			return true;
		}
	}

	public boolean checkOpcUaVertexBindValueEquals(String nodeId, String expectedValue) {
		try {
			resetCache();
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + ".Bind");
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final String value = (String) results[0].value().getValue();
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return expectedValue.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexBrowserNameNameValueEquals(String nodeId, String expectedValue) {
		try {
			resetCache();
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.BrowseName.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final String value = ((QualifiedName) results[0].value().getValue()).getName();
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return expectedValue.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexDescriptionValueEquals(String nodeId, String expectedValue) {
		try {
			resetCache();
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.Description.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final String value = ((LocalizedText) results[0].value().getValue()).getText();
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return expectedValue.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexDisplayNameValueEquals(String nodeId, String expectedValue) {
		try {
			resetCache();
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.DisplayName.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final String value = ((LocalizedText) results[0].value().getValue()).getText();
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return expectedValue.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexEdge(long nodeOut, long nodeIn, String typeEdge) {
		try {
			resetCache();
			final NodeId out = graph.getWaldotNamespace().generateNodeId(nodeOut);
			final NodeId in = graph.getWaldotNamespace().generateNodeId(nodeIn);
			final UaNode outObject = client.getOpcUaClient().getAddressSpace().getNode(out);
			final UaNode inObject = client.getOpcUaClient().getAddressSpace().getNode(in);
			final BrowseOptions searchOutOptions = new BrowseOptions(BrowseDirection.Forward,
					NodeIds.NonHierarchicalReferences, true, uint(0xFF), uint(0x3F), uint(0));

			boolean descriptionOut = false;
			boolean foundOut = false;
			for (final ReferenceDescription r : outObject.browse(searchOutOptions)) {
				// System.out.println("Browsing from out node: " +
				// r.getReferenceTypeId().getIdentifier());
				if (typeEdge.equals(r.getReferenceTypeId().getIdentifier())) {
					foundOut = true;
				}
				if (MiloSingleServerBaseReferenceNodeBuilder.HAS_REFERENCE_DESCRIPTION_REFERENCE
						.equals(r.getReferenceTypeId().getIdentifier())) {
					final ReferenceDescription ref = r;
					final BrowseOptions edgetOptions = new BrowseOptions(BrowseDirection.Forward,
							ref.getReferenceTypeId(), true, uint(0xFF), uint(0x3F), uint(0));
					final List<? extends UaNode> browseNodes = outObject.browseNodes(edgetOptions);

					for (final UaNode nodeEdge : browseNodes) {
						final String edgeNodeLabel = graph.getWaldotNamespace().getEdgeNode(nodeEdge.getNodeId())
								.label();
						if (edgeNodeLabel.equals(typeEdge)) {
							descriptionOut = true;
						}
					}
				}
			}
			if (!foundOut || !descriptionOut) {
				return false;
			}
			final BrowseOptions searchInOptions = new BrowseOptions(BrowseDirection.Inverse,
					NodeIds.NonHierarchicalReferences, true, uint(0xFF), uint(0x3F), uint(0));
			boolean foundIn = false;
			for (final ReferenceDescription r : inObject.browse(searchInOptions)) {
				// System.out.println("Browsing from in node: " +
				// r.getReferenceTypeId().getIdentifier());
				if (typeEdge.equals(r.getReferenceTypeId().getIdentifier())) {
					foundIn = true;
					break;
				}
			}
			if (!foundIn) {
				return false;
			}
			final BrowseOptions searchInDescriptionOptions = new BrowseOptions(BrowseDirection.Forward,
					NodeIds.NonHierarchicalReferences, true, uint(0xFF), uint(0x3F), uint(0));
			boolean descriptionIn = false;
			for (final ReferenceDescription r : inObject.browse(searchInDescriptionOptions)) {
				// System.out.println("Browsing from in node: " +
				// r.getReferenceTypeId().getIdentifier());
				if (MiloSingleServerBaseReferenceNodeBuilder.HAS_REFERENCE_DESCRIPTION_REFERENCE
						.equals(r.getReferenceTypeId().getIdentifier())) {
					// descriptionIn = true;
					final ReferenceDescription ref = r;
					final BrowseOptions edgetOptions = new BrowseOptions(BrowseDirection.Forward,
							ref.getReferenceTypeId(), true, uint(0xFF), uint(0x3F), uint(0));
					final UaNode nodeEdge = outObject.browseNodes(edgetOptions).get(0);
					if (nodeEdge == null) {
						return false;
					}
					final String edgeNodeLabel = graph.getWaldotNamespace().getEdgeNode(nodeEdge.getNodeId()).label();
					if (edgeNodeLabel.equals(typeEdge)) {
						descriptionIn = true;
					}
				}
			}
			if (!descriptionIn) {
				return false;
			}
			return true;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexExists(long nodeId) {
		try {
			resetCache();
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final UaNode node = client.getOpcUaClient().getAddressSpace().getNode(completedNodeId);
			return node != null;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public boolean checkOpcUaVertexExists(String nodeId) {
		try {
			resetCache();
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final UaNode node = client.getOpcUaClient().getAddressSpace().getNode(completedNodeId);
			return node != null;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public boolean checkOpcUaVertexLabelReadOnly(String nodeId) {
		final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + ".Label");
		final OpcUaClient clientOpc = client.getOpcUaClient();
		try {
			resetCache();
			final List<WriteValue> writeValues = new ArrayList<>();
			writeValues.add(new WriteValue(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					DataValue.valueOnly(new Variant(UUID.randomUUID().toString()))));
			final WriteResponse writeResponse = clientOpc.write(writeValues);
			System.out.println("Write response: " + writeResponse);
			if (writeResponse.getResults()[0].isGood()) {
				System.out.println("Label is not read-only");
				return false;
			} else {
				System.out.println("Label is read-only");
				return true;
			}
		} catch (final Exception e) {
			e.printStackTrace();
			return true;
		}
	}

	public boolean checkOpcUaVertexLabelValueEquals(String nodeId, String expectedValue) {
		try {
			resetCache();
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + ".Label");
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final String value = (String) results[0].value().getValue();
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return expectedValue.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexPortReadOnly(String nodeId) {
		final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + ".Port");
		final OpcUaClient clientOpc = client.getOpcUaClient();
		try {
			resetCache();
			final List<WriteValue> writeValues = new ArrayList<>();
			writeValues.add(new WriteValue(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					DataValue.valueOnly(new Variant(UUID.randomUUID().toString()))));
			final WriteResponse writeResponse = clientOpc.write(writeValues);
			System.out.println("Write response: " + writeResponse);
			if (writeResponse.getResults()[0].isGood()) {
				System.out.println("Port is not read-only");
				return false;
			} else {
				System.out.println("Port is read-only");
				return true;
			}
		} catch (final Exception e) {
			e.printStackTrace();
			return true;
		}
	}

	public boolean checkOpcUaVertexPortValueEquals(String nodeId, String expectedValue) {
		try {
			resetCache();
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + ".Port");
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final String value = String.valueOf(results[0].value().getValue());
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return expectedValue.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexTypeValueEquals(String nodeId, String expectedValue) {
		try {
			resetCache();
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + ".Type");
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final String value = String.valueOf(results[0].value().getValue());
			System.out.println("Read value: " + value + " expected: " + expectedValue);
			return expectedValue.equals(value);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexValueBetween(String nodeId, String valueLabel, int minValue, int maxValue) {
		try {
			resetCache();
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

	public boolean checkOpcUaVertexValueEquals(long nodeId, String valueLabel, Object expectedValue) {
		try {
			resetCache();
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId + "/" + valueLabel);
			final List<ReadValueId> readValueIds = new ArrayList<>();
			readValueIds.add(new ReadValueId(completedNodeId, AttributeId.Value.uid(), null, // indexRange
					QualifiedName.NULL_VALUE));
			final ReadResponse readResponse = client.getOpcUaClient().read(0.0, // maxAge
					TimestampsToReturn.Both, readValueIds);
			final DataValue[] results = readResponse.getResults();
			final Object value = results[0].value().getValue();
			return value.equals(expectedValue);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkOpcUaVertexValueEquals(String nodeId, String valueLabel, Object expectedValue) {
		try {
			resetCache();
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

	public boolean checkVertexExists(long nodeId) {
		try {
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final Vertex v = graph.vertex(completedNodeId);
			return v != null;
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

	public boolean checkVertexValueEquals(long nodeId, String valueLabel, Object expectedValue) {
		try {
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final Vertex v = graph.vertex(completedNodeId);
			final Object value = v.property(valueLabel).value();
			return value.equals(expectedValue);
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

	public String[] createEdgeWithOpcUa(String type, String sourceNodeId, String destinationNodeId,
			String[] keyValues) {
		try {
			return client.createEdge(type, sourceNodeId, destinationNodeId, keyValues);
		} catch (final Exception e) {
			e.printStackTrace();
			return new String[] { "", e.getMessage() };
		}

	}

	public String[] createVertexWithOpcUa(String id, String label, String type, String[] keyValues) {
		try {
			return client.createVertex(id, label, type, keyValues);
		} catch (final Exception e) {
			e.printStackTrace();
			return new String[] { "", e.getMessage() };
		}

	}

	public String[] deleteVertexWithOpcUa(int id) {
		try {
			return client.deleteVertex(id);
		} catch (final Exception e) {
			e.printStackTrace();
			return new String[] { "", e.getMessage() };
		}
	}

	public String[] deleteVertexWithOpcUa(String id) {
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

	public List<String> getServerInfo() throws UaException {
		return client.getServerInfo();
	}

	public long readIntOpcUaVertexValue(String nodeId, String valueLabel) throws UaException {
		resetCache();
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

	private void resetCache() {
		client.getOpcUaClient().getAddressSpace().setNodeCache(new NodeCache());

	}

	public List<String> runExpression(String expression) {
		try {
			return client.runExpression(expression);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public OpcUaSubscription subscribeToOpcUaVertexEventsMessage(String nodeId, EventObserver observer) {
		final var subscription = new OpcUaSubscription(client.getOpcUaClient());
		try {
			subscription.create();
		} catch (final UaException e) {
			e.printStackTrace();
		}
		final EventFilter eventFilter = new EventFilter(new SimpleAttributeOperand[] {
				new SimpleAttributeOperand(NodeIds.BaseEventType,
						new QualifiedName[] { new QualifiedName(0, "EventId") }, AttributeId.Value.uid(), null),
				new SimpleAttributeOperand(NodeIds.BaseEventType,
						new QualifiedName[] { new QualifiedName(0, "EventType") }, AttributeId.Value.uid(), null),
				new SimpleAttributeOperand(NodeIds.BaseEventType,
						new QualifiedName[] { new QualifiedName(0, "Severity") }, AttributeId.Value.uid(), null),
				new SimpleAttributeOperand(NodeIds.BaseEventType, new QualifiedName[] { new QualifiedName(0, "Time") },
						AttributeId.Value.uid(), null),
				new SimpleAttributeOperand(NodeIds.BaseEventType,
						new QualifiedName[] { new QualifiedName(0, "Message") }, AttributeId.Value.uid(), null) },
				new ContentFilter(null));

		final var monitoredItem = OpcUaMonitoredItem.newEventItem(graph.getWaldotNamespace().generateNodeId(nodeId),
				eventFilter);
		monitoredItem.setEventValueListener((item, vs) -> {
			observer.onEvent(nodeId, vs[1].getValue().toString(), vs[4].getValue());
		});
		subscription.addMonitoredItem(monitoredItem);
		try {
			subscription.synchronizeMonitoredItems();
		} catch (final MonitoredItemSynchronizationException e) {
			e.printStackTrace();
		}
		return subscription;
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
