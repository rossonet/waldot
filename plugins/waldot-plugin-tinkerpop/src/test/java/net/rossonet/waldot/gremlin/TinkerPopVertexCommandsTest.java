package net.rossonet.waldot.gremlin;

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
