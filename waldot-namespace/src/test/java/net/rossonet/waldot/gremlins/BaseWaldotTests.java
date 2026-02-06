package net.rossonet.waldot.gremlins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.configuration.DefaultHomunculusConfiguration;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.BaseHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

@TestMethodOrder(OrderAnnotation.class)
public class BaseWaldotTests {
	private WaldotGraph g;
	private final NamespaceListener listener = new TestNamespaceListener();
	private WaldotTestClientHandler waldotTestClientHandler;

	@AfterEach
	public void afterEach() {
		clean();
		System.out.println("Test completed");

	}

	@BeforeEach
	public void beforeEach() {
		System.out.println("Starting test...");
		clean();
	}

	private void bootstrapUrlServerInit(String url)
			throws ConfigurationException, InterruptedException, ExecutionException {
		LogHelper.changeJulLogLevel("fine");
		g = OpcFactory.getOpcGraph(url, new BaseHistoryStrategy());
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
	public void runAboutCommandExpression() throws Exception {
		simpleServerInit();
		final List<String> comparation = new ArrayList<>();
		final Object result = g.getWaldotNamespace().getConsoleStrategy().runExpression("about.exec()");
		if (result != null && result instanceof String[]) {
			final String[] aboutLines = (String[]) result;
			assert aboutLines.length == 5;
			for (final String line : aboutLines) {
				comparation.add(line);
			}
			assert aboutLines[3].equals("https://www.apache.org/licenses/LICENSE-2.0");
		} else {
			throw new Exception("About command did not return expected result");
		}
		final List<String> info = waldotTestClientHandler.getServerInfo();
		assert info.size() == 5;
		for (int i = 0; i < info.size(); i++) {
			assert info.get(i).equals(comparation.get(i));
		}

	}

	@Test
	public void runGraphExpression() throws Exception {
		simpleServerInit();
		g.addVertex("id", "test-id", "label", "test-query", "value", 12);
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "value", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "value", 12);
	}

	@Test
	public void runGraphExpressionViaClient() throws Exception {
		simpleServerInit();
		waldotTestClientHandler.runExpression("g.addVertex('id', 'test-id', 'label', 'test-query', 'value', 28)");
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "value", 28);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "value", 28);
	}

	@Test
	public void runHelpCommandExpression() throws Exception {
		DefaultHomunculusConfiguration.DEFAULT_HELP_DIRECTORY = "/tmp";
		final StringBuilder sb = new StringBuilder();
		sb.append("HELP_TEST\n");
		sb.append("SECONDA LINEA\n");
		Files.writeString(Path.of("/tmp/index.txt"), sb.toString());
		final StringBuilder sb2 = new StringBuilder();
		sb2.append("https://www.apache.org/licenses/LICENSE-2.0\n");
		Files.writeString(Path.of("/tmp/funzione.txt"), sb2.toString());
		simpleServerInit();
		final Object result = g.getWaldotNamespace().getConsoleStrategy().runExpression("help.exec()");
		if (result != null && result instanceof String[]) {
			final String[] helpLines = (String[]) result;
			assert helpLines.length > 0;
			assert helpLines[0].contains("HELP_TEST");
			assert helpLines[0].contains("SECONDA LINEA");
		} else {
			throw new Exception("Help command did not return expected result");
		}
		final Object resultSub = g.getWaldotNamespace().getConsoleStrategy().runExpression("help.exec(\"funzione\")");
		if (resultSub != null && resultSub instanceof String[]) {
			final String[] subLines = (String[]) resultSub;
			assert subLines.length > 0;
			System.out.println("Sub help output: " + subLines[0]);
			assert subLines[0].equals("https://www.apache.org/licenses/LICENSE-2.0\n");
		} else {
			throw new Exception("Help command did not return expected result");
		}
	}

	@Test
	public void runQueryCommandExpression() throws Exception {
		simpleServerInit();
		g.addVertex("id", "test-id", "label", "test-query", "value", "prova");
		final Object result = g.getWaldotNamespace().getConsoleStrategy()
				.runExpression("query.exec(\"g.traversal().V().has('label', 'test-query').values('value').next()\")");
		if (result != null && result instanceof String[]) {
			final String[] queryLines = (String[]) result;
			assert queryLines.length > 0;
			assert queryLines[0].equals("prova");
		} else {
			throw new Exception("Query command did not return expected result");
		}
	}

	@Test
	public void runRemoteConfigurationExpression() throws Exception {
		bootstrapUrlServerInit(
				"https://raw.githubusercontent.com/rossonet/waldot/refs/heads/master/waldot-namespace/src/test/resources/remote_conf.txt");
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-remote");
		assert waldotTestClientHandler.checkVertexExists("test-remote");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-remote", "value", 55);
		assert waldotTestClientHandler.checkVertexValueEquals("test-remote", "value", 55);
	}

	@Test
	public void runSimpleConfigurationExpression() throws Exception {
		final StringBuilder sb = new StringBuilder();
		sb.append("g.addVertex(\"id\", \"test-id\", \"label\", \"test-query\", \"value\", 12);\n");
		Files.writeString(Path.of("/tmp/boot.conf"), sb.toString());
		simpleServerInit();
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "value", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "value", 12);
	}

	@Test
	public void runSimpleConsoleExpression() throws Exception {
		simpleServerInit();
		g.getWaldotNamespace().getConsoleStrategy()
				.runExpression("g.addVertex('id', 'test-id', 'label', 'test-query', 'value', 10)");
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "value", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "value", 10);
	}

	private void simpleServerInit() throws ConfigurationException, InterruptedException, ExecutionException {
		bootstrapUrlServerInit("file:///tmp/boot.conf");
	}

}
