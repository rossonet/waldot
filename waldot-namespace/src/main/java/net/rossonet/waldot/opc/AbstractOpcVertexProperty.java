package net.rossonet.waldot.opc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PropertyObserver;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;
import net.rossonet.waldot.opc.gremlin.GremlinProperty;
import net.rossonet.waldot.utils.LogHelper;

public abstract class AbstractOpcVertexProperty<DATA_TYPE> extends GremlinProperty<DATA_TYPE>
		implements WaldotVertexProperty<DATA_TYPE> {
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final WaldotGraph graph;

	protected boolean allowNullPropertyValues = false;

	protected final List<PropertyObserver> propertyObservers = new ArrayList<>();
	private ByteString icon;
	private final WaldotVertex referenceVertex;

	public AbstractOpcVertexProperty(WaldotGraph graph, final WaldotVertex vertex, final String key,
			final DATA_TYPE value, UaNodeContext context, NodeId nodeId, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions, UByte accessLevel,
			UByte userAccessLevel, Double minimumSamplingInterval, boolean historizing) {
		super(graph, key, value, context, nodeId, description, writeMask, userWriteMask, dataType, valueRank,
				arrayDimensions, accessLevel, userAccessLevel, minimumSamplingInterval, historizing);
		this.referenceVertex = vertex;
		this.graph = graph;
		this.allowNullPropertyValues = graph.features().vertex().supportsNullPropertyValues();
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
	public void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
		// not implemented
		throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	public Vertex element() {
		return getVertexPropertyReference();
	}

	@Override
	public ByteString getIcon() {
		return icon;
	}

	@Override
	public WaldotNamespace getNamespace() {
		return graph.getWaldotNamespace();
	}

	public WaldotVertex getReferenceVertex() {
		return referenceVertex;
	}

	@Override
	public WaldotVertex getVertexPropertyReference() {
		return getNamespace().getVertexPropertyReference(this);
	}

	@Override
	public Graph graph() {
		return this.graph;
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
	public <U> Iterator<Property<U>> properties(String... propertyKeys) {
		return null;
	}

	@Override
	public <V> VertexProperty<V> property(final String key) {
		return VertexProperty.<V>empty();
	}

	@Override
	public <V> Property<V> property(String key, V value) {
		return Property.empty();
	}

	@Override
	public void propertyChanged(UaNode sourceNode, AttributeId attributeId, Object value) {
		propertyObservers.forEach(observer -> observer.propertyChanged(sourceNode, attributeId, value));

	}

	@Override
	public void remove() {
		getVertexPropertyReference().removeComponent(this);
	}

	@Override
	public void setIcon(ByteString icon) {
		this.icon = icon;

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
