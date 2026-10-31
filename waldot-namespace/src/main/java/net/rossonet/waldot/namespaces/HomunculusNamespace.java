package net.rossonet.waldot.namespaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Graph.Variables;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.typetree.ReferenceTypeTree;
import org.eclipse.milo.opcua.sdk.server.EventNotifier;
import org.eclipse.milo.opcua.sdk.server.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.ObjectTypeManager;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.items.EventItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.factories.EventFactory;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadResult;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;
import net.rossonet.waldot.api.models.IdManager;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotGraphComputerView;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotProperty;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;
import net.rossonet.waldot.api.strategies.BootstrapStrategy;
import net.rossonet.waldot.api.strategies.ClientManagementStrategy;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.api.strategies.HistoryStrategy;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.client.auth.ClientRegisterAnonymousValidator;
import net.rossonet.waldot.client.auth.ClientRegisterUsernameIdentityValidator;
import net.rossonet.waldot.client.auth.ClientRegisterX509IdentityValidator;
import net.rossonet.waldot.commands.AboutCommand;
import net.rossonet.waldot.commands.AddFireMonitoredEdgeCommand;
import net.rossonet.waldot.commands.AddLinkMonitoredEdgeCommand;
import net.rossonet.waldot.commands.ClearGraphCommand;
import net.rossonet.waldot.commands.DeleteDirectory;
import net.rossonet.waldot.commands.DescribeElementCommand;
import net.rossonet.waldot.commands.ExportGraphCommand;
import net.rossonet.waldot.commands.HelpCommand;
import net.rossonet.waldot.commands.ImportGraphCommand;
import net.rossonet.waldot.commands.QueryCommand;
import net.rossonet.waldot.commands.QueryJsonCommand;
import net.rossonet.waldot.commands.ReadGraphBlockCommand;
import net.rossonet.waldot.commands.RenderGraphCommand;
import net.rossonet.waldot.commands.exchange.GraphTransferStore;
import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphVariables;
import net.rossonet.waldot.jexl.JexlCmdFunction;
import net.rossonet.waldot.logger.TraceLogger;
import net.rossonet.waldot.logger.TraceLogger.ContexLogger;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;
import net.rossonet.waldot.opc.WaldotOpcUaServer;
import net.rossonet.waldot.utils.ThreadHelper;

public class HomunculusNamespace extends ManagedNamespaceWithLifecycle
		implements
			WaldotNamespace {

	private ClientRegisterAnonymousValidator agentAnonymousValidator;
	private final AtomicBoolean resetInProgress = new AtomicBoolean(false);
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private ClientRegisterUsernameIdentityValidator agentIdentityValidator;
	private ClientRegisterX509IdentityValidator agentX509IdentityValidator;

	private final Logger bootLogger = new TraceLogger(ContexLogger.BOOT);
	private final BootstrapStrategy bootstrapProcedureStrategy;
	private final String bootstrapUrl;
	private final ClientManagementStrategy clientManagementStrategy;
	private final WaldotConfiguration configuration;
	private final Logger consoleLogger = new TraceLogger(ContexLogger.CONSOLE);

	private final ConsoleStrategy consoleStrategy;
	// private final DataTypeManager dictionaryManager;
	private final WaldotGraph gremlin;
	private final GraphTransferStore graphTransfers = new GraphTransferStore();
	private final HistoryStrategy historyStrategy;
	private final JexlCmdFunction jexlWaldotCommandHelper;
	private final List<NamespaceListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Graph.Variables opcGraphVariables;
	private final MiloStrategy opcMappingStrategy;
	private final Set<NodeId> reservedIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private final Set<PluginListener> plugins = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private final SubscriptionModel subscriptionModel;

	private final ScheduledExecutorService timer = ThreadHelper
			.newVirtualSchedulerExecutor("timer");

	private WaldotOpcUaServer waldotOpcUaServer;

	public HomunculusNamespace(final WaldotOpcUaServer server,
			final MiloStrategy opcMappingStrategy,
			HistoryStrategy historyStrategy,
			final ConsoleStrategy consoleStrategy,
			final WaldotConfiguration configuration,
			final BootstrapStrategy bootstrapProcedureStrategy,
			final ClientManagementStrategy agentManagementStrategy,
			final String bootstrapUrl) {
		this(server, opcMappingStrategy, historyStrategy, consoleStrategy, configuration,
				bootstrapProcedureStrategy, agentManagementStrategy, bootstrapUrl, null);
	}

	/**
	 * @param gremlinConfiguration configuration of the Gremlin graph (e.g.
	 *                             {@code gremlin.opcgraph.defaultVertexPropertyCardinality});
	 *                             {@code null} for the defaults
	 */
	public HomunculusNamespace(final WaldotOpcUaServer server,
			final MiloStrategy opcMappingStrategy,
			HistoryStrategy historyStrategy,
			final ConsoleStrategy consoleStrategy,
			final WaldotConfiguration configuration,
			final BootstrapStrategy bootstrapProcedureStrategy,
			final ClientManagementStrategy agentManagementStrategy,
			final String bootstrapUrl,
			final Configuration gremlinConfiguration) {
		super(server.getServer(), configuration.getManagerNamespaceUri());
		this.waldotOpcUaServer = server;
		this.opcMappingStrategy = opcMappingStrategy;
		this.historyStrategy = historyStrategy;
		this.historyStrategy.setNamespace(this);
		this.consoleStrategy = consoleStrategy;
		this.jexlWaldotCommandHelper = new JexlCmdFunction(this);
		this.configuration = configuration;
		this.bootstrapProcedureStrategy = bootstrapProcedureStrategy;
		this.clientManagementStrategy = agentManagementStrategy;
		this.bootstrapUrl = bootstrapUrl;
		opcGraphVariables = new OpcGraphVariables(this);
		subscriptionModel = new SubscriptionModel(server.getServer(), this);
		// dictionaryManager = new DefaultDataTypeManager();
		// getLifecycleManager().addLifecycle(dictionaryManager);
		getLifecycleManager().addLifecycle(subscriptionModel);
		getLifecycleManager().addStartupTask(this::runBootstrapProcedure);
		gremlin = gremlinConfiguration == null ? OpcGraph.open() : OpcGraph.open(gremlinConfiguration);
		gremlin.setNamespace(this);
		agentManagementStrategy.initialize(this);
		opcMappingStrategy.initialize(this);
		consoleStrategy.initialize(this);
		addBaseCommands();
		logger.info("HomunculusNamespace initialized");
		listeners.forEach(listener -> listener.onNamespaceCreated(this));
		bootstrapProcedureStrategy.initialize(this);
	}

	@Override
	public void addAssetAgentNode(UaNode assetManagerComponent) {
		getStorageManager().addNode(assetManagerComponent);
		clientManagementStrategy.getAssetClientsFolderNode()
				.addOrganizes(assetManagerComponent);
	}

	private void addBaseCommands() {
		if (configuration.getWaldotCommandLabel() != null) {
			registerCommand(new QueryCommand(this));
		}
		if (configuration.getHelpCommandLabel() != null) {
			registerCommand(new HelpCommand(this));
		}
		if (configuration.getAboutCommandLabel() != null) {
			registerCommand(new AboutCommand(this));
		}
		if (configuration.getDeleteDirectoryLabel() != null) {
			registerCommand(new DeleteDirectory(this));
		}
		registerCommand(new AddFireMonitoredEdgeCommand(this));
		registerCommand(new AddLinkMonitoredEdgeCommand(this));
		addDataExchangeCommands();
	}

	/**
	 * Commands used by the clients to exchange structured data: JSON query
	 * results, element descriptions, graph snapshots, graph drawings (see
	 * {@link DataExchangeProtocol}). Disabled with
	 * {@code -Dwaldot.commands.exchange.enabled=false}.
	 */
	private void addDataExchangeCommands() {
		if (!DataExchangeProtocol.isEnabled()) {
			logger.info("data exchange commands disabled");
			return;
		}
		registerCommand(new QueryJsonCommand(this));
		registerCommand(new DescribeElementCommand(this));
		registerCommand(new ExportGraphCommand(this, graphTransfers));
		registerCommand(new ReadGraphBlockCommand(this, graphTransfers));
		registerCommand(new ImportGraphCommand(this, graphTransfers));
		registerCommand(new ClearGraphCommand(this));
		registerCommand(new RenderGraphCommand(this, graphTransfers));
	}

	@Override
	public WaldotEdge addEdge(final WaldotVertex sourceVertex,
			final WaldotVertex targetVertex, final String label,
			final Object[] keyValues) {
		final Object[] safeKeyValues = keyValues == null
				? new Object[0]
				: keyValues;
		final Object readId = MiloStrategy.getIdValue(safeKeyValues)
				.orElse(null);
		if (readId == null) {
			return (WaldotEdge) opcMappingStrategy.addEdge(sourceVertex,
					targetVertex, label, safeKeyValues);
		}
		// explicit id: reserve it atomically before the creation
		final NodeId edgeId = MiloStrategy.getNodeIdManager()
				.convert(getGremlinGraph(), readId);
		if (edgeId == null) {
			return (WaldotEdge) opcMappingStrategy.addEdge(sourceVertex,
					targetVertex, label, safeKeyValues);
		}
		if (!reservedIds.add(edgeId)) {
			throw org.apache.tinkerpop.gremlin.structure.Graph.Exceptions
					.edgeWithIdAlreadyExists(edgeId);
		}
		try {
			if (hasNodeId(edgeId)) {
				throw org.apache.tinkerpop.gremlin.structure.Graph.Exceptions
						.edgeWithIdAlreadyExists(edgeId);
			}
			return (WaldotEdge) opcMappingStrategy.addEdge(sourceVertex,
					targetVertex, label, safeKeyValues);
		} finally {
			reservedIds.remove(edgeId);
		}
	}

	@Override
	public void addListener(final NamespaceListener listener) {
		listeners.add(listener);
	}

	@Override
	public WaldotVertex addVertex(final NodeId nodeId,
			final Object[] keyValues) {
		if (nodeId == null) {
			return opcMappingStrategy.addVertex(nodeId, keyValues);
		}
		// atomic reservation: two threads cannot create the same id
		if (!reservedIds.add(nodeId)) {
			throw org.apache.tinkerpop.gremlin.structure.Graph.Exceptions
					.vertexWithIdAlreadyExists(nodeId);
		}
		try {
			if (hasNodeId(nodeId)) {
				throw org.apache.tinkerpop.gremlin.structure.Graph.Exceptions
						.vertexWithIdAlreadyExists(nodeId);
			}
			return opcMappingStrategy.addVertex(nodeId, keyValues);
		} finally {
			reservedIds.remove(nodeId);
		}
	}

	/**
	 * Closes the namespace and releases every resource reachable from it.
	 * <p>
	 * Order: timer, bootstrap and console strategies; the
	 * {@link AutoCloseable} vertices of the graph (while the plugins, whose
	 * sessions and clients they may use, are still alive); every plugin with
	 * {@link PluginListener#stop()} followed by {@code close()} when it is
	 * {@link AutoCloseable}; a second pass on the vertices created meanwhile;
	 * client management, mapping and history strategies; the OPC UA server;
	 * the static registry of the reference nodes; the Gremlin graph.
	 * </p>
	 * <p>
	 * The vertices are not removed from the graph (no address space change and
	 * no event during the shutdown) and each one is closed at most once by the
	 * namespace. Every step is isolated: an exception is logged and the
	 * shutdown goes on. Steps longer than {@value #SLOW_CLOSE_STEP_MSEC} ms are
	 * logged with their duration. A second call does nothing.
	 * </p>
	 */
	@Override
	public void close() throws Exception {
		if (!closed.compareAndSet(false, true)) {
			return;
		}
		final long begin = System.nanoTime();
		// each close is isolated: a failing strategy must not prevent the
		// cleanup of the others
		if (timer != null) {
			try {
				timer.shutdownNow();
			} catch (final Exception e) {
				logger.warn("Error shutting down timer: {}", e.getMessage());
			}
		}
		closeQuietly("bootstrap procedure strategy", bootstrapProcedureStrategy);
		closeQuietly("console strategy", consoleStrategy);
		final Set<Object> closedVertices = java.util.Collections
				.newSetFromMap(new java.util.IdentityHashMap<>());
		final int firstPass = closeVertices(closedVertices);
		for (final PluginListener plugin : plugins) {
			closePlugin(plugin);
		}
		logger.info("all plugins stopped and closed");
		// vertices created concurrently (e.g. by a plugin pass running while
		// the namespace was closing)
		final int secondPass = closeVertices(closedVertices);
		closeQuietly("client management strategy", clientManagementStrategy);
		closeQuietly("opc mapping strategy", opcMappingStrategy);
		closeQuietly("history strategy", historyStrategy);
		timedStep("OPC UA server", () -> {
			if (waldotOpcUaServer != null) {
				waldotOpcUaServer.close();
				logger.info("opcua server closed");
			}
		});
		// release the static per-namespace registry, otherwise the closed
		// namespace (and its whole address space) is never garbage collected
		timedStep("reference nodes registry",
				() -> MiloSingleServerBaseReferenceNodeBuilder.unregister(this));
		// the Gremlin graph owns the service registry; closing it here covers
		// callers that only close the namespace. OpcGraph.close() is idempotent.
		closeQuietly("gremlin graph", gremlin);
		logger.info("namespace closed in {} ms ({} + {} vertices closed)",
				(System.nanoTime() - begin) / 1_000_000L, firstPass,
				secondPass);
	}

	/** Duration over which a close step is logged as slow. */
	private static final long SLOW_CLOSE_STEP_MSEC = 1_000L;

	@FunctionalInterface
	private interface CloseStep {
		void run() throws Exception;
	}

	/**
	 * Closes the plugin for good: {@link PluginListener#stop()} and then
	 * {@code close()} if it is {@link AutoCloseable}. Each call is isolated.
	 */
	private void closePlugin(final PluginListener plugin) {
		final String name = plugin.getClass().getSimpleName();
		timedStep("stop of plugin " + name, plugin::stop);
		if (plugin instanceof final AutoCloseable closeable) {
			timedStep("close of plugin " + name, closeable::close);
		}
	}

	/**
	 * Closes the {@link AutoCloseable} vertices of the graph not closed yet.
	 *
	 * @param alreadyClosed identity set of the vertices already closed,
	 *                      updated by the call
	 * @return number of vertices closed by this pass
	 */
	private int closeVertices(final Set<Object> alreadyClosed) {
		final List<WaldotVertex> snapshot;
		try {
			snapshot = new ArrayList<>(getVertices().values());
		} catch (final Throwable e) {
			rethrowIfFatal(e);
			logger.warn("Error reading the vertices to close: {}",
					e.getMessage());
			return 0;
		}
		int count = 0;
		for (final WaldotVertex vertex : snapshot) {
			if (vertex instanceof final AutoCloseable closeable
					&& alreadyClosed.add(vertex)) {
				count++;
				timedStep("vertex " + vertex.getNodeId() + " ("
						+ vertex.getClass().getSimpleName() + ")",
						closeable::close);
			}
		}
		return count;
	}

	private static void rethrowIfFatal(final Throwable e) {
		if (e instanceof final VirtualMachineError fatal) {
			throw fatal;
		}
	}

	/**
	 * Runs a close step: logs (and swallows) its failure, logs its duration
	 * when it is slow.
	 */
	private void timedStep(final String name, final CloseStep step) {
		final long start = System.nanoTime();
		try {
			step.run();
		} catch (final Throwable e) {
			rethrowIfFatal(e);
			logger.warn("Error closing {}: {}", name, e.toString(), e);
		} finally {
			final long elapsed = (System.nanoTime() - start) / 1_000_000L;
			if (elapsed > SLOW_CLOSE_STEP_MSEC) {
				logger.info("closing {} took {} ms", name, elapsed);
			}
		}
	}

	private void closeQuietly(final String name, final AutoCloseable resource) {
		timedStep(name, () -> {
			if (resource != null) {
				resource.close();
			}
			logger.info("{} closed", name);
		});
	}

	/**
	 * GraphComputer (OLAP) is <b>not supported</b>: the Milo strategies throw
	 * {@link UnsupportedOperationException}, so no view is ever created,
	 * {@link #getGraphComputerView()} returns {@code null} and
	 * {@link #inComputerMode()} is always {@code false}.
	 *
	 * @throws UnsupportedOperationException always, with the shipped strategies
	 */
	@Override
	public WaldotGraphComputerView createGraphComputerView(
			final WaldotGraph graph, final GraphFilter graphFilter,
			final Set<VertexComputeKey> object) {
		// delegated so that the strategy can log the request; the result is
		// deliberately not kept (see the Javadoc)
		return opcMappingStrategy.createGraphComputerView(graph, graphFilter,
				object);
	}

	@Override
	public <DATA_TYPE> WaldotProperty<DATA_TYPE> createOrUpdateWaldotEdgeProperty(
			final WaldotEdge waldotEdge, final String key,
			final DATA_TYPE value) {
		return opcMappingStrategy.createOrUpdateWaldotEdgeProperty(waldotEdge,
				key, value);
	}

	@Override
	public <DATA_TYPE> WaldotVertexProperty<DATA_TYPE> createOrUpdateWaldotVertexProperty(
			final WaldotVertex opcVertex, final String key,
			final DATA_TYPE value) {
		return opcMappingStrategy.createOrUpdateWaldotVertexProperty(opcVertex,
				key, value);
	}

	@Override
	public String deleteDirectory(String directoryNodeId) {
		return opcMappingStrategy.deleteOpcNodeId(directoryNodeId);
	}

	@Override
	public void dropGraphComputerView() {
		opcMappingStrategy.dropGraphComputerView();
	}

	@Override
	public NodeId generateNodeId(final Long nodeId) {
		return newNodeId(nodeId);
	}

	@Override
	public NodeId generateNodeId(final String nodeId) {
		return newNodeId(nodeId);
	}

	@Override
	public NodeId generateNodeId(final UInteger nodeId) {
		return newNodeId(nodeId);
	}

	@Override
	public NodeId generateNodeId(final UUID nodeId) {
		return newNodeId(nodeId);
	}

	@Override
	public QualifiedName generateQualifiedName(final String text) {
		return newQualifiedName(text);
	}

	@Override
	public Logger getBootLogger() {
		return bootLogger;
	}

	@Override
	public String getBootstrapUrl() {
		return bootstrapUrl;
	}

	@Override
	public ClientManagementStrategy getClientManagementStrategy() {
		return clientManagementStrategy;
	}

	@Override
	public Object getCommandsAsFunction() {
		return jexlWaldotCommandHelper;
	}

	@Override
	public WaldotConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public Logger getConsoleLogger() {
		return consoleLogger;
	}

	@Override
	public ConsoleStrategy getConsoleStrategy() {
		return consoleStrategy;
	}

	@Override
	public WaldotVertex getEdgeInVertex(final WaldotEdge opcWaldotEdge) {
		return opcMappingStrategy.getEdgeInVertex(opcWaldotEdge);
	}

	@Override
	public WaldotEdge getEdgeNode(final NodeId nodeId) {
		return getEdges().get(nodeId);
	}

	@Override
	public WaldotVertex getEdgeOutVertex(final WaldotEdge waldotEdge) {
		return opcMappingStrategy.getEdgeOutVertex(waldotEdge);
	}

	@Override
	public <DATA_TYPE> List<WaldotProperty<DATA_TYPE>> getEdgeProperties(
			final WaldotEdge waldotEdge) {
		return opcMappingStrategy.getProperties(waldotEdge);
	}

	@Override
	public <DATA_TYPE> WaldotProperty<DATA_TYPE> getEdgeProperty(
			final WaldotEdge waldotEdge, final String key) {
		return opcMappingStrategy.getEdgeProperty(waldotEdge, key);
	}

	@Override
	public Map<NodeId, WaldotEdge> getEdges() {
		return opcMappingStrategy.getEdges();
	}

	@Override
	public java.util.Collection<WaldotEdge> getEdges(final WaldotVertex vertex,
			final Direction direction, final String[] edgeLabels) {
		return opcMappingStrategy.getEdges(vertex, direction, edgeLabels);
	}

	@Override
	public int getEdgesCount() {
		return getEdges().size();
	}

	@Override
	public EventNotifier getEventBus() {
		return getServer().getEventNotifier();
	}

	@Override
	public EventFactory getEventFactory() {
		return getServer().getEventFactory();
	}

	/**
	 * @return always {@code null}: GraphComputer (OLAP) is not supported, see
	 *         {@link #createGraphComputerView(WaldotGraph, GraphFilter, Set)}
	 */
	@Override
	public WaldotGraphComputerView getGraphComputerView() {
		return null;
	}

	@Override
	public WaldotGraph getGremlinGraph() {
		return gremlin;
	}

	@Override
	public HistoryStrategy getHistoryStrategy() {
		return historyStrategy;
	}

	@Override
	public Collection<NamespaceListener> getListeners() {
		return listeners;
	}

	@Override
	public NamespaceTable getNamespaceTable() {
		return getServer().getNamespaceTable();
	}

	@Override
	public IdManager<NodeId> getNodeIdManager() {
		return MiloStrategy.getNodeIdManager();
	}

	@Override
	public ObjectTypeManager getObjectTypeManager() {
		return getServer().getObjectTypeManager();

	}

	@Override
	public UaNodeContext getOpcUaNodeContext() {
		return getNodeContext();
	}

	@Override
	public WaldotOpcUaServer getOpcuaServer() {
		return waldotOpcUaServer;
	}

	@Override
	public Set<PluginListener> getPlugins() {
		return Set.copyOf(plugins);
	}

	@Override
	public <DATA_TYPE> WaldotEdge getPropertyReference(
			final WaldotProperty<DATA_TYPE> opcProperty) {
		return opcMappingStrategy.getPropertyReference(opcProperty);
	}

	@Override
	public ReferenceTypeTree getReferenceTypes() {
		return getServer().getReferenceTypeTree();
	}

	@Override
	public UaNodeManager getStorageManager() {
		return getNodeManager();

	}

	@Override
	public ScheduledExecutorService getTimer() {
		return timer;
	}

	@Override
	public Graph.Variables getVariables() {
		return opcGraphVariables;
	}

	@Override
	public WaldotVertex getVertexNode(final NodeId nodeId) {
		return getVertices().get(nodeId);
	}

	@Override
	public <DATA_TYPE> Map<String, WaldotVertexProperty<DATA_TYPE>> getVertexProperties(
			final WaldotVertex opcVertex) {
		return opcMappingStrategy.getVertexProperties(opcVertex);
	}

	@Override
	public <DATA_TYPE> WaldotVertexProperty<DATA_TYPE> getVertexProperty(
			final WaldotVertex opcVertex, final String key) {
		return opcMappingStrategy.getVertexProperty(opcVertex, key);
	}

	@Override
	public <DATA_TYPE> WaldotVertex getVertexPropertyReference(
			final WaldotVertexProperty<DATA_TYPE> opcVertexProperty) {
		return opcMappingStrategy.getVertexPropertyReference(opcVertexProperty);
	}

	@Override
	public Map<NodeId, WaldotVertex> getVertices() {
		return opcMappingStrategy.getVertices();
	}

	@Override
	public java.util.Collection<WaldotVertex> getVertices(final WaldotVertex opcVertex,
			final Direction direction, final String[] edgeLabels) {
		return opcMappingStrategy.getVertices(opcVertex, direction, edgeLabels);
	}

	@Override
	public int getVerticesCount() {
		return getVertices().size();
	}

	@Override
	public boolean hasNodeId(final NodeId nodeId) {
		return getNodeManager().containsNode(nodeId);
	}

	@Override
	public List<HistoryReadResult> historyRead(HistoryReadContext context,
			HistoryReadDetails readDetails, TimestampsToReturn timestamps,
			List<HistoryReadValueId> readValueIds) {
		return historyStrategy.historyRead(context, readDetails, timestamps,
				readValueIds);
	}

	@Override
	public List<HistoryUpdateResult> historyUpdate(HistoryUpdateContext context,
			List<HistoryUpdateDetails> updateDetails) {
		return historyStrategy.historyUpdate(context, updateDetails);
	}

	/**
	 * @return always {@code false}: GraphComputer (OLAP) is not supported
	 */
	@Override
	public boolean inComputerMode() {
		return false;
	}

	@Override
	public Collection<String> listConfiguredCommands() {
		return consoleStrategy.listConfiguredCommands();
	}

	@Override
	public Object namespaceParametersGet(final String key) {
		return opcMappingStrategy.namespaceParametersGet(key);
	}

	@Override
	public Set<String> namespaceParametersKeySet() {
		return opcMappingStrategy.namespaceParametersKeySet();
	}

	@Override
	public void namespaceParametersPut(final String key, final Object value) {
		opcMappingStrategy.namespaceParametersPut(key, value);

	}

	@Override
	public void namespaceParametersRemove(final String key) {
		opcMappingStrategy.namespaceParametersRemove(key);

	}

	@Override
	public Variables namespaceParametersToVariables() {
		return opcMappingStrategy.namespaceParametersToVariables();
	}

	@Override
	public void onDataItemsCreated(final List<DataItem> dataItems) {
		subscriptionModel.onDataItemsCreated(dataItems);
		listeners.forEach(listener -> listener.onDataItemsCreated(dataItems));
		historyStrategy.onDataItemsCreated(dataItems);
	}

	@Override
	public void onDataItemsDeleted(final List<DataItem> dataItems) {
		subscriptionModel.onDataItemsDeleted(dataItems);
		listeners.forEach(listener -> listener.onDataItemsDeleted(dataItems));
		historyStrategy.onDataItemsDeleted(dataItems);
	}

	@Override
	public void onDataItemsModified(final List<DataItem> dataItems) {
		subscriptionModel.onDataItemsModified(dataItems);
		listeners.forEach(listener -> listener.onDataItemsModified(dataItems));
		historyStrategy.onDataItemsModified(dataItems);
	}

	@Override
	public void onEventItemsCreated(List<EventItem> eventItems) {
		eventItems.stream().filter(MonitoredItem::isSamplingEnabled)
				.forEach(item -> getServer().getEventNotifier().register(item));
	}

	@Override
	public void onEventItemsDeleted(List<EventItem> eventItems) {
		eventItems.forEach(
				item -> getServer().getEventNotifier().unregister(item));
	}

	@Override
	public void onEventItemsModified(List<EventItem> eventItems) {
		for (final EventItem item : eventItems) {
			if (item.isSamplingEnabled()) {
				getServer().getEventNotifier().register(item);
			} else {
				getServer().getEventNotifier().unregister(item);
			}
		}
	}

	@Override
	public void onMonitoringModeChanged(
			final List<MonitoredItem> monitoredItems) {
		subscriptionModel.onMonitoringModeChanged(monitoredItems);
		listeners.forEach(
				listener -> listener.onMonitoringModeChanged(monitoredItems));
	}

	@Override
	public void opcuaUpdateEvent(final UaNode sourceNode) {
		opcMappingStrategy.updateEventGenerator(sourceNode, "update", "update",
				"node " + sourceNode.getNodeId() + " updated", 0);
		listeners.forEach(listener -> listener.onUpdateNode(sourceNode));
		historyStrategy.opcuaUpdateEvent(sourceNode);
	}

	@Override
	public void registerAgentValidators(
			final ClientRegisterAnonymousValidator agentAnonymousValidator,
			final ClientRegisterUsernameIdentityValidator agentIdentityValidator,
			final ClientRegisterX509IdentityValidator agentX509IdentityValidator) {
		this.agentAnonymousValidator = agentAnonymousValidator;
		this.agentIdentityValidator = agentIdentityValidator;
		this.agentX509IdentityValidator = agentX509IdentityValidator;
		clientManagementStrategy.activate(agentAnonymousValidator,
				agentIdentityValidator, agentX509IdentityValidator);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The command must be an {@link AbstractOpcCommand}: the Milo strategies
	 * link it into the OPC UA address space as a method node. The type is
	 * checked before any registration, so a wrong command leaves neither the
	 * strategies nor the console in a half registered state.</p>
	 *
	 * @throws IllegalArgumentException if the command is {@code null} or not
	 *                                  an {@link AbstractOpcCommand}
	 */
	@Override
	public void registerCommand(final WaldotCommand command) {
		if (!(command instanceof final AbstractOpcCommand opcCommand)) {
			throw new IllegalArgumentException("command must extend "
					+ AbstractOpcCommand.class.getName() + ": "
					+ (command == null ? "null" : command.getClass().getName()));
		}
		opcMappingStrategy.registerCommand(opcCommand);
		consoleStrategy.registerCommand(opcCommand);
		getStorageManager().addNode(opcCommand);
		listeners.forEach(listener -> listener.onCommandRegistered(opcCommand));
	}

	@Override
	public void registerPlugin(final PluginListener plugin) {
		plugins.add(plugin);
		plugin.initialize(this);
		logger.info("Registering commands from plugin {}",
				plugin.getClass().getSimpleName());
		plugin.getCommands().forEach(command -> registerCommand(command));
	}

	@Override
	public void removeCommand(final WaldotCommand command) {
		opcMappingStrategy.removeCommand(command);
		consoleStrategy.removeCommand(command);
		getStorageManager().removeNode(command.getNodeId());
		listeners.forEach(listener -> listener.onCommandRemoved(command));
	}

	@Override
	public void removeEdge(final NodeId expandedNodeId) {
		opcMappingStrategy.removeEdge(expandedNodeId);

	}

	@Override
	public void removeListener(final NamespaceListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void removeReference(Reference reference) {
		opcMappingStrategy.removeReference(reference);

	}

	@Override
	public void removeVertex(final NodeId nodeId) {
		opcMappingStrategy.removeVertex(nodeId);

	}

	@Override
	public void removeVertexProperty(final NodeId nodeId) {
		opcMappingStrategy.removeVertexProperty(nodeId);

	}

	@Override
	public void resetNameSpace() {
		if (!resetInProgress.compareAndSet(false, true)) {
			// cmd.reset() called by the bootstrap that the reset is running
			logger.warn("Namespace reset already in progress, request ignored");
			return;
		}
		try {
			// remove every vertex and edge first (generators, rules and monitors
			// are stopped by their removal), then let the plugins clear their
			// state
			opcMappingStrategy.resetNameSpace();
			for (final PluginListener plugin : plugins) {
				// stop background work before clearing state; start() runs
				// again from the bootstrap below
				stopPlugin(plugin);
				try {
					plugin.reset();
				} catch (final Exception e) {
					logger.warn("Error resetting plugin {}: {}",
							plugin.getClass().getSimpleName(), e.getMessage());
				}
			}
			logger.info("Namespace reset done");
			listeners.forEach(listener -> listener.onNamespaceReset());
			logger.info("Running bootstrap procedure after reset");
			runBootstrapProcedure();
		} finally {
			resetInProgress.set(false);
		}
	}

	private void runBootstrapProcedure() {
		plugins.forEach(plugin -> plugin.start());
		bootstrapProcedureStrategy.runBootstrapProcedure();
		listeners.forEach(listener -> listener.onBootstrapProcedureCompleted());
	}

	@Override
	public Object runExpression(final String expression) {
		return consoleStrategy.runExpression(expression);
	}

	public void setOpcuaServer(final WaldotOpcUaServer opcuaServer) {
		this.waldotOpcUaServer = opcuaServer;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Namespace [VerticesCount=");
		builder.append(getVerticesCount());
		builder.append(", WaldotEdgesCount=");
		builder.append(getEdgesCount());
		builder.append("]");
		return builder.toString();
	}

	private void stopPlugin(final PluginListener plugin) {
		try {
			plugin.stop();
		} catch (final Exception e) {
			logger.warn("Error stopping plugin {}: {}",
					plugin.getClass().getSimpleName(), e.getMessage());
		}
	}

	@Override
	public void unregisterPlugin(final PluginListener plugin) {
		if (plugin == null || !plugins.remove(plugin)) {
			return;
		}
		stopPlugin(plugin);
		final Collection<WaldotCommand> commands = plugin.getCommands();
		if (commands == null) {
			return;
		}
		for (final WaldotCommand command : List.copyOf(commands)) {
			try {
				removeCommand(command);
			} catch (final Exception e) {
				logger.warn("Error removing command of plugin {}: {}",
						plugin.getClass().getSimpleName(), e.getMessage());
			}
		}
		logger.info("Plugin {} unregistered", plugin.getClass().getSimpleName());
	}

}