package net.rossonet.waldot.rules.vertices;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.jexl.HysteresisPriorityQueue;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.rules.WaldotRulesEnginePlugin;
import net.rossonet.waldot.rules.events.FireableAction;
import net.rossonet.waldot.rules.events.RunnableEvent;

/**
 * Abstract base class for vertices that can receive events and enqueue actions for execution.
 * <p>
 * Classe base astratta per i vertici che possono ricevere eventi OPC-UA e
 * metterli in coda per l'esecuzione. Implementa una coda con priorità e supporto
 * per isteresi temporale (hysteresis) per evitare flooding di eventi.
 * </p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Event reception</b>: Receives OPC-UA events and property changes</li>
 *   <li><b>Priority queuing</b>: Events queued by priority for ordered execution</li>
 *   <li><b>Hysteresis support</b>: Time-based deduplication to prevent event flooding</li>
 *   <li><b>Action generation</b>: Abstract methods to create executable actions from events</li>
 * </ul>
 * 
 * <h2>Hysteresis Mechanism</h2>
 * <p>
 * When hysteresis is enabled (hysteresisTimeMs > 0), duplicate events within the
 * time window are automatically deduplicated to prevent flooding. For example,
 * if hysteresis is 1000ms, only one event per second is queued even if 100 events
 * arrive in that second.
 * </p>
 * 
 * <h2>Lifecycle</h2>
 * <pre>
 * Event arrives → fireEvent/fireProperty called
 *                    ↓
 *              getRunnableEvent/getRunnablePropertyEvent
 *                    ↓
 *              Create RunnableEvent with FireableAction
 *                    ↓
 *              offer() to priority queue (with hysteresis check)
 *                    ↓
 *              ComputeVertex polls event for execution
 * </pre>
 * 
 * <h2>Implementation Requirements</h2>
 * <p>
 * Subclasses must implement:
 * <ul>
 *   <li>{@link #getRunnableEvent(UaNode, BaseEventType)}: Create action for OPC-UA event</li>
 *   <li>{@link #getRunnablePropertyEvent(UaNode, String)}: Create action for property change</li>
 * </ul>
 * </p>
 * 
 * @see RuleVertex
 * @see ComputeVertex
 * @see HysteresisPriorityQueue
 * 
 * @author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @since 0.4.0
 */
public abstract class ComputableFireableAbstractOpcVertex extends AbstractOpcVertex implements AutoCloseable {

	// Intervallo di pulizia della coda (10 secondi)
	private static final long CLEAN_UP_INTERVAL_MS = 10000L;

	private HysteresisPriorityQueue<RunnableEvent> eventQueue;

	private long hysteresisTimeMs = 0;

	private final long lastCleanUpTimeMs = System.currentTimeMillis();

	public ComputableFireableAbstractOpcVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
	}

	private void cleanUpIfNeeded() {
		if (isHisteresisEnabled()) {
			final long now = System.currentTimeMillis();
			if (now - lastCleanUpTimeMs >= CLEAN_UP_INTERVAL_MS) {
				eventQueue.cleanUp();
			}
		}

	}

	@Override
	public void close() throws Exception {
		eventQueue.cleanUp();

	}

	/**
	 * Handles OPC-UA event firing.
	 * <p>
	 * Chiamato quando un FireMonitoredEdge sollecita questo vertice con un evento OPC-UA.
	 * Crea un'azione eseguibile e la mette in coda con la priorità specificata.
	 * </p>
	 * 
	 * @param node OPC-UA node that generated the event
	 * @param event OPC-UA event
	 * @param priority event priority
	 */
	@Override
	public void fireEvent(UaNode node, BaseEventType event, int priority) {
		// Crea RunnableEvent e mette in coda con priorità
		offer(new RunnableEvent(node, event, getRunnableEvent(node, event)), priority);
	}

	/**
	 * Handles OPC-UA property change firing.
	 * <p>
	 * Chiamato quando un FireMonitoredEdge sollecita questo vertice con una modifica
	 * di proprietà. Crea un'azione eseguibile e la mette in coda con la priorità specificata.
	 * </p>
	 * 
	 * @param node OPC-UA node with property change
	 * @param propertyLabel property name that changed
	 * @param value new property value
	 * @param priority event priority
	 */
	@Override
	public void fireProperty(UaNode node, String propertyLabel, DataValue value, int priority) {
		// Crea RunnableEvent per property change e mette in coda
		offer(new RunnableEvent(node, propertyLabel, value, getRunnablePropertyEvent(node, propertyLabel)), priority);
	}

	/**
	 * Returns the hysteresis time in milliseconds.
	 * 
	 * @return hysteresis time in ms (0 = disabled)
	 */
	public long getHysteresisTimeMs() {
		return hysteresisTimeMs;
	}

	/**
	 * Creates executable action for an OPC-UA event.
	 * <p>
	 * Metodo astratto implementato dalle sottoclassi per creare l'azione
	 * da eseguire quando arriva un evento OPC-UA.
	 * </p>
	 * 
	 * @param node source node
	 * @param event OPC-UA event
	 * @return executable action
	 */
	protected abstract FireableAction getRunnableEvent(UaNode node, BaseEventType event);

	/**
	 * Creates executable action for a property change.
	 * <p>
	 * Metodo astratto implementato dalle sottoclassi per creare l'azione
	 * da eseguire quando una proprietà OPC-UA cambia.
	 * </p>
	 * 
	 * @param node source node
	 * @param propertyLabel property that changed
	 * @return executable action
	 */
	protected abstract FireableAction getRunnablePropertyEvent(UaNode node, String propertyLabel);

	public boolean isHisteresisEnabled() {
		return getHysteresisTimeMs() != 0;
	}

	public boolean offer(RunnableEvent message, int priority) {
		final boolean ok = eventQueue.offer(message, priority);
		if (ok) {
			property(WaldotRulesEnginePlugin.QUEUE_SIZE_LABEL.toLowerCase(), eventQueue.size());
		}
		return ok;
	}

	public RunnableEvent poll() {
		final RunnableEvent poll = eventQueue.poll();
		if (poll != null) {
			property(WaldotRulesEnginePlugin.QUEUE_SIZE_LABEL.toLowerCase(), eventQueue.size());
		}
		cleanUpIfNeeded();
		return poll;
	}

	protected void setHysteresisTimeMs(long hysteresisTimeMs) {
		if (hysteresisTimeMs < 0) {
			throw new IllegalArgumentException("Hysteresis time must be non-negative");
		}
		if (this.hysteresisTimeMs != hysteresisTimeMs) {
			this.hysteresisTimeMs = hysteresisTimeMs;
			if (eventQueue != null) {
				eventQueue.cleanUp();
				eventQueue = null;
			}
			eventQueue = new HysteresisPriorityQueue<>(hysteresisTimeMs);
		} else {
			if (eventQueue == null) {
				eventQueue = new HysteresisPriorityQueue<>(hysteresisTimeMs);
			}
		}
	}

	public RunnableEvent take() throws InterruptedException {
		final RunnableEvent take = eventQueue.take();
		property(WaldotRulesEnginePlugin.QUEUE_SIZE_LABEL.toLowerCase(), eventQueue.size());
		cleanUpIfNeeded();
		return take;
	}

}
