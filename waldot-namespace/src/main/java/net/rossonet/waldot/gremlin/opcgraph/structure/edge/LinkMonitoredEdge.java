package net.rossonet.waldot.gremlin.opcgraph.structure.edge;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import net.rossonet.waldot.api.models.MonitoredEdge;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;

public class LinkMonitoredEdge extends MonitoredEdge {

	public enum LinkDirection {
		FROM, TO
	}

	private final LinkDirection direction;

	public LinkMonitoredEdge(final WaldotNamespace engine, final LinkDirection direction, final WaldotEdge edge,
			final WaldotVertex sourceVertex, final WaldotVertex targetVertex) {
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
	public void fireEvent(final UaNode node, final BaseEventType event) {
		// per ora non faccio nulla, ma in futuro potrei voler propagare eventi anche
		// con questo tipo di edge

	}

	private String getDestinationLabel(final String propertyLabel) {
		final Property<Object> destinationProperty = getEdge().property(propertyLabel);
		if (destinationProperty.isPresent() && destinationProperty.value() instanceof String) {
			return (String) destinationProperty.value();
		} else {
			return propertyLabel;
		}
	}

	@Override
	protected Object getLastValue(final String propertyLabel) {
		if (direction.equals(LinkDirection.TO)) {
			return getTargetVertex().property(getDestinationLabel(propertyLabel)).value();
		}
		if (direction.equals(LinkDirection.FROM)) {
			return getSourceVertex().property(getDestinationLabel(propertyLabel)).value();
		} else {
			logger.error("Invalid direction for LinkMonitoredEdge: {}", direction);
			return null;
		}
	}

	@Override
	public void propertyChanged(final UaNode sourceNode, final String propertyLabel, final DataValue value) {
		if (isActive() && isPropertyNotificationActive() && isMonitoredProperty(propertyLabel)) {
			if (isDeadBandExceeded(propertyLabel, value)) {
				if (direction.equals(LinkDirection.TO)) {
					if (isDelayProperty()) {
						sendWithDelay(getTargetVertex(), propertyLabel, value.getValue().getValue());
					} else {
						getTargetVertex().property(getDestinationLabel(propertyLabel), value.getValue().getValue());
					}
				}
				if (direction.equals(LinkDirection.FROM)) {
					if (isDelayProperty()) {
						sendWithDelay(getSourceVertex(), propertyLabel, value.getValue().getValue());
					} else {
						getSourceVertex().property(getDestinationLabel(propertyLabel), value.getValue().getValue());
					}
				}
			}
		}

	}

}
