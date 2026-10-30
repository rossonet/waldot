package net.rossonet.agent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.HotSpotDiagnosticMXBean.ThreadDumpFormat;

import net.rossonet.waldot.WaldotGeneratorPlugin;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex;
import net.rossonet.waldot.dataGenerator.GeneratorConstants;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.opc.WaldotOpcUaServer;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Closing the namespace must stop every generator thread and release the
 * namespace and its OPC UA server: before the fix every closed namespace
 * stayed in memory, kept alive by the virtual threads of its generators.
 */
public class GeneratorCloseReleasesNamespaceTest {

	/** Long delay: a generator stops only if its thread is interrupted. */
	private static final String LONG_DELAY = "600000";

	private static final int GENERATORS = 20;

	private static final Pattern THREAD_NAME = Pattern
			.compile("\"name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

	private static final long TIMEOUT_MS = 15_000;

	/**
	 * Names of the live threads of the process, virtual threads included (the
	 * JSON thread dump of the JDK lists them, Thread.getAllStackTraces() does
	 * not).
	 */
	private static Set<String> liveThreadNames() throws IOException {
		final Path dump = Files.createTempFile("generator-threads-", ".json");
		Files.delete(dump);
		try {
			ManagementFactory
					.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
					.dumpThreads(dump.toString(), ThreadDumpFormat.JSON);
			final Set<String> names = new HashSet<>();
			final Matcher matcher = THREAD_NAME
					.matcher(Files.readString(dump));
			while (matcher.find()) {
				names.add(matcher.group(1));
			}
			return names;
		} finally {
			Files.deleteIfExists(dump);
		}
	}

	private static void waitFor(final String what,
			final BooleanSupplier condition) throws InterruptedException {
		final long end = System.currentTimeMillis() + TIMEOUT_MS;
		while (System.currentTimeMillis() < end) {
			if (condition.getAsBoolean()) {
				return;
			}
			Thread.sleep(50);
		}
		throw new AssertionError("timeout waiting for " + what);
	}

	private static boolean anyAlive(final Set<String> names) {
		try {
			final Set<String> live = liveThreadNames();
			for (final String name : names) {
				if (live.contains(name)) {
					return true;
				}
			}
			return false;
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static boolean allAlive(final Set<String> names) {
		try {
			return liveThreadNames().containsAll(names);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
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
	public void closeInterruptsTheGeneratorThreadImmediately()
			throws Exception {
		final DataGeneratorVertex generator = create("single-close");
		final Set<String> name = Set.of(generator.getNodeId()
				.toParseableString());
		waitFor("generator thread", () -> allAlive(name));
		generator.close();
		assertTrue(generator.isClosed());
		waitFor("generator thread end", () -> !anyAlive(name));
		assertFalse(generator.isGenerating());
		// idempotent, and a closed generator does not restart
		generator.close();
		generator.resumeGeneration();
		assertFalse(generator.isGenerating());
	}

	private DataGeneratorVertex create(final String label) {
		final Vertex vertex = g.addVertex(MiloStrategy.TYPE_FIELD.toLowerCase(),
				GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL, "label",
				label, "algorithm", "incremental", "delay", LONG_DELAY, "min",
				"1", "max", "100");
		return assertInstanceOf(DataGeneratorVertex.class, vertex);
	}

	private WaldotGeneratorPlugin generatorPlugin() {
		for (final PluginListener plugin : g.getWaldotNamespace()
				.getPlugins()) {
			if (plugin instanceof final WaldotGeneratorPlugin generator) {
				return generator;
			}
		}
		throw new AssertionError("generator plugin not registered");
	}

	/** Only weak references and names: nothing that keeps the graph alive. */
	private record ClosedScenario(Set<String> names,
			List<WeakReference<DataGeneratorVertex>> generators,
			WeakReference<WaldotNamespace> namespace,
			WeakReference<WaldotOpcUaServer> server,
			WeakReference<WaldotGraph> graph) {
	}

	private static void assertGeneratorsClosed(
			final List<WeakReference<DataGeneratorVertex>> generators) {
		for (final WeakReference<DataGeneratorVertex> ref : generators) {
			final DataGeneratorVertex generator = ref.get();
			if (generator != null) {
				assertTrue(generator.isClosed());
				assertFalse(generator.isGenerating());
			}
		}
	}

	/**
	 * Creates the generators and closes the namespace; the strong references
	 * stay in this frame.
	 */
	private ClosedScenario createGeneratorsAndCloseTheNamespace()
			throws Exception {
		final Set<String> names = new HashSet<>();
		final List<WeakReference<DataGeneratorVertex>> generators = new ArrayList<>();
		for (int i = 0; i < GENERATORS; i++) {
			final DataGeneratorVertex generator = create("leak-" + i);
			names.add(generator.getNodeId().toParseableString());
			generators.add(new WeakReference<>(generator));
		}
		// the detection works: the threads are visible before the close
		waitFor("generator threads", () -> allAlive(names));
		final WaldotGeneratorPlugin plugin = generatorPlugin();
		final WaldotNamespace namespace = g.getWaldotNamespace();
		final WaldotOpcUaServer server = namespace.getOpcuaServer();
		assertNotNull(server);
		final ClosedScenario scenario = new ClosedScenario(names, generators,
				new WeakReference<>(namespace), new WeakReference<>(server),
				new WeakReference<>(g));

		namespace.close();

		assertTrue(plugin.isClosed(), "plugin closed by the namespace");
		g = null;
		return scenario;
	}

	@Test
	public void namespaceCloseStopsTheGeneratorsAndReleasesTheNamespace()
			throws Exception {
		final ClosedScenario scenario = createGeneratorsAndCloseTheNamespace();
		waitFor("end of the generator threads",
				() -> !anyAlive(scenario.names()));
		assertGeneratorsClosed(scenario.generators());
		assertTrue(WaldotTestEnvironment.awaitOpcPortReleased());
		WaldotTestEnvironment.deleteDefaultFiles();

		// no strong reference left in the test: the closed namespace, its
		// graph and its server must be collectable
		waitFor("collection of the closed namespace", () -> {
			System.gc();
			return scenario.namespace().get() == null
					&& scenario.server().get() == null
					&& scenario.graph().get() == null;
		});
	}

	@Test
	public void pluginStopPausesAndStartResumesTheGenerators()
			throws Exception {
		final DataGeneratorVertex generator = create("pause");
		final Set<String> name = Set.of(generator.getNodeId()
				.toParseableString());
		waitFor("generator thread", () -> allAlive(name));
		final WaldotGeneratorPlugin plugin = generatorPlugin();
		plugin.stop();
		waitFor("paused generator", () -> !generator.isGenerating());
		waitFor("thread released by the pause", () -> !anyAlive(name));
		assertFalse(generator.isClosed());
		assertFalse(plugin.isClosed());
		plugin.start();
		assertTrue(generator.isGenerating());
		waitFor("generator thread after the restart", () -> allAlive(name));
	}
}
