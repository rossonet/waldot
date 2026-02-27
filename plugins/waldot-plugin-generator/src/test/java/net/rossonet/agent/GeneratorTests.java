package net.rossonet.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import net.rossonet.waldot.WaldotGeneratorPlugin;
import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

public class GeneratorTests {
	private WaldotGraph g;
	private final NamespaceListener listener = new TestNamespaceListener();
	private WaldotTestClientHandler waldotTestClientHandler;

	@AfterEach
	public void afterEach() {
		clean();
		System.out.println("Test completed");

	}

	@BeforeEach
	public void beforeEach(TestInfo testInfo) {
		System.out.println("Starting test " + testInfo.getTestMethod().get().getName());
		clean();
	}

	private void bootstrapUrlServerInit(String url)
			throws ConfigurationException, InterruptedException, ExecutionException {
		LogHelper.changeJulLogLevel("fine");
		g = OpcFactory.getOpcGraph(url, new LoggerHistoryStrategy());
		g.getWaldotNamespace().addListener(listener);
		Thread.sleep(500);
		waldotTestClientHandler = new WaldotTestClientHandler(g);
	}

	private void clean() {
		try {
			Files.deleteIfExists(Path.of("/tmp/boot.conf"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(Path.of("/tmp/index.txt"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		if (waldotTestClientHandler != null) {
			try {
				waldotTestClientHandler.disconnect();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		if (g != null && g.getWaldotNamespace() != null) {
			try {

				g.getWaldotNamespace().close();
				System.out.println("Graph namespace closed");
				g = null;
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		try {
			while (!NetworkHelper.checkLocalPortAvailable(12686)) {
				System.out.println("Waiting for server shutdown");
				Thread.sleep(5_000);
			}
			Thread.sleep(500);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(Path.of("/tmp/waldot-client.ks"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void create1000GeneratorNodes() throws Exception {
		simpleServerInit();
		for (int i = 0; i < 1000; i++) {
			g.addVertex(MiloStrategy.ID_PARAMETER, "test" + i, WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase(),
					"random", MiloStrategy.LABEL_FIELD.toLowerCase(), "test" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					WaldotGeneratorPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase(), "1000",
					WaldotGeneratorPlugin.MAX_VALUE_FIELD.toLowerCase(), "100",
					WaldotGeneratorPlugin.MIN_VALUE_FIELD.toLowerCase(), "90");
		}
		Thread.sleep(500L);
		for (int i = 0; i < 1000; i++) {
			assert waldotTestClientHandler.checkOpcUaVertexExists("test" + i);
			assert waldotTestClientHandler.checkVertexExists("test" + i);
			assert waldotTestClientHandler.checkOpcUaVertexValueBetween("test" + i, DataGeneratorVertex.VALUE_KEY, 90,
					100);
			assert waldotTestClientHandler.checkVertexValueBetween("test" + i, DataGeneratorVertex.VALUE_KEY, 90, 100);
		}
	}

	@Test
	public void createAllGenerator() throws Exception {
		simpleServerInit();
		for (int i = 0; i < 100; i++) {
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "random", MiloStrategy.ID_PARAMETER, "random" + i,
					WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase(), "random",
					MiloStrategy.LABEL_FIELD.toLowerCase(), "random" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					WaldotGeneratorPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase(), "1000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "incremental", MiloStrategy.ID_PARAMETER, "incremental" + i,
					WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase(), "incremental",
					MiloStrategy.LABEL_FIELD.toLowerCase(), "incremental" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					WaldotGeneratorPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase(), "1000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "decremental", MiloStrategy.ID_PARAMETER, "decremental" + i,
					WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase(), "decremental",
					MiloStrategy.LABEL_FIELD.toLowerCase(), "decremental" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					WaldotGeneratorPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase(), "1000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "sinusoidal", MiloStrategy.ID_PARAMETER, "sinusoidal" + i,
					WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase(), "sinusoidal",
					MiloStrategy.LABEL_FIELD.toLowerCase(), "sinusoidal" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					WaldotGeneratorPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase(), "5000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "triangular", MiloStrategy.ID_PARAMETER, "triangular" + i,
					WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase(), "triangular",
					MiloStrategy.LABEL_FIELD.toLowerCase(), "triangular" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					WaldotGeneratorPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase(), "8000");
		}
		Thread.sleep(500L);
		for (int i = 0; i < 100; i++) {
			assert waldotTestClientHandler.checkOpcUaVertexExists("random" + i);
			assert waldotTestClientHandler.checkVertexExists("incremental" + i);
			assert waldotTestClientHandler.checkOpcUaVertexExists("decremental" + i);
			assert waldotTestClientHandler.checkVertexExists("sinusoidal" + i);
			assert waldotTestClientHandler.checkOpcUaVertexExists("triangular" + i);
		}
	}

	@Test
	public void createSingleDecremental() throws Exception {
		simpleServerInit();
		final Vertex v = g.addVertex(MiloStrategy.ID_PARAMETER, "dectest", MiloStrategy.LABEL_FIELD.toLowerCase(),
				"dectest", MiloStrategy.TYPE_FIELD.toLowerCase(),
				WaldotGeneratorPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL, WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase(),
				"1000", WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase(), "decremental",
				WaldotGeneratorPlugin.MAX_VALUE_FIELD.toLowerCase(), "100",
				WaldotGeneratorPlugin.MIN_VALUE_FIELD.toLowerCase(), "1");
		System.out.println("created " + v);
		for (final String k : v.keys()) {
			System.out.println(k + ": " + v.property(k));
		}
		Thread.sleep(700);
		long value = 0;
		for (int i = 1; i < 20; i++) {
			if (value == 0) {
				value = waldotTestClientHandler.readIntOpcUaVertexValue("dectest", DataGeneratorVertex.VALUE_KEY);
				System.out.println("start from " + value);
				System.out.flush();
			}
			assert waldotTestClientHandler.checkOpcUaVertexValueEquals("dectest", DataGeneratorVertex.VALUE_KEY, value);
			assert waldotTestClientHandler.checkVertexValueEquals("dectest", DataGeneratorVertex.VALUE_KEY, value);
			System.out.println("ok: " + value);
			System.out.flush();
			value--;
			if (value < 1) {
				value = 100;
			}
			Thread.sleep(1000);
		}
	}

	@Test
	public void createSingleIncremetal() throws Exception {
		simpleServerInit();
		final Vertex v = g.addVertex(MiloStrategy.ID_PARAMETER, "inctest", MiloStrategy.LABEL_FIELD.toLowerCase(),
				"inctest", MiloStrategy.TYPE_FIELD.toLowerCase(),
				WaldotGeneratorPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL, WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase(),
				"1000", WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase(), "incremental",
				WaldotGeneratorPlugin.MAX_VALUE_FIELD.toLowerCase(), "100",
				WaldotGeneratorPlugin.MIN_VALUE_FIELD.toLowerCase(), "1");
		System.out.println("created " + v);
		for (final String k : v.keys()) {
			System.out.println(k + ": " + v.property(k));
		}
		Thread.sleep(700);
		long value = 0;
		for (int i = 1; i < 20; i++) {
			if (value == 0) {
				value = waldotTestClientHandler.readIntOpcUaVertexValue("inctest", DataGeneratorVertex.VALUE_KEY);
				System.out.println("start from " + value);
				System.out.flush();
			}
			assert waldotTestClientHandler.checkOpcUaVertexValueEquals("inctest", DataGeneratorVertex.VALUE_KEY, value);
			assert waldotTestClientHandler.checkVertexValueEquals("inctest", DataGeneratorVertex.VALUE_KEY, value);
			System.out.println("ok: " + value);
			System.out.flush();
			value++;
			Thread.sleep(1000);
		}
	}

	private void simpleServerInit() throws ConfigurationException, InterruptedException, ExecutionException {
		bootstrapUrlServerInit("file:///tmp/boot.conf");
	}
}
