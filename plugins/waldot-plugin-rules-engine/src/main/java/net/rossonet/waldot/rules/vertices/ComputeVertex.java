package net.rossonet.waldot.rules.vertices;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.rules.RulesEngineConstants;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.exception.PluginBlockingException;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.plugin.EdgeMonitoredVertex;
import net.rossonet.waldot.rules.edges.ComputeMonitoredEdge;
import net.rossonet.waldot.rules.events.FireableAction;
import net.rossonet.waldot.rules.events.RunnableEvent;
import net.rossonet.waldot.utils.ThreadHelper;

/**
 * Thread manager vertex that executes rule actions from multiple RuleVertices.
 * <p>
 * ComputeVertex è il "motore di esecuzione" del sistema di regole. Gestisce un pool
 * di thread virtuali che eseguono le azioni delle regole con priorità e timeout.
 * Riceve notifiche dai RuleVertex tramite ComputeMonitoredEdge quando ci sono eventi
 * in coda da elaborare.
 * </p>
 * 
 * <h2>Architecture</h2>
 * <pre>
 * [ComputeVertex] --rules:execute--&gt; [RuleVertex 1]
 *        |         --rules:execute--&gt; [RuleVertex 2]
 *        |         --rules:execute--&gt; [RuleVertex N]
 *        v
 *  [dirty rules queue, ordered by weight]
 *        |
 *        v
 *  [virtual threads, at most "threads" at the same time]
 * </pre>
 * The edge goes from the compute vertex (source) to the rule (target). When the edge is created the
 * vertex installs a {@link ComputeMonitoredEdge}; an edge in the opposite direction, or towards a vertex that
 * is not a rule, is rejected.
 * 
 * <h2>Execution Flow</h2>
 * <ol>
 *   <li><b>Queue notification</b>: the rule publishes its <code>queue</code> size and the edge notifies this vertex</li>
 *   <li><b>Priority calculation</b>: weight = edge_priority * factor + queue_size</li>
 *   <li><b>Dirty rules queue</b>: the rule is queued by weight (the highest weight is served first)</li>
 *   <li><b>Thread manager</b>: when a slot is free it takes the heaviest rule and polls <b>one</b> event from it</li>
 *   <li><b>Action execution</b>: the event is submitted to the virtual thread pool; if the rule still has
 *       events it is queued again with its new weight, so the rules are served in turn</li>
 *   <li><b>Timeout monitoring</b>: actions exceeding the execution timeout are cancelled</li>
 * </ol>
 * 
 * <h2>Priority Mechanism</h2>
 * <ul>
 *   <li><b>Edge priority</b>: property <code>priority</code> of the <code>rules:execute</code> edge</li>
 *   <li><b>Queue size</b>: number of pending events in the rule</li>
 *   <li><b>Priority factor</b>: multiplier of the edge priority (default: 100.0)</li>
 * </ul>
 * <p>
 * Formula: <code>weight = edge_priority * priority_factor + queue_size</code>.
 * Higher weight = served first.
 * </p>
 * 
 * <h2>Thread Management</h2>
 * <p>
 * The manager thread is started when the first rule needs it, so a vertex that is never used does not
 * consume threads. All the limits are read at every cycle, so they can be changed at runtime.
 * </p>
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Vertex compute = graph.addVertex(
 *     "type", "rules:compute",
 *     "label", "main-compute",
 *     "threads", "4",
 *     "execution-timeout-ms", "60000",
 *     "factor", "100.0"
 * );
 * 
 * compute.addEdge("runner", rule1, "type", "rules:execute", "priority", "100");
 * compute.addEdge("runner", rule2, "type", "rules:execute", "priority", "10");
 * }</pre>
 * 
 * <h2>Properties</h2>
 * <ul>
 *   <li><b>threads</b>: max actions executed at the same time (default: 1, minimum 1)</li>
 *   <li><b>execution-timeout-ms</b>: action timeout in ms (default: 120000)</li>
 *   <li><b>factor</b>: priority factor multiplier (default: 100.0)</li>
 *   <li><b>queue</b>: number of rules with events awaiting execution</li>
 * </ul>
 * Invalid values (not a number, below the minimum) are replaced by the default.
 * 
 * @see RuleVertex
 * @see ComputableFireableAbstractOpcVertex
 * 
 * @author Andrea Ambrosini - Zona Autonoma srl
 * @since 0.4.0
 */
public class ComputeVertex extends AbstractOpcVertex implements AutoCloseable, EdgeMonitoredVertex {

	/**
	 * Thread manager runnable that polls dirty nodes and executes actions.
	 */
	private final class ComputeThreadManager implements Runnable {

		@Override
		public void run() {
			while (isActive()) {
				try {
					reapRunners();
					if (runners.size() >= getThreads()) {
						// pool pieno: attende che si liberi uno slot senza bloccare i controlli di timeout
						Thread.sleep(FULL_POOL_WAIT_MS);
						continue;
					}
					final DirtyNode dirty = dirtyNodes.poll(MANAGER_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
					if (dirty == null) {
						continue;
					}
					final ComputableFireableAbstractOpcVertex vertex = servedFireableNodes.get(dirty.getNodeId());
					if (vertex == null) {
						// la regola è stata scollegata nel frattempo
						continue;
					}
					// un solo evento per volta: la regola, se ha ancora eventi, rientra in coda
					// con il suo nuovo peso e le altre regole non restano indietro
					final RunnableEvent newEvent = vertex.poll();
					if (newEvent != null) {
						submit(dirty.getNodeId(), newEvent);
					}
				} catch (final InterruptedException e) {
					if (!isActive()) {
						Thread.currentThread().interrupt();
						return;
					}
				} catch (final Throwable t) {
					logger.error("Error in ComputeThreadManager", t);
				}
			}
		}

	}

	private static final class DirtyNode implements Comparable<DirtyNode> {

		private static final AtomicLong SEQ = new AtomicLong();

		private final NodeId nodeId;
		private final long sequence = SEQ.getAndIncrement();
		private final int weight;

		public DirtyNode(NodeId nodeId, int weight) {
			this.nodeId = nodeId;
			this.weight = weight;
		}

		/**
		 * Higher weight first; the same weight is served in arrival order.
		 */
		@Override
		public int compareTo(DirtyNode other) {
			final int byWeight = Integer.compare(other.weight, this.weight);
			return byWeight != 0 ? byWeight : Long.compare(this.sequence, other.sequence);
		}

		public NodeId getNodeId() {
			return nodeId;
		}

		public int getWeight() {
			return weight;
		}

	}

	private static final long FULL_POOL_WAIT_MS = 5;

	private static final long MANAGER_POLL_INTERVAL_MS = 50;

	public static void generateParameters(WaldotNamespace waldotNamespace, UaObjectTypeNode typeNode) {
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode,
				RulesEngineConstants.THREAD_POOL_SIZE_FIELD, NodeIds.Int32,
				RulesEngineConstants.THREAD_POOL_SIZE_FIELD_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode,
				RulesEngineConstants.EXECUTION_TIMEOUT_MS_FIELD, NodeIds.Int64,
				RulesEngineConstants.EXECUTION_TIMEOUT_MS_FIELD_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode,
				RulesEngineConstants.PRIORITY_FACTOR_FIELD, NodeIds.Double,
				RulesEngineConstants.PRIORITY_FACTOR_FIELD_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode, RulesEngineConstants.QUEUE_SIZE_LABEL,
				NodeIds.Int64, RulesEngineConstants.COMPUTE_QUEUE_LABEL_DESCRIPTION);
	}

	private volatile boolean active = true;

	private final PriorityBlockingQueue<DirtyNode> dirtyNodes = new PriorityBlockingQueue<>();

	private ExecutorService executor;

	private final Object lifecycleLock = new Object();

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Object publishLock = new Object();

	private final Map<Future<?>, FireableAction> runners = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<NodeId, ComputableFireableAbstractOpcVertex> servedFireableNodes = new ConcurrentHashMap<>();

	private Future<?> threadManager;

	protected final WaldotNamespace waldotNamespace;

	public ComputeVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId, QualifiedName browseName,
			LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			UByte eventNotifier, long version, Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		waldotNamespace = graph.getWaldotNamespace();
		addManagedProperty(RulesEngineConstants.THREAD_POOL_SIZE_FIELD, NodeIds.Int32, Integer.class, false,
				RulesEngineConstants.DEFAULT_THREAD_POOL_SIZE_IN_COMPUTE, null, false, propertyKeyValues);
		addManagedProperty(RulesEngineConstants.EXECUTION_TIMEOUT_MS_FIELD, NodeIds.Int64, Long.class, false,
				RulesEngineConstants.DEFAULT_EXECUTION_TIMEOUT_MS_IN_COMPUTE, null, false, propertyKeyValues);
		addManagedProperty(RulesEngineConstants.PRIORITY_FACTOR_FIELD, NodeIds.Double, Double.class, false,
				RulesEngineConstants.DEFAULT_PRIORITY_FACTOR_IN_COMPUTE, null, false, propertyKeyValues);
		addManagedProperty(RulesEngineConstants.QUEUE_SIZE_LABEL, NodeIds.Int64, Long.class, false, 0L, null,
				false, propertyKeyValues);
	}

	public int calcolateWeight(int executorEdgePriority, int queueSize) {
		final double weight = executorEdgePriority * getPriorityFactor() + queueSize;
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.round(weight)));
	}

	@Override
	public Object clone() {
		return new ComputeVertex(graph, getNodeContext(), getNodeId(), getBrowseName(), getDisplayName(),
				getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version(),
				getPropertiesAsStringArray());
	}

	@Override
	public void close() throws Exception {
		synchronized (lifecycleLock) {
			active = false;
			for (final Future<?> running : runners.keySet()) {
				if (!running.isDone()) {
					running.cancel(true);
				}
			}
			if (threadManager != null) {
				threadManager.cancel(true);
			}
			if (executor != null) {
				executor.shutdownNow();
			}
		}
		dirtyNodes.clear();
		runners.clear();
		servedFireableNodes.clear();
	}

	private void ensureStarted() {
		synchronized (lifecycleLock) {
			if (active && executor == null) {
				executor = ThreadHelper.newVirtualThreadExecutor();
				threadManager = executor.submit(new ComputeThreadManager());
			}
		}
	}

	/**
	 * Stops serving a rule: removes it from the queue of the rules to execute.
	 * Called when the <code>rules:execute</code> edge is removed.
	 * 
	 * @param ruleNodeId node id of the rule
	 */
	public void forget(NodeId ruleNodeId) {
		servedFireableNodes.remove(ruleNodeId);
		synchronized (dirtyNodes) {
			dirtyNodes.removeIf(dirtyNode -> dirtyNode.getNodeId().equals(ruleNodeId));
		}
		publishQueueSize();
	}

	/**
	 * Returns the timeout of an action in ms; invalid values give the default.
	 * 
	 * @return execution timeout in ms
	 */
	public long getExecutionTimeoutMs() {
		final Long value = getRuntimeParameter(RulesEngineConstants.EXECUTION_TIMEOUT_MS_FIELD);
		return value == null || value <= 0
				? RulesEngineConstants.DEFAULT_EXECUTION_TIMEOUT_MS_IN_COMPUTE
				: value;
	}

	/**
	 * Returns the number of rules waiting to be executed.
	 * 
	 * @return pending rules
	 */
	public int getPendingRulesCount() {
		return dirtyNodes.size();
	}

	/**
	 * Returns the multiplier of the edge priority; invalid values give the default.
	 * 
	 * @return priority factor
	 */
	public double getPriorityFactor() {
		final Double value = getRuntimeParameter(RulesEngineConstants.PRIORITY_FACTOR_FIELD);
		return value == null || value.isNaN() || value.isInfinite() || value < 0
				? RulesEngineConstants.DEFAULT_PRIORITY_FACTOR_IN_COMPUTE
				: value;
	}

	/**
	 * Returns the number of actions running now.
	 * 
	 * @return running actions
	 */
	public int getRunningCount() {
		reapRunners();
		return runners.size();
	}

	/**
	 * Returns the max number of actions executed at the same time; invalid values give the default.
	 * 
	 * @return thread pool size
	 */
	public int getThreads() {
		final Integer value = getRuntimeParameter(RulesEngineConstants.THREAD_POOL_SIZE_FIELD);
		return value == null || value < 1 ? RulesEngineConstants.DEFAULT_THREAD_POOL_SIZE_IN_COMPUTE : value;
	}

	public boolean isActive() {
		return active;
	}

	/**
	 * Installs the {@link ComputeMonitoredEdge} for a <code>rules:execute</code> edge going out of this vertex.
	 */
	@Override
	public void notifyAddTargetMonitoredEdge(WaldotEdge edge, WaldotVertex targetVertex, String label, String type,
			Object[] propertyKeyValues) throws PluginBlockingException {
		if (!RulesEngineConstants.EXECUTE_EDGE_LABEL.equals(type)) {
			return;
		}
		if (!(targetVertex instanceof ComputableFireableAbstractOpcVertex)) {
			throw new PluginBlockingException(
					"The target of a " + type + " edge must be a rule vertex, but it is " + targetVertex);
		}
		servedFireableNodes.put(targetVertex.getNodeId(), (ComputableFireableAbstractOpcVertex) targetVertex);
		edge.setMonitor(new ComputeMonitoredEdge(getNamespace(), edge, this, targetVertex));
	}

	/**
	 * A compute vertex cannot be the target of a <code>rules:execute</code> edge.
	 */
	@Override
	public void notifyAddSourceMonitoredEdge(WaldotEdge edge, WaldotVertex sourceVertex, String label, String type,
			Object[] propertyKeyValues) throws PluginBlockingException {
		if (RulesEngineConstants.EXECUTE_EDGE_LABEL.equals(type)) {
			throw new PluginBlockingException("A " + type
					+ " edge must go from the compute vertex to the rule: a compute vertex cannot be its target");
		}
	}

	/**
	 * Called by the {@link ComputeMonitoredEdge} when the queue of a rule changes.
	 * The rule is queued again with a weight that follows the new size, or removed if it is empty.
	 * 
	 * @param sourceNode rule that changed
	 * @param queueSizeLabel label of the property
	 * @param queueSize new size of the queue of the rule
	 * @param executorEdgePriority priority of the edge
	 */
	public void notifyQueueSizeChange(UaNode sourceNode, String queueSizeLabel, DataValue queueSize,
			int executorEdgePriority) {
		final Object size = queueSize == null || queueSize.getValue() == null ? null
				: queueSize.getValue().getValue();
		if (!isActive() || !(size instanceof Number) || !(sourceNode instanceof ComputableFireableAbstractOpcVertex)) {
			return;
		}
		final int intSize = ((Number) size).intValue();
		final NodeId ruleId = sourceNode.getNodeId();
		synchronized (dirtyNodes) {
			dirtyNodes.removeIf(dirtyNode -> dirtyNode.getNodeId().equals(ruleId));
			if (intSize > 0) {
				servedFireableNodes.put(ruleId, (ComputableFireableAbstractOpcVertex) sourceNode);
				dirtyNodes.offer(new DirtyNode(ruleId, calcolateWeight(executorEdgePriority, intSize)));
			}
		}
		if (intSize > 0) {
			ensureStarted();
		}
		publishQueueSize();
	}

	private void publishQueueSize() {
		synchronized (publishLock) {
			if (!isRemoved()) {
				property(RulesEngineConstants.QUEUE_SIZE_LABEL, Long.valueOf(dirtyNodes.size()));
			}
		}
	}

	/**
	 * Forgets the finished actions and cancels the ones over the timeout.
	 */
	private void reapRunners() {
		final long now = System.currentTimeMillis();
		final long timeout = getExecutionTimeoutMs();
		for (final Entry<Future<?>, FireableAction> running : runners.entrySet()) {
			final Future<?> future = running.getKey();
			if (future.isDone()) {
				runners.remove(future);
			} else if (running.getValue().getStartingTime() + timeout < now) {
				logger.warn("Action started at {} is taking more than {} ms to execute. Cancelling...",
						running.getValue().getStartingTime(), timeout);
				future.cancel(true);
				runners.remove(future);
			}
		}
	}

	private void submit(NodeId ruleId, RunnableEvent event) {
		final FireableAction action = event.getAction(System.currentTimeMillis());
		final FutureTask<Void> task = new FutureTask<>(() -> {
			try {
				action.run();
			} catch (final Throwable t) {
				logger.error("Error executing action for node " + ruleId, t);
			}
		}, null);
		// l'azione è registrata prima di partire: il conteggio delle azioni in corso non ha buchi
		runners.put(task, action);
		executor.execute(task);
	}

}
