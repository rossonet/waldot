package net.rossonet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.function.BooleanSupplier;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex;
import net.rossonet.waldot.dataGenerator.GeneratorConstants;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

public class DataGeneratorVertexBehaviorTest {

	private static final long WAIT_MS = 5000;

	private static void waitFor(String what, BooleanSupplier condition)
			throws InterruptedException {
		final long end = System.currentTimeMillis() + WAIT_MS;
		while (System.currentTimeMillis() < end) {
			if (condition.getAsBoolean()) {
				return;
			}
			Thread.sleep(10);
		}
		throw new AssertionError("timeout waiting for " + what);
	}

	private WaldotGraph g;

	@AfterEach
	public void afterEach() throws Exception {
		WaldotTestEnvironment.cleanup(g, null);
		g = null;
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		g = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(),
				new LoggerHistoryStrategy());
	}

	@Test
	public void closeStopsTheGeneratorLoop() throws Exception {
		final DataGeneratorVertex generator = create("label", "close-test",
				"algorithm", "incremental", "delay", "20", "min", "1", "max",
				"100");
		waitFor("first value", () -> data(generator) > 0);
		generator.close();
		final double frozen = data(generator);
		Thread.sleep(150);
		assertEquals(frozen, data(generator), 0.000001);
	}

	private DataGeneratorVertex create(Object... keyValues) {
		final Object[] all = new Object[keyValues.length + 2];
		all[0] = MiloStrategy.TYPE_FIELD.toLowerCase();
		all[1] = GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL;
		System.arraycopy(keyValues, 0, all, 2, keyValues.length);
		final Vertex vertex = g.addVertex(all);
		return assertInstanceOf(DataGeneratorVertex.class, vertex);
	}

	private double data(DataGeneratorVertex generator) {
		return Double.parseDouble(generator
				.property(DataGeneratorVertex.VALUE_KEY).value().toString());
	}

	@Test
	public void runtimeMinMaxUpdatesRejectInvalidRanges() throws Exception {
		final DataGeneratorVertex generator = create("label", "range-test",
				"algorithm", "random", "delay", "20", "min", "0", "max", "10");
		generator.property("min", 20L);
		generator.property("max", -1L);
		assertEquals("0", generator.property("min").value().toString());
		assertEquals("10", generator.property("max").value().toString());
	}

	@Test
	public void sinusoidalUsesFractionalAmplitudeForSmallRange()
			throws Exception {
		final DataGeneratorVertex generator = create("label", "sin-small",
				"algorithm", "sinusoidal", "delay", "20", "min", "0", "max",
				"1");
		waitFor("fractional sinusoidal value", () -> {
			final double value = data(generator);
			return value > 0.0 && value < 1.0;
		});
	}

	@Test
	public void stoppedAlgorithmKeepsValueStable() throws Exception {
		final DataGeneratorVertex generator = create("label", "stopped-test",
				"algorithm", "incremental", "delay", "20", "min", "1", "max",
				"100");
		waitFor("first value", () -> data(generator) > 0);
		generator.property("algorithm", "stopped");
		Thread.sleep(40);
		final double frozen = data(generator);
		Thread.sleep(120);
		assertEquals(frozen, data(generator), 0.000001);
	}

	@Test
	public void triangularStaysWithinConfiguredRange() throws Exception {
		final DataGeneratorVertex generator = create("label", "tri-range",
				"algorithm", "triangular", "delay", "20", "min", "5", "max",
				"7");
		waitFor("triangular values", () -> data(generator) >= 5.0);
		for (int i = 0; i < 10; i++) {
			final double value = data(generator);
			assertTrue(value >= 5.0 && value <= 7.0, "value " + value);
			Thread.sleep(20);
		}
	}
}
