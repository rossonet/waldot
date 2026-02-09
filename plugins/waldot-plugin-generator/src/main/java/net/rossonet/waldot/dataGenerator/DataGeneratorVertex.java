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

public class DataGeneratorVertex extends AbstractOpcVertex implements AutoCloseable {

	public enum Algorithm {
		decremental, incremental, random, sinusoidal, stopped, triangular
	}

	private static final long MIN_DELAY = 10L;
	public static final String VALUE_KEY = "data";

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

	private transient boolean active = true;
	private long actualValue;
	private Algorithm algorithm;
	private final QualifiedProperty<String> algorithmProperty;
	private long delay;
	private final QualifiedProperty<Long> delayProperty;
	private final ExecutorService executor;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private long max;

	private final QualifiedProperty<Long> maxProperty;
	private long min;
	private final QualifiedProperty<Long> minProperty;
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
			min = Long.valueOf(keyValuesPropertyMin);
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
			max = Long.valueOf(keyValuesPropertyMax);
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

		seed = (long) (Math.random() * (max - min)) + min;
		actualValue = seed;
		executor.submit(runner);
	}

	private void assignValue() {
		property(Cardinality.single, VALUE_KEY, actualValue);

	}

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
	public void notifyPropertyValueChanging(String label, DataValue value) {
		super.notifyPropertyValueChanging(label, value);
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
		if (label.equals(WaldotGeneratorPlugin.MIN_VALUE_FIELD.toLowerCase())) {
			setProperty(minProperty, Long.valueOf(value.getValue().getValue().toString()));
		}
		if (label.equals(WaldotGeneratorPlugin.MAX_VALUE_FIELD.toLowerCase())) {
			setProperty(maxProperty, Long.valueOf(value.getValue().getValue().toString()));
		}
	}

	@Override
	public void notifyRemoveVertex() {
		close();

	}

}
