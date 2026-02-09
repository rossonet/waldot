package net.rossonet.waldot.gremlins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.BaseHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

@TestMethodOrder(OrderAnnotation.class)
public class LabelInterationTests {
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
	public void descriptionRewriteTest() throws Exception {
		simpleServerInit();
		String expectedValue = UUID.randomUUID().toString();
		final Vertex v = g.addVertex("description", expectedValue, "id", "description1");
		Thread.sleep(500);
		assert waldotTestClientHandler.checkOpcUaVertexExists("description1");
		assert waldotTestClientHandler.checkOpcUaVertexDescriptionValueEquals("description1", expectedValue);
		for (int i = 0; i < 10; i++) {
			expectedValue = UUID.randomUUID().toString();
			v.property("description", expectedValue);
			Thread.sleep(500);
			assert waldotTestClientHandler.checkOpcUaVertexDescriptionValueEquals("description1", expectedValue);

		}

	}

	@Test
	public void edgeDescriptionRewriteTest() throws Exception {
		simpleServerInit();
		String expectedValue = UUID.randomUUID().toString();
		final Vertex a = g.addVertex("id", 1);
		final Vertex b = g.addVertex("id", 2);
		final WaldotEdge e = (WaldotEdge) a.addEdge("test label", b, "description", expectedValue);
		Thread.sleep(500);
		assert waldotTestClientHandler.checkOpcUaVertexEdge(1, 2, "test label");
		assert waldotTestClientHandler.checkOpcUaEdgeDescriptionValueEquals(e, expectedValue);
		for (int i = 0; i < 10; i++) {
			expectedValue = UUID.randomUUID().toString();
			e.property("description", expectedValue);
			Thread.sleep(500);
			assert waldotTestClientHandler.checkOpcUaEdgeDescriptionValueEquals(e, expectedValue);

		}

	}

	@Test
	public void labelReadOnlyInEdgeTest() throws Exception {
		simpleServerInit();
		final String expectedValue = UUID.randomUUID().toString();
		final Vertex a = g.addVertex("id", 1);
		final Vertex b = g.addVertex("id", 2);
		final WaldotEdge edge = (WaldotEdge) a.addEdge(expectedValue, b);
		Thread.sleep(500);
		assert waldotTestClientHandler.checkOpcUaVertexEdge(1, 2, expectedValue);
		assert waldotTestClientHandler.checkOpcUaEdgeLabelValueEquals(edge, expectedValue);
		assert waldotTestClientHandler.checkOpcUaEdgeLabelReadOnly(edge);
		assert waldotTestClientHandler.checkOpcUaEdgeLabelValueEquals(edge, expectedValue);

	}

	@Test
	public void labelReadOnlyInVertexTest() throws Exception {
		simpleServerInit();
		g.addVertex("label", "prova123", "id", "label1");
		Thread.sleep(500);
		assert waldotTestClientHandler.checkOpcUaVertexExists("label1");
		assert waldotTestClientHandler.checkOpcUaVertexLabelValueEquals("label1", "prova123");
		assert waldotTestClientHandler.checkOpcUaVertexLabelReadOnly("label1");
		assert waldotTestClientHandler.checkOpcUaVertexLabelValueEquals("label1", "prova123");

	}

	@Test
	public void labelRewriteByOpcTest() throws Exception {
		simpleServerInit();
		final String expectedValue = UUID.randomUUID().toString();
		g.addVertex("label", expectedValue, "id", "label2");
		Thread.sleep(500);
		assert waldotTestClientHandler.checkOpcUaVertexExists("label2");
		assert waldotTestClientHandler.checkOpcUaVertexLabelValueEquals("label2", expectedValue);
		for (int i = 0; i < 10; i++) {
			final String newValue = UUID.randomUUID().toString();
			try {
				waldotTestClientHandler.writeOpcUaVertexValue("label2", "label", newValue);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			Thread.sleep(500);
			assert waldotTestClientHandler.checkOpcUaVertexLabelValueEquals("label2", expectedValue);
		}

	}

	@Test
	public void labelRewriteTest() throws Exception {
		simpleServerInit();
		final String expectedValue = UUID.randomUUID().toString();
		final Vertex v = g.addVertex("label", expectedValue, "id", "label2");
		Thread.sleep(500);
		assert waldotTestClientHandler.checkOpcUaVertexExists("label2");
		assert waldotTestClientHandler.checkOpcUaVertexLabelValueEquals("label2", expectedValue);
		for (int i = 0; i < 10; i++) {
			final String newValue = UUID.randomUUID().toString();
			try {
				v.property("label", newValue);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			Thread.sleep(500);
			assert waldotTestClientHandler.checkOpcUaVertexLabelValueEquals("label2", expectedValue);
		}

	}

	@Test
	public void nameEdgeRewriteTest() throws Exception {
		simpleServerInit();
		String expectedValue = UUID.randomUUID().toString();
		final Vertex a = g.addVertex("id", 11);
		final Vertex b = g.addVertex("id", 22);
		final WaldotEdge edge = (WaldotEdge) a.addEdge("test_name", b, "name", expectedValue);
		Thread.sleep(500);
		assert waldotTestClientHandler.checkOpcUaVertexEdge(11, 22, "test_name");
		assert waldotTestClientHandler.checkOpcUaEdgeDisplayNameValueEquals(edge, expectedValue);
		assert waldotTestClientHandler.checkOpcUaEdgeBrowserNameNameValueEquals(edge, expectedValue);
		for (int i = 0; i < 10; i++) {
			expectedValue = UUID.randomUUID().toString();
			edge.property("name", expectedValue);
			Thread.sleep(500);
			assert waldotTestClientHandler.checkOpcUaEdgeDisplayNameValueEquals(edge, expectedValue);
			assert waldotTestClientHandler.checkOpcUaEdgeBrowserNameNameValueEquals(edge, expectedValue);

		}

	}

	@Test
	public void nameRewriteTest() throws Exception {
		simpleServerInit();
		String expectedValue = UUID.randomUUID().toString();
		final Vertex v = g.addVertex("name", expectedValue, "id", "name1");
		Thread.sleep(500);
		assert waldotTestClientHandler.checkOpcUaVertexExists("name1");
		assert waldotTestClientHandler.checkOpcUaVertexDisplayNameValueEquals("name1", expectedValue);
		assert waldotTestClientHandler.checkOpcUaVertexBrowserNameNameValueEquals("name1", expectedValue);
		for (int i = 0; i < 10; i++) {
			expectedValue = UUID.randomUUID().toString();
			v.property("name", expectedValue);
			Thread.sleep(500);
			assert waldotTestClientHandler.checkOpcUaVertexDisplayNameValueEquals("name1", expectedValue);
			assert waldotTestClientHandler.checkOpcUaVertexBrowserNameNameValueEquals("name1", expectedValue);

		}

	}

	private void simpleServerInit() throws ConfigurationException, InterruptedException, ExecutionException {
		bootstrapUrlServerInit("file:///tmp/boot.conf");
	}

	@Test
	public void typeRewriteByOpcTest() throws Exception {
		simpleServerInit();
		final String expectedValue = "vertex";
		final Vertex v = g.addVertex("label", "prova", "id", "label2");
		Thread.sleep(500);
		assert waldotTestClientHandler.checkOpcUaVertexExists("label2");
		assert waldotTestClientHandler.checkOpcUaVertexTypeValueEquals("label2", expectedValue);
		for (int i = 0; i < 10; i++) {
			final String newValue = UUID.randomUUID().toString();
			try {
				waldotTestClientHandler.writeOpcUaVertexValue("label2", "type", newValue);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			try {
				v.property("type", newValue);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			Thread.sleep(500);
			assert waldotTestClientHandler.checkOpcUaVertexTypeValueEquals("label2", expectedValue);
		}

	}
}
