package net.rossonet.waldot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.plugin.AbstractPluginListener;
import net.rossonet.waldot.tinkerpop.TinkerPopVertex;

/**
 * WaldOT TinkerPop Plugin - Enables native TinkerPop/Gremlin client connections to WaldOT graph.
 * <p>
 * Questo plugin permette ai client TinkerPop nativi (Gremlin Console, driver, Graph-Explorer)
 * di connettersi al grafo WaldOT tramite Gremlin Server embedded. Crea vertici di tipo "gremlin"
 * che avviano server Gremlin su porte configurabili, esponendo il grafo WaldOT via WebSocket
 * e HTTP con serializzatori GraphSON v3 e GraphBinary v1.
 * </p>
 * 
 * <h2>Overview</h2>
 * <p>
 * The TinkerPop plugin bridges the gap between WaldOT's OPC-UA-centric graph and standard
 * TinkerPop clients. It embeds Apache TinkerPop Gremlin Server instances as graph vertices,
 * allowing:
 * <ul>
 *   <li><b>Native Gremlin queries</b>: Use standard TinkerPop drivers and tools</li>
 *   <li><b>Graph visualization</b>: Connect Graph-Explorer, GraphExp, etc.</li>
 *   <li><b>Remote traversals</b>: Execute Gremlin scripts from external applications</li>
 *   <li><b>Multi-protocol access</b>: OPC-UA + Gremlin on the same graph</li>
 * </ul>
 * 
 * <h2>Vertex Type</h2>
 * <p>
 * Registers vertex type: <code>gremlin</code> (TinkerPopVertex)
 * </p>
 * <p>
 * Each "gremlin" vertex starts an embedded Gremlin Server that exposes the WaldOT graph
 * via standard TinkerPop protocols (WebSocket + HTTP).
 * </p>
 * 
 * <h2>Configuration Properties</h2>
 * <ul>
 *   <li><b>Port</b>: Gremlin Server port (default: 1025)</li>
 *   <li><b>Bind</b>: Bind address (default: 0.0.0.0 - all interfaces)</li>
 *   <li><b>Status</b>: Server status (Running/Stopped/Failed) - read-only</li>
 * </ul>
 * 
 * <h2>Supported Serializers</h2>
 * <ul>
 *   <li><b>GraphSON v3</b>: JSON-based, required by Graph-Explorer and web clients</li>
 *   <li><b>GraphBinary v1</b>: Binary protocol for high-performance WebSocket clients</li>
 * </ul>
 * <p>
 * Both serializers include custom OPC-UA IoRegistries for WaldOT-specific types.
 * </p>
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create Gremlin Server vertex on port 8182
 * Vertex gremlinServer = graph.addVertex(
 *     "type", "gremlin",
 *     "label", "main-gremlin-server",
 *     "Port", "8182",
 *     "Bind", "0.0.0.0"
 * );
 * 
 * // Connect from Gremlin Console
 * // :remote connect tinkerpop.server conf/remote.yaml
 * // :remote console
 * // g.V().count()
 * 
 * // Connect from Java driver
 * Cluster cluster = Cluster.build()
 *     .addContactPoint("localhost")
 *     .port(8182)
 *     .serializer(new GraphBinaryMessageSerializerV1())
 *     .create();
 * GraphTraversalSource g = traversal()
 *     .withRemote(DriverRemoteConnection.using(cluster, "g"));
 * long count = g.V().count().next();
 * }</pre>
 * 
 * <h2>Integration Benefits</h2>
 * <ul>
 *   <li><b>Dual access</b>: Same graph accessible via OPC-UA and Gremlin</li>
 *   <li><b>Standard tools</b>: Use existing TinkerPop ecosystem</li>
 *   <li><b>Visualization</b>: Connect graph visualization tools</li>
 *   <li><b>Remote queries</b>: Execute Gremlin from any language with TinkerPop driver</li>
 *   <li><b>Live synchronization</b>: Changes via Gremlin reflect in OPC-UA and vice versa</li>
 * </ul>
 * 
 * @see TinkerPopVertex
 * @author Andrea Ambrosini - Zona Autonoma srl
 * @since 0.4.0
 */
@WaldotPlugin
public class WaldotTinkerPopPlugin extends AbstractPluginListener {
	/** Property name for bind address/host */
	public static final String BIND_HOST_FIELD = "Bind";

	/** OPC-UA display name for Gremlin Server type */
	private static final String GREMLIN_TYPE_DISPLAY_NAME = "Tinkerpop endpoint";

	/** Vertex type label for Gremlin Server vertices */
	public static final String GREMLIN_TYPE_LABEL = "gremlin";

	/** OPC-UA object type node ID */
	private static final String GREMLIN_TYPE_NODE_ID = "GremlinServerObjectType";

	/** Property name for server port */
	public static final String PORT_FIELD = "Port";

	/** Status value: server failed to start */
	public static final String STATUS_FAILED = "Failed";

	/** Property name for server status */
	public static final String STATUS_FIELD = "Status";

	/** Status value: server running */
	public static final String STATUS_RUNNING = "Running";

	/** Status value: server stopped */
	public static final String STATUS_STOPPED = "Stopped";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Initializes the plugin and registers vertex types.
	 * <p>
	 * Chiamato dal framework WaldOT all'avvio. Crea il tipo OPC-UA per i Gremlin Server
	 * e lo registra nel namespace.
	 * </p>
	 * 
	 * @param waldotNamespace WaldOT namespace instance
	 */
	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		super.initialize(waldotNamespace);
		registerTypeNode(TinkerPopVertex.class, GREMLIN_TYPE_NODE_ID,
				GREMLIN_TYPE_DISPLAY_NAME, GREMLIN_TYPE_LABEL);
		logger.info("Initializing Waldot TinkerPop Plugin");
	}

}
