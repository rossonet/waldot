package net.rossonet.waldot.rules.edges;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.PropertyObserver;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.rules.WaldotRulesEnginePlugin;
import net.rossonet.waldot.rules.vertices.FireableAbstractOpcVertex;

public abstract class MonitoredEdge implements EventObserver, PropertyObserver {

	protected static final String ABSOLUTE = "absolute";
	protected static final String ACTIVE_LABEL = "active";
	protected static final String DEADBAND_LABEL = "deadband-value";
	protected static final String DEADBAND_TYPE_LABEL = "deadband-type";
	protected static final String DELAY_LABEL = "delay";
	protected static final String EVENT_ACTIVE_LABEL = "active-event";
	protected static final String JOLLY_LABEL = "*";
	protected final static Logger logger = LoggerFactory.getLogger(MonitoredEdge.class);
	protected static final String MONITORED_PROPERTIES_LABEL = null;
	protected static final String PERCENTAGE = "percentage";
	private static final String PROPERTY_ACTIVE_LABEL = "active-property";
	private static final String SEPARATOR = ",";
	private final WaldotEdge edge;
	private final WaldotRulesEnginePlugin engine;
	private final WaldotVertex sourceVertex;
	private final WaldotVertex targetVertex;

	public MonitoredEdge(WaldotRulesEnginePlugin engine, WaldotEdge edge, WaldotVertex sourceVertex,
			WaldotVertex targetVertex) {
		this.engine = engine;
		this.edge = edge;
		this.sourceVertex = sourceVertex;
		this.targetVertex = targetVertex;
		createObserverNeeded();
	}

	protected boolean checkBinaryInProperty(String propertyLabel) {
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

	protected boolean checkContainsInPropertyArray(String propertyLabel, String label) {
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
					if (values.contains(JOLLY_LABEL)) {
						return true;
					}
					if (values.contains(label)) {
						return true;
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

	protected long checkLongInProperty(String propertyLabel) {
		try {
			final Property<Object> property = edge.property(propertyLabel);
			if (property.isPresent()) {
				final Object value = property.value();
				if (value instanceof Long) {
					return (Long) value;
				} else if (value instanceof String) {
					return Long.valueOf((String) value);
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
		int calcolatedPriority = WaldotRulesEnginePlugin.DEFAULT_PRIORITY_VALUE;
		final Property<Object> priorityValue = getEdge().property(WaldotRulesEnginePlugin.PRIORITY_FIELD.toLowerCase());
		if (priorityValue.isPresent()) {
			final Object priority = priorityValue.value();
			if (priority instanceof Integer) {
				calcolatedPriority = (Integer) priority;
			} else if (priority instanceof String) {
				try {
					calcolatedPriority = Integer.parseInt((String) priority);
				} catch (final NumberFormatException e) {
					logger.warn("Invalid priority value: {}", priorityValue);
					calcolatedPriority = WaldotRulesEnginePlugin.DEFAULT_PRIORITY_VALUE;
				}
			} else {
				logger.warn("Unsupported priority value type: {}", priorityValue.getClass());
				calcolatedPriority = WaldotRulesEnginePlugin.DEFAULT_PRIORITY_VALUE;
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

	protected boolean isActive() {
		return checkBinaryInProperty(ACTIVE_LABEL);
	}

	// attenzione alle performance di questa logica, se è necessario potrebbe essere
	// utile implementare un sistema di caching del valore del deadband
	protected boolean isDeadBandExceeded(String propertyLabel, Object value) {
		if (value == null || !(value instanceof Number)) {
			return true; // se il valore non è numerico o è null, non applico il deadband
		}
		final Property<Object> deadBandValue = edge.property(DEADBAND_LABEL);
		if (deadBandValue.isPresent()) {
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
							} else {
								// deadband assoluto
								return Math.abs(((Number) value).doubleValue() - last) > ((Number) dbValue)
										.doubleValue();
							}
						} else {
							// se il tipo di deadband non è specificato, considero sempre superato
							return true;
						}
					} else {
						// se il tipo di deadband non è specificato, considero sempre superato
						return true;
					}
				} else {
					// se il deadband è specificato ma non è un numero, considero sempre superato
					return true;
				}
			} else {
				// se non è specificato il deadband, considero sempre superato
				return true;
			}
		} else {
			// se il valore non è numerico o è null, non applico il deadband
			return true;
		}
	}

	protected boolean isDelayProperty() {
		return checkLongInProperty(PROPERTY_ACTIVE_LABEL) > 0;
	}

	protected boolean isEventNotificationActive() {
		return checkBinaryInProperty(EVENT_ACTIVE_LABEL);
	}

	protected boolean isMonitoredProperty(String label) {
		return checkContainsInPropertyArray(MONITORED_PROPERTIES_LABEL, label);
	}

	protected boolean isPropertyNotificationActive() {
		return checkBinaryInProperty(PROPERTY_ACTIVE_LABEL);
	}

	public void remove() {
		// implementare le logiche di pulizia prima di rimuovere l'arco
	}

	protected void sendFireWithDelay(FireableAbstractOpcVertex destinationVertex, UaNode node, BaseEventType event,
			int calcolatedPriority) {
		engine.getTimer().schedule(() -> {
			destinationVertex.fireEvent(node, event, calcolatedPriority);
		}, checkLongInProperty(DELAY_LABEL), TimeUnit.MILLISECONDS);

	}

	protected void sendFireWithDelay(FireableAbstractOpcVertex destinationVertex, UaNode node, String propertyLabel,
			Object value, int calcolatedPriority) {
		engine.getTimer().schedule(() -> {
			destinationVertex.fireProperty(node, propertyLabel, value, calcolatedPriority);
		}, checkLongInProperty(DELAY_LABEL), TimeUnit.MILLISECONDS);

	}

	protected void sendWithDelay(WaldotVertex destinationVertex, String propertyLabel, Object value) {
		engine.getTimer().schedule(() -> {
			destinationVertex.property(propertyLabel, value);
		}, checkLongInProperty(DELAY_LABEL), TimeUnit.MILLISECONDS);

	}

}
