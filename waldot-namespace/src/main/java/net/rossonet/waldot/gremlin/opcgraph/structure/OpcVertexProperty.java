
package net.rossonet.waldot.gremlin.opcgraph.structure;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;
import net.rossonet.waldot.utils.LogHelper;

public class OpcVertexProperty<DATA_TYPE> extends UaVariableNode implements WaldotVertexProperty<DATA_TYPE> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final WaldotGraph graph;
	private final boolean allowNullPropertyValues;

	public OpcVertexProperty(WaldotGraph graph, final WaldotVertex vertex, final String key, final DATA_TYPE value,
			UaNodeContext context, NodeId nodeId, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			NodeId dataType, Integer valueRank, UInteger[] arrayDimensions, UByte accessLevel, UByte userAccessLevel,
			Double minimumSamplingInterval, boolean historizing, boolean allowNullPropertyValues) {
		super(context, nodeId, QualifiedName.parse(key), LocalizedText.english(key), description, writeMask,
				userWriteMask, null, dataType, valueRank, arrayDimensions, accessLevel, userAccessLevel,
				minimumSamplingInterval, historizing);
		this.graph = graph;
		this.allowNullPropertyValues = allowNullPropertyValues;
		try {
			final Variant variant = new Variant(value);
			final DataValue dataValue = DataValue.newValue().setStatus(StatusCode.GOOD).setSourceTime(DateTime.now())
					.setValue(variant).build();
			setValue(dataValue);
			vertex.addComponent(this);
		} catch (final Exception a) {
			final DataValue errorDataValue = DataValue.newValue().setStatus(StatusCode.BAD).build();
			setValue(errorDataValue);
			vertex.addComponent(this);
			logger.error(LogHelper.stackTraceToString(a));
		}

	}

	@Override
	public Object clone() {
		return new OpcVertexProperty<DATA_TYPE>(graph, getVertexPropertyReference(), key(), value(), getNodeContext(),
				getNodeId(), getDescription(), getWriteMask(), getUserWriteMask(), getDataType(), getValueRank(),
				getArrayDimensions(), getAccessLevel(), getUserAccessLevel(), getMinimumSamplingInterval(),
				getHistorizing(), allowNullPropertyValues);
	}

	public OpcVertexProperty<DATA_TYPE> copy(final OpcVertex newOwner) {
		return new OpcVertexProperty<DATA_TYPE>(graph, newOwner, key(), value(), getNodeContext(), getNodeId(),
				getDescription(), getWriteMask(), getUserWriteMask(), getDataType(), getValueRank(),
				getArrayDimensions(), getAccessLevel(), getUserAccessLevel(), getMinimumSamplingInterval(),
				getHistorizing(), allowNullPropertyValues);
	}

	@Override
	public Vertex element() {
		return getVertexPropertyReference();
	}

	@Override
	public boolean equals(final Object object) {
		return ElementHelper.areEqual(this, object);
	}

	@Override
	public WaldotNamespace getNamespace() {
		return graph.getOpcNamespace();
	}

	@Override
	public WaldotEdge getPropertyReference() {
		return getNamespace().getPropertyReference(this);
	}

	private WaldotVertex getVertexPropertyReference() {
		return getNamespace().getVertexPropertyReference(this);
	}

	@Override
	public NodeId id() {
		return getNodeId();
	}

	@Override
	public boolean isPresent() {
		return getValue().getStatusCode().isGood();
	}

	@Override
	public String key() {
		return getBrowseName().getName();
	}

	@Override
	public Set<String> keys() {
		logger.error("keys() is not implemented");
		return new HashSet<>();
	}

	@Override
	public <U> Iterator<Property<U>> properties(final String... propertyKeys) {
		logger.error("properties() is not implemented");
		return new HashSet<Property<U>>().iterator();
	}

	@Override
	public <U> Property<U> property(final String key) {
		logger.error("property() is not implemented");
		return Property.empty();
	}

	@Override
	public <U> Property<U> property(final String key, final U value) {
		logger.error("property() is not implemented");
		return Property.empty();
	}

	@Override
	public void remove() {
		getNamespace().removeVertexProperty(this.getNodeId());
	}

	@Override
	public String toString() {
		if (!isPresent()) {
			return AbstractOpcGraph.EMPTY_VERTEX_PROPERTY;
		}
		final String valueString = String.valueOf(value());
		return AbstractOpcGraph.VP + AbstractOpcGraph.L_BRACKET + getBrowseName().getName() + AbstractOpcGraph.ARROW
				+ StringUtils.abbreviate(valueString, 20) + AbstractOpcGraph.R_BRACKET;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DATA_TYPE value() {
		return (DATA_TYPE) getValue().getValue().getValue();
	}

}
