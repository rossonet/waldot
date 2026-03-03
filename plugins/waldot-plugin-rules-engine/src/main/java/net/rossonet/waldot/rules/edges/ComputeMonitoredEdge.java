package net.rossonet.waldot.rules.edges;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import net.rossonet.waldot.api.models.MonitoredEdge;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.rules.WaldotRulesEnginePlugin;
import net.rossonet.waldot.rules.vertices.ComputeVertex;
import net.rossonet.waldot.rules.vertices.ComputableFireableAbstractOpcVertex;

public class ComputeMonitoredEdge extends MonitoredEdge {

	public ComputeMonitoredEdge(final WaldotNamespace engine, final WaldotEdge edge, final WaldotVertex sourceVertex,
			final WaldotVertex targetVertex) {
		super(engine, edge, sourceVertex, targetVertex);
	}

	@Override
	protected void createObserverNeeded() {
		if (getTargetVertex() instanceof ComputableFireableAbstractOpcVertex && getSourceVertex() instanceof ComputeVertex) {
			getTargetVertex().addEventObserver(this);
		}
	}

	@Override
	public void fireEvent(final UaNode node, final BaseEventType event) {
		// In questo tipo di edge, gli eventi non sono gestiti direttamente.
		// L'attivazione dell'edge è determinata dalla logica di esecuzione del vertice
		// di destinazione (ComputeVertex), quindi non è necessario implementare questa
		// logica qui.
	}

	@Override
	protected Object getLastValue(final String propertyLabel) {
		// non è usata in questo tipo di edge
		return null;
	}

	@Override
	public void propertyChanged(final UaNode node, final String label, final DataValue value) {
		if (label.equalsIgnoreCase(WaldotRulesEnginePlugin.QUEUE_SIZE_LABEL)) {
			final int calcolatedPriority = getPriority();
			((ComputeVertex) getSourceVertex()).notifyQueueSizeChange(node, label, value, calcolatedPriority);
		}
	}
}
