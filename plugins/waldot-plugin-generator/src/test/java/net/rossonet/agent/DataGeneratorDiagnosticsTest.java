package net.rossonet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.function.BooleanSupplier;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.rossonet.waldot.WaldotGeneratorPlugin;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex;
import net.rossonet.waldot.dataGenerator.GeneratorConstants;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Decimal bounds of sim:generator and the errors that were silent: a value that
 * is not a number, min greater than max, an unknown algorithm, an invalid delay
 * end up in run!last-error instead of only in the log.
 */
public class DataGeneratorDiagnosticsTest {

	private static final long WAIT_MS = 5000;

	private static void waitFor(final String what, final BooleanSupplier condition) throws InterruptedException {
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
		g = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
	}

	private DataGeneratorVertex create(final Object... keyValues) {
		final Object[] all = new Object[keyValues.length + 2];
		all[0] = MiloStrategy.TYPE_FIELD.toLowerCase();
		all[1] = GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL;
		System.arraycopy(keyValues, 0, all, 2, keyValues.length);
		final Vertex vertex = g.addVertex(all);
		return assertInstanceOf(DataGeneratorVertex.class, vertex);
	}

	private static double data(final DataGeneratorVertex generator) {
		final VertexProperty<Object> p = generator.property(DataGeneratorVertex.VALUE_KEY);
		return p.isPresent() ? Double.parseDouble(p.value().toString()) : Double.NaN;
	}

	private static String lastError(final Vertex v) {
		final VertexProperty<Object> p = v.property("run!last-error");
		return p.isPresent() ? String.valueOf(p.value()) : "";
	}

	@Test
	public void algorithmIsCaseInsensitive() {
		final DataGeneratorVertex generator = create("algorithm", "Random", "delay", "20");
		assertEquals("", lastError(generator));
		assertEquals("random", generator.property("algorithm").value().toString().toLowerCase());
	}

	@Test
	public void createCommandAcceptsDecimals() {
		AbstractCreateVertexCommand command = null;
		for (final PluginListener p : g.getWaldotNamespace().getPlugins()) {
			if (p instanceof WaldotGeneratorPlugin) {
				for (final WaldotCommand c : p.getCommands()) {
					if (c instanceof final AbstractCreateVertexCommand create
							&& create.getVertexType().equals(GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL)) {
						command = create;
					}
				}
			}
		}
		final String[] result = command.runCommand(null,
				new String[] { "gen-decimal", "", "random", "20", "20.6", "21.5", "" });
		assertFalse(result[0].startsWith(AbstractCreateVertexCommand.ERROR_PREFIX), result[0]);
		final Vertex vertex = g.getWaldotNamespace().getVertexNode(NodeId.parse(result[0]));
		assertEquals("", lastError(vertex));
		final String refused = command.runCommand(null,
				new String[] { "gen-wrong", "", "random", "20", "21.5", "20.6", "" })[0];
		assertTrue(refused.startsWith(AbstractCreateVertexCommand.ERROR_PREFIX), refused);
	}

	@Test
	public void decimalBoundsAreKept() throws Exception {
		// the case of the session: gas pressure between 20.6 and 21.5 mbar
		final DataGeneratorVertex generator = create("algorithm", "random", "delay", "20", "min", "20.6", "max",
				"21.5");
		assertEquals("", lastError(generator));
		waitFor("a value", () -> !Double.isNaN(data(generator)));
		for (int i = 0; i < 20; i++) {
			final double value = data(generator);
			assertTrue(value >= 20.6 && value <= 21.5, "value " + value);
			Thread.sleep(25);
		}
	}

	@Test
	public void decimalCommaIsAccepted() throws Exception {
		final DataGeneratorVertex generator = create("algorithm", "random", "delay", "20", "min", "1,5", "max",
				"2,5");
		assertEquals("", lastError(generator));
		waitFor("a value", () -> !Double.isNaN(data(generator)));
		assertTrue(data(generator) >= 1.5 && data(generator) <= 2.5);
	}

	@Test
	public void incrementalWrapsToMin() throws Exception {
		final DataGeneratorVertex generator = create("algorithm", "incremental", "delay", "20", "min", "10", "max",
				"12");
		waitFor("wrap to min", () -> data(generator) == 10.0);
		waitFor("then 11", () -> data(generator) == 11.0);
	}

	@Test
	public void invalidDelayIsReported() {
		final DataGeneratorVertex generator = create("algorithm", "random", "delay", "5");
		assertTrue(lastError(generator).contains("'delay'"), lastError(generator));
		assertTrue(lastError(generator).contains("10 ms"), lastError(generator));
	}

	@Test
	public void longValuesOfOldSnapshotsStillWork() throws Exception {
		final DataGeneratorVertex generator = create("algorithm", "incremental", "delay", "20", "min", 3L, "max",
				5L);
		assertEquals("", lastError(generator));
		waitFor("integer values", () -> data(generator) == 4.0 || data(generator) == 5.0);
		final double value = data(generator);
		assertEquals(Math.rint(value), value, "with whole bounds the values stay whole");
	}

	@Test
	public void minGreaterThanMaxIsReported() {
		final DataGeneratorVertex generator = create("algorithm", "random", "delay", "20", "min", "30", "max", "10");
		// the values given at creation are applied again as runtime changes: the last
		// check refuses max=10 because it is lower than min
		final String error = lastError(generator);
		assertTrue(error.contains("maggiore di max") || error.contains("minore di min"), error);
		final double min = Double.parseDouble(generator.property("min").value().toString());
		final double max = Double.parseDouble(generator.property("max").value().toString());
		assertTrue(min <= max, min + " > " + max);
	}

	@ParameterizedTest
	@ValueSource(strings = { "abc", "20..6", "NaN", "Infinity", "1e400" })
	public void notANumberIsReported(final String bound) {
		final DataGeneratorVertex generator = create("algorithm", "random", "delay", "20", "min", bound);
		final String error = lastError(generator);
		assertTrue(error.contains("'min'"), error);
		assertTrue(error.contains("non è un numero"), error);
		assertTrue(error.contains("default") || error.contains("resta"), error);
		assertEquals(0.0, Double.parseDouble(generator.property("min").value().toString()));
		assertTrue(!String.valueOf(generator.property("run!last-error-time").value()).isBlank());
	}

	@Test
	public void runtimeChangesAreValidated() {
		final DataGeneratorVertex generator = create("algorithm", "random", "delay", "20", "min", "0", "max", "10");
		generator.property("min", "2.5");
		assertEquals("", lastError(generator));
		generator.property("max", "abc");
		assertTrue(lastError(generator).contains("'max'"), lastError(generator));
		assertEquals("10", generator.property("max").value().toString(), "the previous value is restored");
		generator.property("min", 50);
		assertTrue(lastError(generator).contains("maggiore di max"), lastError(generator));
		generator.property("algorithm", "zigzag");
		assertTrue(lastError(generator).contains("zigzag"), lastError(generator));
		assertTrue(lastError(generator).contains("random"), "the list of the algorithms: " + lastError(generator));
	}

	@Test
	public void unknownAlgorithmIsReported() {
		final DataGeneratorVertex generator = create("algorithm", "zigzag", "delay", "20");
		final String error = lastError(generator);
		assertTrue(error.contains("'algorithm'"), error);
		assertTrue(error.contains("incremental"), error);
	}
}
