package net.rossonet.waldot.rules.edges;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import net.rossonet.waldot.api.models.MonitoredEdge;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.rules.RulesEngineConstants;
import net.rossonet.waldot.rules.vertices.ComputableFireableAbstractOpcVertex;
import net.rossonet.waldot.rules.vertices.ComputeVertex;

/**
 * Monitored edge connecting RuleVertex to ComputeVertex for execution routing.
 * <p>
 * ComputeMonitoredEdge monitora la coda del RuleVertex e notifica il ComputeVertex
 * quando ci sono eventi in attesa di essere elaborati. Agisce come un "sensore di carico"
 * che informa il ComputeVertex quali RuleVertex hanno lavoro da fare.
 * </p>
 * 
 * <h2>Architecture</h2>
 * <pre>
 * [ComputeVertex] --rules:execute--> [RuleVertex]
 *      ^                                  |
 *      |  notifyQueueSizeChange           v (property change "queue")
 *      +---------- [ComputeMonitoredEdge.propertyChanged]
 * </pre>
 * 
 * <h2>Monitoring Mechanism</h2>
 * <ol>
 *   <li>ComputeMonitoredEdge observes RuleVertex "queue" property</li>
 *   <li>When queue size changes, propertyChanged() is called</li>
 *   <li>Calculates edge priority from edge properties</li>
 *   <li>Notifies ComputeVertex with queue size and priority</li>
 *   <li>ComputeVertex adds RuleVertex to dirty nodes priority queue</li>
 *   <li>ComputeVertex later polls events from RuleVertex for execution</li>
 * </ol>
 * 
 * <h2>Priority Propagation</h2>
 * <p>
 * The edge priority (from edge properties) is propagated to ComputeVertex to calculate
 * the execution weight. This allows different rules to have different execution priorities.
 * </p>
 * 
 * <h2>Example</h2>
 * <pre>{@code
 * // High priority rule
 * compute.addEdge("runner", alarmRule, "type", "rules:execute", "priority", "100");
 * 
 * // Low priority rule
 * compute.addEdge("runner", logRule, "type", "rules:execute", "priority", "10");
 * 
 * // When both have events, alarmRule is processed first
 * }</pre>
 * 
 * @see ComputeVertex
 * @see MonitoredEdge
 * 
 * @author Andrea Ambrosini - Zona Autonoma srl
 * @since 0.4.0
 */
public class ComputeMonitoredEdge extends MonitoredEdge {

	/**
	 * Creates a compute monitored edge.
	 * 
	 * @param engine WaldOT namespace
	 * @param edge underlying graph edge
	 * @param sourceVertex source vertex (ComputeVertex)
	 * @param targetVertex target vertex (RuleVertex)
	 */
	public ComputeMonitoredEdge(final WaldotNamespace engine,
			final WaldotEdge edge, final WaldotVertex sourceVertex,
			final WaldotVertex targetVertex) {
		super(engine, edge, sourceVertex, targetVertex);
	}

	/**
	 * Creates property observer on target vertex (RuleVertex).
	 * <p>
	 * Registra questo edge come observer delle property del RuleVertex target,
	 * in particolare della property "queue" che indica quanti eventi sono in coda.
	 * </p>
	 */
	@Override
	protected void createObserverNeeded() {
		// Verifica che source sia ComputeVertex e target sia RuleVertex
		if (getTargetVertex() instanceof ComputableFireableAbstractOpcVertex
				&& getSourceVertex() instanceof ComputeVertex) {
			// Osserva le property del RuleVertex (target)
			getTargetVertex().addPropertyObserver(this);
			// gli eventi già in coda prima del collegamento non produrrebbero
			// più notifiche: il compute vertex deve conoscerli subito
			final ComputableFireableAbstractOpcVertex rule = (ComputableFireableAbstractOpcVertex) getTargetVertex();
			final int pending = rule.size();
			if (pending > 0) {
				((ComputeVertex) getSourceVertex()).notifyQueueSizeChange(rule,
						RulesEngineConstants.QUEUE_SIZE_LABEL,
						new DataValue(new Variant(Long.valueOf(pending))),
						getPriority());
			}
		}
	}

	/**
	 * Not used in ComputeMonitoredEdge.
	 * <p>
	 * In questo tipo di edge, gli eventi OPC-UA non sono gestiti direttamente.
	 * L'attivazione dell'edge è determinata solo dalle modifiche della property "queue"
	 * del RuleVertex.
	 * </p>
	 */
	@Override
	public void fireEvent(final UaNode node, final BaseEventType event) {
		// Non implementato: eventi non gestiti da ComputeMonitoredEdge
	}

	/**
	 * Not used in ComputeMonitoredEdge.
	 * 
	 * @return null
	 */
	@Override
	protected Object getLastValue(final String propertyLabel) {
		// Non usato in questo tipo di edge
		return null;
	}

	/**
	 * Handles property change notifications from RuleVertex.
	 * <p>
	 * Quando la property "queue" del RuleVertex cambia, notifica il ComputeVertex
	 * che c'è lavoro da fare. Il ComputeVertex calcola il peso basato sulla priorità
	 * dell'edge e sulla dimensione della coda, poi mette il RuleVertex nella sua
	 * priority queue di nodi sporchi.
	 * </p>
	 * 
	 * @param node RuleVertex node
	 * @param label property name ("queue")
	 * @param value new queue size
	 */
	@Override
	public void propertyChanged(final UaNode node, final String label,
			final DataValue value) {
		// Solo la property "queue" è monitorata
		if (label.equals(RulesEngineConstants.QUEUE_SIZE_LABEL)) {
			// Calcola priorità dall'edge property
			final int calcolatedPriority = getPriority();
			// Notifica ComputeVertex (source) del cambio di dimensione coda
			((ComputeVertex) getSourceVertex()).notifyQueueSizeChange(node,
					label, value, calcolatedPriority);
		}
	}

	/**
	 * Stops observing the rule and tells the compute vertex to forget it.
	 */
	@Override
	public void remove() {
		if (getTargetVertex() instanceof ComputableFireableAbstractOpcVertex
				&& getSourceVertex() instanceof ComputeVertex) {
			getTargetVertex().removePropertyObserver(this);
			((ComputeVertex) getSourceVertex())
					.forget(getTargetVertex().getNodeId());
		}
	}
}
