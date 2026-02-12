package net.rossonet.waldot.gremlins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
public class DeleteVertexAndEdgeTests {
	private WaldotGraph g;
	private final NamespaceListener listener = new TestNamespaceListener();
	private WaldotTestClientHandler waldotTestClientHandler;

	@AfterEach
	public void afterEach() {
		clean();
		System.out.println("Test completed");

	}

	@Test
	@Disabled("Edge deletion is not tested yet")
	public void baseDeleteEdgeWithOpcTest() throws Exception {
		// TODO verifica cancellazione edge
		simpleServerInit();
		OpcFactory.generateModern(g);
		System.out.println("Graph generated");
		assert waldotTestClientHandler.checkOpcUaVertexExists(9);
		// assert waldotTestClientHandler.checkOpcUaVertexEdge(1, 3, "created");
		waldotTestClientHandler.deleteVertexWithOpcUa(9);
		Thread.sleep(500);
		assert !waldotTestClientHandler.checkOpcUaVertexExists(9);
		// assert !waldotTestClientHandler.checkOpcUaVertexEdge(1, 3, "created");
	}

	@Test
	public void baseDeleteVertexTest() throws Exception {
		simpleServerInit();
		OpcFactory.generateModern(g);
		System.out.println("Graph generated");
		assert waldotTestClientHandler.checkOpcUaVertexExists(1);
		assert waldotTestClientHandler.checkVertexExists(1);
		g.traversal().V().has("name", "marko").drop().iterate();
		Thread.sleep(500);
		assert !waldotTestClientHandler.checkOpcUaVertexExists(1);
		assert !waldotTestClientHandler.checkVertexExists(1);
	}

	@Test
	public void baseDeleteVertexWithOpcTest() throws Exception {
		simpleServerInit();
		OpcFactory.generateModern(g);
		System.out.println("Graph generated");
		assert waldotTestClientHandler.checkOpcUaVertexExists(1);
		assert waldotTestClientHandler.checkVertexExists(1);
		waldotTestClientHandler.deleteVertexWithOpcUa(1);
		Thread.sleep(500);
		assert !waldotTestClientHandler.checkOpcUaVertexExists(1);
		assert !waldotTestClientHandler.checkVertexExists(1);
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

	private void simpleServerInit() throws ConfigurationException, InterruptedException, ExecutionException {
		bootstrapUrlServerInit("file:///tmp/boot.conf");
	}
}
