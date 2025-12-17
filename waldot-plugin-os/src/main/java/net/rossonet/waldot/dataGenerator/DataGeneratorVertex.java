package net.rossonet.waldot.dataGenerator;

import java.util.concurrent.ExecutorService;

import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.opc.AbstractOpcVertex;

public class DataGeneratorVertex extends AbstractOpcVertex implements AutoCloseable {

	public enum Algorithm {
		decremental, incremental, random, sinusoidal, stopped, triangular
	}

	private static final String VALUE_KEY = "generated";
	private volatile boolean active = true;

	private long actualValue;
	private final Algorithm algorithm;
	private final long delay;
	private final ExecutorService executor;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final long max;
	private final long min;
	private volatile Runnable runner = new Runnable() {

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

	public DataGeneratorVertex(ExecutorService executor, WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version, long delay, long min, long max,
			Algorithm algorithm) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		this.executor = executor;
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
				delay, min, max, algorithm);

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

	@Override
	protected void propertyUpdateValueEvent(UaNode node, AttributeId attributeId, Object value) {
		// TODO aggiornare se necessario le label e i comportamenti legati alle property
	}

}
