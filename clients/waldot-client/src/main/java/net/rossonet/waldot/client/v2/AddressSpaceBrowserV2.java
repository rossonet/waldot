package net.rossonet.waldot.client.v2;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;

import net.rossonet.waldot.client.api.WaldotClientException;
import net.rossonet.waldot.client.api.address.AddressSpaceBrowser;
import net.rossonet.waldot.client.api.address.BrowseFilter;
import net.rossonet.waldot.client.api.address.NodeInfo;
import net.rossonet.waldot.client.api.address.ReferenceInfo;
import net.rossonet.waldot.client.support.ArgumentConverter;

/** Generic OPC UA address space access of the client v2. */
final class AddressSpaceBrowserV2 implements AddressSpaceBrowser {

	private static String text(final Object value) {
		if (value instanceof LocalizedText localized) {
			return localized.getText();
		}
		if (value instanceof QualifiedName qualified) {
			return qualified.getName();
		}
		return value == null ? null : value.toString();
	}

	private final WaldotClientImplV2 owner;

	AddressSpaceBrowserV2(final WaldotClientImplV2 owner) {
		this.owner = owner;
	}

	@Override
	public List<ReferenceInfo> browse(final NodeId nodeId, final BrowseFilter filter) {
		final OpcUaClient client = owner.requireClient();
		final BrowseFilter browseFilter = filter == null ? BrowseFilter.children() : filter;
		try {
			final BrowseDescription description = new BrowseDescription(nodeId, browseFilter.direction(),
					browseFilter.referenceTypeId(), browseFilter.includeSubtypes(), uint(browseFilter.nodeClassMask()),
					uint(BrowseResultMask.All.getValue()));
			final List<ReferenceInfo> references = new ArrayList<>();
			BrowseResult result = client.browse(description);
			final NamespaceTable namespaces = client.getNamespaceTable();
			while (true) {
				if (result.getStatusCode() != null && result.getStatusCode().isBad()) {
					throw new WaldotClientException("browse of " + nodeId + " failed", result.getStatusCode(), null);
				}
				if (result.getReferences() != null) {
					for (final ReferenceDescription reference : result.getReferences()) {
						references.add(toReferenceInfo(reference, namespaces));
					}
				}
				final ByteString continuationPoint = result.getContinuationPoint();
				if (continuationPoint == null || continuationPoint.isNullOrEmpty()) {
					break;
				}
				final BrowseResult[] next = client.browseNext(false, List.of(continuationPoint)).getResults();
				if (next == null || next.length == 0) {
					break;
				}
				result = next[0];
			}
			return references;
		} catch (final UaException e) {
			throw WaldotClientException.of("browse of " + nodeId + " failed", e);
		}
	}

	@Override
	public List<Object> call(final NodeId objectId, final NodeId methodId, final Object... inputs) {
		final Variant[] variants = ArgumentConverter.toVariants(inputs, null);
		final CallMethodResult result = callMethod(objectId, methodId, variants);
		if (result.getStatusCode() != null && result.getStatusCode().isBad()) {
			throw new WaldotClientException("call of " + methodId + " failed: " + result.getStatusCode(),
					result.getStatusCode(), null);
		}
		return outputs(result);
	}

	CallMethodResult callMethod(final NodeId objectId, final NodeId methodId, final Variant[] inputs) {
		final OpcUaClient client = owner.requireClient();
		try {
			final CallMethodResult[] results = client
					.call(List.of(new CallMethodRequest(objectId, methodId, inputs))).getResults();
			if (results == null || results.length == 0) {
				throw new WaldotClientException("empty call response for " + methodId);
			}
			return results[0];
		} catch (final UaException e) {
			throw WaldotClientException.of("call of " + methodId + " failed", e);
		}
	}

	@Override
	public int namespaceIndex() {
		return owner.namespaceIndex();
	}

	@Override
	public Optional<NodeInfo> node(final NodeId nodeId) {
		final Map<AttributeId, DataValue> values = readAttributes(nodeId, AttributeId.NodeClass,
				AttributeId.BrowseName, AttributeId.DisplayName, AttributeId.Description);
		final DataValue nodeClass = values.get(AttributeId.NodeClass);
		if (nodeClass == null || nodeClass.getStatusCode() != null && nodeClass.getStatusCode().isBad()) {
			return Optional.empty();
		}
		final Object classValue = nodeClass.getValue().getValue();
		final NodeClass resolved = classValue instanceof NodeClass nc ? nc
				: classValue instanceof Number number ? NodeClass.from(number.intValue()) : null;
		return Optional.of(new NodeInfo(nodeId, resolved, text(values.get(AttributeId.BrowseName).getValue().getValue()),
				text(values.get(AttributeId.DisplayName).getValue().getValue()),
				text(values.get(AttributeId.Description).getValue().getValue())));
	}

	List<Object> outputs(final CallMethodResult result) {
		final List<Object> outputs = new ArrayList<>();
		if (result.getOutputArguments() != null) {
			for (final Variant output : result.getOutputArguments()) {
				outputs.add(ArgumentConverter.fromVariant(output));
			}
		}
		return outputs;
	}

	@Override
	public NodeId parseNodeId(final String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("empty node id");
		}
		final String trimmed = value.trim();
		if (trimmed.contains("=")) {
			final Optional<NodeId> parsed = NodeId.parseSafe(trimmed);
			if (parsed.isPresent()) {
				return parsed.get();
			}
		}
		return waldotNodeId(trimmed);
	}

	@Override
	public Map<AttributeId, DataValue> readAttributes(final NodeId nodeId, final AttributeId... attributes) {
		final OpcUaClient client = owner.requireClient();
		final List<ReadValueId> reads = new ArrayList<>();
		for (final AttributeId attribute : attributes) {
			reads.add(new ReadValueId(nodeId, attribute.uid(), null, QualifiedName.NULL_VALUE));
		}
		try {
			final DataValue[] results = client.read(0.0, TimestampsToReturn.Both, reads).getResults();
			final Map<AttributeId, DataValue> values = new LinkedHashMap<>();
			for (int i = 0; i < attributes.length; i++) {
				values.put(attributes[i], results != null && i < results.length ? results[i]
						: new DataValue(new StatusCode(StatusCodes.Bad_UnexpectedError)));
			}
			return values;
		} catch (final UaException e) {
			throw WaldotClientException.of("read of " + nodeId + " failed", e);
		}
	}

	@Override
	public DataValue readValue(final NodeId nodeId) {
		return readValues(List.of(nodeId)).get(0);
	}

	@Override
	public List<DataValue> readValues(final List<NodeId> nodeIds) {
		final OpcUaClient client = owner.requireClient();
		try {
			return client.readValues(0.0, TimestampsToReturn.Both, nodeIds);
		} catch (final UaException e) {
			throw WaldotClientException.of("read of " + nodeIds + " failed", e);
		}
	}

	@Override
	public Optional<NodeId> resolvePath(final NodeId start, final String... browseNames) {
		NodeId current = start;
		for (final String name : browseNames) {
			final QualifiedName qualified = name.matches("^\\d+:.*") ? QualifiedName.parse(name) : null;
			NodeId found = null;
			for (final ReferenceInfo reference : browse(current, BrowseFilter.children())) {
				if (reference.targetId() == null) {
					continue;
				}
				if (qualified != null ? qualified.getName().equals(reference.browseName())
						: name.equals(reference.browseName())) {
					found = reference.targetId();
					break;
				}
			}
			if (found == null) {
				return Optional.empty();
			}
			current = found;
		}
		return Optional.of(current);
	}

	private ReferenceInfo toReferenceInfo(final ReferenceDescription reference, final NamespaceTable namespaces) {
		final ExpandedNodeId target = reference.getNodeId();
		final ExpandedNodeId type = reference.getTypeDefinition();
		return new ReferenceInfo(reference.getReferenceTypeId(), Boolean.TRUE.equals(reference.getIsForward()),
				target == null ? null : target.toNodeId(namespaces).orElse(null),
				reference.getBrowseName() == null ? null : reference.getBrowseName().getName(),
				reference.getDisplayName() == null ? null : reference.getDisplayName().getText(),
				reference.getNodeClass(),
				type == null || type.isNull() ? null : type.toNodeId(namespaces).orElse(null));
	}

	@Override
	public NodeId waldotNodeId(final String identifier) {
		return new NodeId(owner.namespaceIndex(), identifier);
	}

	@Override
	public StatusCode writeValue(final NodeId nodeId, final Object value) {
		final OpcUaClient client = owner.requireClient();
		Object converted = value;
		if (!(value instanceof Variant) && !(value instanceof DataValue)) {
			final DataValue dataType = readAttributes(nodeId, AttributeId.DataType).get(AttributeId.DataType);
			if (dataType.getStatusCode() == null || dataType.getStatusCode().isGood()) {
				final Object type = dataType.getValue().getValue();
				if (type instanceof NodeId typeId) {
					try {
						converted = ArgumentConverter.convertScalar(value, typeId);
					} catch (final IllegalArgumentException e) {
						converted = value;
					}
				}
			}
		}
		final DataValue dataValue = converted instanceof DataValue dv ? dv
				: DataValue.valueOnly(converted instanceof Variant v ? v : new Variant(converted));
		try {
			return client.writeValues(List.of(nodeId), List.of(dataValue)).get(0);
		} catch (final UaException e) {
			throw WaldotClientException.of("write of " + nodeId + " failed", e);
		}
	}

	@Override
	public String toString() {
		return "AddressSpaceBrowserV2[ns=" + owner.namespaceIndex() + "]";
	}
}
