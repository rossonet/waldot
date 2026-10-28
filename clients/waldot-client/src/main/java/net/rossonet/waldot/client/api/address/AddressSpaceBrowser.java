package net.rossonet.waldot.client.api.address;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

/**
 * Generic access to the OPC UA address space of the server: browse, read,
 * write and method calls. The errors are reported with
 * {@link net.rossonet.waldot.client.api.WaldotClientException}.
 */
public interface AddressSpaceBrowser {

	/** Browses the forward hierarchical references of a node. */
	default List<ReferenceInfo> browse(final NodeId nodeId) {
		return browse(nodeId, BrowseFilter.children());
	}

	/** Browses the references of a node (continuation points are followed). */
	List<ReferenceInfo> browse(NodeId nodeId, BrowseFilter filter);

	/**
	 * Calls a method.
	 *
	 * @return the output values
	 */
	List<Object> call(NodeId objectId, NodeId methodId, Object... inputs);

	/** @return the index of the WaldOT namespace on the server */
	int namespaceIndex();

	/** @return the main attributes of a node, empty if the node does not exist */
	Optional<NodeInfo> node(NodeId nodeId);

	/**
	 * Parses a node id: the parseable form ({@code ns=2;s=x}, {@code i=85})
	 * or a plain string identifier in the WaldOT namespace.
	 */
	NodeId parseNodeId(String value);

	/** Reads some attributes of a node. */
	Map<AttributeId, DataValue> readAttributes(NodeId nodeId, AttributeId... attributes);

	/** Reads the value attribute of a node. */
	DataValue readValue(NodeId nodeId);

	/** Reads the value attribute of some nodes in a single request. */
	List<DataValue> readValues(List<NodeId> nodeIds);

	/**
	 * Follows a path of browse names from a start node (hierarchical forward
	 * references).
	 *
	 * @return the node at the end of the path, empty if not found
	 */
	Optional<NodeId> resolvePath(NodeId start, String... browseNames);

	/** @return a node id in the WaldOT namespace with a string identifier */
	NodeId waldotNodeId(String identifier);

	/** Writes the value attribute of a node. */
	StatusCode writeValue(NodeId nodeId, Object value);
}
