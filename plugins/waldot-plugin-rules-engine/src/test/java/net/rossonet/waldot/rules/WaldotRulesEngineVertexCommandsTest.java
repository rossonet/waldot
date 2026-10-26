package net.rossonet.waldot.rules;

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

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;
import net.rossonet.waldot.rules.vertices.ComputeVertex;
import net.rossonet.waldot.rules.vertices.RuleVertex;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Verifies the commands that create the vertices of the rules engine plugin.
 */
public class WaldotRulesEngineVertexCommandsTest {

	private static void assertError(final String[] result) {
		assertEquals(1, result.length);
		assertTrue(
				result[0].startsWith(AbstractCreateVertexCommand.ERROR_PREFIX),
				result[0]);
	}

	private WaldotGraph g;

	private WaldotRulesEnginePlugin plugin;

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
			if (p instanceof WaldotRulesEnginePlugin) {
				plugin = (WaldotRulesEnginePlugin) p;
			}
		}
		assertNotNull(plugin, "rules engine plugin not loaded");
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

	@Test
	public void createComputeWithAllParameters() {
		final ComputeVertex compute = assertInstanceOf(ComputeVertex.class,
				created(command(RulesEngineConstants.COMPUTE_NODE_PARAMETER)
						.runCommand(null, new String[]{"compute-test",
								"tests/compute", "4", "5000", "10.5"})));
		assertEquals("4",
				compute.property(RulesEngineConstants.THREAD_POOL_SIZE_FIELD)
						.value().toString());
		assertEquals("5000", compute
				.property(RulesEngineConstants.EXECUTION_TIMEOUT_MS_FIELD)
				.value().toString());
		assertEquals("10.5",
				compute.property(RulesEngineConstants.PRIORITY_FACTOR_FIELD)
						.value().toString());
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
	public void createRuleWithAllParameters() {
		final RuleVertex rule = assertInstanceOf(RuleVertex.class,
				created(command(RulesEngineConstants.RULE_NODE_PARAMETER)
						.runCommand(null,
								new String[]{"rule-test", "tests/rules",
										"1 > 0", "log.info('ok')", "2",
										"500"})));
		assertEquals("1 > 0",
				rule.property(RulesEngineConstants.CONDITION_FIELD).value()
						.toString());
		assertEquals("log.info('ok')",
				rule.property(RulesEngineConstants.ACTION_FIELD).value()
						.toString());
		assertEquals("2", rule.property(RulesEngineConstants.DEBUG_LEVEL_LABEL)
				.value().toString());
		assertEquals("500", rule.property(RulesEngineConstants.HYSTERESIS_LABEL)
				.value().toString());
	}

	@Test
	public void createWithDefaults() {
		for (final AbstractCreateVertexCommand command : createCommands()) {
			created(command.runCommand(null,
					new String[]{"default-" + command.getVertexType()}));
		}
	}

	@Test
	public void everyVertexTypeHasACreateCommand() {
		final Set<String> covered = new HashSet<>();
		createCommands().forEach(c -> covered.add(c.getVertexType()));
		assertEquals(
				Set.of(RulesEngineConstants.RULE_NODE_PARAMETER,
						RulesEngineConstants.COMPUTE_NODE_PARAMETER),
				plugin.getManagedVertexTypes());
		assertTrue(covered.containsAll(plugin.getManagedVertexTypes()),
				"covered: " + covered);
	}

	@Test
	public void invalidParametersReturnAnError() {
		final AbstractCreateVertexCommand rule = command(
				RulesEngineConstants.RULE_NODE_PARAMETER);
		assertError(rule.runCommand(null,
				new String[]{"x", "", "true", "", "high"}));
		assertError(rule.runCommand(null,
				new String[]{"x", "", "true", "", "", "-1"}));
		final AbstractCreateVertexCommand compute = command(
				RulesEngineConstants.COMPUTE_NODE_PARAMETER);
		assertError(compute.runCommand(null, new String[]{"x", "", "0"}));
		assertError(compute.runCommand(null,
				new String[]{"x", "", "", "", "fast"}));
	}
}
