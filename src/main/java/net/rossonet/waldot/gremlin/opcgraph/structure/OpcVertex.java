
package net.rossonet.waldot.gremlin.opcgraph.structure;

import java.util.ArrayList;
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
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.rossonet.waldot.api.Rule;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputerView;
import net.rossonet.waldot.namespaces.HomunculusNamespace;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class OpcVertex extends OpcElement implements Vertex, AttributeObserver {
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(getClass());
	protected final AbstractOpcGraph graph;
	private final boolean allowNullPropertyValues;
	private final List<Rule> propertyObservers = new ArrayList<>();

	public OpcVertex(final AbstractOpcGraph graph, UaNodeContext context, final NodeId nodeId,
			final QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version) {
		super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier, version);
		this.graph = graph;
		this.allowNullPropertyValues = graph.features().vertex().supportsNullPropertyValues();
	}

	@Override
	public Edge addEdge(final String label, final Vertex vertex, final Object... keyValues) {
		if (null == vertex) {
			throw Graph.Exceptions.argumentCanNotBeNull("vertex");
		}
		if (this.removed || ((OpcVertex) vertex).removed) {
			throw elementAlreadyRemoved(Vertex.class, getNodeId());
		}
		return getNamespace().addEdge(this, (OpcVertex) vertex, label, keyValues);
	}

	public void addPropertyObserver(Rule rule) {
		propertyObservers.add(rule);

	}

	@Override
	public void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
		propertyObservers.forEach(observer -> observer.propertyChanged(node, attributeId, value));
	}

	@Override
	public Object clone() {
		final OpcVertex vertex = new OpcVertex(graph, getNodeContext(), getNodeId(), getBrowseName(), getDisplayName(),
				getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version());
		return vertex;
	}

	@Override
	public Iterator<Edge> edges(final Direction direction, final String... edgeLabels) {
		final Iterator<Edge> edgeIterator = getNamespace().getEdges(this, direction, edgeLabels).values().iterator();
		return inComputerMode()
				? IteratorUtils.filter(edgeIterator, edge -> getGraphComputerView().legalEdge(this, edge))
				: edgeIterator;
	}

	private OpcGraphComputerView getGraphComputerView() {
		return getNamespace().getGraphComputerView();
	}

	private HomunculusNamespace getNamespace() {
		return graph.getOpcNamespace();
	}

	public List<Rule> getPropertyObservers() {
		return propertyObservers;
	}

	@SuppressWarnings("rawtypes")
	public ImmutableMap<String, OpcVertexProperty> getVertexProperties() {
		return ImmutableMap.copyOf(getNamespace().getVertexProperties(this));
	}

	@Override
	public Graph graph() {
		return this.graph;
	}

	private boolean inComputerMode() {
		return getNamespace().inComputerMode();
	}

	@Override
	public Set<String> keys() {
		if (null == getVertexProperties()) {
			return Collections.emptySet();
		}
		return inComputerMode() ? Vertex.super.keys() : getVertexProperties().keySet();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <V> Iterator<VertexProperty<V>> properties(final String... propertyKeys) {
		if (this.removed) {
			return Collections.emptyIterator();
		}
		if (inComputerMode()) {
			return (Iterator) getGraphComputerView().getProperties(OpcVertex.this).stream()
					.filter(p -> ElementHelper.keyExists(p.key(), propertyKeys)).iterator();
		} else {
			final ImmutableMap<String, OpcVertexProperty> vertexProperties = getVertexProperties();
			if (null == vertexProperties) {
				return Collections.emptyIterator();
			}
			if (propertyKeys.length == 0) {
				return (Iterator) vertexProperties.values().iterator();
			}
			final Set<VertexProperty<V>> result = new HashSet<>();
			for (final Entry<String, OpcVertexProperty> p : vertexProperties.entrySet()) {
				for (final String key : propertyKeys) {
					if (p.getKey().equals(key)) {
						result.add(p.getValue());
						break;
					}
				}
			}
			return result.iterator();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <V> VertexProperty<V> property(final String key) {
		if (this.removed) {
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
			for (final Entry<String, OpcVertexProperty> p : getVertexProperties().entrySet()) {
				if (p.getKey().equals(key)) {
					return p.getValue();
				}
			}
			return VertexProperty.<V>empty();
		}
	}

	@Override
	public <V> VertexProperty<V> property(final VertexProperty.Cardinality cardinality, final String key, final V value,
			final Object... keyValues) {

		if (this.removed) {
			throw elementAlreadyRemoved(Vertex.class, this.getNodeId());
		}
		ElementHelper.legalPropertyKeyValueArray(keyValues);
		ElementHelper.validateProperty(key, value);

		// if we don't allow null property values and the value is null then the key can
		// be removed but only if the
		// cardinality is single. if it is list/set then we can just ignore the null.
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
			return getNamespace().createOrUpdateOpcVertexProperty(this, key, value);
		}
	}

	@Override
	public void remove() {
		this.graph.removeVertex(this.getNodeId());
		this.removed = true;
	}

	public void removePropertyObserver(Rule rule) {
		propertyObservers.add(rule);

	}

	@Override
	public String toString() {
		return AbstractOpcGraph.V + AbstractOpcGraph.L_BRACKET + getNodeId().toParseableString()
				+ AbstractOpcGraph.R_BRACKET;
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

		return (Iterator) getNamespace().getVertices(this, direction, edgeLabels);
	}
}
