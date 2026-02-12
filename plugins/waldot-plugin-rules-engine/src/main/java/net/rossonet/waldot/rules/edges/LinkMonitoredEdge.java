package net.rossonet.waldot.rules.edges;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.rules.WaldotRulesEnginePlugin;

public class LinkMonitoredEdge extends MonitoredEdge {

	public enum LinkDirection {
		FROM, TO
	}

	private final LinkDirection direction;

	public LinkMonitoredEdge(WaldotRulesEnginePlugin engine, LinkDirection direction, WaldotEdge edge,
			WaldotVertex sourceVertex, WaldotVertex targetVertex) {
		super(engine, edge, sourceVertex, targetVertex);
		this.direction = direction;
	}

	@Override
	protected void createObserverNeeded() {
		if (direction.equals(LinkDirection.TO)) {
			getSourceVertex().addEventObserver(this);
			getSourceVertex().addPropertyObserver(this);
		}
		if (direction.equals(LinkDirection.FROM)) {
			getTargetVertex().addEventObserver(this);
			getTargetVertex().addPropertyObserver(this);
		}
	}

	@Override
	public void fireEvent(UaNode node, BaseEventType event) {
		// per ora non faccio nulla, ma in futuro potrei voler propagare eventi anche
		// con questo tipo di edge

	}

	private String getDestinationLabel(String propertyLabel) {
		final Property<Object> destinationProperty = getEdge().property(propertyLabel);
		if (destinationProperty.isPresent() && destinationProperty.value() instanceof String) {
			return (String) destinationProperty.value();
		} else {
			return propertyLabel;
		}
	}

	@Override
	protected Object getLastValue(String propertyLabel) {
		if (direction.equals(LinkDirection.TO)) {
			return getTargetVertex().property(getDestinationLabel(propertyLabel));
		}
		if (direction.equals(LinkDirection.FROM)) {
			return getSourceVertex().property(getDestinationLabel(propertyLabel));
		} else {
			logger.error("Invalid direction for LinkMonitoredEdge: {}", direction);
			return null;
		}
	}

	@Override
	public void propertyChanged(UaNode sourceNode, String propertyLabel, Object value) {
		if (isActive() && isPropertyNotificationActive() && isMonitoredProperty(propertyLabel)) {
			if (isDeadBandExceeded(propertyLabel, value)) {
				if (direction.equals(LinkDirection.TO)) {
					if (isDelayProperty()) {
						sendWithDelay(getTargetVertex(), propertyLabel, value);
					} else {
						getTargetVertex().property(getDestinationLabel(propertyLabel), value);
					}
				}
				if (direction.equals(LinkDirection.FROM)) {
					if (isDelayProperty()) {
						sendWithDelay(getSourceVertex(), propertyLabel, value);
					} else {
						getSourceVertex().property(getDestinationLabel(propertyLabel), value);
					}
				}
			}
		}

	}

}
