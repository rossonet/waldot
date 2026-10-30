package net.rossonet.waldot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

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

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex;
import net.rossonet.waldot.dataGenerator.EventGeneratorVertex;
import net.rossonet.waldot.dataGenerator.GeneratorConstants;
import net.rossonet.waldot.dataGenerator.commands.CreateEventSimulatorCommand;
import net.rossonet.waldot.dataGenerator.commands.CreateSimulatorCommand;
import net.rossonet.waldot.plugin.AbstractPluginListener;
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
 * Registers vertex types: <code>sim:generator</code> (DataGeneratorVertex)
 * and <code>sim:event-generator</code> (EventGeneratorVertex, simulated OPC
 * UA events)
 * </p>
 *
 * <h2>Registered Commands</h2>
 * <ul>
 *   <li><code>create generator</code>: creates <code>sim:generator</code> vertices</li>
 *   <li><code>create event generator</code>: creates <code>sim:event-generator</code> vertices</li>
 * </ul>
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
 *     "type", "sim:generator",
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
 * @see GeneratorConstants
 * @author Andrea Ambrosini - Zona Autonoma srl
 * @since 0.4.0
 */
@WaldotPlugin
public class WaldotGeneratorPlugin extends AbstractPluginListener {
	/** OPC-UA display name for data generator type */
	private static final String WALDOT_DATA_GENERATOR_DISPLAY_NAME = "WaldOT Data Generator";

	/** OPC-UA object type name */
	private static final String WALDOT_DATA_GENERATOR_OBJECT_TYPE = "WaldOTDataGeneratorObjectType";

	/** OPC-UA display name for the event generator type */
	private static final String WALDOT_EVENT_GENERATOR_DISPLAY_NAME = "WaldOT Event Generator";

	/** OPC-UA object type name of the event generator */
	private static final String WALDOT_EVENT_GENERATOR_OBJECT_TYPE = "WaldOTEventGeneratorObjectType";

	// Nodo tipo OPC-UA per i generatori
	private UaObjectTypeNode dataGeneratorTypeNode;

	// Executor per i thread virtuali dei generatori
	private final ExecutorService executor = ThreadHelper
			.newVirtualThreadExecutor();

	protected WaldotNamespace waldotNamespace;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	/**
	 * Closes the plugin for good: stops the generators of the namespace and
	 * shuts down the executor, interrupting every generator thread still
	 * alive. Idempotent; the generators created afterwards do not start.
	 * <p>
	 * Chiude il plugin fermando tutti i thread virtuali dei generatori attivi.
	 * </p>
	 *
	 * @throws Exception if shutdown fails
	 */
	@Override
	public void close() throws Exception {
		if (!closed.compareAndSet(false, true)) {
			return;
		}
		for (final WaldotVertex vertex : generators()) {
			if (vertex instanceof final AutoCloseable closeable) {
				try {
					closeable.close();
				} catch (final Exception e) {
					logger.warn("Error closing generator {}: {}",
							vertex.getNodeId(), e.getMessage());
				}
			}
		}
		// Ferma tutti i thread virtuali dei generatori
		executor.shutdownNow();
		super.close();
	}

	/**
	 * @return the generator vertices currently in the graph of the namespace
	 *         (empty before the initialization or if the graph is not
	 *         readable)
	 */
	private List<WaldotVertex> generators() {
		if (waldotNamespace == null) {
			return List.of();
		}
		try {
			final List<WaldotVertex> result = new ArrayList<>();
			for (final WaldotVertex vertex : waldotNamespace.getVertices()
					.values()) {
				if (vertex instanceof DataGeneratorVertex
						|| vertex instanceof EventGeneratorVertex) {
					result.add(vertex);
				}
			}
			return result;
		} catch (final RuntimeException e) {
			logger.warn("Error reading the generators: {}", e.getMessage());
			return List.of();
		}
	}

	/**
	 * @return {@code true} after {@link #close()}
	 */
	public boolean isClosed() {
		return closed.get();
	}

	/**
	 * Resumes the generators paused by {@link #stop()}.
	 */
	@Override
	public void start() {
		if (closed.get()) {
			return;
		}
		for (final WaldotVertex vertex : generators()) {
			if (vertex instanceof final DataGeneratorVertex data) {
				data.resumeGeneration();
			} else if (vertex instanceof final EventGeneratorVertex event) {
				event.resumeGeneration();
			}
		}
	}

	/**
	 * Pauses every generator of the namespace (restartable with
	 * {@link #start()}): the data generators release their thread, the event
	 * generators stop publishing. The executor stays open, it is shut down by
	 * {@link #close()}.
	 */
	@Override
	public void stop() {
		for (final WaldotVertex vertex : generators()) {
			if (vertex instanceof final DataGeneratorVertex data) {
				data.pauseGeneration();
			} else if (vertex instanceof final EventGeneratorVertex event) {
				event.pauseGeneration();
			}
		}
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
		dataGeneratorTypeNode = UaObjectTypeNode
				.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(
						OBJECT_TYPES + WALDOT_DATA_GENERATOR_OBJECT_TYPE))
				.setBrowseName(waldotNamespace.generateQualifiedName(
						WALDOT_DATA_GENERATOR_OBJECT_TYPE))
				.setDisplayName(LocalizedText
						.english(WALDOT_DATA_GENERATOR_DISPLAY_NAME))
				.setIsAbstract(false).build();

		// Aggiunge proprietà standard label
		PluginListener.addParameterToTypeNode(waldotNamespace,
				dataGeneratorTypeNode, MiloStrategy.LABEL_FIELD,
				NodeIds.String);

		// Aggiunge proprietà specifiche del generatore (Algorithm, Delay, Min,
		// Max)
		DataGeneratorVertex.generateParameters(waldotNamespace,
				dataGeneratorTypeNode);

		// Registra il tipo nell'address space OPC-UA
		waldotNamespace.getStorageManager().addNode(dataGeneratorTypeNode);
		dataGeneratorTypeNode.addReference(new Reference(
				dataGeneratorTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(
				dataGeneratorTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);
		// rende il tipo visibile ai metodi di AbstractPluginListener
		labelTypeNodes.put(GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL,
				dataGeneratorTypeNode);
		typeClassNodes.put(dataGeneratorTypeNode, DataGeneratorVertex.class);
	}

	/**
	 * Creates a new vertex of the specified type.
	 * <p>
	 * Il costruttore di {@link DataGeneratorVertex} richiede l'executor del
	 * plugin, per questo la creazione non usa la reflection di
	 * {@link AbstractPluginListener}.
	 * </p>
	 */
	@Override
	public WaldotVertex createVertex(NodeId typeDefinitionNodeId,
			WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName,
			LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version,
			Object[] propertyKeyValues) {
		if (dataGeneratorTypeNode == null || !dataGeneratorTypeNode.getNodeId()
				.equals(typeDefinitionNodeId)) {
			// the other types (event generator) use the standard constructor
			return super.createVertex(typeDefinitionNodeId, graph, context,
					nodeId, browseName, displayName, description, writeMask,
					userWriteMask, eventNotifier, version, propertyKeyValues);
		}
		return new DataGeneratorVertex(executor, graph, context, nodeId,
				browseName, displayName, description, writeMask, userWriteMask,
				eventNotifier, version, propertyKeyValues);
	}

	/**
	 * Initializes the plugin, registers the managed vertex types and both create
	 * commands.
	 * 
	 * @param waldotNamespace WaldOT namespace instance
	 */
	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		super.initialize(waldotNamespace);
		this.waldotNamespace = waldotNamespace;
		commands.clear();
		// Crea e registra il tipo DataGenerator nell'OPC-UA address space
		createDataGeneratorTypeNode();
		registerTypeNode(EventGeneratorVertex.class,
				WALDOT_EVENT_GENERATOR_OBJECT_TYPE,
				WALDOT_EVENT_GENERATOR_DISPLAY_NAME,
				GeneratorConstants.EVENT_GENERATOR_OBJECT_TYPE_LABEL);
		waldotNamespace.getOpcuaServer().updateReferenceTypeTree();
		registerCommand(new CreateSimulatorCommand(waldotNamespace));
		registerCommand(new CreateEventSimulatorCommand(waldotNamespace));
	}

}
