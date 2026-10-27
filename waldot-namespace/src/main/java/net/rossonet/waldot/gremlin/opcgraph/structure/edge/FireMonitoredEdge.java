package net.rossonet.waldot.gremlin.opcgraph.structure.edge;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import net.rossonet.waldot.api.models.MonitoredEdge;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;

/**
 * Notifies the target vertex of source events and property changes without
 * writing the value. Deadband compares each numeric change with the last
 * value actually fired for that property: the target does not store it.
 */
public class FireMonitoredEdge extends MonitoredEdge {

	private final Map<String, Object> lastFiredValues = new HashMap<>();

	public FireMonitoredEdge(final WaldotNamespace engine, final WaldotEdge edge, final WaldotVertex sourceVertex,
			final WaldotVertex targetVertex) {
		super(engine, edge, sourceVertex, targetVertex);
	}

	@Override
	protected void createObserverNeeded() {
		getSourceVertex().addEventObserver(this);
		getSourceVertex().addPropertyObserver(this);
	}

	@Override
	public void fireEvent(final UaNode node, final BaseEventType event) {
		if (isActive() && isEventNotificationActive()) {
			final int calcolatedPriority = getPriority();
			if (isDelayProperty()) {
				sendFireWithDelay(getTargetVertex(), node, event, calcolatedPriority);
			} else {
				getTargetVertex().fireEvent(node, event, calcolatedPriority);
			}
		} else {
			logger.debug("Event notification not active or target vertex is not fireable for event '{}'", event);
		}
	}

	@Override
	protected Object getLastValue(final String propertyLabel) {
		synchronized (lastFiredValues) {
			return lastFiredValues.get(propertyLabel);
		}
	}

	private void rememberFiredValue(final String propertyLabel, final DataValue value) {
		if (propertyLabel == null || value == null || value.getValue() == null) {
			return;
		}
		final Object raw = value.getValue().getValue();
		synchronized (lastFiredValues) {
			if (raw instanceof Number) {
				lastFiredValues.put(propertyLabel, raw);
			} else {
				// a non-numeric sample is not a deadband baseline
				lastFiredValues.remove(propertyLabel);
			}
		}
	}

	@Override
	public void propertyChanged(final UaNode node, final String label, final DataValue value) {
		if (isActive() && isPropertyNotificationActive() && isMonitoredProperty(label)) {
			// the check and the cache update share the map lock, otherwise two
			// notifications can both pass against the same baseline
			synchronized (lastFiredValues) {
				if (isDeadBandExceeded(label, value)) {
					rememberFiredValue(label, value);
					final int calcolatedPriority = getPriority();
					if (isDelayProperty()) {
						sendFireWithDelay(getTargetVertex(), node, label, value, calcolatedPriority);
					} else {
						getTargetVertex().fireProperty(node, label, value, calcolatedPriority);
					}
				} else {
					logger.debug("Deadband not exceeded for property '{}', value: {}", label, value);
				}
			}
		} else {
			logger.debug(
					"Property notification not active, property '{}' not monitored or target vertex is not fireable for property '{}'",
					label, value);
		}

	}

}