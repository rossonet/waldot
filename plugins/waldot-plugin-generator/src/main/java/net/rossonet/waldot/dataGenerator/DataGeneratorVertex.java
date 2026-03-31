package net.rossonet.waldot.dataGenerator;

import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.EnumUtils;
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
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

import net.rossonet.waldot.WaldotGeneratorPlugin;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;

/**
 * WaldOT vertex that generates continuously changing data values using various algorithms.
 * <p>
 * DataGeneratorVertex simula un sensore o dispositivo IoT generando valori che cambiano
 * nel tempo secondo algoritmi configurabili. Ogni generatore esegue in un thread virtuale
 * dedicato che aggiorna la proprietà "data" a intervalli regolari.
 * </p>
 * 
 * <h2>Generation Algorithms</h2>
 * <ul>
 *   <li><b>incremental</b>: Value increases by 1 from min to max, then wraps to min</li>
 *   <li><b>decremental</b>: Value decreases by 1 from max to min, then wraps to max</li>
 *   <li><b>random</b>: Random value uniformly distributed between min and max</li>
 *   <li><b>sinusoidal</b>: Smooth sine wave: (max-min)/2 * sin(seed++) + (max+min)/2</li>
 *   <li><b>triangular</b>: Triangle wave using arccos transformation</li>
 *   <li><b>stopped</b>: No generation, value remains constant</li>
 * </ul>
 * 
 * <h2>Configuration Properties</h2>
 * <ul>
 *   <li><b>Algorithm</b>: Generation algorithm name (String)</li>
 *   <li><b>Delay</b>: Update interval in milliseconds (Long, min: 10ms)</li>
 *   <li><b>Min</b>: Minimum value (Long, default: 0)</li>
 *   <li><b>Max</b>: Maximum value (Long, default: 20000)</li>
 * </ul>
 * 
 * <h2>Generated Value</h2>
 * <p>
 * The generated value is stored in the "data" property and updated at each interval.
 * The value is accessible via:
 * <ul>
 *   <li>TinkerPop graph: {@code vertex.property("data").value()}</li>
 *   <li>OPC-UA client: Browse to vertex and read "data" property</li>
 * </ul>
 * </p>
 * 
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Constructor initializes properties and starts generation thread</li>
 *   <li>Thread runs loop: generate value → update property → sleep(delay)</li>
 *   <li>close() stops thread and terminates generation</li>
 * </ol>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * Each generator runs in its own virtual thread. Property updates are thread-safe
 * thanks to the underlying WaldOT framework synchronization.
 * </p>
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create sinusoidal generator simulating temperature sensor
 * Vertex tempSensor = graph.addVertex(
 *     "type", "generator",
 *     "label", "office-temp",
 *     "Algorithm", "sinusoidal",
 *     "Min", "18",
 *     "Max", "26",
 *     "Delay", "5000"  // Update every 5 seconds
 * );
 * 
 * // Read current value
 * double temperature = tempSensor.property("data").value();
 * 
 * // Change algorithm at runtime
 * tempSensor.property("Algorithm", "random");
 * 
 * // Stop generation
 * tempSensor.property("Algorithm", "stopped");
 * }</pre>
 * 
 * @see WaldotGeneratorPlugin
 * @author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @since 0.4.0
 */
public class DataGeneratorVertex extends AbstractOpcVertex implements AutoCloseable {

	/**
	 * Available data generation algorithms.
	 * <p>
	 * Algoritmi disponibili per la generazione dei dati.
	 * </p>
	 */
	public enum Algorithm {
		/** Value decreases from max to min, wraps around */
		decremental,
		/** Value increases from min to max, wraps around */
		incremental,
		/** Random value between min and max */
		random,
		/** Smooth sine wave pattern */
		sinusoidal,
		/** No generation, value stays constant */
		stopped,
		/** Triangle wave pattern */
		triangular
	}

	/** Minimum allowed delay between updates (10ms) */
	private static final long MIN_DELAY = 10L;
	
	/** Property key for generated value */
	public static final String VALUE_KEY = "data";

	/**
	 * Registers OPC-UA properties for data generator type node.
	 * <p>
	 * Aggiunge le proprietà Algorithm, Delay, Min, Max al tipo OPC-UA del generatore.
	 * </p>
	 * 
	 * @param waldotNamespace WaldOT namespace
	 * @param dataGeneratorTypeNode OPC-UA type node for generators
	 */
	public static void generateParameters(WaldotNamespace waldotNamespace, UaObjectTypeNode dataGeneratorTypeNode) {
		PluginListener.addParameterToTypeNode(waldotNamespace, dataGeneratorTypeNode,
				WaldotGeneratorPlugin.ALGORITHM_FIELD, NodeIds.String);
		PluginListener.addParameterToTypeNode(waldotNamespace, dataGeneratorTypeNode, WaldotGeneratorPlugin.DELAY_FIELD,
				NodeIds.UInt64);
		PluginListener.addParameterToTypeNode(waldotNamespace, dataGeneratorTypeNode,
				WaldotGeneratorPlugin.MIN_VALUE_FIELD, NodeIds.UInt64);
		PluginListener.addParameterToTypeNode(waldotNamespace, dataGeneratorTypeNode,
				WaldotGeneratorPlugin.MAX_VALUE_FIELD, NodeIds.UInt64);
	}

	// Flag per controllare l'attività del thread di generazione
	private transient boolean active = true;
	
	// Valore attuale generato
	private double actualValue;
	
	// Algoritmo di generazione corrente
	private Algorithm algorithm;
	
	// Proprietà OPC-UA per l'algoritmo
	private final QualifiedProperty<String> algorithmProperty;
	
	// Ritardo tra gli aggiornamenti (ms)
	private long delay;
	
	// Proprietà OPC-UA per il delay
	private final QualifiedProperty<Long> delayProperty;
	
	// Executor per il thread virtuale del generatore
	private final ExecutorService executor;
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	// Valore massimo
	private long max;

	// Proprietà OPC-UA per il massimo
	private final QualifiedProperty<Long> maxProperty;
	
	// Valore minimo
	private long min;
	
	// Proprietà OPC-UA per il minimo
	private final QualifiedProperty<Long> minProperty;
	/**
	 * Runnable per il thread di generazione dei dati.
	 * <p>
	 * Loop infinito che genera valori secondo l'algoritmo configurato
	 * e aggiorna la proprietà "data" a intervalli regolari.
	 * </p>
	 */
	private transient Runnable runner = new Runnable() {

		@Override
		public void run() {
			logger.info("Thread for generator node " + getNodeId().toParseableString() + " started");
			Thread.currentThread().setName(getNodeId().toParseableString());
			
			// Loop principale di generazione
			while (active == true) {
				// Seleziona l'algoritmo di generazione
				switch (algorithm) {
				case decremental:
					generateNextDecremental();
					break;
				case incremental:
					generateNextIncremental();
					break;
				case random:
					generateNextRandom();
					break;
				case sinusoidal:
					generateNextSinusoidal();
					break;
				case triangular:
					generateNextTriangular();
					break;
				case stopped:
					// Non genera nulla, mantiene il valore corrente
					break;
				default:
					logger.warn("algorithm not implemented: " + algorithm);
					break;
				}
				try {
					// Attende prima della prossima generazione
					Thread.sleep(delay);
				} catch (final InterruptedException e) {
					logger.info("exception in generator", e);
				}
			}
			logger.info("Thread for generator node " + getNodeId().toParseableString() + " stopped");
		}
	};
	
	// Seed per algoritmi sinusoidali e triangolari
	private long seed;
	
	private final WaldotNamespace waldotNamespace;

	/**
	 * Creates a new data generator vertex.
	 * <p>
	 * Costruisce un generatore dati inizializzando le proprietà Min, Max, Delay, Algorithm
	 * dai valori forniti o usando i default. Avvia immediatamente il thread di generazione.
	 * </p>
	 * 
	 * @param executor executor service for virtual thread
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
	 * @param propertyKeyValues initial property key-value pairs
	 */
	public DataGeneratorVertex(ExecutorService executor, WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		this.executor = executor;
		waldotNamespace = graph.getWaldotNamespace();

		final String keyValuesPropertyDelay = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase());
		delay = WaldotGeneratorPlugin.DEFAULT_DELAY_FIELD;
		checkDelay(keyValuesPropertyDelay);
		delayProperty = new QualifiedProperty<Long>(getNamespace().getNamespaceUri(), WaldotGeneratorPlugin.DELAY_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				Long.class);
		setProperty(delayProperty, delay);
		final String keyValuesPropertyMin = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotGeneratorPlugin.MIN_VALUE_FIELD.toLowerCase());
		min = WaldotGeneratorPlugin.DEFAULT_MIN_VALUE_FIELD;
		if (keyValuesPropertyMin != null && !keyValuesPropertyMin.isEmpty()) {
			try {
				min = Long.valueOf(keyValuesPropertyMin);
			} catch (final Exception e) {
				logger.info("min value is not a number, using default {} '{}'", WaldotGeneratorPlugin.MIN_VALUE_FIELD,
						WaldotGeneratorPlugin.DEFAULT_MIN_VALUE_FIELD);
				min = WaldotGeneratorPlugin.DEFAULT_MIN_VALUE_FIELD;
			}
		} else {
			logger.info(
					WaldotGeneratorPlugin.MIN_VALUE_FIELD.toLowerCase()
							+ " not found in propertyKeyValues, using default {} '{}'",
					WaldotGeneratorPlugin.MIN_VALUE_FIELD, WaldotGeneratorPlugin.DEFAULT_MIN_VALUE_FIELD);
		}
		minProperty = new QualifiedProperty<Long>(getNamespace().getNamespaceUri(),
				WaldotGeneratorPlugin.MIN_VALUE_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				Long.class);
		setProperty(minProperty, min);

		final String keyValuesPropertyMax = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotGeneratorPlugin.MAX_VALUE_FIELD.toLowerCase());
		max = WaldotGeneratorPlugin.DEFAULT_MAX_VALUE_FIELD;
		if (keyValuesPropertyMax != null && !keyValuesPropertyMax.isEmpty()) {
			try {
				max = Long.valueOf(keyValuesPropertyMax);
			} catch (final Exception e) {
				logger.info("max value is not a number, using default {} '{}'", WaldotGeneratorPlugin.MAX_VALUE_FIELD,
						WaldotGeneratorPlugin.DEFAULT_MAX_VALUE_FIELD);
				max = WaldotGeneratorPlugin.DEFAULT_MAX_VALUE_FIELD;
			}
		} else {
			logger.info(
					WaldotGeneratorPlugin.MAX_VALUE_FIELD.toLowerCase()
							+ " not found in propertyKeyValues, using default {} '{}'",
					WaldotGeneratorPlugin.MAX_VALUE_FIELD, WaldotGeneratorPlugin.DEFAULT_MAX_VALUE_FIELD);
		}
		maxProperty = new QualifiedProperty<Long>(getNamespace().getNamespaceUri(),
				WaldotGeneratorPlugin.MAX_VALUE_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				Long.class);
		setProperty(maxProperty, max);

		final String keyValuesPropertyAlgorithm = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase());
		algorithm = Algorithm.valueOf(WaldotGeneratorPlugin.DEFAULT_ALGORITHM_FIELD);
		checkAlgorithm(keyValuesPropertyAlgorithm);
		algorithmProperty = new QualifiedProperty<String>(getNamespace().getNamespaceUri(),
				WaldotGeneratorPlugin.ALGORITHM_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				String.class);
		setProperty(algorithmProperty, algorithm.name());

		// Inizializza seed con valore casuale nel range
		seed = (long) (Math.random() * (max - min)) + min;
		actualValue = seed;
		
		// Avvia il thread di generazione
		executor.submit(runner);
	}

	/**
	 * Updates the "data" property with the current generated value.
	 * <p>
	 * Aggiorna la proprietà "data" del vertice con il valore attuale generato.
	 * L'aggiornamento propaga sia al grafo TinkerPop che all'OPC-UA address space.
	 * </p>
	 */
	private void assignValue() {
		property(Cardinality.single, VALUE_KEY, actualValue);

	}

	/**
	 * Validates and sets the algorithm from a string value.
	 * <p>
	 * Valida che l'algoritmo richiesto esista nell'enum Algorithm.
	 * Se valido, imposta il nuovo algoritmo; altrimenti mantiene quello corrente.
	 * </p>
	 * 
	 * @param keyValuesNewAlgorithm algorithm name to set
	 * @return true if algorithm valid and set, false otherwise
	 */
	private boolean checkAlgorithm(final String keyValuesNewAlgorithm) {
		boolean ok = false;
		if (keyValuesNewAlgorithm != null && !keyValuesNewAlgorithm.isEmpty()) {
			if (EnumUtils.isValidEnum(Algorithm.class, keyValuesNewAlgorithm)) {
				try {
					final Algorithm targetAlgorithm = Algorithm.valueOf(keyValuesNewAlgorithm);
					if (targetAlgorithm != null) {
						algorithm = targetAlgorithm;
						ok = true;
					} else {
						logger.info("Algorithm {} not found", keyValuesNewAlgorithm);
						ok = false;
					}
				} catch (final Exception e) {
					logger.info("Algorithm {} not found", keyValuesNewAlgorithm);
					ok = false;
				}
			} else {
				logger.info("Algorithm {} not found, using {} '{}'", keyValuesNewAlgorithm,
						WaldotGeneratorPlugin.ALGORITHM_FIELD, algorithm);
				logger.info("Available algorithms are: {}", EnumUtils.getEnumList(Algorithm.class).toString());
				ok = false;
			}
		} else {
			logger.info(
					WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase()
							+ " not found in propertyKeyValues, using {} '{}'",
					WaldotGeneratorPlugin.ALGORITHM_FIELD, algorithm);
			ok = false;
		}
		return ok;
	}

	/**
	 * Validates and sets the delay from a string value.
	 * <p>
	 * Valida che il delay sia un numero >= MIN_DELAY (10ms).
	 * Se valido, imposta il nuovo delay; altrimenti mantiene quello corrente.
	 * </p>
	 * 
	 * @param keyValuesNewDelay delay in milliseconds as string
	 * @return true if delay valid and set, false otherwise
	 */
	private boolean checkDelay(final String keyValuesNewDelay) {
		boolean ok = false;
		if (keyValuesNewDelay != null && !keyValuesNewDelay.isEmpty()) {
			long targetDelay = 0L;
			try {
				targetDelay = Long.valueOf(keyValuesNewDelay);
			} catch (final Exception e) {
				logger.info("delay is not a number");
			}
			if (targetDelay < MIN_DELAY) {
				logger.info(WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase() + " is less than {}ms, using {} '{}'",
						MIN_DELAY, WaldotGeneratorPlugin.DELAY_FIELD, delay);
				ok = false;
			} else {
				delay = targetDelay;
				ok = true;
			}
		} else {
			logger.info(
					WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase() + " not found in propertyKeyValues, using {} '{}'",
					WaldotGeneratorPlugin.DELAY_FIELD, delay);
			ok = false;
		}
		return ok;
	}

	/**
	 * Clones this vertex.
	 * 
	 * @return cloned DataGeneratorVertex instance
	 */
	@Override
	public Object clone() {
		return new DataGeneratorVertex(executor, graph, getNodeContext(), getNodeId(), getBrowseName(),
				getDisplayName(), getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version(),
				getPropertiesAsStringArray());

	}

	/**
	 * Stops the generation thread.
	 * <p>
	 * Ferma il thread di generazione impostando active = false.
	 * Il thread terminerà al prossimo ciclo del loop.
	 * </p>
	 */
	@Override
	public void close() {
		active = false;
	}

	/**
	 * Generates next value using decremental algorithm.
	 * <p>
	 * Decrementa il valore di 1. Quando raggiunge min, ritorna a max (wrap-around).
	 * </p>
	 * <p>
	 * Formula: value--, if value < min then value = max
	 * </p>
	 */
	protected void generateNextDecremental() {
		actualValue--;
		if (actualValue < min) {
			actualValue = max;
		}
		assignValue();
	}

	/**
	 * Generates next value using incremental algorithm.
	 * <p>
	 * Incrementa il valore di 1. Quando raggiunge max, ritorna a min (wrap-around).
	 * </p>
	 * <p>
	 * Formula: value++, if value > max then value = min
	 * </p>
	 */
	protected void generateNextIncremental() {
		actualValue++;
		if (actualValue > max) {
			actualValue = min;
		}
		assignValue();
	}

	/**
	 * Generates next value using random algorithm.
	 * <p>
	 * Genera un valore casuale uniformemente distribuito tra min e max.
	 * </p>
	 * <p>
	 * Formula: random() * (max - min) + min
	 * </p>
	 */
	protected void generateNextRandom() {
		actualValue = Math.random() * (max - min) + min;
		assignValue();
	}

	/**
	 * Generates next value using sinusoidal algorithm.
	 * <p>
	 * Genera un'onda sinusoidale che oscilla tra min e max.
	 * Usa seed come parametro temporale incrementale.
	 * </p>
	 * <p>
	 * Formula: (max-min)/2 * sin(seed++) + (max+min)/2
	 * </p>
	 * <p>
	 * Creates smooth periodic oscillation centered at (max+min)/2 with amplitude (max-min)/2.
	 * </p>
	 */
	protected void generateNextSinusoidal() {
		actualValue = (max - min) / 2 * Math.sin(seed++) + (max + min) / 2;
		assignValue();
	}

	/**
	 * Generates next value using triangular algorithm.
	 * <p>
	 * Genera un'onda triangolare che sale e scende linearmente tra min e max.
	 * Usa trasformazione arccos per creare rampe lineari.
	 * </p>
	 * <p>
	 * Formula: min + (max-min) * (2/π * acos(|cos(seed++)|))
	 * </p>
	 * <p>
	 * Creates triangle wave with linear ramps up and down between min and max.
	 * </p>
	 */
	protected void generateNextTriangular() {
		actualValue = min + ((max - min) * (2 / Math.PI * Math.acos(Math.abs(Math.cos(seed++)))));
		assignValue();
	}

	/**
	 * Returns the WaldOT namespace.
	 * 
	 * @return WaldOT namespace instance
	 */
	public WaldotNamespace getWaldotNamespace() {
		return waldotNamespace;
	}

	/**
	 * Handles property value changes.
	 * <p>
	 * Chiamato quando una proprietà del vertice viene modificata (da OPC-UA o grafo).
	 * Valida e applica i nuovi valori per Algorithm, Delay, Min, Max.
	 * Le modifiche non valide vengono rifiutate e il valore originale ripristinato.
	 * </p>
	 * 
	 * @param label property name
	 * @param value new property value
	 */
	@Override
	public void notifyPropertyValueChanging(String label, DataValue value) {
		super.notifyPropertyValueChanging(label, value);
		
		// Gestisce modifica del Delay
		if (label.equals(WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase())) {
			final String delayTarget = value.value().value().toString();
			if (checkDelay(delayTarget)) {
				setProperty(delayProperty, delay);
			} else {
				logger.warn("Changing delay from {} to {} is not allowed, reverting to original value", delay,
						value.getValue().getValue().toString());
				property(WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase(), delay);
			}
		}
		
		// Gestisce modifica dell'Algorithm
		if (label.equals(WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase())) {
			final String algorithmTarget = value.value().value().toString();
			if (checkAlgorithm(algorithmTarget)) {
				setProperty(algorithmProperty, algorithm.name());
			} else {
				logger.warn("Changing algorithm from {} to {} is not allowed, reverting to original value",
						algorithm.name(), value.getValue().getValue().toString());
				property(WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase(), algorithm.name());
			}
		}
		
		// Gestisce modifica del Min
		if (label.equals(WaldotGeneratorPlugin.MIN_VALUE_FIELD.toLowerCase())) {
			min = Long.valueOf(value.getValue().getValue().toString());
			setProperty(minProperty, min);
		}
		
		// Gestisce modifica del Max
		if (label.equals(WaldotGeneratorPlugin.MAX_VALUE_FIELD.toLowerCase())) {
			max = Long.valueOf(value.getValue().getValue().toString());
			setProperty(maxProperty, max);
		}
	}

	/**
	 * Handles vertex removal notification.
	 * <p>
	 * Chiamato quando il vertice viene rimosso dal grafo. Ferma il thread di generazione.
	 * </p>
	 */
	@Override
	public void notifyRemoveVertex() {
		close();

	}

}
