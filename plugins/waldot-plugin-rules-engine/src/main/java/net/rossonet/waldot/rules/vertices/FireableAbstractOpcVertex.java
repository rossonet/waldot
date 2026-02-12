package net.rossonet.waldot.rules.vertices;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
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

public abstract class FireableAbstractOpcVertex extends AbstractOpcVertex implements AutoCloseable {

	private static final long CLEAN_UP_INTERVAL_MS = 10000L; // intervallo di pulizia della coda (10 secondi)
	private final HysteresisPriorityQueue<RunnableEvent> eventQueue;
	private long hysteresisTimeMs;
	private final long lastCleanUpTimeMs = System.currentTimeMillis();

	public FireableAbstractOpcVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId, QualifiedName browseName,
			LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			UByte eventNotifier, long version) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		eventQueue = new HysteresisPriorityQueue<>(getHysteresisTimeMs());
	}

	private void cleanUpIfNeeded() {
		if (isHisteresisEnabled()) {
			final long now = System.currentTimeMillis();
			if (now - lastCleanUpTimeMs >= CLEAN_UP_INTERVAL_MS) {
				eventQueue.cleanUp();
			}
		}

	}

	public void fireEvent(UaNode node, BaseEventType event, int priority) {
		eventQueue.offer(new RunnableEvent(node, event, getRunnableEvent(node, event)), priority);
	}

	public void fireProperty(UaNode node, String propertyLabel, Object value, int priority) {
		eventQueue.offer(new RunnableEvent(node, propertyLabel, value, getRunnablePropertyEvent(node, propertyLabel)),
				priority);
	}

	public long getHysteresisTimeMs() {
		return hysteresisTimeMs;
	}

	protected abstract FireableAction getRunnableEvent(UaNode node, BaseEventType event);

	protected abstract FireableAction getRunnablePropertyEvent(UaNode node, String propertyLabel);

	public boolean isHisteresisEnabled() {
		return getHysteresisTimeMs() != 0;
	}

	public boolean offer(RunnableEvent message, int priority) {
		final boolean ok = eventQueue.offer(message, priority);
		if (ok) {
			property(WaldotRulesEnginePlugin.QUEUE_SIZE_LABEL, eventQueue.size());
		}
		return ok;
	}

	public RunnableEvent poll() {
		final RunnableEvent poll = eventQueue.poll();
		if (poll != null) {
			property(WaldotRulesEnginePlugin.QUEUE_SIZE_LABEL, eventQueue.size());
		}
		cleanUpIfNeeded();
		return poll;
	}

	protected void setHysteresisTimeMs(long hysteresisTimeMs) {
		this.hysteresisTimeMs = hysteresisTimeMs;
	}

	public RunnableEvent take() throws InterruptedException {
		final RunnableEvent take = eventQueue.take();
		property(WaldotRulesEnginePlugin.QUEUE_SIZE_LABEL, eventQueue.size());
		cleanUpIfNeeded();
		return take;
	}

}
