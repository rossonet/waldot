package net.rossonet.waldot.dataGenerator;

import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.EnumUtils;
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
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

public class DataGeneratorVertex extends AbstractOpcVertex implements AutoCloseable {

	public enum Algorithm {
		decremental, incremental, random, sinusoidal, stopped, triangular
	}

	public static final String VALUE_KEY = "data";

	public static void generateParameters(WaldotNamespace waldotNamespace, UaObjectTypeNode dataGeneratorTypeNode) {
		PluginListener.addParameterToTypeNode(waldotNamespace, dataGeneratorTypeNode,
				WaldotGeneratorPlugin.ALGORITHM_FIELD, NodeIds.UInt64);
		PluginListener.addParameterToTypeNode(waldotNamespace, dataGeneratorTypeNode, WaldotGeneratorPlugin.DELAY_FIELD,
				NodeIds.UInt64);
		PluginListener.addParameterToTypeNode(waldotNamespace, dataGeneratorTypeNode,
				WaldotGeneratorPlugin.MIN_VALUE_FIELD, NodeIds.UInt64);
		PluginListener.addParameterToTypeNode(waldotNamespace, dataGeneratorTypeNode,
				WaldotGeneratorPlugin.MAX_VALUE_FIELD, NodeIds.UInt64);
	}

	private transient boolean active = true;
	private long actualValue;
	private final Algorithm algorithm;
	private final long delay;
	private final ExecutorService executor;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final long max;
	private final long min;
	private transient Runnable runner = new Runnable() {

		@Override
		public void run() {
			logger.info("Thread for generator node " + getNodeId().toParseableString() + " started");
			Thread.currentThread().setName(getNodeId().toParseableString());
			while (active == true) {
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
					break;
				default:
					logger.warn("algorithm not implemented: " + algorithm);
					break;
				}
				try {
					Thread.sleep(delay);
				} catch (final InterruptedException e) {
					logger.info("exception in generator", e);
				}
			}
			logger.info("Thread for generator node " + getNodeId().toParseableString() + " stopped");
		}
	};

	private long seed;
	private final WaldotNamespace waldotNamespace;

	public DataGeneratorVertex(ExecutorService executor, WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		this.executor = executor;
		waldotNamespace = graph.getWaldotNamespace();

		final String keyValuesPropertyDelay = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase());
		long delay = WaldotGeneratorPlugin.DEFAULT_DELAY_FIELD;
		if (keyValuesPropertyDelay != null && !keyValuesPropertyDelay.isEmpty()) {
			if (delay < 100L) {
				delay = Long.valueOf(keyValuesPropertyDelay);
			} else {
				logger.info(
						WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase() + " is less than 100ms, using default {} '{}'",
						WaldotGeneratorPlugin.DELAY_FIELD, WaldotGeneratorPlugin.DEFAULT_DELAY_FIELD);
			}
		} else {
			logger.info(
					WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase()
							+ " not found in propertyKeyValues, using default {} '{}'",
					WaldotGeneratorPlugin.DELAY_FIELD, WaldotGeneratorPlugin.DEFAULT_DELAY_FIELD);
		}

		final String keyValuesPropertyMin = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotGeneratorPlugin.MIN_VALUE_FIELD.toLowerCase());
		long min = WaldotGeneratorPlugin.DEFAULT_MIN_VALUE_FIELD;
		if (keyValuesPropertyMin != null && !keyValuesPropertyMin.isEmpty()) {
			min = Long.valueOf(keyValuesPropertyMin);
		} else {
			logger.info(
					WaldotGeneratorPlugin.MIN_VALUE_FIELD.toLowerCase()
							+ " not found in propertyKeyValues, using default {} '{}'",
					WaldotGeneratorPlugin.MIN_VALUE_FIELD, WaldotGeneratorPlugin.DEFAULT_MIN_VALUE_FIELD);
		}

		final String keyValuesPropertyMax = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotGeneratorPlugin.MAX_VALUE_FIELD.toLowerCase());
		long max = WaldotGeneratorPlugin.DEFAULT_MAX_VALUE_FIELD;
		if (keyValuesPropertyMax != null && !keyValuesPropertyMax.isEmpty()) {
			max = Long.valueOf(keyValuesPropertyMax);
		} else {
			logger.info(
					WaldotGeneratorPlugin.MAX_VALUE_FIELD.toLowerCase()
							+ " not found in propertyKeyValues, using default {} '{}'",
					WaldotGeneratorPlugin.MAX_VALUE_FIELD, WaldotGeneratorPlugin.DEFAULT_MAX_VALUE_FIELD);
		}

		final String keyValuesPropertyAlgorithm = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase());
		Algorithm algorithm = Algorithm.valueOf(WaldotGeneratorPlugin.DEFAULT_ALGORITHM_FIELD);
		if (keyValuesPropertyAlgorithm != null && !keyValuesPropertyAlgorithm.isEmpty()) {
			if (EnumUtils.isValidEnum(Algorithm.class, keyValuesPropertyAlgorithm)) {
				algorithm = Algorithm.valueOf(keyValuesPropertyAlgorithm);
			} else {
				logger.info("Algorithm {} not found, using default {} '{}'", keyValuesPropertyAlgorithm,
						WaldotGeneratorPlugin.ALGORITHM_FIELD, WaldotGeneratorPlugin.DEFAULT_ALGORITHM_FIELD);
				logger.info("Available algorithms are: {}", EnumUtils.getEnumList(Algorithm.class).toString());
			}
		} else {
			logger.info(
					WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase()
							+ " not found in propertyKeyValues, using default {} '{}'",
					WaldotGeneratorPlugin.ALGORITHM_FIELD, WaldotGeneratorPlugin.DEFAULT_ALGORITHM_FIELD);
		}
		this.delay = delay;
		this.min = min;
		this.max = max;
		this.algorithm = algorithm;
		seed = (long) (Math.random() * (max - min)) + min;
		actualValue = seed;
		executor.submit(runner);
	}

	private void assignValue() {
		property(Cardinality.single, VALUE_KEY, actualValue);

	}

	@Override
	public Object clone() {
		return new DataGeneratorVertex(executor, graph, getNodeContext(), getNodeId(), getBrowseName(),
				getDisplayName(), getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version(),
				getPropertiesAsStringArray());

	}

	@Override
	public void close() {
		active = false;
	}

	protected void generateNextDecremental() {
		actualValue--;
		if (actualValue < min) {
			actualValue = max;
		}
		assignValue();
	}

	protected void generateNextIncremental() {
		actualValue++;
		if (actualValue > max) {
			actualValue = min;
		}
		assignValue();
	}

	protected void generateNextRandom() {
		actualValue = (long) (Math.random() * (max - min)) + min;
		assignValue();
	}

	protected void generateNextSinusoidal() {
		actualValue = (long) ((max - min) / 2 * Math.sin(seed++) + (max + min) / 2);
		assignValue();
	}

	protected void generateNextTriangular() {
		actualValue = (long) (min + ((max - min) * (2 / Math.PI * Math.acos(Math.abs(Math.cos(seed++))))));
		assignValue();
	}

	public WaldotNamespace getWaldotNamespace() {
		return waldotNamespace;
	}

	@Override
	public void notifyRemoveVertex() {
		close();

	}

}
