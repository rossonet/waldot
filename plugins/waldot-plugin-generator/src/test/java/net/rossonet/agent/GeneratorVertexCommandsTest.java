package net.rossonet.agent;

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

import net.rossonet.waldot.WaldotGeneratorPlugin;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex;
import net.rossonet.waldot.dataGenerator.GeneratorConstants;
import net.rossonet.waldot.dataGenerator.commands.CreateSimulatorCommand;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Verifies the commands that create the vertices of the generator plugin.
 */
public class GeneratorVertexCommandsTest {

	private WaldotGraph g;

	private WaldotGeneratorPlugin plugin;

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
			if (p instanceof WaldotGeneratorPlugin) {
				plugin = (WaldotGeneratorPlugin) p;
			}
		}
		assertNotNull(plugin, "generator plugin not loaded");
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
		for (final WaldotCommand c : plugin.getCommands()) {
			if (c instanceof AbstractCreateVertexCommand
					&& ((AbstractCreateVertexCommand) c).getVertexType()
							.equals(type)) {
				return (AbstractCreateVertexCommand) c;
			}
		}
		throw new AssertionError("no create command for " + type);
	}

	@Test
	public void commandsDescribeEveryInputAndHaveASingleOutput() {
		for (final WaldotCommand c : plugin.getCommands()) {
			final AbstractCreateVertexCommand command = (AbstractCreateVertexCommand) c;
			final Argument[] outputs = command.getOutputArguments();
			assertEquals(1, outputs.length);
			final Argument[] inputs = command.getInputArguments();
			assertEquals(command.getParameters().size() + 2, inputs.length);
			for (final Argument input : inputs) {
				assertNotNull(input.getDescription().getText());
				assertTrue(input.getDescription().getText().length() > 20,
						"description too short for " + input.getName());
			}
		}
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
	public void createGeneratorWithAllParameters() {
		final AbstractCreateVertexCommand command = command(
				GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL);
		assertInstanceOf(CreateSimulatorCommand.class, command);
		final WaldotVertex vertex = created(
				command.runCommand(null, new String[]{"gen-test",
						"tests/generators", "random", "50", "-10", "10", ""}));
		final DataGeneratorVertex generator = assertInstanceOf(
				DataGeneratorVertex.class, vertex);
		assertEquals("random",
				generator.property("algorithm").value().toString());
		assertEquals("50", generator.property("delay").value().toString());
		assertEquals("-10", generator.property("min").value().toString());
		assertEquals("10", generator.property("max").value().toString());
	}

	@Test
	public void createGeneratorWithDefaults() {
		final WaldotVertex vertex = created(
				command(GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL)
						.runCommand(null, new String[]{"gen-default"}));
		assertInstanceOf(DataGeneratorVertex.class, vertex);
	}

	@Test
	public void everyVertexTypeHasACreateCommand() {
		final Set<String> covered = new HashSet<>();
		for (final WaldotCommand c : plugin.getCommands()) {
			if (c instanceof AbstractCreateVertexCommand) {
				covered.add(((AbstractCreateVertexCommand) c).getVertexType());
			}
		}
		assertFalse(plugin.getManagedVertexTypes().isEmpty());
		assertTrue(covered.containsAll(plugin.getManagedVertexTypes()),
				"types without command: " + plugin.getManagedVertexTypes()
						+ " vs " + covered);
	}

	@Test
	public void invalidParametersReturnAnError() {
		final AbstractCreateVertexCommand command = command(
				GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL);
		for (final String[] args : List.of(new String[]{"x", "", "unknown"},
				new String[]{"x", "", "", "abc"},
				new String[]{"x", "", "", "5"},
				new String[]{"x", "", "", "", "100", "1"})) {
			final String[] result = command.runCommand(null, args);
			assertEquals(1, result.length);
			assertTrue(
					result[0].startsWith(
							AbstractCreateVertexCommand.ERROR_PREFIX),
					result[0]);
		}
	}
}
