package net.rossonet.waldot.gremlin.opcgraph.structure;

import java.util.Collections;

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

import net.rossonet.waldot.api.models.WaldotGraphComputerView;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.gremlin.opcgraph.process.traversal.strategy.optimization.OpcGraphCountStrategy;
import net.rossonet.waldot.gremlin.opcgraph.process.traversal.strategy.optimization.OpcGraphStepStrategy;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcServiceRegistry;

/**
 * @author Andrea Ambrosini ( - andrea DOT ambrosini AT rossonet DOT org - )
 */
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_INTEGRATE)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_COMPUTER)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_LIMITED_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_LIMITED_COMPUTER)
public class OpcGraph extends AbstractOpcGraph {

	public class OpcGraphFeatures implements Features {
		private final OpcGraphEdgeFeatures edgeFeatures;
		@SuppressWarnings("unused")
		private final AbstractOpcGraph graph;
		private final OpcGraphGraphFeatures graphFeatures;
		private final OpcGraphVertexFeatures vertexFeatures;

		private OpcGraphFeatures(AbstractOpcGraph graph) {
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

	private WaldotNamespace opcNamespace;

	OpcGraph(final Configuration configuration) {
		this.configuration = configuration;
		defaultVertexPropertyCardinality = VertexProperty.Cardinality.valueOf(configuration.getString(
				GREMLIN_OPCGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY, VertexProperty.Cardinality.single.name()));
		allowNullPropertyValues = configuration.getBoolean(GREMLIN_OPCGRAPH_ALLOW_NULL_PROPERTY_VALUES, false);
		serviceRegistry = new OpcServiceRegistry(this);
		configuration.getList(String.class, GREMLIN_OPCGRAPH_SERVICE, Collections.emptyList())
				.forEach(serviceClass -> serviceRegistry.registerService(instantiate(serviceClass)));
	}

	@Override
	public Vertex addVertex(final Object... keyValues) {
		if (opcNamespace == null) {
			throw new IllegalArgumentException("Namespace not set");
		}
		ElementHelper.legalPropertyKeyValueArray(keyValues);
		NodeId nodeId = vertexIdManager.convert(this, ElementHelper.getIdValue(keyValues).orElse(null));
		if (null != nodeId) {
			if (opcNamespace.hasNodeId(nodeId)) {
				throw Exceptions.vertexWithIdAlreadyExists(nodeId);
			}
		} else {
			nodeId = vertexIdManager.getNextId(this);
		}
		final WaldotVertex vertex = opcNamespace.addVertex(nodeId, keyValues);
		return vertex;
	}

	@Override
	public void clear() {
		if (opcNamespace == null) {
			throw new IllegalArgumentException("Namespace not set");
		}
		opcNamespace.resetNameSpace();
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
	public void setNamespace(WaldotNamespace waldotNamespace) {
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
