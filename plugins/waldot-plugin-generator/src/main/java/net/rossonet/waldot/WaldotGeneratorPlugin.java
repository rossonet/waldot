package net.rossonet.waldot;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

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
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex.Algorithm;
import net.rossonet.waldot.utils.ThreadHelper;

/**
 * WaldOT Data Generator Plugin - Simulates dynamic data values for testing and simulation.
 * <p>
 * Questo plugin fornisce generatori di dati che simulano sensori e dispositivi IoT
 * con vari algoritmi di generazione (incrementale, decrementale, random, sinusoidale, triangolare).
 * </p>
 * 
 * <h2>Overview</h2>
 * <p>
 * The Data Generator plugin creates vertices that continuously generate changing values
 * according to configurable algorithms. This is useful for:
 * <ul>
 *   <li><b>Testing</b>: Simulate sensor behavior without physical hardware</li>
 *   <li><b>Demos</b>: Showcase rule engine and graph capabilities with live data</li>
 *   <li><b>Load testing</b>: Generate thousands of data points for performance testing</li>
 *   <li><b>Development</b>: Develop applications without access to real devices</li>
 * </ul>
 * </p>
 * 
 * <h2>Available Algorithms</h2>
 * <ul>
 *   <li><b>incremental</b>: Value increases from min to max, then wraps to min</li>
 *   <li><b>decremental</b>: Value decreases from max to min, then wraps to max</li>
 *   <li><b>random</b>: Random value between min and max</li>
 *   <li><b>sinusoidal</b>: Smooth sine wave pattern between min and max</li>
 *   <li><b>triangular</b>: Linear ramp up/down triangle wave pattern</li>
 *   <li><b>stopped</b>: Generation paused, value unchanged</li>
 * </ul>
 * 
 * <h2>Vertex Type</h2>
 * <p>
 * Registers vertex type: <code>generator</code> (DataGeneratorVertex)
 * </p>
 * 
 * <h2>Configuration Properties</h2>
 * <ul>
 *   <li><b>Algorithm</b>: Generation algorithm (default: incremental)</li>
 *   <li><b>Delay</b>: Update interval in milliseconds (default: 1000ms, min: 10ms)</li>
 *   <li><b>Min</b>: Minimum value (default: 0)</li>
 *   <li><b>Max</b>: Maximum value (default: 20000)</li>
 * </ul>
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create generator with random values
 * Vertex generator = graph.addVertex(
 *     "type", "generator",
 *     "label", "temp-sensor-sim",
 *     "Algorithm", "random",
 *     "Min", "20",
 *     "Max", "80",
 *     "Delay", "1000"  // Update every second
 * );
 * 
 * // Access generated value
 * double value = generator.property("data").value();
 * }</pre>
 * 
 * @see DataGeneratorVertex
 * @author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @since 0.4.0
 */
@WaldotPlugin
public class WaldotGeneratorPlugin implements AutoCloseable, PluginListener {
	/** Property name for generation algorithm */
	public static final String ALGORITHM_FIELD = "Algorithm";
	
	/** Vertex type label for data generator vertices */
	public static final String DATA_GENERATOR_OBJECT_TYPE_LABEL = "generator";
	
	/** Default algorithm: incremental */
	public static final String DEFAULT_ALGORITHM_FIELD = Algorithm.incremental.toString();
	
	/** Default update delay: 1 second */
	public static final Long DEFAULT_DELAY_FIELD = 1000L;
	
	/** Default maximum value: 20000 */
	public static final Long DEFAULT_MAX_VALUE_FIELD = 20000L;
	
	/** Default minimum value: 0 */
	public static final Long DEFAULT_MIN_VALUE_FIELD = 0L;
	
	/** Property name for update delay */
	public static final String DELAY_FIELD = "Delay";
	
	private final static Logger logger = LoggerFactory.getLogger(WaldotGeneratorPlugin.class);
	
	/** Property name for maximum value */
	public static final String MAX_VALUE_FIELD = "Max";
	
	/** Property name for minimum value */
	public static final String MIN_VALUE_FIELD = "Min";
	
	/** OPC-UA display name for data generator type */
	private static final String WALDOT_DATA_GENERATOR_DISPLAY_NAME = "WaldOT Data Generator";
	
	/** OPC-UA object type name */
	private static final String WALDOT_DATA_GENERATOR_OBJECT_TYPE = "WaldOTDataGeneratorObjectType";
	
	// Nodo tipo OPC-UA per i generatori
	private UaObjectTypeNode dataGeneratorTypeNode;

	// Executor per i thread virtuali dei generatori
	private final ExecutorService executor = ThreadHelper.newVirtualThreadExecutor();
	
	protected WaldotNamespace waldotNamespace;

	/**
	 * Closes the plugin and shuts down the executor service.
	 * <p>
	 * Chiude il plugin fermando tutti i thread virtuali dei generatori attivi.
	 * </p>
	 * 
	 * @throws Exception if shutdown fails
	 */
	@Override
	public void close() throws Exception {
		// Ferma tutti i thread virtuali dei generatori
		executor.shutdownNow();
		logger.info("WaldotGeneratorPlugin closed");
	}

	/**
	 * Checks if this plugin handles the specified vertex type label.
	 * 
	 * @param typeDefinitionLabel vertex type label
	 * @return true if label is "generator"
	 */
	@Override
	public boolean containsVertexType(String typeDefinitionLabel) {
		return DATA_GENERATOR_OBJECT_TYPE_LABEL.equals(typeDefinitionLabel);
	}

	/**
	 * Checks if this plugin handles the specified OPC-UA type node ID.
	 * 
	 * @param typeDefinitionNodeId OPC-UA type node ID
	 * @return true if matches data generator type node
	 */
	@Override
	public boolean containsVertexTypeNode(NodeId typeDefinitionNodeId) {
		return dataGeneratorTypeNode.getNodeId().equals(typeDefinitionNodeId);
	}

	/**
	 * Creates the OPC-UA type node for data generator vertices.
	 * <p>
	 * Crea il tipo OPC-UA "WaldOTDataGeneratorObjectType" nell'address space
	 * con le proprietà Algorithm, Delay, Min, Max.
	 * </p>
	 */
	private void createDataGeneratorTypeNode() {
		// Crea il nodo tipo OPC-UA
		dataGeneratorTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + WALDOT_DATA_GENERATOR_OBJECT_TYPE))
				.setBrowseName(waldotNamespace.generateQualifiedName(WALDOT_DATA_GENERATOR_OBJECT_TYPE))
				.setDisplayName(LocalizedText.english(WALDOT_DATA_GENERATOR_DISPLAY_NAME)).setIsAbstract(false).build();
		
		// Aggiunge proprietà standard label
		PluginListener.addParameterToTypeNode(waldotNamespace, dataGeneratorTypeNode, MiloStrategy.LABEL_FIELD,
				NodeIds.String);
		
		// Aggiunge proprietà specifiche del generatore (Algorithm, Delay, Min, Max)
		DataGeneratorVertex.generateParameters(waldotNamespace, dataGeneratorTypeNode);
		
		// Registra il tipo nell'address space OPC-UA
		waldotNamespace.getStorageManager().addNode(dataGeneratorTypeNode);
		dataGeneratorTypeNode.addReference(new Reference(dataGeneratorTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(dataGeneratorTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);
	}

	/**
	 * Creates a new DataGeneratorVertex instance.
	 * <p>
	 * Crea un nuovo vertice generatore con le proprietà specificate.
	 * Il generatore inizia automaticamente a generare valori in un thread virtuale.
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
	 * @param propertyKeyValues initial property values
	 * @return new DataGeneratorVertex
	 */
	private WaldotVertex createDataGeneratorVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		return new DataGeneratorVertex(executor, graph, context, nodeId, browseName, displayName, description,
				writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	/**
	 * Creates a new vertex of the specified type.
	 * <p>
	 * Chiamato dal framework WaldOT quando viene aggiunto un vertice di tipo "generator".
	 * Crea un DataGeneratorVertex che inizia immediatamente a generare valori.
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
	 * @return new vertex or null if type not handled
	 */
	@Override
	public WaldotVertex createVertex(NodeId typeDefinitionNodeId, WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		// Verifica che il tipo sia gestito da questo plugin
		if (!containsVertexTypeNode(typeDefinitionNodeId)) {
			return null;
		}
		if (dataGeneratorTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			return createDataGeneratorVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
					userWriteMask, eventNotifier, version, propertyKeyValues);
		} else {
			return null;
		}
	}

	/**
	 * Returns plugin-specific console commands.
	 * <p>
	 * Il plugin generator non fornisce comandi console.
	 * </p>
	 * 
	 * @return empty collection
	 */
	@Override
	public Collection<WaldotCommand> getCommands() {
		return Arrays.asList();
	}

	/**
	 * Returns the OPC-UA type node ID for the specified type label.
	 * 
	 * @param typeDefinitionLabel type label ("generator")
	 * @return type node ID or null if not found
	 */
	@Override
	public NodeId getVertexTypeNode(String typeDefinitionLabel) {
		if (DATA_GENERATOR_OBJECT_TYPE_LABEL.equals(typeDefinitionLabel)) {
			return dataGeneratorTypeNode.getNodeId();
		} else {
			return null;
		}
	}

	/**
	 * Initializes the plugin and registers vertex types.
	 * <p>
	 * Chiamato dal framework WaldOT all'avvio. Crea il tipo OPC-UA per i generatori
	 * e lo registra nel namespace.
	 * </p>
	 * 
	 * @param waldotNamespace WaldOT namespace instance
	 */
	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		// Crea e registra il tipo DataGenerator nell'OPC-UA address space
		createDataGeneratorTypeNode();
	}

}
