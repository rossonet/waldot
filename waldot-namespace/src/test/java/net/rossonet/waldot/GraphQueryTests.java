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
		final int size = g.traversal().V().has("name", "loop").in("self", "prova").toList().size();
		assert size == 1;
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

		// Verify vertex and edge counts
		assert g.getWaldotNamespace().getVerticesCount() == 6;
		assert g.getWaldotNamespace().getEdgesCount() == 6;

		// Verify all vertices exist via OPC UA and graph
		assert waldotTestClientHandler.checkOpcUaVertexExists(1);
		assert waldotTestClientHandler.checkVertexExists(1);
		assert waldotTestClientHandler.checkOpcUaVertexExists(2);
		assert waldotTestClientHandler.checkVertexExists(2);
		assert waldotTestClientHandler.checkOpcUaVertexExists(3);
		assert waldotTestClientHandler.checkVertexExists(3);
		assert waldotTestClientHandler.checkOpcUaVertexExists(4);
		assert waldotTestClientHandler.checkVertexExists(4);
		assert waldotTestClientHandler.checkOpcUaVertexExists(5);
		assert waldotTestClientHandler.checkVertexExists(5);
		assert waldotTestClientHandler.checkOpcUaVertexExists(6);
		assert waldotTestClientHandler.checkVertexExists(6);

		// Verify vertex properties - marko (person)
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(1, "name", "marko");
		assert waldotTestClientHandler.checkVertexValueEquals(1, "name", "marko");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(1, "age", 29);
		assert waldotTestClientHandler.checkVertexValueEquals(1, "age", 29);

		// Verify vertex properties - vadas (person)
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(2, "name", "vadas");
		assert waldotTestClientHandler.checkVertexValueEquals(2, "name", "vadas");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(2, "age", 27);
		assert waldotTestClientHandler.checkVertexValueEquals(2, "age", 27);

		// Verify vertex properties - lop (software)
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(3, "name", "lop");
		assert waldotTestClientHandler.checkVertexValueEquals(3, "name", "lop");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(3, "lang", "java");
		assert waldotTestClientHandler.checkVertexValueEquals(3, "lang", "java");

		// Verify vertex properties - josh (person)
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(4, "name", "josh");
		assert waldotTestClientHandler.checkVertexValueEquals(4, "name", "josh");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(4, "age", 32);
		assert waldotTestClientHandler.checkVertexValueEquals(4, "age", 32);

		// Verify vertex properties - ripple (software)
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(5, "name", "ripple");
		assert waldotTestClientHandler.checkVertexValueEquals(5, "name", "ripple");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(5, "lang", "java");
		assert waldotTestClientHandler.checkVertexValueEquals(5, "lang", "java");

		// Verify vertex properties - peter (person)
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(6, "name", "peter");
		assert waldotTestClientHandler.checkVertexValueEquals(6, "name", "peter");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals(6, "age", 35);
		assert waldotTestClientHandler.checkVertexValueEquals(6, "age", 35);

		// Verify graph traversal - marko knows vadas and josh
		assert g.traversal().V().has("name", "marko").out("knows").toList().size() == 2;
		// marko knows vadas (id=2) and josh (id=4)
		assert g.traversal().V().has("name", "marko").out("knows").values("name").toList().contains("vadas");
		assert g.traversal().V().has("name", "marko").out("knows").values("name").toList().contains("josh");

		// Verify graph traversal - marko created lop
		assert g.traversal().V().has("name", "marko").out("created").toList().size() == 1;
		assert g.traversal().V().has("name", "marko").out("created").values("name").next().equals("lop");

		// Verify josh created ripple and lop
		assert g.traversal().V().has("name", "josh").out("created").toList().size() == 2;
		assert g.traversal().V().has("name", "josh").out("created").values("name").toList().contains("ripple");
		assert g.traversal().V().has("name", "josh").out("created").values("name").toList().contains("lop");

		// Verify peter created lop
		assert g.traversal().V().has("name", "peter").out("created").toList().size() == 1;
		assert g.traversal().V().has("name", "peter").out("created").values("name").next().equals("lop");

		// Verify in edges
		assert g.traversal().V().has("name", "vadas").in("knows").toList().size() == 1;
		assert g.traversal().V().has("name", "vadas").in("knows").values("name").next().equals("marko");

		// Verify edge weights via traversal
		assert g.traversal().V().has("name", "marko").outE("knows").values("weight").next().equals(0.5d);
		// Verify josh is known by marko with weight 1.0
		assert g.traversal().V().has("name", "marko").out("knows").has("name", "josh").toList().size() == 1;

		// Verify software vertices (created by multiple people)
		assert g.traversal().V().has("name", "lop").in("created").toList().size() == 3;
		assert g.traversal().V().has("name", "ripple").in("created").toList().size() == 1;

		// Verify person vertices
		assert g.traversal().V().has("age").values("age").toList().size() == 4;
		assert g.traversal().V().has("lang").values("lang").toList().size() == 2;
	}

	@Test
	public void runTheCrew() throws Exception {
		LogHelper.changeJulLogLevel("fine");
		g = OpcFactory.createTheCrew();
		g.getWaldotNamespace().addListener(listener);
		Thread.sleep(500);
		waldotTestClientHandler = new WaldotTestClientHandler(g);

		// Verify vertex and edge counts (6 vertices, 14 edges)
		assert g.getWaldotNamespace().getVerticesCount() == 6;
		assert g.getWaldotNamespace().getEdgesCount() == 14;

		// Verify all person vertices exist via graph traversal
		assert g.traversal().V().has("name", "marko").toList().size() == 1;
		assert g.traversal().V().has("name", "stephen").toList().size() == 1;
		assert g.traversal().V().has("name", "matthias").toList().size() == 1;
		assert g.traversal().V().has("name", "daniel").toList().size() == 1;

		// Verify software vertices exist
		assert g.traversal().V().has("name", "gremlin").toList().size() == 1;
		assert g.traversal().V().has("name", "tinkergraph").toList().size() == 1;

		// Verify vertex properties via traversal - marko
		assert g.traversal().V().has("name", "marko").values("name").next().equals("marko");
		assert g.traversal().V().has("name", "marko").values("directory").next().equals("crew/marko");

		// Verify vertex properties - stephen
		assert g.traversal().V().has("name", "stephen").values("name").next().equals("stephen");
		assert g.traversal().V().has("name", "stephen").values("directory").next().equals("crew/stephen");

		// Verify vertex properties - matthias
		assert g.traversal().V().has("name", "matthias").values("name").next().equals("matthias");
		assert g.traversal().V().has("name", "matthias").values("directory").next().equals("crew/matthias");

		// Verify vertex properties - daniel
		assert g.traversal().V().has("name", "daniel").values("name").next().equals("daniel");
		assert g.traversal().V().has("name", "daniel").values("directory").next().equals("crew/daniel");

		// Verify vertex properties - gremlin (software)
		assert g.traversal().V().has("name", "gremlin").values("name").next().equals("gremlin");
		assert g.traversal().V().has("name", "gremlin").values("directory").next().equals("crew/gremlin");

		// Verify vertex properties - tinkergraph (software)
		assert g.traversal().V().has("name", "tinkergraph").values("name").next().equals("tinkergraph");
		assert g.traversal().V().has("name", "tinkergraph").values("directory").next().equals("crew/tinkergraph");

		// Verify graph traversal - marko develops gremlin and tinkergraph
		assert g.traversal().V().has("name", "marko").out("develops").toList().size() == 2;
		assert g.traversal().V().has("name", "marko").out("develops").values("name").toList().contains("gremlin");
		assert g.traversal().V().has("name", "marko").out("develops").values("name").toList().contains("tinkergraph");

		// Verify graph traversal - marko uses gremlin and tinkergraph
		assert g.traversal().V().has("name", "marko").out("uses").toList().size() == 2;
		assert g.traversal().V().has("name", "marko").out("uses").values("name").toList().contains("gremlin");
		assert g.traversal().V().has("name", "marko").out("uses").values("name").toList().contains("tinkergraph");

		// Verify graph traversal - stephen develops gremlin and tinkergraph
		assert g.traversal().V().has("name", "stephen").out("develops").toList().size() == 2;

		// Verify graph traversal - matthias develops gremlin
		assert g.traversal().V().has("name", "matthias").out("develops").toList().size() == 1;
		assert g.traversal().V().has("name", "matthias").out("develops").values("name").next().equals("gremlin");

		// Verify graph traversal - daniel uses gremlin and tinkergraph
		assert g.traversal().V().has("name", "daniel").out("uses").toList().size() == 2;

		// Verify graph traversal - gremlin traverses tinkergraph
		assert g.traversal().V().has("name", "gremlin").out("traverses").toList().size() == 1;
		assert g.traversal().V().has("name", "gremlin").out("traverses").values("name").next().equals("tinkergraph");

		// Verify edge properties - marko develops gremlin since 2009
		assert g.traversal().V().has("name", "marko").outE("develops").has("since", 2009).toList().size() == 1;

		// Verify in edges - gremlin developed by marko, stephen, matthias
		assert g.traversal().V().has("name", "gremlin").in("develops").toList().size() == 3;
		assert g.traversal().V().has("name", "gremlin").in("uses").toList().size() == 4;

		// Verify tinkergraph is developed by marko and stephen
		assert g.traversal().V().has("name", "tinkergraph").in("develops").toList().size() == 2;

		// Verify graph variables
		assert g.variables().keys().contains("creator");
		assert g.variables().keys().contains("lastModified");
		assert g.variables().keys().contains("comment");

		// Verify vertex labels
		assert g.traversal().V().hasLabel("person").toList().size() == 4;
		assert g.traversal().V().hasLabel("software").toList().size() == 2;

		// Verify multi-property location (the crew feature)
		// marko has multiple locations
		assert g.traversal().V().has("name", "marko").properties("location").toList().size() == 4;
		// stephen has 3 locations
		assert g.traversal().V().has("name", "stephen").properties("location").toList().size() == 3;
		// matthias has 4 locations
		assert g.traversal().V().has("name", "matthias").properties("location").toList().size() == 4;
		// daniel has 3 locations
		assert g.traversal().V().has("name", "daniel").properties("location").toList().size() == 3;
	}

}