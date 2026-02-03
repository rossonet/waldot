package net.rossonet.waldot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.gremlins.TestNamespaceListener;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

public class GraphQueryTests {
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

	private void clean() {
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
	public void runKitchenSink() throws Exception {
		LogHelper.changeJulLogLevel("fine");
		g = OpcFactory.createKitchenSink();
		g.getWaldotNamespace().addListener(listener);
		Thread.sleep(500);
		waldotTestClientHandler = new WaldotTestClientHandler(g);
		assert g.getWaldotNamespace().getVerticesCount() == 3;
		assert g.getWaldotNamespace().getEdgesCount() == 3;
		assert g.traversal().V().has("name", "loop").values("name").next().equals("loop");
		assert waldotTestClientHandler.checkOpcUaVertexExists(1000);
		assert waldotTestClientHandler.checkVertexExists(1000);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(1000, "name", "loop");
		assert waldotTestClientHandler.checkVertexValueEquals(1000, "name", "loop");
		assert waldotTestClientHandler.checkOpcUaVertexExists(2000);
		assert waldotTestClientHandler.checkVertexExists(2000);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(2000, "name", "a");
		assert waldotTestClientHandler.checkVertexValueEquals(2000, "name", "a");
		assert waldotTestClientHandler.checkOpcUaVertexExists(2001);
		assert waldotTestClientHandler.checkVertexExists(2001);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(2001, "name", "b");
		assert waldotTestClientHandler.checkVertexValueEquals(2001, "name", "b");
		assert g.traversal().V().has("name", "a").out().toList().size() == 2;
		assert g.traversal().V().has("name", "a").in().toList().size() == 1;
		assert g.traversal().V().has("name", "b").out().toList().size() == 0;
		assert g.traversal().V().has("name", "b").in().toList().size() == 1;
		assert g.traversal().V().has("name", "loop").in().toList().size() == 1;
		assert g.traversal().V().has("name", "loop").in().toList().size() == 1;
		assert g.traversal().V().has("name", "loop").in("test", "no").toList().size() == 0;
		assert g.traversal().V().has("name", "loop").in("no", "prova").toList().size() == 0;
		assert g.traversal().V().has("name", "loop").in("test", "prova").toList().size() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(1001, "test", "prova");
		assert !waldotTestClientHandler.checkOpcUaVertexEdge(2001, 2000, "link");
		assert waldotTestClientHandler.checkOpcUaVertexEdge(2000, 2001, "link");
		assert waldotTestClientHandler.checkOpcUaVertexEdge(1000, 1000, "self");
	}

	@Test
	public void runModern() throws Exception {
		LogHelper.changeJulLogLevel("fine");
		g = OpcFactory.createModern();
		g.getWaldotNamespace().addListener(listener);
		Thread.sleep(500);
		waldotTestClientHandler = new WaldotTestClientHandler(g);
		Thread.sleep(5000000);
	}

	@Test
	public void runTheCrew() throws Exception {
		LogHelper.changeJulLogLevel("fine");
		g = OpcFactory.createTheCrew();
		g.getWaldotNamespace().addListener(listener);
		Thread.sleep(500);
		waldotTestClientHandler = new WaldotTestClientHandler(g);
	}

}