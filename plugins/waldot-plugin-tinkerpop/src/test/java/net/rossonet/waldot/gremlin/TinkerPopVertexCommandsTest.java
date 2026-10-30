package net.rossonet.waldot.gremlin;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.util.ser.GraphBinaryMessageSerializerV1;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.WaldotTinkerPopPlugin;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;
import net.rossonet.waldot.test.WaldotTestEnvironment;
import net.rossonet.waldot.tinkerpop.TinkerPopConstants;
import net.rossonet.waldot.tinkerpop.TinkerPopVertex;
import net.rossonet.waldot.utils.NetworkHelper;

/**
 * Verifies the commands that create the vertices of the TinkerPop plugin.
 */
public class TinkerPopVertexCommandsTest {

	private static void assertError(final String[] result) {
		assertEquals(1, result.length);
		assertTrue(
				result[0].startsWith(AbstractCreateVertexCommand.ERROR_PREFIX),
				result[0]);
	}

	private WaldotGraph g;

	private WaldotTinkerPopPlugin plugin;

	@AfterEach
	public void afterEach() throws Exception {
		clean();
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		clean();
		g = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(),
				new LoggerHistoryStrategy());
		for (final PluginListener p : g.getWaldotNamespace().getPlugins()) {
			if (p instanceof WaldotTinkerPopPlugin) {
				plugin = (WaldotTinkerPopPlugin) p;
			}
		}
		assertNotNull(plugin, "tinkerpop plugin not loaded");
	}

	private void clean() throws Exception {
		if (g != null && g.getWaldotNamespace() != null) {
			g.getWaldotNamespace().close();
			g = null;
		}
		plugin = null;
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		Files.deleteIfExists(Path.of("/tmp/index.txt"));
		WaldotTestEnvironment.awaitOpcPortReleased();
	}

	private AbstractCreateVertexCommand command(final String type) {
		return createCommands().stream()
				.filter(c -> c.getVertexType().equals(type)).findFirst()
				.orElseThrow(() -> new AssertionError(
						"no create command for " + type));
	}

	@Test
	public void commandsDescribeEveryInputAndHaveASingleOutput() {
		for (final AbstractCreateVertexCommand command : createCommands()) {
			assertEquals(1, command.getOutputArguments().length);
			final Argument[] inputs = command.getInputArguments();
			assertEquals(command.getParameters().size() + 2, inputs.length);
			for (final Argument input : inputs) {
				assertTrue(input.getDescription().getText().length() > 20,
						"description too short for " + input.getName());
			}
		}
	}

	private List<AbstractCreateVertexCommand> createCommands() {
		return plugin.getCommands().stream()
				.filter(AbstractCreateVertexCommand.class::isInstance)
				.map(AbstractCreateVertexCommand.class::cast).toList();
	}

	private WaldotVertex created(final String[] result) {
		assertEquals(1, result.length);
		assertFalse(
				result[0].startsWith(AbstractCreateVertexCommand.ERROR_PREFIX),
				result[0]);
		final WaldotVertex vertex = g.getWaldotNamespace()
				.getVertexNode(NodeId.parse(result[0]));
		assertNotNull(vertex, "vertex " + result[0] + " not found");
		return vertex;
	}

	@Test
	public void createGremlinServerOnAFreePort() throws Exception {
		final int port = NetworkHelper.findAvailablePort(28182);
		final TinkerPopVertex server = assertInstanceOf(TinkerPopVertex.class,
				created(command(TinkerPopConstants.GREMLIN_TYPE_LABEL)
						.runCommand(null,
								new String[]{"gremlin-test", "tests/gremlin",
										String.valueOf(port), "127.0.0.1"})));
		try {
			assertEquals(String.valueOf(port),
					server.property(TinkerPopConstants.PORT_FIELD).value()
							.toString());
			assertEquals("127.0.0.1",
					server.property(TinkerPopConstants.BIND_HOST_FIELD).value()
							.toString());
			final long end = System.currentTimeMillis() + 20000;
			while (System.currentTimeMillis() < end
					&& NetworkHelper.checkLocalPortAvailable(port)) {
				Thread.sleep(100);
			}
			assertFalse(NetworkHelper.checkLocalPortAvailable(port),
					"the gremlin server is not listening");
		} finally {
			server.remove();
		}
	}

	@Test
	public void changingThePortRestartsTheServer() throws Exception {
		final int firstPort = NetworkHelper.findAvailablePort(28183);
		final TinkerPopVertex server = createServer(firstPort);
		try {
			awaitPortTaken(firstPort);
			int secondPort = NetworkHelper.findAvailablePort(28184);
			while (secondPort == firstPort) {
				secondPort = NetworkHelper.findAvailablePort(28184);
			}
			server.property(TinkerPopConstants.PORT_FIELD,
					String.valueOf(secondPort));
			awaitPortTaken(secondPort);
			final int newPort = secondPort;
			awaitTrue(() -> NetworkHelper.checkLocalPortAvailable(firstPort),
					"the old port " + firstPort + " is still in use");
			awaitTrue(() -> TinkerPopConstants._STATUS_RUNNING.equals(server
					.property(TinkerPopConstants.STATUS_FIELD.toLowerCase())
					.value()), "the restarted server is not running");
			final Cluster cluster = Cluster.build().addContactPoint("127.0.0.1")
					.port(newPort)
					.serializer(new GraphBinaryMessageSerializerV1()).create();
			try {
				final GraphTraversalSource remote = traversal()
						.withRemote(DriverRemoteConnection.using(cluster, "g"));
				assertTrue(remote.V().count().next() >= 1);
			} finally {
				cluster.close();
			}
		} finally {
			server.remove();
		}
	}

	@Test
	public void removingTheServerDoesNotCloseTheGraph() throws Exception {
		final int port = NetworkHelper.findAvailablePort(28185);
		final TinkerPopVertex server = createServer(port);
		awaitPortTaken(port);
		// close() waits for the end of the stop, when Gremlin Server closes
		// the graphs left in its graph manager
		server.remove();
		assertTrue(NetworkHelper.checkLocalPortAvailable(port),
				"the port " + port + " has not been released");
		assertFalse(((OpcGraph) g).isClosed(),
				"stopping the gremlin server closed the WaldOT graph");
		assertNotNull(g.addVertex("id", "after-gremlin-remove"));
	}

	private void awaitPortTaken(final int port) throws Exception {
		awaitTrue(() -> !NetworkHelper.checkLocalPortAvailable(port),
				"the gremlin server is not listening on " + port);
	}

	private void awaitTrue(final Condition condition, final String message)
			throws Exception {
		final long end = System.currentTimeMillis() + 30000;
		while (System.currentTimeMillis() < end && !condition.check()) {
			Thread.sleep(100);
		}
		assertTrue(condition.check(), message);
	}

	private TinkerPopVertex createServer(final int port) {
		return assertInstanceOf(TinkerPopVertex.class,
				created(command(TinkerPopConstants.GREMLIN_TYPE_LABEL)
						.runCommand(null,
								new String[]{"gremlin-test-" + port,
										"tests/gremlin", String.valueOf(port),
										"127.0.0.1"})));
	}

	@FunctionalInterface
	private interface Condition {
		boolean check() throws Exception;
	}

	@Test
	public void everyVertexTypeHasACreateCommand() {
		final Set<String> covered = new HashSet<>();
		createCommands().forEach(c -> covered.add(c.getVertexType()));
		assertEquals(Set.of(TinkerPopConstants.GREMLIN_TYPE_LABEL),
				plugin.getManagedVertexTypes());
		assertTrue(covered.containsAll(plugin.getManagedVertexTypes()),
				"covered: " + covered);
	}

	@Test
	public void invalidParametersReturnAnError() {
		final AbstractCreateVertexCommand command = command(
				TinkerPopConstants.GREMLIN_TYPE_LABEL);
		assertError(command.runCommand(null, new String[]{"x", "", "70000"}));
		assertError(command.runCommand(null, new String[]{"x", "", "0"}));
		assertError(command.runCommand(null, new String[]{"x", "", "port"}));
	}
}
