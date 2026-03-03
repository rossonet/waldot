package net.rossonet.waldot.api.models;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.PropertyObserver;
import net.rossonet.waldot.api.strategies.MiloStrategy;

public abstract class MonitoredEdge implements EventObserver, PropertyObserver {

	public static final String ABSOLUTE = "absolute";
	public static final String ACTIVE_LABEL = "active";
	public static final String DEADBAND_LABEL = "deadband-value";
	public static final String DEADBAND_TYPE_LABEL = "deadband-type";
	public static final String DELAY_LABEL = "delay";
	public static final String EVENT_ACTIVE_LABEL = "active-event";
	public static final String JOLLY_LABEL = "*";
	protected final static Logger logger = LoggerFactory.getLogger(MonitoredEdge.class);
	public static final String MONITORED_PROPERTIES_LABEL = "properties-to-monitor";
	public static final String PERCENTAGE = "percentage";
	public static final String PROPERTY_ACTIVE_LABEL = "active-property";
	public static final String SEPARATOR = ",";
	private final WaldotEdge edge;
	private final WaldotNamespace engine;
	private final WaldotVertex sourceVertex;
	private final WaldotVertex targetVertex;

	public MonitoredEdge(final WaldotNamespace engine, final WaldotEdge edge, final WaldotVertex sourceVertex,
			final WaldotVertex targetVertex) {
		this.engine = engine;
		this.edge = edge;
		this.sourceVertex = sourceVertex;
		this.targetVertex = targetVertex;
	}

	protected boolean checkBinaryInProperty(final String propertyLabel) {
		try {
			final Property<Object> property = edge.property(propertyLabel);
			if (property.isPresent()) {
				final Object value = property.value();
				if (value instanceof Boolean) {
					return (Boolean) value;
				} else if (value instanceof String) {
					return Boolean.parseBoolean((String) value);
				} else {
					// se esiste la proprietà e non è un booleano o string, considero la volontà di
					// inattivarla
					return false;
				}
			}
			return true; // se la proprietà non esiste, considero che non sia stata specificata la
							// volontà di inattivarla
		} catch (final Exception e) {
			// in caso di errori di conversione, per ora considero la volontà di mantenere
			// attiva la notifica
			logger.warn("Error checking binary property '{}', defaulting to active. Error: {}", propertyLabel,
					e.getMessage());
			return true;
		}
	}

	protected boolean checkContainsInPropertyArray(final String propertyLabel, final String label) {
		if (label == null) {
			logger.warn("Label is null, cannot check in property array");
			return true;
		}
		if (label.contains(SEPARATOR)) {
			logger.warn("Label contains separator '{}', which may cause issues in property array parsing: {}",
					SEPARATOR, label);
			return true;
		}
		if (label.isEmpty()) {
			logger.warn("Label is empty, cannot check in property array");
			return true;
		}
		try {
			final Property<Object> property = edge.property(propertyLabel);
			if (property.isPresent()) {
				final Object value = property.value();
				if (value instanceof String && !((String) value).isEmpty()) {
					final List<String> values = Arrays.asList(((String) value).split(SEPARATOR));
					if (values.contains(JOLLY_LABEL) || values.contains(label)) {
						return true;
					} else {
						return false;
					}
				} else {
					// se la proprietà è presente ma non è una stringa o è vuota, considero che non
					// abbia specificato alcuna proprietà da monitorare, quindi considero che non
					// sia monitorata
					return false;
				}
			}
			return true; // se la proprietà non esiste, considero che non sia stata specificata la
							// volontà di monitorare tutte le proprietà
		} catch (final Exception e) {
			// in caso di errori di conversione, per ora considero la volontà di monitorare
			// la proprietà
			logger.warn("Error checking property array '{}', defaulting to include label '{}'. Error: {}",
					propertyLabel, label, e.getMessage());
			return true;
		}

	}

	protected long checkLongInProperty(final String propertyLabel) {
		try {
			final Property<Object> property = edge.property(propertyLabel);
			if (property.isPresent()) {
				final Object value = property.value();
				if (value instanceof Number) {
					return ((Number) value).longValue();
				} else if (value instanceof String) {
					try {
						return Long.parseLong((String) value);
					} catch (final NumberFormatException e) {
						logger.warn("Invalid long value for property '{}': '{}', defaulting to 0", propertyLabel,
								value);
						return 0;
					}
				} else {
					return 0;
				}
			}
			return 0;
		} catch (final Exception e) {
			logger.warn("Error checking long property '{}', defaulting to 0. Error: {}", propertyLabel, e.getMessage());
			return 0;
		}
	}

	protected abstract void createObserverNeeded();

	public WaldotEdge getEdge() {
		return edge;
	}

	protected abstract Object getLastValue(String propertyLabel);

	protected int getPriority() {
		int calcolatedPriority = MiloStrategy.MONITOR_EDGE_DEFAULT_PRIORITY_VALUE;
		final Property<Object> priorityValue = getEdge()
				.property(MiloStrategy.MONITOR_EDGE_PRIORITY_FIELD.toLowerCase());
		if (priorityValue.isPresent()) {
			final Object priority = priorityValue.value();
			if (priority instanceof Integer) {
				calcolatedPriority = (Integer) priority;
			} else if (priority instanceof String) {
				try {
					calcolatedPriority = Integer.parseInt((String) priority);
				} catch (final NumberFormatException e) {
					logger.warn("Invalid priority value: {}", priorityValue);
					calcolatedPriority = MiloStrategy.MONITOR_EDGE_DEFAULT_PRIORITY_VALUE;
				}
			} else {
				logger.warn("Unsupported priority value type: {}", priorityValue.getClass());
				calcolatedPriority = MiloStrategy.MONITOR_EDGE_DEFAULT_PRIORITY_VALUE;
			}
		}
		return calcolatedPriority;
	}

	protected WaldotVertex getSourceVertex() {
		return sourceVertex;
	}

	public WaldotVertex getTargetVertex() {
		return targetVertex;
	}

	public void initialize() {
		createObserverNeeded();
	}

	protected boolean isActive() {
		return checkBinaryInProperty(ACTIVE_LABEL);
	}

	// attenzione alle performance di questa logica, se è necessario potrebbe essere
	// utile implementare un sistema di caching del valore del deadband
	protected boolean isDeadBandExceeded(final String propertyLabel, final DataValue dataValue) {
		final Property<Object> deadBandValue = edge.property(DEADBAND_LABEL);
		if (deadBandValue.isPresent()) {
			if (dataValue == null) {
				logger.warn("DataValue is null for property '{}', cannot check deadband", propertyLabel);
				return true; // se il DataValue è null, non applico il deadband
			}
			if (dataValue.getValue() == null) {
				logger.warn("DataValue value is null for property '{}', cannot check deadband", propertyLabel);
				return true; // se il valore del DataValue è null, non applico il deadband
			}
			final Object value = dataValue.getValue().getValue();
			if (value == null || !(value instanceof Number)) {
				return true; // se il valore non è numerico o è null, non applico il deadband
			}
			final Object dbValue = deadBandValue.value();
			if (dbValue instanceof Number) {
				final Property<Object> deadBandType = edge.property(DEADBAND_TYPE_LABEL);
				if (deadBandValue.isPresent()) {
					final Object dbType = deadBandType.value();
					if (dbType instanceof String) {
						final Object lastVal = getLastValue(propertyLabel);
						if (lastVal instanceof Number) {
							final double last = ((Number) lastVal).doubleValue();
							if (PERCENTAGE.equalsIgnoreCase((String) dbType)) {
								// deadband percentuale
								return Math.abs(((Number) value).doubleValue() - last) > Math
										.abs(last * ((Number) dbValue).doubleValue() / 100);
							} else if (ABSOLUTE.equalsIgnoreCase((String) dbType)) {
								// deadband assoluto
								return Math.abs(((Number) value).doubleValue() - last) > ((Number) dbValue)
										.doubleValue();
							} else {
								logger.warn("Unsupported deadband type '{}', defaulting to no deadband", dbType);
								return true; // se il tipo di deadband non è supportato, non applico il deadband
							}
						} else {
							// se l'ultimo valore non è numerico, non applico il deadband
							return true;
						}
					} else {
						// se deadbant type non è una stringa, considero sempre superato
						return true;
					}
				} else {
					// se il deadband type non è specificato, considero sempre superato
					return true;
				}
			} else {
				// se il valore del deadband non è numerico, non applico il deadband
				return true;
			}
		} else {
			// se il valore del deadband non è specificato, considero sempre superato
			return true;
		}
	}

	protected boolean isDelayProperty() {
		final long checkLongInProperty = checkLongInProperty(DELAY_LABEL);
		return checkLongInProperty > 0;
	}

	protected boolean isEventNotificationActive() {
		return checkBinaryInProperty(EVENT_ACTIVE_LABEL);
	}

	protected boolean isMonitoredProperty(final String label) {
		return checkContainsInPropertyArray(MONITORED_PROPERTIES_LABEL, label);
	}

	protected boolean isPropertyNotificationActive() {
		return checkBinaryInProperty(PROPERTY_ACTIVE_LABEL);
	}

	public void remove() {
		// implementare le logiche di pulizia prima di rimuovere l'arco
	}

	protected void sendFireWithDelay(final WaldotVertex destinationVertex, final UaNode node, final BaseEventType event,
			final int calcolatedPriority) {
		engine.getTimer().schedule(() -> {
			destinationVertex.fireEvent(node, event, calcolatedPriority);
		}, checkLongInProperty(DELAY_LABEL), TimeUnit.MILLISECONDS);

	}

	protected void sendFireWithDelay(final WaldotVertex destinationVertex, final UaNode node,
			final String propertyLabel, final DataValue dataValue, final int calcolatedPriority) {
		engine.getTimer().schedule(() -> {
			destinationVertex.fireProperty(node, propertyLabel, dataValue, calcolatedPriority);
		}, checkLongInProperty(DELAY_LABEL), TimeUnit.MILLISECONDS);

	}

	protected void sendWithDelay(final WaldotVertex destinationVertex, final String propertyLabel, final Object value) {
		engine.getTimer().schedule(() -> {
			destinationVertex.property(propertyLabel, value);
		}, checkLongInProperty(DELAY_LABEL), TimeUnit.MILLISECONDS);

	}

}
