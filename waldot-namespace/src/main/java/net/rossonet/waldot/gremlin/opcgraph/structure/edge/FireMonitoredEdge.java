package net.rossonet.waldot.gremlin.opcgraph.structure.edge;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import net.rossonet.waldot.api.models.MonitoredEdge;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;

public class FireMonitoredEdge extends MonitoredEdge {

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
		// TODO: implementare una cache per gli ultimi valori per propertyLabel
		return null;
	}

	@Override
	public void propertyChanged(final UaNode node, final String label, final DataValue value) {
		if (isActive() && isPropertyNotificationActive() && isMonitoredProperty(label)) {
			if (isDeadBandExceeded(label, value)) {
				final int calcolatedPriority = getPriority();
				if (isDelayProperty()) {
					sendFireWithDelay(getTargetVertex(), node, label, value, calcolatedPriority);
				} else {
					getTargetVertex().fireProperty(node, label, value, calcolatedPriority);
				}
			} else {
				logger.debug("Deadband not exceeded for property '{}', value: {}", label, value);
			}
		} else {
			logger.debug(
					"Property notification not active, property '{}' not monitored or target vertex is not fireable for property '{}'",
					label, value);
		}

	}

}
