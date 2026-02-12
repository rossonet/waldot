package net.rossonet.waldot.gremlins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
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
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.BaseHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

@TestMethodOrder(OrderAnnotation.class)
public class OpcGraphManagerTests {
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

	@Test
	public void checkEdgeCreation() throws Exception {
		simpleServerInit();
		waldotTestClientHandler.createVertexWithOpcUa("a", "A", null, new String[0]);
		waldotTestClientHandler.createVertexWithOpcUa("b", "B", null, new String[0]);
		waldotTestClientHandler.createEdgeWithOpcUa("type_test", "ns=2;s=a", "ns=2;s=b", new String[0]);
		assert g.traversal().V().has("label", "A").toList().size() == 1;
		assert g.traversal().V().has("label", "B").toList().size() == 1;
		assert g.traversal().E().toList().size() == 1;
		assert g.traversal().V().has("label", "A").out().toList().size() == 1;
		assert g.traversal().V().has("label", "B").in().toList().size() == 1;
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

	private void simpleServerInit() throws ConfigurationException, InterruptedException, ExecutionException {
		bootstrapUrlServerInit("file:///tmp/boot.conf");
	}

	@Test
	public void writeNumberWithOpc() throws Exception {
		simpleServerInit();
		waldotTestClientHandler.createVertexWithOpcUa("test-id", "test-query", null, new String[] { "value", "12" });
		System.out.println("VERTICES - " + g.getVerticesCount());
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "value", "12");
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "value", "12");
		waldotTestClientHandler.writeOpcUaVertexValue("test-id", "value", 34);
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "value", 34);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "value", 34);
	}

	@Test
	public void writeNumberWithOpcSimple() throws Exception {
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
		waldotTestClientHandler.createVertexWithOpcUa("test-id", "test-query", null, new String[] { "value", firstValue });
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

	@Test
	public void writeStringWithOpcSimple() throws Exception {
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
