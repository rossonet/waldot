package net.rossonet.waldot.gremlin.opcgraph.structure.edge;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

import net.rossonet.waldot.api.models.MonitoredEdge;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.opc.AbstractOpcVertex;

public class FireMonitoredEdge extends MonitoredEdge {

	public FireMonitoredEdge(WaldotNamespace engine, WaldotEdge edge, WaldotVertex sourceVertex,
			WaldotVertex targetVertex) {
		super(engine, edge, sourceVertex, targetVertex);
	}

	@Override
	protected void createObserverNeeded() {
		if (getTargetVertex() instanceof AbstractOpcVertex) {
			getSourceVertex().addEventObserver(this);
			getSourceVertex().addPropertyObserver(this);
		}
	}

	@Override
	public void fireEvent(UaNode node, BaseEventType event) {
		if (isActive() && isEventNotificationActive() && getTargetVertex() instanceof AbstractOpcVertex) {
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
	protected Object getLastValue(String propertyLabel) {
		// non è usata in questo tipo di edge
		return null;
	}

	@Override
	public void propertyChanged(UaNode node, String label, Object value) {
		if (isActive() && isPropertyNotificationActive() && isMonitoredProperty(label)
				&& getTargetVertex() instanceof AbstractOpcVertex) {
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
