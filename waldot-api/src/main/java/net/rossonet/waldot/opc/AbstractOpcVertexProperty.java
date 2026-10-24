package net.rossonet.waldot.opc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
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
import net.rossonet.waldot.api.models.base.GremlinProperty;
import net.rossonet.waldot.utils.LogHelper;

/**
 * AbstractOpcVertexProperty is an abstract class that implements the
 * WaldotVertexProperty interface. It provides a base implementation for vertex
 * properties in the Waldot graph model, including methods for managing property
 * values, observers, and interactions with the WaldotGraph.
 * 
 * @param <DATA_TYPE> the type of data stored in the vertex property
 * @author Andrea Ambrosini - Zona Autonoma srl
 */
public abstract class AbstractOpcVertexProperty<DATA_TYPE>
		extends
			GremlinProperty<DATA_TYPE>
		implements
			WaldotVertexProperty<DATA_TYPE>,
			AttributeObserver {

	protected boolean allowNullPropertyValues = false;

	protected final WaldotGraph graph;

	private ByteString icon;

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final List<PropertyObserver> propertyObservers = new java.util.concurrent.CopyOnWriteArrayList<>();

	private final WaldotVertex referenceVertex;

	public AbstractOpcVertexProperty(final WaldotGraph graph,
			final WaldotVertex vertex, final String key, final DATA_TYPE value,
			final UaNodeContext context, final NodeId nodeId,
			final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask, final NodeId dataType,
			final Integer valueRank, final UInteger[] arrayDimensions,
			final UByte accessLevel, final UByte userAccessLevel,
			final Double minimumSamplingInterval, final boolean historizing) {
		super(graph, key, value, context, nodeId, description, writeMask,
				userWriteMask, dataType, valueRank, arrayDimensions,
				accessLevel, userAccessLevel, minimumSamplingInterval,
				historizing);
		this.referenceVertex = vertex;
		this.graph = graph;
		this.allowNullPropertyValues = graph.features().vertex()
				.supportsNullPropertyValues();
		try {
			final Variant variant = new Variant(value);
			final DataValue dataValue = DataValue.newValue()
					.setStatus(StatusCode.GOOD).setSourceTime(DateTime.now())
					.setValue(variant).build();
			setValue(dataValue);
			vertex.addComponent(this);
		} catch (final Exception a) {
			final DataValue errorDataValue = DataValue.newValue()
					.setStatus(StatusCode.BAD).build();
			setValue(errorDataValue);
			vertex.addComponent(this);
			logger.error(LogHelper.stackTraceToString(a));
		}
		addAttributeObserver(this);
	}

	@Override
	public void attributeChanged(UaNode node, AttributeId attributeId,
			Object value) {
		if (attributeId.equals(AttributeId.Value)
				&& value instanceof DataValue) {
			referenceVertex.notifyPropertyValueChanging(key(),
					((DataValue) value));
		} else {
			logger.debug("Attribute changed: " + attributeId + " with value: "
					+ value + " is not handled");
		}
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

	/**
	 * Meta-property attached to a vertex property (in-memory).
	 */
	private static final class MetaProperty<V> implements Property<V> {
		private final String key;
		private final V value;
		private final AbstractOpcVertexProperty<?> owner;

		private MetaProperty(final AbstractOpcVertexProperty<?> owner,
				final String key, final V value) {
			this.owner = owner;
			this.key = key;
			this.value = value;
		}

		@Override
		public org.apache.tinkerpop.gremlin.structure.Element element() {
			return owner;
		}

		@Override
		public boolean equals(final Object object) {
			return org.apache.tinkerpop.gremlin.structure.util.ElementHelper
					.areEqual(this, object);
		}

		@Override
		public int hashCode() {
			return org.apache.tinkerpop.gremlin.structure.util.ElementHelper
					.hashCode(this);
		}

		@Override
		public boolean isPresent() {
			return true;
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public void remove() {
			owner.metaProperties.remove(key, this);
		}

		@Override
		public String toString() {
			return org.apache.tinkerpop.gremlin.structure.util.StringFactory
					.propertyString(this);
		}

		@Override
		public V value() {
			return value;
		}
	}

	private final java.util.Map<String, Property<?>> metaProperties = new java.util.concurrent.ConcurrentHashMap<>();

	@Override
	public Set<String> keys() {
		return Collections.unmodifiableSet(new HashSet<>(metaProperties.keySet()));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public <U> Iterator<Property<U>> properties(final String... propertyKeys) {
		final List<Property<U>> result = new ArrayList<>();
		for (final Property<?> p : metaProperties.values()) {
			if (propertyKeys == null || propertyKeys.length == 0
					|| java.util.Arrays.asList(propertyKeys)
							.contains(p.key())) {
				result.add((Property) p);
			}
		}
		return result.iterator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> Property<V> property(final String key) {
		final Property<?> p = metaProperties.get(key);
		return p == null ? Property.<V>empty() : (Property<V>) p;
	}

	@Override
	public <V> Property<V> property(final String key, final V value) {
		org.apache.tinkerpop.gremlin.structure.util.ElementHelper
				.validateProperty(key, value);
		if (value == null) {
			metaProperties.remove(key);
			return Property.empty();
		}
		final MetaProperty<V> p = new MetaProperty<>(this, key, value);
		metaProperties.put(key, p);
		return p;
	}

	@Override
	public void remove() {
		metaProperties.clear();
		referenceVertex.removeRelatedProperty(this);
		getNamespace().removeVertexProperty(getNodeId());
	}

	@Override
	public void setIcon(final ByteString icon) {
		this.icon = icon;

	}

	@Override
	public String toString() {
		if (!isPresent()) {
			return WaldotGraph.EMPTY_VERTEX_PROPERTY;
		}
		final String valueString = String.valueOf(value());
		return WaldotGraph.VP + WaldotGraph.L_BRACKET
				+ getBrowseName().getName() + WaldotGraph.ARROW
				+ StringUtils.abbreviate(valueString, 20)
				+ WaldotGraph.R_BRACKET;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DATA_TYPE value() {
		return (DATA_TYPE) getValue().getValue().getValue();
	}

}
