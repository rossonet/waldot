package net.rossonet.waldot.rules.vertices;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;

import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
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

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;
import net.rossonet.waldot.rules.WaldotRulesEnginePlugin;
import net.rossonet.waldot.rules.events.FireableAction;
import net.rossonet.waldot.rules.events.RunnableEvent;
import net.rossonet.waldot.utils.ThreadHelper;

public class ComputeVertex extends AbstractOpcVertex implements AutoCloseable {

	private final class ComputeThreadManager implements Runnable {

		@Override
		public void run() {
			while (isActive()) {
				try {
					for (final Entry<Future<?>, FireableAction> active : runners.entrySet()) {
						if (active.getKey().isDone()) {
							runners.remove(active.getKey());
						}
						if (active.getValue().getStartingTime() + executionTimeoutMs < System.currentTimeMillis()) {
							logger.warn("Action for node " + active.getValue()
									+ " is taking too long to execute. Cancelling...");
							active.getKey().cancel(true);
							runners.remove(active.getKey());
						}
					}
					if (runners.size() >= threads) {
						Thread.sleep(100); // attende un po' prima di controllare di nuovo
						continue;
					}
					final DirtyNode dirty = dirtyNodes.take();
					final FireableAbstractOpcVertex activeVertex = servedFireableNodes.get(dirty.getNodeId());
					RunnableEvent newEvent = activeVertex.poll();
					while (newEvent != null) {
						final FireableAction action = newEvent.getAction(System.currentTimeMillis());
						runners.put(executor.submit(() -> {
							try {
								action.run();
							} catch (final Throwable t) {
								logger.error("Error executing action for node " + dirty.getNodeId(), t);
							}
						}), action);
						newEvent = activeVertex.poll();
					}
				} catch (final Throwable t) {
					logger.error("Error in ComputeThreadManager", t);
				}
			}
		}

	}

	private final class DirtyNode implements Comparable<DirtyNode> {

		private final NodeId nodeId;
		private final int weight;

		public DirtyNode(NodeId nodeId, int executorEdgePriority, int queueSize) {
			this.nodeId = nodeId;
			this.weight = calcolateWeight(executorEdgePriority, queueSize);
		}

		@Override
		public int compareTo(DirtyNode other) {
			return Integer.compare(this.getWeight(), other.getWeight());
		}

		public NodeId getNodeId() {
			return nodeId;
		}

		public int getWeight() {
			return weight;
		}

	}

	public static void generateParameters(WaldotNamespace waldotNamespace, UaObjectTypeNode dockerTypeNode) {
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode,
				WaldotRulesEnginePlugin.THREAD_POOL_SIZE_FIELD, NodeIds.Int16);
	}

	private boolean active = true;

	private String baseDirectory;

	private final PriorityBlockingQueue<DirtyNode> dirtyNodes = new PriorityBlockingQueue<>();
	private long executionTimeoutMs = WaldotRulesEnginePlugin.DEFAULT_EXECUTION_TIMEOUT_MS_IN_COMPUTE;
	private final QualifiedProperty<Long> executionTimeoutMsProperty;
	private final ExecutorService executor;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private double priorityFactor = WaldotRulesEnginePlugin.DEFAULT_PRIORITY_FACTOR_IN_COMPUTE;
	private final QualifiedProperty<Double> priorityFactorProperty;
	public Map<Future<?>, FireableAction> runners = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<NodeId, FireableAbstractOpcVertex> servedFireableNodes = new ConcurrentHashMap<>();

	private final Future<?> threadManager;

	private int threads = WaldotRulesEnginePlugin.DEFAULT_THREAD_POOL_SIZE_IN_COMPUTE;

	private final QualifiedProperty<Integer> threadsProperty;

	protected final WaldotNamespace waldotNamespace;

	public ComputeVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId, QualifiedName browseName,
			LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			UByte eventNotifier, long version, Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		waldotNamespace = graph.getWaldotNamespace();
		baseDirectory = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				MiloStrategy.DIRECTORY_PARAMETER.toLowerCase());
		if (baseDirectory == null || baseDirectory.isEmpty()) {
			baseDirectory = "compute";
		}
		final String keyValuesThreadPool = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotRulesEnginePlugin.THREAD_POOL_SIZE_FIELD.toLowerCase());
		if (keyValuesThreadPool != null && !keyValuesThreadPool.isEmpty()) {
			try {
				threads = Integer.valueOf(keyValuesThreadPool);
			} catch (final NumberFormatException e) {
				threads = WaldotRulesEnginePlugin.DEFAULT_THREAD_POOL_SIZE_IN_COMPUTE;
				logger.warn("Invalid threads number provided: " + keyValuesThreadPool + ". Using default threads "
						+ threads);
			}
		}
		threadsProperty = new QualifiedProperty<Integer>(getNamespace().getNamespaceUri(),
				WaldotRulesEnginePlugin.THREAD_POOL_SIZE_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				Integer.class);

		final String keyValuesExecutionTimeoutMs = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotRulesEnginePlugin.EXECUTION_TIMEOUT_MS_FIELD.toLowerCase());
		if (keyValuesExecutionTimeoutMs != null && !keyValuesExecutionTimeoutMs.isEmpty()) {
			try {
				executionTimeoutMs = Long.valueOf(keyValuesExecutionTimeoutMs);
			} catch (final NumberFormatException e) {
				executionTimeoutMs = WaldotRulesEnginePlugin.DEFAULT_EXECUTION_TIMEOUT_MS_IN_COMPUTE;
				logger.warn("Invalid execution timeout ms provided: " + keyValuesExecutionTimeoutMs
						+ ". Using default execution timeout ms " + executionTimeoutMs);
			}
		}
		executionTimeoutMsProperty = new QualifiedProperty<Long>(getNamespace().getNamespaceUri(),
				WaldotRulesEnginePlugin.EXECUTION_TIMEOUT_MS_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				Long.class);
		setProperty(executionTimeoutMsProperty, executionTimeoutMs);

		final String keyValuesPriorityFactorMs = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotRulesEnginePlugin.PRIORITY_FACTOR_FIELD.toLowerCase());
		if (keyValuesPriorityFactorMs != null && !keyValuesPriorityFactorMs.isEmpty()) {
			try {
				priorityFactor = Double.valueOf(keyValuesPriorityFactorMs);
			} catch (final NumberFormatException e) {
				priorityFactor = WaldotRulesEnginePlugin.DEFAULT_PRIORITY_FACTOR_IN_COMPUTE;
				logger.warn("Invalid priority factor provided: " + keyValuesPriorityFactorMs
						+ ". Using default priority factor " + priorityFactor);
			}
		}
		priorityFactorProperty = new QualifiedProperty<Double>(getNamespace().getNamespaceUri(),
				WaldotRulesEnginePlugin.PRIORITY_FACTOR_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				Double.class);
		setProperty(priorityFactorProperty, priorityFactor);
		// avvia i servizi di gestione dei thread per l'esecuzione dei nodi
		executor = ThreadHelper.newVirtualThreadExecutor();
		threadManager = executor.submit(new ComputeThreadManager());
	}

	public int calcolateWeight(int executorEdgePriority, int queueSize) {
		// calcola il peso del nodo sporco in base alla priorità dell'arco
		// dell'esecutore e alla dimensione della coda
		// ad esempio, puoi dare più peso alla priorità dell'arco rispetto alla
		// dimensione della coda
		// in questo primo esempio, moltiplichiamo la priorità dell'arco per un fattore
		// e aggiungiamo la dimensione della coda
		return (int) (executorEdgePriority * priorityFactor + queueSize);
	}

	@Override
	public Object clone() {
		return new ComputeVertex(graph, getNodeContext(), getNodeId(), getBrowseName(), getDisplayName(),
				getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version(),
				getPropertiesAsStringArray());
	}

	@Override
	public void close() throws Exception {
		active = false;
		for (final Future<?> active : runners.keySet()) {
			if (!active.isDone()) {
				active.cancel(true);
			}
		}
		threadManager.cancel(true);
		dirtyNodes.clear();
		runners.clear();
		executor.shutdownNow();
	}

	public boolean isActive() {
		return active;
	}

	@Override
	public void notifyPropertyValueChanging(String label, DataValue value) {
		super.notifyPropertyValueChanging(label, value);
		if (label.equals(WaldotRulesEnginePlugin.THREAD_POOL_SIZE_FIELD.toLowerCase())) {
			final int newThreadsValue = Integer.valueOf(value.getValue().getValue().toString());
			threads = newThreadsValue;
			setProperty(threadsProperty, newThreadsValue);
		}
		if (label.equals(WaldotRulesEnginePlugin.EXECUTION_TIMEOUT_MS_FIELD.toLowerCase())) {
			final long newExecutionTimeoutMsValue = Long.valueOf(value.getValue().getValue().toString());
			executionTimeoutMs = newExecutionTimeoutMsValue;
			setProperty(executionTimeoutMsProperty, newExecutionTimeoutMsValue);
		}
		if (label.equals(WaldotRulesEnginePlugin.PRIORITY_FACTOR_FIELD.toLowerCase())) {
			final double newPriorityFactorValue = Double.valueOf(value.getValue().getValue().toString());
			priorityFactor = newPriorityFactorValue;
			setProperty(priorityFactorProperty, newPriorityFactorValue);
		}

	}

	public void notifyQueueSizeChange(UaNode sourceNode, String queueSizeLabel, Object queueSize,
			int executorEdgePriority) {
		if (isActive() && queueSize instanceof Number) {
			final int intSize = ((Number) queueSize).intValue();
			if (intSize > 0 && !dirtyNodes.stream()
					.anyMatch(dirtyNode -> dirtyNode.getNodeId().equals(sourceNode.getNodeId()))) {
				if (!servedFireableNodes.containsKey(sourceNode.getNodeId())) {
					servedFireableNodes.put(sourceNode.getNodeId(), (FireableAbstractOpcVertex) sourceNode);
				}
				dirtyNodes.offer(new DirtyNode(sourceNode.getNodeId(), executorEdgePriority, intSize));
			} else {
				// rimuovi il nodo sporco se la coda è vuota
				dirtyNodes.removeIf(dirtyNode -> dirtyNode.getNodeId().equals(sourceNode.getNodeId()));
				servedFireableNodes.remove(sourceNode.getNodeId());
			}
		}
	}

}
