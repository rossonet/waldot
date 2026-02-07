package net.rossonet.waldot.gremlin;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.util.ser.GraphBinaryMessageSerializerV1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.BaseHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

public class GremlinPluginTest {
	private WaldotGraph g;
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

	@Test
	public void testGremlinServerConnection() throws Exception {
		simpleServerInit();
		waldotTestClientHandler.createVertex("a", "A", null, new String[0]);
		g.addVertex("label", "test gremlin", "type", "gremlin", "directory", "server", "port", "5623");
		waldotTestClientHandler.createVertex("b", "B", null, new String[0]);
		final Cluster cluster = Cluster.build().addContactPoint("127.0.0.1") // host
				.port(5623) // porta
				.serializer( // GraphBinary è default da 3.7
						new GraphBinaryMessageSerializerV1())
				.create();
		final GraphTraversalSource g = traversal().withRemote(DriverRemoteConnection.using(cluster, "g")); // “g” è
																											// l’alias
																											// lato
																											// server

		assert g.V().hasLabel("test gremlin").count().next() == 1;
		assert g.V().hasLabel("A").count().next() == 1;
		assert g.V().hasLabel("B").count().next() == 1;
		assert g.V().count().next() == 3;
		cluster.close();
	}
}
