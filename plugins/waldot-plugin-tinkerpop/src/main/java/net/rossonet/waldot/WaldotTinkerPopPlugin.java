package net.rossonet.waldot;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
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
 * </p>
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
 * @see WaldotGremlinServer
 * @see WaldotGraphManager
 * @author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @since 0.4.0
 */
@WaldotPlugin
public class WaldotTinkerPopPlugin implements PluginListener {
	/** Property name for bind address/host */
	public static final String BIND_HOST_FIELD = "Bind";
	
	/** OPC-UA display name for Gremlin Server type */
	private static final String GREMLIN_TYPE_DISPLAY_NAME = "implementing the Gremlin Server protocol on a specific port";
	
	/** Vertex type label for Gremlin Server vertices */
	private static final String GREMLIN_TYPE_LABEL = "gremlin";
	
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

	// Nodo tipo OPC-UA per i server Gremlin
	private UaObjectTypeNode gremlinTypeNode;
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private WaldotNamespace waldotNamespace;

	/**
	 * Checks if this plugin handles the specified vertex type label.
	 * 
	 * @param typeDefinitionLabel vertex type label
	 * @return true if label is "gremlin"
	 */
	@Override
	public boolean containsVertexType(String typeDefinitionLabel) {
		return GREMLIN_TYPE_LABEL.equals(typeDefinitionLabel);
	}

	/**
	 * Checks if this plugin handles the specified OPC-UA type node ID.
	 * 
	 * @param typeDefinitionNodeId OPC-UA type node ID
	 * @return true if matches Gremlin Server type node
	 */
	@Override
	public boolean containsVertexTypeNode(NodeId typeDefinitionNodeId) {
		return gremlinTypeNode.getNodeId().equals(typeDefinitionNodeId);
	}

	/**
	 * Creates a new TinkerPopVertex instance (Gremlin Server).
	 * <p>
	 * Crea un vertice che avvia un Gremlin Server embedded con le proprietà specificate.
	 * Il server inizia automaticamente alla creazione del vertice.
	 * </p>
	 * 
	 * @param graph WaldOT graph instance
	 * @param context OPC-UA node context
	 * @param nodeId OPC-UA node ID
	 * @param browseName OPC-UA browse name
	 * @param displayName OPC-UA display name
	 * @param description OPC-UA description
	 * @param writeMask OPC-UA write mask
	 * @param userWriteMask OPC-UA user write mask
	 * @param eventNotifier OPC-UA event notifier
	 * @param version vertex version
	 * @param propertyKeyValues initial property values (Port, Bind, etc.)
	 * @return new TinkerPopVertex with embedded Gremlin Server
	 */
	private WaldotVertex createGremlinVertexObject(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		return new TinkerPopVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	/**
	 * Creates a new vertex of the specified type.
	 * <p>
	 * Chiamato dal framework WaldOT quando viene aggiunto un vertice di tipo "gremlin".
	 * Crea un TinkerPopVertex che avvia immediatamente un Gremlin Server.
	 * </p>
	 * 
	 * @param typeDefinitionNodeId OPC-UA type node ID
	 * @param graph WaldOT graph instance
	 * @param context OPC-UA node context
	 * @param nodeId OPC-UA node ID
	 * @param browseName OPC-UA browse name
	 * @param displayName OPC-UA display name
	 * @param description OPC-UA description
	 * @param writeMask OPC-UA write mask
	 * @param userWriteMask OPC-UA user write mask
	 * @param eventNotifier OPC-UA event notifier
	 * @param version vertex version
	 * @param propertyKeyValues initial property values
	 * @return new TinkerPopVertex or null if type not handled
	 */
	@Override
	public WaldotVertex createVertex(NodeId typeDefinitionNodeId, WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		// Verifica che il tipo sia gestito da questo plugin
		if (!containsVertexTypeNode(typeDefinitionNodeId)) {
			logger.warn("TypeDefinitionNodeId: {} not managed by TinkerPopPlugin", typeDefinitionNodeId);
			return null;
		}
		final WaldotVertex vertexObject = createGremlinVertexObject(graph, context, nodeId, browseName, displayName,
				description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		return vertexObject;
	}

	/**
	 * Creates the OPC-UA type node for Gremlin Server vertices.
	 * <p>
	 * Crea il tipo OPC-UA "GremlinServerObjectType" nell'address space
	 * con le proprietà Port, Bind, Status.
	 * </p>
	 */
	private void generateGremlinTypeNode() {
		// Crea il nodo tipo OPC-UA
		gremlinTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + GREMLIN_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(GREMLIN_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(GREMLIN_TYPE_DISPLAY_NAME)).setIsAbstract(false).build();
		
		// Aggiunge proprietà standard label
		PluginListener.addParameterToTypeNode(waldotNamespace, gremlinTypeNode, MiloStrategy.LABEL_FIELD,
				NodeIds.String);
		
		// Aggiunge proprietà specifiche del Gremlin Server (Port, Bind, Status)
		TinkerPopVertex.generateParameters(waldotNamespace, gremlinTypeNode);
		
		// Registra il tipo nell'address space OPC-UA
		waldotNamespace.getStorageManager().addNode(gremlinTypeNode);
		gremlinTypeNode.addReference(new Reference(gremlinTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(gremlinTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);

	}

	/**
	 * Returns the OPC-UA type node ID for the specified type label.
	 * 
	 * @param typeDefinitionLabel type label ("gremlin")
	 * @return type node ID or null if not found
	 */
	@Override
	public NodeId getVertexTypeNode(String typeDefinitionLabel) {
		switch (typeDefinitionLabel) {
		case GREMLIN_TYPE_LABEL:
			return gremlinTypeNode.getNodeId();
		default:
			logger.warn("TypeDefinitionLabel: {} not managed by TinkerPopPlugin", typeDefinitionLabel);
			return null;
		}
	}

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
		this.waldotNamespace = waldotNamespace;
		// Crea e registra il tipo GremlinServer nell'OPC-UA address space
		generateGremlinTypeNode();
		logger.info("Initializing Waldot TinkerPop Plugin");
	}

}
