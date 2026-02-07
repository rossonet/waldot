package net.rossonet.waldot.opc;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.shaded.com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.PropertyObserver;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotGraphComputerView;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotProperty;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;
import net.rossonet.waldot.api.models.base.GremlinElement;
import net.rossonet.waldot.api.strategies.MiloStrategy;

/**
 * AbstractOpcVertex is an abstract class that implements the WaldotVertex
 * interface. It provides common functionality for OPC UA vertices in the Waldot
 * graph model, including property management, event handling, and edge
 * management.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public abstract class AbstractOpcVertex extends GremlinElement implements WaldotVertex, AttributeObserver {

	protected boolean allowNullPropertyValues = false;
	private transient boolean ensurePostActive = false;

	protected transient final List<EventObserver> eventObservers = new ArrayList<>();
	protected transient final WaldotGraph graph;

	private transient final Logger logger = LoggerFactory.getLogger(getClass());
	protected transient final List<PropertyObserver> propertyObservers = new ArrayList<>();

	public AbstractOpcVertex(final WaldotGraph graph, final UaNodeContext context, final NodeId nodeId,
			final QualifiedName browseName, final LocalizedText displayName, final LocalizedText description,
			final UInteger writeMask, final UInteger userWriteMask, final UByte eventNotifier, final long version) {
		super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier, version);
		this.graph = graph;
		this.allowNullPropertyValues = graph.features().vertex().supportsNullPropertyValues();
		addAttributeObserver(this);
	}

	@Override
	public Edge addEdge(final String label, final Vertex vertex, final Object... keyValues) {
		if (null == vertex) {
			throw Graph.Exceptions.argumentCanNotBeNull("vertex");
		}
		if (this.isRemoved() || ((WaldotVertex) vertex).isRemoved()) {
			throw elementAlreadyRemoved(Vertex.class, getNodeId());
		}
		return getNamespace().addEdge(this, (WaldotVertex) vertex, label, keyValues);
	}

	@Override
	public void addEventObserver(final EventObserver eventObserver) {
		eventObservers.add(eventObserver);
	}

	@Override
	public void addPropertyObserver(final PropertyObserver propertyObserver) {
		propertyObservers.add(propertyObserver);
	}

	@Override
	public void addRelatedProperty(WaldotProperty<?> property) {
		// TODO Auto-generated method stub

	}

	@Override
	public void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
		logger.debug("Attribute changed: node {}, attribute {}, new value {}", node, attributeId, value);
	}

	@Override
	public Iterator<Edge> edges(final Direction direction, final String... edgeLabels) {
		final Collection<WaldotEdge> values = getNamespace().getEdges(this, direction, edgeLabels).values();
		final Collection<Edge> edges = new ArrayList<>();
		for (final WaldotEdge edge : values) {
			edges.add(edge);
		}
		final Iterator<Edge> edgeIterator = edges.iterator();
		return inComputerMode()
				? IteratorUtils.filter(edgeIterator, edge -> getGraphComputerView().legalEdge(this, edge))
				: edgeIterator;
	}

	@Override
	public List<EventObserver> getEventObservers() {
		return eventObservers;
	}

	@Override
	public WaldotGraphComputerView getGraphComputerView() {
		return getNamespace().getGraphComputerView();
	}

	@Override
	public WaldotNamespace getNamespace() {
		return graph.getWaldotNamespace();
	}

	@Override
	public String[] getPropertiesAsStringArray() {
		final List<String> props = new ArrayList<>();
		final ImmutableMap<String, WaldotVertexProperty<Object>> vertexProperties = getVertexProperties();
		for (final Entry<String, WaldotVertexProperty<Object>> entry : vertexProperties.entrySet()) {
			final String key = entry.getKey();
			props.add(key);
			final String value = entry.getValue().value().toString();
			props.add(value);
		}
		return props.toArray(new String[0]);
	}

	@Override
	public List<PropertyObserver> getPropertyObservers() {
		return propertyObservers;
	}

	@Override
	public ImmutableMap<String, WaldotVertexProperty<Object>> getVertexProperties() {
		return ImmutableMap.copyOf(getNamespace().getVertexProperties(this));
	}

	@Override
	public Graph graph() {
		return this.graph;
	}

	@Override
	public boolean inComputerMode() {
		return getNamespace().inComputerMode();
	}

	@Override
	public Set<String> keys() {
		if (null == getVertexProperties()) {
			return Collections.emptySet();
		}
		return inComputerMode() ? super.keys() : getVertexProperties().keySet();
	}

	@Override
	public void notifyPropertyValueChanging(String label, DataValue value) {
		if (label.equals(MiloStrategy.LABEL_FIELD.toLowerCase())) {
			final QualifiedProperty<String> newLabel = new QualifiedProperty<String>(
					graph.getWaldotNamespace().getNamespaceUri(), MiloStrategy.LABEL_FIELD,
					MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(),
					ValueRanks.Scalar, String.class);
			setProperty(newLabel, (String) value.getValue().getValue());
		}
		if (label.equals(MiloStrategy.NAME_FIELD.toLowerCase())) {
			final QualifiedName browseName = graph.getWaldotNamespace()
					.generateQualifiedName((String) value.getValue().getValue());
			final LocalizedText displayName = new LocalizedText((String) value.getValue().getValue());
			setBrowseName(browseName);
			setDisplayName(displayName);
		}
		if (label.equals(MiloStrategy.DESCRIPTION_PARAMETER.toLowerCase())) {
			final LocalizedText description = new LocalizedText((String) value.getValue().getValue());
			setDescription(description);
		}
		propertyObservers.forEach(observer -> observer.propertyChanged(this, label, value));
	}

	@Override
	public void postEvent(final BaseEventTypeNode event) {
		if (!ensurePostActive) {
			setEventNotifier(ubyte(1));
			ensurePostActive = true;
		}
		getNamespace().getEventBus().fire(event);
		eventObservers.forEach(observer -> observer.fireEvent(this, event));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <V> Iterator<VertexProperty<V>> properties(final String... propertyKeys) {
		if (this.isRemoved()) {
			return Collections.emptyIterator();
		}
		if (inComputerMode()) {
			return (Iterator) getGraphComputerView().getProperties(this).stream()
					.filter(p -> ElementHelper.keyExists(p.key(), propertyKeys)).iterator();
		} else {
			final ImmutableMap<String, WaldotVertexProperty<Object>> vertexProperties = getVertexProperties();
			if (null == vertexProperties) {
				return Collections.emptyIterator();
			}
			if (propertyKeys.length == 0) {
				return (Iterator) vertexProperties.values().iterator();
			}
			final Set<VertexProperty<V>> result = new HashSet<>();
			for (final Entry<String, WaldotVertexProperty<Object>> p : vertexProperties.entrySet()) {
				for (final String key : propertyKeys) {
					if (p.getKey().equals(key)) {
						result.add((VertexProperty<V>) p.getValue());
						break;
					}
				}
			}
			return result.iterator();
		}
	}

	@Override
	public <V> VertexProperty<V> property(final String key) {
		if (this.isRemoved()) {
			return VertexProperty.empty();
		}
		if (inComputerMode()) {
			final List<VertexProperty> list = (List) getGraphComputerView().getProperty(this, key);
			if (list.size() == 0) {
				return VertexProperty.<V>empty();
			} else if (list.size() == 1) {
				return list.get(0);
			} else {
				throw Vertex.Exceptions.multiplePropertiesExistForProvidedKey(key);
			}
		} else {
			for (final Entry<String, WaldotVertexProperty<Object>> p : getVertexProperties().entrySet()) {
				if (p.getKey().equals(key)) {
					return (VertexProperty<V>) p.getValue();
				}
			}
			return VertexProperty.<V>empty();
		}
	}

	@Override
	public <V> VertexProperty<V> property(final VertexProperty.Cardinality cardinality, final String key, final V value,
			final Object... keyValues) {
		if (this.isRemoved()) {
			throw elementAlreadyRemoved(Vertex.class, this.getNodeId());
		}
		ElementHelper.legalPropertyKeyValueArray(keyValues);
		ElementHelper.validateProperty(key, value);
		if (!allowNullPropertyValues && null == value) {
			final VertexProperty.Cardinality card = null == cardinality ? graph.features().vertex().getCardinality(key)
					: cardinality;
			if (VertexProperty.Cardinality.single == card) {
				properties(key).forEachRemaining(Property::remove);
			}
			return VertexProperty.empty();
		}
		if (inComputerMode()) {
			final VertexProperty<V> vertexProperty = getGraphComputerView().addProperty(this, key, value);
			ElementHelper.attachProperties(vertexProperty, keyValues);
			return vertexProperty;
		} else {
			return getNamespace().createOrUpdateWaldotVertexProperty(this, key, value);
		}
	}

	@Override
	public void remove() {
		this.graph.removeVertex(this.getNodeId());
		super.remove();
	}

	@Override
	public void removeEventObserver(final EventObserver observer) {
		eventObservers.remove(observer);
	}

	@Override
	public void removePropertyObserver(final PropertyObserver observer) {
		propertyObservers.remove(observer);
	}

	@Override
	public void removeRelatedOpcUaNodes() {
		// TODO Auto-generated method stub

	}

	@Override
	public String toString() {
		return WaldotGraph.V + WaldotGraph.L_BRACKET + getNodeId().toParseableString() + WaldotGraph.R_BRACKET;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Iterator<Vertex> vertices(final Direction direction, final String... edgeLabels) {
		if (inComputerMode()) {
			return direction.equals(Direction.BOTH)
					? IteratorUtils.concat(IteratorUtils.map(this.edges(Direction.OUT, edgeLabels), Edge::inVertex),
							IteratorUtils.map(this.edges(Direction.IN, edgeLabels), Edge::outVertex))
					: IteratorUtils.map(this.edges(direction, edgeLabels),
							edge -> edge.vertices(direction.opposite()).next());
		}
		return (Iterator) getNamespace().getVertices(this, direction, edgeLabels).values().iterator();
	}

}
