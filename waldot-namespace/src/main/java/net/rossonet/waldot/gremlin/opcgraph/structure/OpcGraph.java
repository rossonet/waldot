package net.rossonet.waldot.gremlin.opcgraph.structure;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotGraphComputerView;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.gremlin.opcgraph.process.traversal.strategy.optimization.OpcGraphCountStrategy;
import net.rossonet.waldot.gremlin.opcgraph.process.traversal.strategy.optimization.OpcGraphStepStrategy;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcCommandListServiceFactory;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcCommandServiceFactory;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcServiceRegistry;

/**
 * Gremlin graph backed by a WaldOT OPC UA namespace.
 *
 * <h2>OPC UA commands from Gremlin</h2> The service registry always contains
 * two services, usable with the TinkerPop {@code call()} step from any query
 * (JEXL console, rules, Gremlin Server):
 * <ul>
 * <li>{@value OpcCommandServiceFactory#NAME}: runs the commands of vertices and
 * edges (Barrier mode, after all the elements are collected) or the global
 * commands ({@code g.call(...)}), e.g.
 * {@code g.V(id).call("waldot.command", [command: "property", args: ["color", "red"]])}.
 * It is disabled unless {@code waldot.gremlin.commands.enabled=true} (namespace
 * configuration or system property, default {@code false}); commands run
 * without OPC UA session or user ({@code InvocationContext} {@code null}). See
 * {@link OpcCommandServiceFactory} for the result format.</li>
 * <li>{@value OpcCommandListServiceFactory#NAME}: lists the commands of the
 * elements or the global ones; always available.</li>
 * </ul>
 * Services configured with {@link #GREMLIN_OPCGRAPH_SERVICE} are registered
 * after them.
 *
 * @author Andrea Ambrosini ( - andrea DOT ambrosini AT rossonet DOT org - )
 */
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_INTEGRATE)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_LIMITED_STANDARD)
// GraphComputer (OLAP) is not supported: no *_COMPUTER suites
public class OpcGraph extends AbstractOpcGraph {

	public class OpcGraphFeatures implements Features {
		private final OpcGraphEdgeFeatures edgeFeatures;
		@SuppressWarnings("unused")
		private final AbstractOpcGraph graph;
		private final OpcGraphGraphFeatures graphFeatures;
		private final OpcGraphVertexFeatures vertexFeatures;

		private OpcGraphFeatures(final AbstractOpcGraph graph) {
			this.graph = graph;
			graphFeatures = new OpcGraphGraphFeatures();
			edgeFeatures = new OpcGraphEdgeFeatures(graph);
			vertexFeatures = new OpcGraphVertexFeatures(graph);
		}

		@Override
		public EdgeFeatures edge() {
			return edgeFeatures;
		}

		@Override
		public GraphFeatures graph() {
			return graphFeatures;
		}

		@Override
		public String toString() {
			return StringFactory.featureString(this);
		}

		@Override
		public VertexFeatures vertex() {
			return vertexFeatures;
		}

	}

	public class OpcGraphGraphFeatures implements Features.GraphFeatures {

		private OpcGraphGraphFeatures() {
		}

		/**
		 * GraphComputer (OLAP) is not supported, see {@link OpcGraph#compute()}.
		 */
		@Override
		public boolean supportsComputer() {
			return false;
		}

		@Override
		public boolean supportsConcurrentAccess() {
			return false;
		}

		@Override
		public boolean supportsServiceCall() {
			return false;
		}

		@Override
		public boolean supportsThreadedTransactions() {
			return false;
		}

		@Override
		public boolean supportsTransactions() {
			return false;
		}

	}

	private static final Configuration EMPTY_CONFIGURATION = new BaseConfiguration() {
		{
			this.setProperty(Graph.GRAPH, OpcGraph.class.getName());
		}
	};

	static {
		TraversalStrategies.GlobalCache.registerStrategies(OpcGraph.class,
				TraversalStrategies.GlobalCache.getStrategies(Graph.class).clone()
						.addStrategies(OpcGraphStepStrategy.instance(), OpcGraphCountStrategy.instance()));
	}

	public static OpcGraph open() {
		return open(EMPTY_CONFIGURATION);
	}

	public static OpcGraph open(final Configuration configuration) {
		return new OpcGraph(configuration);
	}

	private final OpcGraphFeatures features = new OpcGraphFeatures(this);

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final AtomicBoolean closed = new AtomicBoolean(false);

	private WaldotNamespace opcNamespace;

	OpcGraph(final Configuration configuration) {
		this.configuration = configuration;
		defaultVertexPropertyCardinality = VertexProperty.Cardinality.valueOf(configuration.getString(
				GREMLIN_OPCGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY, VertexProperty.Cardinality.single.name()));
		allowNullPropertyValues = configuration.getBoolean(GREMLIN_OPCGRAPH_ALLOW_NULL_PROPERTY_VALUES, false);
		serviceRegistry = new OpcServiceRegistry(this);
		// built-in services: OPC UA commands from Gremlin (call() step)
		serviceRegistry.registerService(new OpcCommandServiceFactory(this));
		serviceRegistry.registerService(new OpcCommandListServiceFactory(this));
		configuration.getList(String.class, GREMLIN_OPCGRAPH_SERVICE, Collections.emptyList())
				.forEach(serviceClass -> serviceRegistry.registerService(instantiate(serviceClass)));
	}

	@Override
	public Vertex addVertex(final Object... keyValues) {
		if (opcNamespace == null) {
			throw new IllegalArgumentException("Namespace not set");
		}
		ElementHelper.legalPropertyKeyValueArray(keyValues);
		/*
		 * for (int i = 0; i < keyValues.length; i = i + 2) {
		 * logger.debug("Key: {}, Value: {}", keyValues[i], keyValues[i + 1]); }
		 */
		final Object readId = MiloStrategy.getIdValue(keyValues).orElse(null);
		// logger.debug("Read id: {}", readId);
		NodeId nodeId = vertexIdManager.convert(this, readId);
		// logger.debug("NodeId determined: {}", nodeId);
		if (null != nodeId) {
			if (opcNamespace.hasNodeId(nodeId)) {
				throw Exceptions.vertexWithIdAlreadyExists(nodeId);
			}
		} else {
			nodeId = vertexIdManager.getNextId(this);
			logger.info("NodeId generated: {}", nodeId);
		}
		final WaldotVertex vertex = opcNamespace.addVertex(nodeId, keyValues);
		return vertex;
	}

	@Override
	public void close() {
		if (!closed.compareAndSet(false, true)) {
			return;
		}
		try {
			closeProcedure();
		} finally {
			serviceRegistry.close();
		}
	}

	/**
	 * @return {@code true} after {@link #close()} has run once
	 */
	public boolean isClosed() {
		return closed.get();
	}

	@Override
	public void closeProcedure() {
		logger.info("opc graph closed");
		// closing the graph must also release the namespace (OPC UA server,
		// plugins, strategies). HomunculusNamespace.close() is idempotent and
		// closes this graph only when it was not already closing.
		if (opcNamespace != null) {
			try {
				opcNamespace.close();
			} catch (final Exception e) {
				logger.warn("Error closing namespace from graph: {}", e.getMessage());
			}
		}
	}

	@Override
	public Features features() {
		return features;
	}

	@Override
	public WaldotGraphComputerView getGraphComputerView() {
		if (opcNamespace == null) {
			throw new IllegalArgumentException("Namespace not set");
		}
		return opcNamespace.getGraphComputerView();
	}

	@Override
	public OpcServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public WaldotNamespace getWaldotNamespace() {
		return opcNamespace;
	}

	@Override
	public void setNamespace(final WaldotNamespace waldotNamespace) {
		if (waldotNamespace == null) {
			throw new IllegalArgumentException("Namespace cannot be null");
		}
		this.opcNamespace = waldotNamespace;

	}

	@Override
	public Transaction tx() {
		throw Exceptions.transactionsNotSupported();
	}

}