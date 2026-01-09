package net.rossonet.waldot.gremlins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.WaldotTestClientHandler;
import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.models.WaldotGraph;
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
		try {
			Files.deleteIfExists(Path.of("/tmp/boot.conf"));
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
		try {
			g.getWaldotNamespace().close();
			System.out.println("Graph namespace closed");
			g = null;
		} catch (final Exception e) {
			e.printStackTrace();
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
		System.out.println("Test completed");

	}

	private void bootstrapUrlServerInit(String url)
			throws ConfigurationException, InterruptedException, ExecutionException {
		LogHelper.changeJulLogLevel("fine");
		g = OpcFactory.getOpcGraph(url, new BaseHistoryStrategy());
		g.getWaldotNamespace().addListener(listener);
		Thread.sleep(500);
		waldotTestClientHandler = new WaldotTestClientHandler(g);
	}

	@Test
	public void runAboutCommandExpression() throws Exception {
		simpleServerInit();
		final Object result = g.getWaldotNamespace().getConsoleStrategy().runExpression("about.exec()");
		if (result != null && result instanceof String[]) {
			final String[] aboutLines = (String[]) result;
			assert aboutLines.length > 0;
			assert aboutLines[3].equals("https://www.apache.org/licenses/LICENSE-2.0");
		} else {
			throw new Exception("About command did not return expected result");
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
	public void runRemoteConfigurationExpression() throws Exception {
		simpleServerInit();
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "value", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "value", 12);
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

	@Test
	public void writeNumberWithOpc() throws Exception {
		simpleServerInit();
		g.addVertex("id", "test-id", "label", "test-query", "value", 12);
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "value", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "value", 12);
		waldotTestClientHandler.writeOpcUaVertexValue("test-id", "value", 34);
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "value", 34);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "value", 34);
	}

	@Test
	public void writeStringWithOpc() throws Exception {
		simpleServerInit();
		final String firstValue = UUID.randomUUID().toString();
		g.addVertex("id", "test-id", "label", "test-query", "value", firstValue);
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "value", firstValue);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "value", firstValue);
		for (int i = 0; i < 20; i++) {
			final String newValue = UUID.randomUUID().toString();
			waldotTestClientHandler.writeOpcUaVertexValue("test-id", "value", newValue);
			assert g.getWaldotNamespace().getVerticesCount() == 1;
			assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "value", newValue);
			assert waldotTestClientHandler.checkVertexValueEquals("test-id", "value", newValue);
		}
	}

}
