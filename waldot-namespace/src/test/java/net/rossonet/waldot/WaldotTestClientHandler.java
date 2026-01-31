package net.rossonet.waldot;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.ArrayList;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.client.AddressSpace.BrowseOptions;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.agent.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.agent.client.v1.DefaultWaldOTAgentClientConfigurationV1;
import net.rossonet.waldot.agent.client.v1.WaldOTAgentClientImplV1;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;

public class WaldotTestClientHandler implements AutoCloseable {

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

	public boolean checkOpcUaVertexEdge(long nodeOut, long nodeIn, String typeEdge) {
		try {
			final NodeId out = graph.getWaldotNamespace().generateNodeId(nodeOut);
			final NodeId in = graph.getWaldotNamespace().generateNodeId(nodeIn);
			final UaObjectNode outObject = client.getOpcUaClient().getAddressSpace().getObjectNode(out);
			final UaObjectNode inObject = client.getOpcUaClient().getAddressSpace().getObjectNode(in);
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
					final UaNode nodeEdge = outObject.browseNodes(edgetOptions).get(0);
					if (nodeEdge == null) {
						return false;
					}
					if (nodeEdge.getBrowseName().getName().equals(typeEdge)) {
						descriptionOut = true;
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
					if (nodeEdge.getBrowseName().getName().equals(typeEdge)) {
						descriptionIn = true;
						break;
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
			final NodeId completedNodeId = graph.getWaldotNamespace().generateNodeId(nodeId);
			final UaObjectNode node = client.getOpcUaClient().getAddressSpace().getObjectNode(completedNodeId);
			return node != null;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
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

	public boolean checkOpcUaVertexValueEquals(long nodeId, String valueLabel, Object expectedValue) {
		try {
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

	public List<String> getServerInfo() throws UaException {
		return client.getServerInfo();
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
			// final WriteResponse writeResponse =
			client.getOpcUaClient().write(writeValues);
			// System.out.println("response: " + writeResponse);
		} catch (final UaException e) {
			e.printStackTrace();
		}
	}

}
