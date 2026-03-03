package net.rossonet.waldot.gremlins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.models.MonitoredEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

public class LinkAndFireTests {
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

	private void bootstrapUrlServerInit(final String url)
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
	public void runLinkFromActiveTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = destination.addEdge("link from test", source, "type", "link-from");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.ACTIVE_LABEL, "false");
		source.property("a", 12);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.ACTIVE_LABEL, "true");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 14);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 14);
	}

	@Test
	public void runLinkFromDeadBandTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = destination.addEdge("link from test", source, "type", "link-from");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		e.property(MonitoredEdge.DEADBAND_TYPE_LABEL, MonitoredEdge.PERCENTAGE);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.DEADBAND_LABEL, 5);
		e.property(MonitoredEdge.DEADBAND_TYPE_LABEL, MonitoredEdge.ABSOLUTE);
		source.property("a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 6);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 15);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 16);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 16);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 16);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 5);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 4);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 4);
		source.property("c", "test");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "c", "test");
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "c", "test");
		source.property("a", 1000);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		e.property(MonitoredEdge.DEADBAND_LABEL, 10);
		e.property(MonitoredEdge.DEADBAND_TYPE_LABEL, MonitoredEdge.PERCENTAGE);
		source.property("a", 1100);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		source.property("a", 1067);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		source.property("a", 1101);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1101);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1101);
		e.property(MonitoredEdge.DEADBAND_LABEL, 1);
		source.property("a", 1000);
		e.property(MonitoredEdge.DEADBAND_LABEL, 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		source.property("a", 907);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		source.property("a", 900);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		source.property("a", 899);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 899);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 899);
	}

	@Test
	public void runLinkFromDelayTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = destination.addEdge("link from test", source, "type", "link-from");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.DELAY_LABEL, 1000);
		source.property("a", 12);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		Thread.sleep(400);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		Thread.sleep(601);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 12);
		e.property(MonitoredEdge.DELAY_LABEL, 5000);
		source.property("a", 13);
		e.property(MonitoredEdge.DELAY_LABEL, 0);
		source.property("a", 101);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 101);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 101);
		Thread.sleep(4000);
		source.property("a", 102);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 102);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 102);
		Thread.sleep(1001);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 13);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 13);

	}

	@Test
	public void runLinkFromDestinationTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = destination.addEdge("link from test", source, "type", "link-from");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property("a", "f");
		source.property("a", 12);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "f", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "f", 12);
		e.property("a", "a");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "f", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "f", 12);
		source.property("a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 14);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "f", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "f", 12);
	}

	@Test
	public void runLinkFromMonitoredPropertyTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = destination.addEdge("link from test", source, "type", "link-from");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.MONITORED_PROPERTIES_LABEL, "b,c");
		source.property("a", 12);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("b", 129);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 129);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 129);
		e.property(MonitoredEdge.MONITORED_PROPERTIES_LABEL, "*");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 14);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 14);
	}

	@Test
	public void runLinkFromPropertyActiveTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = destination.addEdge("link from test", source, "type", "link-from");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.PROPERTY_ACTIVE_LABEL, "false");
		source.property("a", 12);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.PROPERTY_ACTIVE_LABEL, "true");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 14);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 14);
	}

	@Test
	public void runLinkFromTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		destination.addEdge("link from test", source, "type", "link-from");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("c", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "c", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "c", 10);
		waldotTestClientHandler.writeOpcUaVertexValue("source", "c", 356);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "c", 356);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "c", 356);
		source.property("d", "nan");
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "d", "nan");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "d", "nan");
		waldotTestClientHandler.writeOpcUaVertexValue("source", "d", "_pro_test_");
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "d", "_pro_test_");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "d", "_pro_test_");
		destination.property("a", 40);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 40);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 40);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		waldotTestClientHandler.writeOpcUaVertexValue("source", "a", 15);
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 15);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 15);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 15);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 15);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "d", "_pro_test_");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "d", "_pro_test_");
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "d", "_pro_test_");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "d", "_pro_test_");
		assert waldotTestClientHandler.checkVertexValueEquals("source", "c", 356);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "c", 356);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "c", 356);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "c", 356);

	}

	@Test
	public void runLinkToActiveTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = source.addEdge("link from test", destination, "type", "link-to");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.ACTIVE_LABEL, false);
		source.property("a", 12);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.ACTIVE_LABEL, true);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 14);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 14);
	}

	@Test
	public void runLinkToDeadBandTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = source.addEdge("link from test", destination, "type", "link-to");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.DEADBAND_LABEL, 5);
		e.property(MonitoredEdge.DEADBAND_TYPE_LABEL, MonitoredEdge.ABSOLUTE);
		source.property("a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 6);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 15);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 16);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 16);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 16);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 5);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 4);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 4);
		source.property("c", "test");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "c", "test");
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "c", "test");
		source.property("a", 1000);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		e.property(MonitoredEdge.DEADBAND_LABEL, 10);
		e.property(MonitoredEdge.DEADBAND_TYPE_LABEL, MonitoredEdge.PERCENTAGE);
		source.property("a", 1100);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		source.property("a", 1067);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		source.property("a", 1101);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1101);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1101);
		e.property(MonitoredEdge.DEADBAND_LABEL, 1);
		source.property("a", 1000);
		e.property(MonitoredEdge.DEADBAND_LABEL, 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		source.property("a", 907);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		source.property("a", 900);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 1000);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 1000);
		source.property("a", 899);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 899);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 899);
	}

	@Test
	public void runLinkToDelayTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = source.addEdge("link from test", destination, "type", "link-to");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.DELAY_LABEL, "1000");
		source.property("a", 12);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		Thread.sleep(400);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		Thread.sleep(601);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 12);
		e.property(MonitoredEdge.DELAY_LABEL, "5000");
		source.property("a", 13);
		e.property(MonitoredEdge.DELAY_LABEL, "NaN");
		source.property("a", 101);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 101);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 101);
		Thread.sleep(4000);
		source.property("a", 102);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 102);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 102);
		Thread.sleep(1001);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 13);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 13);

	}

	@Test
	public void runLinkToDestinationTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = source.addEdge("link from test", destination, "type", "link-to");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property("a", "f");
		source.property("a", 12);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "f", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "f", 12);
		e.property("a", "a");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "f", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "f", 12);
		source.property("a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 14);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "f", 12);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "f", 12);
	}

	@Test
	public void runLinkToMonitoredPropertyTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = source.addEdge("link from test", destination, "type", "link-to");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.MONITORED_PROPERTIES_LABEL, "b");
		source.property("a", 12);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("b", 129);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 129);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 129);
		e.property(MonitoredEdge.MONITORED_PROPERTIES_LABEL, "b,a");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 14);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 14);
	}

	@Test
	public void runLinkToPropertyActiveTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		final Edge e = source.addEdge("link from test", destination, "type", "link-to");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("a", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.PROPERTY_ACTIVE_LABEL, false);
		e.property(MonitoredEdge.ACTIVE_LABEL, true);
		source.property("a", 12);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		e.property(MonitoredEdge.PROPERTY_ACTIVE_LABEL, true);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 10);
		source.property("a", 14);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 14);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 14);
	}

	@Test
	public void runLinkToTest() throws Exception {
		simpleServerInit();
		final Vertex source = g.addVertex("id", "source", "name", "source vertex", "a", "1", "b", "2");
		final Vertex destination = g.addVertex("id", "destination", "name", "destination vertex", "a", "3", "b", "4");
		source.addEdge("link to test", destination, "type", "link-to");
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 3);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		source.property("c", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "c", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "c", 10);
		waldotTestClientHandler.writeOpcUaVertexValue("source", "c", 356);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "c", 356);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "c", 356);
		source.property("d", "nan");
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "d", "nan");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "d", "nan");
		waldotTestClientHandler.writeOpcUaVertexValue("source", "d", "_pro_test_");
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "d", "_pro_test_");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "d", "_pro_test_");
		destination.property("a", 40);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 40);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 40);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 1);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		waldotTestClientHandler.writeOpcUaVertexValue("source", "a", 15);
		assert waldotTestClientHandler.checkVertexExists("source");
		assert waldotTestClientHandler.checkVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexExists("source");
		assert waldotTestClientHandler.checkOpcUaVertexExists("destination");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "a", 15);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "a", 15);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "b", 2);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "a", 15);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "a", 15);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "b", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("source", "d", "_pro_test_");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "d", "_pro_test_");
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "d", "_pro_test_");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "d", "_pro_test_");
		assert waldotTestClientHandler.checkVertexValueEquals("source", "c", 356);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("source", "c", 356);
		assert waldotTestClientHandler.checkVertexValueEquals("destination", "c", 356);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("destination", "c", 356);

	}

	private void simpleServerInit() throws ConfigurationException, InterruptedException, ExecutionException {
		bootstrapUrlServerInit("file:///tmp/boot.conf");
	}

}