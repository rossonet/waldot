package net.rossonet.waldot.opc;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

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
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.shaded.com.google.common.collect.ImmutableMap;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.PropertyObserver;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotGraphComputerView;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;
import net.rossonet.waldot.api.models.base.GremlinElement;
import net.rossonet.waldot.api.strategies.HistoryStrategy;
import net.rossonet.waldot.api.strategies.MiloStrategy;

/**
 * AbstractOpcVertex is an abstract class that implements the WaldotVertex
 * interface. It provides common functionality for OPC UA vertices in the Waldot
 * graph model, including property management, event handling, and edge
 * management.
 * 
 * @author Andrea Ambrosini - Zona Autonoma srl
 */
public abstract class AbstractOpcVertex extends GremlinElement
		implements
			WaldotVertex,
			AttributeObserver {

	public interface ChangedPropertyCallback<DATA_TYPE> {
		public void onChange(String label, DATA_TYPE oldValue,
				DATA_TYPE newValue) throws PropertyChangeException;
	}

	public interface CommandHandler {
		public String[] handleCommand(InvocationContext invocationContext,
				String[] inputValues);
	}

	public final class MetaDataProperty<DATA_TYPE> {

		private final ChangedPropertyCallback<DATA_TYPE> callbackOnChange;
		private final DATA_TYPE defaultValue;
		private final boolean isNecessaryRestartVertex;
		private final boolean isRequired;
		private final Class<DATA_TYPE> javaType;
		private final String label;
		private final QualifiedProperty<DATA_TYPE> qualifiedProperty;
		private final NodeId uaDataType;

		public MetaDataProperty(String label, NodeId dataType,
				Class<DATA_TYPE> javaType, boolean isRequired,
				DATA_TYPE defaultValue,
				ChangedPropertyCallback<DATA_TYPE> callbackOnChange,
				boolean isNecessaryRestartVertex,
				QualifiedProperty<DATA_TYPE> qualifiedProperty) {
			this.javaType = javaType;
			this.isRequired = isRequired;
			this.callbackOnChange = callbackOnChange;
			this.isNecessaryRestartVertex = isNecessaryRestartVertex;
			this.label = label;
			this.uaDataType = dataType;
			this.defaultValue = defaultValue;
			this.qualifiedProperty = qualifiedProperty;
		}

		public ChangedPropertyCallback<DATA_TYPE> getCallbackOnChange() {
			return callbackOnChange;
		}

		public DATA_TYPE getDefaultValue() {
			return defaultValue;
		}

		public Class<DATA_TYPE> getJavaType() {
			return javaType;
		}

		public String getLabel() {
			return label;
		}

		public QualifiedProperty<DATA_TYPE> getQualifiedProperty() {
			return qualifiedProperty;
		}

		public NodeId getUaDataType() {
			return uaDataType;
		}

		public boolean isNecessaryRestartVertex() {
			return isNecessaryRestartVertex;
		}

		public boolean isRequired() {
			return isRequired;
		}

	}

	public static class PropertyChangeException extends Exception {

		private static final long serialVersionUID = 6543733607549354598L;

		public PropertyChangeException(String message) {
			super(message);
		}

		public PropertyChangeException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	// chiavi delle proprieta' gestite, in minuscolo: calcolate una volta sola
	// perche' notifyPropertyValueChanging gira a ogni valore ricevuto
	private static final String LABEL_KEY = MiloStrategy.LABEL_FIELD.toLowerCase();
	private static final String TYPE_KEY = MiloStrategy.TYPE_FIELD.toLowerCase();
	private static final String HISTORY_KEY = MiloStrategy.HISTORY_FIELD.toLowerCase();
	private static final String HISTORY_CONTEXT_KEY = MiloStrategy.HISTORY_CONTEXT_FIELD.toLowerCase();
	private static final String NAME_KEY = MiloStrategy.NAME_FIELD.toLowerCase();
	private static final String DESCRIPTION_KEY = MiloStrategy.DESCRIPTION_PARAMETER.toLowerCase();

	private boolean activeHistory = false;

	protected boolean allowNullPropertyValues = false;

	private boolean dirtyStateToUpdate = false;

	private boolean ensurePostActive = false;

	protected final List<EventObserver> eventObservers = new java.util.concurrent.CopyOnWriteArrayList<>();

	protected final WaldotGraph graph;

	private String historyContext = null;
	private final QualifiedProperty<String> historyContextProperty;
	private final QualifiedProperty<Boolean> historyProperty;

	private final QualifiedProperty<String> labelProperty;

	private final Set<WaldotVertexProperty<?>> linkedProperties = java.util.concurrent.ConcurrentHashMap
			.newKeySet();

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@SuppressWarnings("rawtypes")
	private final Map<String, MetaDataProperty> managedProperties = new java.util.concurrent.ConcurrentHashMap<>();

	protected final List<PropertyObserver> propertyObservers = new java.util.concurrent.CopyOnWriteArrayList<>();

	private final QualifiedProperty<String> typeProperty;

	public AbstractOpcVertex(final WaldotGraph graph,
			final UaNodeContext context, final NodeId nodeId,
			final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask, final UByte eventNotifier,
			final long version) {
		super(context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version);
		this.graph = graph;
		this.allowNullPropertyValues = graph.features().vertex()
				.supportsNullPropertyValues();
		addAttributeObserver(this);
		labelProperty = new QualifiedProperty<String>(
				graph.getWaldotNamespace().getNamespaceUri(),
				MiloStrategy.LABEL_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.forNamespace(
						graph.getWaldotNamespace()).labelVertexTypeNode
						.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		typeProperty = new QualifiedProperty<String>(
				graph.getWaldotNamespace().getNamespaceUri(),
				MiloStrategy.TYPE_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder
						.forNamespace(graph.getWaldotNamespace()).vertexTypeNode
						.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		historyProperty = new QualifiedProperty<Boolean>(
				graph.getWaldotNamespace().getNamespaceUri(),
				MiloStrategy.HISTORY_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder
						.forNamespace(graph.getWaldotNamespace()).vertexTypeNode
						.getNodeId().expanded(),
				ValueRanks.Scalar, Boolean.class);
		historyContextProperty = new QualifiedProperty<String>(
				graph.getWaldotNamespace().getNamespaceUri(),
				MiloStrategy.HISTORY_CONTEXT_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder
						.forNamespace(graph.getWaldotNamespace()).vertexTypeNode
						.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
	}
	@Override
	public Edge addEdge(final String label, final Vertex vertex,
			final Object... keyValues) {
		if (null == vertex) {
			throw Graph.Exceptions.argumentCanNotBeNull("vertex");
		}
		if (this.isRemoved() || ((WaldotVertex) vertex).isRemoved()) {
			throw elementAlreadyRemoved(Vertex.class, getNodeId());
		}
		return getNamespace().addEdge(this, (WaldotVertex) vertex, label,
				keyValues);
	}

	@Override
	public void addEventObserver(final EventObserver eventObserver) {
		eventObservers.add(eventObserver);
	}

	protected <DATA_TYPE> void addManagedProperty(String label, NodeId dataType,
			Class<DATA_TYPE> javaType, boolean isRequired,
			DATA_TYPE defaultValue,
			ChangedPropertyCallback<DATA_TYPE> callbackOnChange,
			boolean isNecessaryRestartVertex, Object[] propertyKeyValues) {
		final MetaDataProperty<DATA_TYPE> metadataProperty = new MetaDataProperty<DATA_TYPE>(
				label, dataType, javaType, isRequired, defaultValue,
				callbackOnChange, isNecessaryRestartVertex,
				new QualifiedProperty<DATA_TYPE>(
						getNamespace().getNamespaceUri(), label,
						MiloSingleServerBaseReferenceNodeBuilder.forNamespace(
								graph.getWaldotNamespace()).labelVertexTypeNode
								.getNodeId().expanded(),
						ValueRanks.Scalar, javaType));
		managedProperties.put(label, metadataProperty);
		final String value = MiloStrategy
				.getKeyValuesProperty(propertyKeyValues, label);
		if (value != null && !value.isEmpty()) {
			final DATA_TYPE convertedValue = valueConversion(label, javaType,
					value);
			if (convertedValue != null) {
				property(label, convertedValue);
				setProperty(metadataProperty.getQualifiedProperty(),
						convertedValue);
				if (callbackOnChange != null) {
					try {
						callbackOnChange.onChange(label, null, convertedValue);
					} catch (final PropertyChangeException e) {
						logger.warn(
								"Error executing callback for property {} with value {}",
								label, convertedValue, e);
						if (defaultValue != null) {
							logger.info(
									"because of the error, using default value {} for property {} of type {}",
									label, value, javaType.getSimpleName(),
									defaultValue);
							property(label, defaultValue);
							setProperty(metadataProperty.getQualifiedProperty(),
									defaultValue);
						} else {
							logger.info(
									"because of the error, no default value is provided for property {} of type {}, setting property to null",
									label, value, javaType.getSimpleName());
							setProperty(metadataProperty.getQualifiedProperty(),
									null);
						}
					}
				}
			} else {
				if (defaultValue != null) {
					logger.warn(
							"Property {} value {} is not valid for type {}, using default value {}",
							label, value, javaType.getSimpleName(),
							defaultValue);
					property(label, defaultValue);
					setProperty(metadataProperty.getQualifiedProperty(),
							defaultValue);
				} else {
					logger.warn(
							"Property {} value {} is not valid for type {}, and no default value is provided, setting property to null",
							label, value, javaType.getSimpleName());
					setProperty(metadataProperty.getQualifiedProperty(), null);
				}
			}
		} else if (defaultValue != null) {
			logger.trace("Property {} not provided, using default value {}",
					label, defaultValue);
			property(label, defaultValue);
			setProperty(metadataProperty.getQualifiedProperty(), defaultValue);
		} else {
			logger.debug(
					"Property {} not provided and no default value is set, setting property to null",
					label);
			setProperty(metadataProperty.getQualifiedProperty(), null);
		}
	}

	@Override
	public void addPropertyObserver(final PropertyObserver propertyObserver) {
		propertyObservers.add(propertyObserver);
	}

	@Override
	public void addRelatedProperty(WaldotVertexProperty<?> property) {
		linkedProperties.add(property);
		property.setHistorizing(activeHistory);

	}

	protected AbstractOpcCommand addVertexCommand(String commandName,
			String commandDescription, CommandHandler commandHandler) {
		final AbstractOpcCommand command = new AbstractOpcCommand(
				graph.getWaldotNamespace().getGremlinGraph(),
				graph.getWaldotNamespace(),
				id().getIdentifier().toString() + "." + commandName,
				commandName, commandDescription, null,
				graph.getWaldotNamespace().getConfiguration()
						.getWaldotCommandWriteMask(),
				graph.getWaldotNamespace().getConfiguration()
						.getWaldotCommandUserWriteMask(),
				graph.getWaldotNamespace().getConfiguration()
						.getWaldotCommandExecutable(),
				graph.getWaldotNamespace().getConfiguration()
						.getWaldotCommandUserExecutable()) {

			@Override
			public Object[] runCommand(InvocationContext invocationContext,
					String[] inputValues) {
				return commandHandler.handleCommand(invocationContext,
						inputValues);
			}
		};
		graph.getWaldotNamespace().getStorageManager().addNode(command);
		addComponent((UaNode) command);
		return command;
	}

	@Override
	public void attributeChanged(UaNode node, AttributeId attributeId,
			Object value) {
		logger.debug("Attribute changed: node {}, attribute {}, new value {}",
				node, attributeId, value);
	}

	/**
	 * Checks if a property value has changed and updates it if necessary. If the
	 * changed property label matches the key in the property key values, it retrieves
	 * the original value of the property and compares it with the new value. If they
	 * are different, it updates the property with the new value and invokes the
	 * callback if provided.
	 * @param <DATA_TYPE>
	 * @param changedPropertyLabel
	 * @param changedPropertyValue
	 * @param keyInPropertyKeyValues
	 * @param qualifiedPropertySlot
	 * @param callback
	 * @return true if the property value was changed, false otherwise
	 */
	private <DATA_TYPE> boolean checkPropertyValueChange(
			String changedPropertyLabel, DATA_TYPE changedPropertyValue,
			QualifiedProperty<DATA_TYPE> qualifiedPropertySlot,
			ChangedPropertyCallback<DATA_TYPE> callback) {
		if (changedPropertyLabel == null || changedPropertyLabel.isEmpty()) {
			logger.warn("Changed property label is null or empty");
			return false;
		}
		// la notifica arriva dopo la scrittura della proprietà gremlin: il
		// valore precedente è quello ancora presente nello slot gestito
		DATA_TYPE originalValue = null;
		try {
			originalValue = getProperty(qualifiedPropertySlot).orElse(null);
		} catch (final Throwable e) {
			logger.debug("Previous value of property {} not available",
					changedPropertyLabel, e);
		}
		if (changedPropertyValue != null) {
			try {
				final DATA_TYPE value = changedPropertyValue;
				if (originalValue == null || !originalValue.equals(value)) {
					if (callback != null) {
						try {
							callback.onChange(changedPropertyLabel,
									originalValue, value);
							setProperty(qualifiedPropertySlot, value);
							return true;
						} catch (final PropertyChangeException e) {
							logger.warn(
									"Error executing callback for property {} with value {}",
									changedPropertyLabel, value, e);
							return false;
						}
					} else {
						setProperty(qualifiedPropertySlot, value);
						return true;
					}
				} else {
					logger.debug("Property {} value unchanged, remains {}",
							changedPropertyLabel, originalValue);
					return false;
				}
			} catch (final Throwable e) {
				logger.error("Error while setting property {} with value {}",
						changedPropertyLabel, changedPropertyValue, e);
				return false;
			}
		} else {
			logger.debug("Changed property value is null for property {}",
					changedPropertyLabel);
			return false;
		}
	}

	@Override
	public Iterator<Edge> edges(final Direction direction,
			final String... edgeLabels) {
		final Collection<WaldotEdge> values = getNamespace().getEdges(this,
				direction, edgeLabels);
		final Collection<Edge> edges = new ArrayList<>();
		for (final WaldotEdge edge : values) {
			edges.add(edge);
		}
		final Iterator<Edge> edgeIterator = edges.iterator();
		return inComputerMode()
				? IteratorUtils.filter(edgeIterator,
						edge -> getGraphComputerView().legalEdge(this, edge))
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

	public String getHistoryContext() {
		return historyContext != null
				? historyContext
				: HistoryStrategy.DEFAULT_DATA_CONTEXT;
	}

	@Override
	public WaldotNamespace getNamespace() {
		return graph.getWaldotNamespace();
	}

	@Override
	public String[] getPropertiesAsStringArray() {
		final List<String> props = new ArrayList<>();
		final ImmutableMap<String, WaldotVertexProperty<Object>> vertexProperties = getVertexProperties();
		for (final Entry<String, WaldotVertexProperty<Object>> entry : vertexProperties
				.entrySet()) {
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

	protected <DATA_TYPE> DATA_TYPE getRuntimeParameter(String label) {
		@SuppressWarnings("unchecked")
		final MetaDataProperty<DATA_TYPE> entry = managedProperties.get(label);
		if (entry != null) {
			return getProperty(entry.getQualifiedProperty()).orElse(null);
		} else {
			return null;
		}
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

	public boolean isDirtyStateToUpdate() {
		return dirtyStateToUpdate;
	}

	@Override
	public Set<String> keys() {
		if (inComputerMode()) {
			return super.keys();
		}
		final ImmutableMap<String, WaldotVertexProperty<Object>> vertexProperties = getVertexProperties();
		return vertexProperties == null
				? Collections.emptySet()
				: vertexProperties.keySet();
	}

	@Override
	public String label() {
		return getProperty(labelProperty).orElseGet(() -> null);
	}

	protected void markDirtyStateToUpdate() {
		dirtyStateToUpdate = false;
	}

	@Override
	public void notifyPropertyValueChanging(String label, DataValue value) {
		if (label.equals(LABEL_KEY)) {
			final String originalLabel;
			try {
				originalLabel = getProperty(labelProperty)
						.orElseGet(() -> null);
				if (originalLabel != null && !originalLabel.equals(String
						.valueOf(value.getValue().getValue().toString()))) {
					logger.warn(
							"Changing label from {} to {} is not allowed, reverting to original value",
							originalLabel,
							value.getValue().getValue().toString());
					property(LABEL_KEY,
							originalLabel);
				}
			} catch (final Throwable e) {
				logger.warn("Error retrieving original label property", e);
			}
		}
		if (label.equals(TYPE_KEY)) {
			final String originalType;
			try {
				originalType = getProperty(typeProperty).orElseGet(() -> null);
			} catch (final Throwable e) {
				logger.warn("Error retrieving original type property", e);
				return;
			}
			if (originalType != null && !originalType.equals(
					String.valueOf(value.getValue().getValue().toString()))) {
				logger.warn(
						"Changing type from {} to {} is not allowed, reverting to original value",
						originalType, value.getValue().getValue().toString());
				property(TYPE_KEY, originalType);
			}
		}
		if (label.equals(HISTORY_KEY)) {
			final boolean newHistory = Boolean
					.parseBoolean(String.valueOf(value.getValue().getValue()));
			if (newHistory) {
				activeHistory = true;
				logger.info("History activated for vertex {}", this);
			} else {
				activeHistory = false;
				logger.info("History deactivated for vertex {}", this);
			}
			setProperty(historyProperty, activeHistory);
			for (final WaldotVertexProperty<?> property : linkedProperties) {
				property.setHistorizing(activeHistory);
			}
		}
		if (label.equals(HISTORY_CONTEXT_KEY)) {
			final String newContext = String
					.valueOf(value.getValue().getValue());
			if (newContext != null && !newContext.isEmpty()) {
				historyContext = newContext;
			}
		}
		if (label.equals(NAME_KEY)) {
			final QualifiedName browseName = graph.getWaldotNamespace()
					.generateQualifiedName(
							(String) value.getValue().getValue());
			final LocalizedText displayName = new LocalizedText(
					(String) value.getValue().getValue());
			setBrowseName(browseName);
			setDisplayName(displayName);
		}
		if (label.equals(DESCRIPTION_KEY)) {
			final LocalizedText description = new LocalizedText(
					(String) value.getValue().getValue());
			setDescription(description);
		}
		updateManagedProperties(label, value);
		propertyObservers.forEach(
				observer -> observer.propertyChanged(this, label, value));
		if (activeHistory) {
			getNamespace().getHistoryStrategy()
					.registerHistoryRecord(historyContext, this, label, value);
		}
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

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public <V> Iterator<VertexProperty<V>> properties(
			final String... propertyKeys) {
		if (this.isRemoved()) {
			return Collections.emptyIterator();
		}
		if (inComputerMode()) {
			return (Iterator) getGraphComputerView().getProperties(this)
					.stream()
					.filter(p -> ElementHelper.keyExists(p.key(), propertyKeys))
					.iterator();
		} else {
			if (propertyKeys.length == 0) {
				final ImmutableMap<String, WaldotVertexProperty<Object>> vertexProperties = getVertexProperties();
				return vertexProperties == null
						? Collections.emptyIterator()
						: (Iterator) vertexProperties.values().iterator();
			}
			final Set<VertexProperty<V>> result = new HashSet<>();
			for (final String key : propertyKeys) {
				final WaldotVertexProperty<V> property = getNamespace()
						.getVertexProperty(this, key);
				if (property != null) {
					result.add(property);
				}
			}
			return result.iterator();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> VertexProperty<V> property(final String key) {
		if (this.isRemoved()) {
			return VertexProperty.empty();
		}
		if (inComputerMode()) {
			@SuppressWarnings({"rawtypes"})
			final List<VertexProperty> list = (List) getGraphComputerView()
					.getProperty(this, key);
			if (list.size() == 0) {
				return VertexProperty.<V>empty();
			} else if (list.size() == 1) {
				return list.get(0);
			} else {
				throw Vertex.Exceptions
						.multiplePropertiesExistForProvidedKey(key);
			}
		} else {
			// direct lookup: copying every property to read one was the main
			// CPU cost of the plugins that poll their configuration
			final WaldotVertexProperty<V> property = getNamespace()
					.getVertexProperty(this, key);
			return property != null ? property : VertexProperty.<V>empty();
		}
	}

	@Override
	public <V> VertexProperty<V> property(
			final VertexProperty.Cardinality cardinality, final String key,
			final V value, final Object... keyValues) {
		if (this.isRemoved()) {
			throw elementAlreadyRemoved(Vertex.class, this.getNodeId());
		}
		ElementHelper.legalPropertyKeyValueArray(keyValues);
		ElementHelper.validateProperty(key, value);
		if (!allowNullPropertyValues && null == value) {
			final VertexProperty.Cardinality card = null == cardinality
					? graph.features().vertex().getCardinality(key)
					: cardinality;
			if (VertexProperty.Cardinality.single == card) {
				properties(key).forEachRemaining(Property::remove);
			}
			return VertexProperty.empty();
		}
		if (inComputerMode()) {
			final VertexProperty<V> vertexProperty = getGraphComputerView()
					.addProperty(this, key, value);
			ElementHelper.attachProperties(vertexProperty, keyValues);
			return vertexProperty;
		} else {
			// the OPC UA model keeps one property node per key: list and set
			// collapse to a single value. Only an explicit 'set' cardinality
			// with an equal value is a no-op; the default path always writes (a
			// write of an equal value still notifies observers and rules).
			if (VertexProperty.Cardinality.set == cardinality) {
				final VertexProperty<V> existing = property(key);
				if (existing.isPresent()
						&& java.util.Objects.equals(existing.value(), value)) {
					ElementHelper.attachProperties(existing, keyValues);
					return existing;
				}
			}
			final VertexProperty<V> vertexProperty = getNamespace()
					.createOrUpdateWaldotVertexProperty(this, key, value);
			ElementHelper.attachProperties(vertexProperty, keyValues);
			return vertexProperty;
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
		// TODO rimuovere le variabili e gli oggetti collegati al vertice

	}

	@Override
	public void removeRelatedProperty(final WaldotVertexProperty<?> property) {
		if (property == null) {
			return;
		}
		linkedProperties.remove(property);
		final NodeId propertyNodeId = property.getNodeId();
		for (final org.eclipse.milo.opcua.sdk.core.Reference reference : new java.util.ArrayList<>(
				getReferences())) {
			if (reference.isForward() && reference.getTargetNodeId()
					.toNodeId(getNodeContext().getNamespaceTable())
					.map(propertyNodeId::equals).orElse(false)) {
				removeReference(reference);
			}
		}
	}

	protected void resetDirtyStateToUpdate() {
		dirtyStateToUpdate = false;
	}

	public void sendLogEvent(String category, String message, int severity) {
		if (category == null || category.isEmpty()) {
			logger.warn(
					"sendLogEvent called with null or empty category, event not sent");
			return;
		}
		if (message == null || message.isEmpty()) {
			logger.warn(
					"sendLogEvent called with null or empty message, event not sent");
			return;
		}
		try {
			final UUID randomUUID = UUID.randomUUID();
			final BaseEventTypeNode eventNode = getNamespace().getEventFactory()
					.createEvent(getNamespace().generateNodeId(randomUUID),
							NodeIds.BaseEventType);
			eventNode.setBrowseName(
					getNamespace().generateQualifiedName(category));
			eventNode.setDisplayName(LocalizedText.english(category));
			eventNode.setEventId(
					ByteString.of(randomUUID.toString().getBytes()));
			eventNode.setEventType(NodeIds.SystemEventType);
			eventNode.setSourceNode(getNodeId());
			eventNode.setSourceName(getBrowseName().getName());
			eventNode.setTime(DateTime.now());
			eventNode.setReceiveTime(DateTime.NULL_VALUE);
			eventNode.setMessage(LocalizedText.english(message));
			eventNode.setSeverity(ushort(severity));
			postEvent(eventNode);
		} catch (final Throwable e) {
			logger.error("Error sending log event", e);
		}
	}

	protected <DATA_TYPE> void setRuntimeParameter(String label, Object value) {
		@SuppressWarnings("unchecked")
		final MetaDataProperty<DATA_TYPE> entry = managedProperties.get(label);
		if (entry != null && value != null
				&& entry.getJavaType().isInstance(value)) {
			final DATA_TYPE typedValue = entry.getJavaType().cast(value);
			// espone il parametro anche come proprieta' del vertice
			// (property(key), getVertexProperties()), come avviene per i
			// valori passati alla creazione
			property(label, typedValue);
			setProperty(entry.getQualifiedProperty(), typedValue);
		}
	}

	@Override
	public String toString() {
		return WaldotGraph.V + WaldotGraph.L_BRACKET
				+ getNodeId().toParseableString() + WaldotGraph.R_BRACKET;
	}

	private <DATA_TYPE> void updateManagedProperties(String label,
			DataValue opcDataValue) {
		try {
			@SuppressWarnings("unchecked")
			final MetaDataProperty<DATA_TYPE> entry = managedProperties
					.get(label);
			if (entry != null && opcDataValue != null
					&& opcDataValue.getValue() != null
					&& opcDataValue.getValue().getValue() != null) {
				final Object convertedValue = valueConversion(label,
						entry.getJavaType(),
						opcDataValue.getValue().getValue().toString());
				final boolean isDirty = checkPropertyValueChange(label,
						entry.getJavaType().cast(convertedValue),
						entry.getQualifiedProperty(),
						entry.getCallbackOnChange());
				dirtyStateToUpdate = dirtyStateToUpdate || isDirty;
			}
		} catch (final Throwable e) {
			logger.error("Error updating managed property {} with value {}",
					label, opcDataValue, e);
		}
	}

	protected boolean updatePropertyIfChanged(String key, Object value) {
		if (key == null) {
			return false;
		}
		if (value == null) {
			return false;
		}
		final VertexProperty<Object> currentValue = property(key);
		if (currentValue == null || !currentValue.isPresent()
				|| !currentValue.value().equals(value)) {
			property(key, value);
			return true;
		} else {
			return false;
		}
	}

	@SuppressWarnings({"unchecked"})
	private <DATA_TYPE> DATA_TYPE valueConversion(String label,
			Class<DATA_TYPE> javaType, final String value) {
		DATA_TYPE convertedValue = null;
		try {
			switch (javaType.getSimpleName()) {
				case "String" :
					convertedValue = (DATA_TYPE) value;
					break;
				case "Integer" :
					convertedValue = (DATA_TYPE) Integer.valueOf(value);
					break;
				case "Long" :
					convertedValue = (DATA_TYPE) Long.valueOf(value);
					break;
				case "Boolean" :
					convertedValue = (DATA_TYPE) Boolean.valueOf(value);
					break;
				case "Double" :
					convertedValue = (DATA_TYPE) Double.valueOf(value);
					break;
				case "Float" :
					convertedValue = (DATA_TYPE) Float.valueOf(value);
					break;
				case "Byte" :
					convertedValue = (DATA_TYPE) Byte.valueOf(value);
					break;
				case "Instant" :
					convertedValue = (DATA_TYPE) Instant.parse(value);
					break;
				case "JSONObject" :
					convertedValue = (DATA_TYPE) new JSONObject(value);
					break;
				default :
					logger.warn("Unsupported data type {} for property {}",
							javaType.getSimpleName(), label);
			}
		} catch (final Throwable e) {
			logger.error("Error converting value {} to type {} for property {}",
					value, javaType.getSimpleName(), label, e);
		}
		return convertedValue;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Iterator<Vertex> vertices(final Direction direction,
			final String... edgeLabels) {
		if (inComputerMode()) {
			return direction.equals(Direction.BOTH)
					? IteratorUtils.concat(
							IteratorUtils.map(
									this.edges(Direction.OUT, edgeLabels),
									Edge::inVertex),
							IteratorUtils.map(
									this.edges(Direction.IN, edgeLabels),
									Edge::outVertex))
					: IteratorUtils.map(this.edges(direction, edgeLabels),
							edge -> edge.vertices(direction.opposite()).next());
		}
		return (Iterator) getNamespace()
				.getVertices(this, direction, edgeLabels).iterator();
	}

}
