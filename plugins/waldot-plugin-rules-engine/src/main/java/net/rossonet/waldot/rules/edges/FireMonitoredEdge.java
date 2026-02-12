package net.rossonet.waldot.rules.edges;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.rules.WaldotRulesEnginePlugin;
import net.rossonet.waldot.rules.vertices.FireableAbstractOpcVertex;

public class FireMonitoredEdge extends MonitoredEdge {

	public FireMonitoredEdge(WaldotRulesEnginePlugin engine, WaldotEdge edge, WaldotVertex sourceVertex,
			WaldotVertex targetVertex) {
		super(engine, edge, sourceVertex, targetVertex);
	}

	@Override
	protected void createObserverNeeded() {
		if (getTargetVertex() instanceof FireableAbstractOpcVertex) {
			getSourceVertex().addEventObserver(this);
			getSourceVertex().addPropertyObserver(this);
		}
	}

	@Override
	public void fireEvent(UaNode node, BaseEventType event) {
		if (isActive() && isEventNotificationActive() && getTargetVertex() instanceof FireableAbstractOpcVertex) {
			final int calcolatedPriority = getPriority();
			if (isDelayProperty()) {
				sendFireWithDelay((FireableAbstractOpcVertex) getTargetVertex(), node, event, calcolatedPriority);
			} else {
				((FireableAbstractOpcVertex) getTargetVertex()).fireEvent(node, event, calcolatedPriority);
			}
		} else {
			logger.debug("Event notification not active or target vertex is not fireable for event '{}'", event);
		}
	}

	@Override
	protected Object getLastValue(String propertyLabel) {
		// non è usata in questo tipo di edge
		return null;
	}

	@Override
	public void propertyChanged(UaNode node, String label, Object value) {
		if (isActive() && isPropertyNotificationActive() && isMonitoredProperty(label)
				&& getTargetVertex() instanceof FireableAbstractOpcVertex) {
			if (isDeadBandExceeded(label, value)) {
				final int calcolatedPriority = getPriority();
				if (isDelayProperty()) {
					sendFireWithDelay((FireableAbstractOpcVertex) getTargetVertex(), node, label, value,
							calcolatedPriority);
				} else {
					((FireableAbstractOpcVertex) getTargetVertex()).fireProperty(node, label, value,
							calcolatedPriority);
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
