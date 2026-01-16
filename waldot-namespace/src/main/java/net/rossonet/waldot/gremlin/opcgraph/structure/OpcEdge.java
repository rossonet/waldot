
package net.rossonet.waldot.gremlin.opcgraph.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.shaded.com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PropertyObserver;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotProperty;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.base.GremlinElement;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;

public class OpcEdge extends GremlinElement implements WaldotEdge {

	private final boolean allowNullPropertyValues;

	private final WaldotGraph graph;
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final List<PropertyObserver> propertyObservers = new ArrayList<>();

	public OpcEdge(WaldotGraph graph, final NodeId nodeId, final WaldotVertex outVertex, final WaldotVertex inVertex,
			final String label, final String name, String description, UInteger writeMask, UInteger userWriteMask,
			UByte eventNotifier, long currentVersion) {
		this(graph, outVertex.getNodeContext(), nodeId, inVertex.getNodeId(), outVertex.getNodeId(), label, name,
				description, writeMask, userWriteMask, eventNotifier, currentVersion);
	}

	private OpcEdge(WaldotGraph graph, UaNodeContext context, final NodeId nodeId, final NodeId inVertexId,
			final NodeId outVertexId, final String label, final String name, String description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, final long currentVersion) {
		super(context, nodeId, graph.getWaldotNamespace().generateQualifiedName(name), LocalizedText.english(name),
				LocalizedText.english(description), userWriteMask, userWriteMask, eventNotifier, currentVersion);
		this.graph = graph;
		this.allowNullPropertyValues = graph.features().edge().supportsNullPropertyValues();
	}

	@Override
	public void addPropertyObserver(final PropertyObserver propertyObserver) {
		propertyObservers.add(propertyObserver);
	}

	@Override
	public Object clone() {
		final OpcEdge edge = new OpcEdge(graph(), getNodeId(), outVertex(), inVertex(), label(),
				getBrowseName().getName(), getDescription().getText(), getWriteMask(), getUserWriteMask(),
				getEventNotifier(), version());
		return edge;
	}

	@Override
	public WaldotNamespace getNamespace() {
		return graph.getWaldotNamespace();
	}

	@Override
	public ImmutableList<WaldotProperty<Object>> getProperties() {
		return ImmutableList.copyOf(getNamespace().getEdgeProperties(this));
	}

	@Override
	public List<PropertyObserver> getPropertyObservers() {
		return propertyObservers;
	}

	@Override
	public WaldotGraph graph() {
		return this.graph;
	}

	@Override
	public WaldotVertex inVertex() {
		return getNamespace().getEdgeInVertex(this);
	}

	@Override
	public Set<String> keys() {
		if (null == getProperties()) {
			return Collections.emptySet();
		}
		if (null == getProperties()) {
			return Collections.emptySet();
		}
		final Set<String> result = new HashSet<>();
		for (final WaldotProperty<Object> p : getProperties()) {
			result.add(p.getBrowseName().getName());
		}
		return result;
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
		// TODO: gestire il cambio di directory
		// TODO: gestire i cambi di source/target e type
		propertyObservers.forEach(observer -> observer.propertyChanged(this, label, value));

	}

	@Override
	public WaldotVertex outVertex() {
		return getNamespace().getEdgeOutVertex(this);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <V> Iterator<Property<V>> properties(final String... propertyKeys) {
		final ImmutableList<WaldotProperty<Object>> properties = getProperties();
		if (null == properties) {
			return Collections.emptyIterator();
		}
		if (propertyKeys.length == 0) {
			return (Iterator) properties.iterator();
		}
		final Set<Property<V>> result = new HashSet<>();
		for (final WaldotProperty<Object> p : properties) {
			for (final String key : propertyKeys) {
				if (p.getBrowseName().getName().equals(key)) {
					result.add((Property<V>) p);
					break;
				}
			}
		}
		return result.iterator();
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <V> Property<V> property(final String key) {
		if (this.isRemoved()) {
			return Property.empty();
		}
		for (final WaldotProperty<Object> p : getProperties()) {
			if (p.getBrowseName().getName().equals(key)) {
				return (Property<V>) p;
			}
		}
		return VertexProperty.<V>empty();
	}

	@Override
	public <V> Property<V> property(final String key, final V value) {
		if (this.isRemoved()) {
			throw elementAlreadyRemoved(Edge.class, getNodeId());
		}
		ElementHelper.validateProperty(key, value);
		if (!allowNullPropertyValues && null == value) {
			properties(key).forEachRemaining(Property::remove);
			return Property.empty();
		}
		return getNamespace().createOrUpdateWaldotEdgeProperty(this, key, value);
	}

	@Override
	public void remove() {
		getNamespace().removeEdge(this.id());
		super.remove();
	}

	@Override
	public void removePropertyObserver(final PropertyObserver observer) {
		propertyObservers.remove(observer);
	}

	@Override
	public String toString() {
		return AbstractOpcGraph.E + AbstractOpcGraph.L_BRACKET + getNodeId().toParseableString()
				+ AbstractOpcGraph.R_BRACKET + AbstractOpcGraph.L_BRACKET + outVertex().getNodeId().toParseableString()
				+ AbstractOpcGraph.DASH + label() + AbstractOpcGraph.ARROW + inVertex().getNodeId().toParseableString()
				+ AbstractOpcGraph.R_BRACKET;

	}

	@Override
	public Iterator<Vertex> vertices(final Direction direction) {
		if (isRemoved()) {
			return Collections.emptyIterator();
		}
		switch (direction) {
		case OUT:
			return IteratorUtils.of(this.outVertex());
		case IN:
			return IteratorUtils.of(this.inVertex());
		default:
			return IteratorUtils.of(this.outVertex(), this.inVertex());
		}
	}

}
