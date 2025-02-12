package net.rossonet.waldot.opc;

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
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.PropertyObserver;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotElement;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotGraphComputerView;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;
import net.rossonet.waldot.api.models.base.GremlinCommandVertex;

public abstract class AbstractOpcCommand extends GremlinCommandVertex implements WaldotCommand {

	public enum VariableNodeTypes {
		Boolean(Identifiers.Boolean), Byte(Identifiers.Byte), ByteString(Identifiers.ByteString),
		DateTime(Identifiers.DateTime), Double(Identifiers.Double), Duration(Identifiers.Duration),
		Float(Identifiers.Float), Guid(Identifiers.Guid), Int16(Identifiers.Int16), Int32(Identifiers.Int32),
		Int64(Identifiers.Int64), Integer(Identifiers.Integer), LocalizedText(Identifiers.LocalizedText),
		NodeId(Identifiers.NodeId), QualifiedName(Identifiers.QualifiedName), SByte(Identifiers.SByte),
		String(Identifiers.String), UInt16(Identifiers.UInt16), UInt32(Identifiers.UInt32), UInt64(Identifiers.UInt64),
		UInteger(Identifiers.UInteger), UtcTime(Identifiers.UtcTime), Variant(Identifiers.BaseDataType),
		XmlElement(Identifiers.XmlElement);

		private final NodeId nodeId;

		VariableNodeTypes(NodeId nodeId) {
			this.nodeId = nodeId;
		}

		public NodeId getNodeId() {
			return nodeId;
		}
	}

	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractOpcCommand.class);

	protected boolean allowNullPropertyValues = false;

	protected final List<EventObserver> eventObservers = new ArrayList<>();

	protected final WaldotGraph graph;

	protected ByteString icon;

	protected final List<Argument> inputArguments = new ArrayList<>();

	protected final List<Argument> outputArguments = new ArrayList<>();

	protected final List<PropertyObserver> propertyObservers = new ArrayList<>();

	protected final WaldotNamespace waldotNamespace;

	public AbstractOpcCommand(WaldotGraph graph, WaldotNamespace waldotNamespace, String command, String description,
			UInteger writeMask, UInteger userWriteMask, Boolean executable, Boolean userExecutable) {
		super(waldotNamespace.getOpcUaNodeContext(), waldotNamespace.generateNodeId(command),
				waldotNamespace.generateQualifiedName(command), LocalizedText.english(command),
				LocalizedText.english(description), writeMask, userWriteMask, executable, userExecutable);
		this.waldotNamespace = waldotNamespace;
		this.graph = graph;
	}

	@Override
	public void addComponent(WaldotElement waldotElement) {
		LOGGER.warn("Adding component to command is not supported");

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
	public void addEventObserver(EventObserver eventObserver) {
		eventObservers.add(eventObserver);
	}

	@Override
	public void addInputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description) {
		final Argument arg = new Argument(name, dataType, valueRank, arrayDimensions, description);
		inputArguments.add(arg);
		waldotNamespace.registerMethodInputArgument(this, inputArguments);
	}

	@Override
	public void addOutputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description) {
		final Argument arg = new Argument(name, dataType, valueRank, arrayDimensions, description);
		outputArguments.add(arg);
		waldotNamespace.registerMethodOutputArguments(this, outputArguments);
	}

	@Override
	public void addPropertyObserver(PropertyObserver propertyObserver) {
		propertyObservers.add(propertyObserver);

	}

	@Override
	public void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
		propertyObservers.forEach(observer -> observer.propertyChanged(node, attributeId, value));
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
	public UaMethodNode findMethodNode(NodeId methodId) {
		return null;
	}

	@Override
	public UByte getEventNotifier() {
		return UByte.valueOf(0);
	}

	@Override
	public List<EventObserver> getEventObservers() {
		return Collections.emptyList();
	}

	public WaldotGraph getGraph() {
		return graph;
	}

	@Override
	public WaldotGraphComputerView getGraphComputerView() {
		return null;
	}

	@Override
	public ByteString getIcon() {
		return icon;
	}

	@Override
	public List<UaMethodNode> getMethodNodes() {
		return Collections.emptyList();
	}

	@Override
	public WaldotNamespace getNamespace() {
		return graph.getWaldotNamespace();
	}

	@Override
	public List<PropertyObserver> getPropertyObservers() {
		return Collections.emptyList();
	}

	@Override
	public ImmutableMap<String, WaldotVertexProperty<Object>> getVertexProperties() {
		return ImmutableMap.of();
	}

	@Override
	public Graph graph() {
		return this.graph;
	}

	@Override
	public boolean inComputerMode() {
		return false;
	}

	@Override
	public void postEvent(BaseEventTypeNode event) {
		LOGGER.warn("postEvent not implemented for Command");

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

	@SuppressWarnings({ "rawtypes", "unchecked" })
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
			return getNamespace().createOrUpdateWaldotVertexProperty((WaldotVertex) this, key, value);
		}
	}

	@Override
	public void propertyChanged(UaNode sourceNode, AttributeId attributeId, Object value) {
		LOGGER.warn("propertyChanged not implemented for Command");

	}

	@Override
	public void removeComponent(WaldotElement waldotElement) {
		LOGGER.warn("removeComponent not implemented for Command");

	}

	@Override
	public void removeEventObserver(EventObserver observer) {
		LOGGER.warn("removeEventObserver not implemented for Command");

	}

	@Override
	public void removePropertyObserver(PropertyObserver observer) {
		LOGGER.warn("removePropertyObserver not implemented for Command");

	}

	@Override
	public Object[] runCommand(String[] methodInputs) {
		return runCommand(null, methodInputs);
	}

	@Override
	public void setEventNotifier(UByte eventNotifier) {
		LOGGER.warn("setEventNotifier not implemented for Command");

	}

	@Override
	public void setIcon(ByteString icon) {
		this.icon = icon;

	}

	@Override
	public String toString() {
		return WaldotGraph.V + WaldotGraph.L_BRACKET + getNodeId().toParseableString();
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
