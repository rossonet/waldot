package net.rossonet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.dataGenerator.GeneratorConstants;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * type list / type describe on a server with the generator plugin, and the
 * generic checks of the create commands (range, allowed values).
 */
public class TypeSchemaCommandsTest {

	private static final ObjectMapper JSON = new ObjectMapper();

	private WaldotGraph g;

	@AfterEach
	public void afterEach() throws Exception {
		WaldotTestEnvironment.cleanup(g, null);
		g = null;
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		g = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
	}

	private WaldotCommand command(final String browseName) {
		return g.getWaldotNamespace().getConsoleStrategy().getCommands().stream()
				.filter(c -> c.getBrowseName() != null && browseName.equals(c.getBrowseName().getName())).findFirst()
				.orElseThrow(() -> new AssertionError("command " + browseName + " not registered"));
	}

	private JsonNode json(final String browseName, final String... inputs) throws Exception {
		final Object[] out = command(browseName).runCommand(null, inputs);
		assertEquals("", out[1], String.valueOf(out[1]));
		return JSON.readTree((String) out[0]);
	}

	private AbstractCreateVertexCommand createCommand() {
		return (AbstractCreateVertexCommand) g.getWaldotNamespace().getPlugins().stream()
				.flatMap(p -> p.getCommands().stream()).filter(c -> c instanceof final AbstractCreateVertexCommand a
						&& a.getVertexType().equals(GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL))
				.findFirst().orElseThrow();
	}

	@Test
	public void describeGeneratorSchema() throws Exception {
		final JsonNode schema = json(DataExchangeProtocol.TYPE_DESCRIBE_COMMAND,
				GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL);
		assertEquals(GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL, schema.get("type").asText());
		assertEquals("create-command", schema.get("source").asText());
		assertNotNull(schema.get("create_command").get("name").asText());
		JsonNode min = null;
		JsonNode algorithm = null;
		JsonNode delay = null;
		for (final JsonNode p : schema.get("properties")) {
			switch (p.get("key").asText()) {
			case "min" -> min = p;
			case "algorithm" -> algorithm = p;
			case "delay" -> delay = p;
			default -> {
			}
			}
		}
		assertEquals("double", min.get("type").asText());
		assertEquals("0", min.get("default").asText(), "default read from the description");
		assertFalse(min.get("required").asBoolean());
		final List<String> algorithms = new ArrayList<>();
		algorithm.get("enum").forEach(a -> algorithms.add(a.asText()));
		assertTrue(algorithms.containsAll(List.of("random", "incremental", "sinusoidal")), algorithms.toString());
		assertEquals(10.0, delay.get("min").asDouble());
		assertTrue(schema.get("runtime").isArray());
	}

	@Test
	public void describeUnknownTypeIsAnError() {
		final Object[] out = command(DataExchangeProtocol.TYPE_DESCRIBE_COMMAND).runCommand(null,
				new String[] { "nessuno:tipo" });
		assertTrue(String.valueOf(out[1]).contains("sconosciuto"), String.valueOf(out[1]));
		assertTrue(String.valueOf(out[1]).contains(GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL),
				"lists the known types: " + out[1]);
	}

	@Test
	public void listContainsTheGeneratorTypes() throws Exception {
		final JsonNode list = json(DataExchangeProtocol.TYPE_LIST_COMMAND);
		final List<String> types = new ArrayList<>();
		list.get("types").forEach(t -> types.add(t.get("type").asText()));
		assertTrue(types.contains(GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL), types.toString());
		for (final JsonNode t : list.get("types")) {
			assertEquals("WaldotGeneratorPlugin", t.get("plugin").asText());
			assertFalse(t.get("create_command").isNull(), t.toString());
		}
	}

	@Test
	public void createCommandChecksAllowedValuesAndRanges() {
		final AbstractCreateVertexCommand create = createCommand();
		final String badAlgorithm = create.runCommand(null, new String[] { "x1", "", "zigzag" })[0];
		assertTrue(badAlgorithm.startsWith(AbstractCreateVertexCommand.ERROR_PREFIX), badAlgorithm);
		final String badDelay = create.runCommand(null, new String[] { "x2", "", "random", "5" })[0];
		assertTrue(badDelay.contains("intervallo") || badDelay.contains("range") || badDelay.contains("10"),
				badDelay);
		final String upperCase = create.runCommand(null, new String[] { "x3", "", "RANDOM", "20" })[0];
		assertFalse(upperCase.startsWith(AbstractCreateVertexCommand.ERROR_PREFIX),
				"allowed values ignore the case: " + upperCase);
	}

	@Test
	public void parameterSchemaHelpers() {
		final AbstractCreateVertexCommand.VertexParameter p = AbstractCreateVertexCommand.VertexParameter
				.optional("x", net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes.Int32,
						"Something. Default: 42")
				.withRange(1, 100).withAllowed("1", "42");
		assertEquals("42", p.effectiveDefault());
		assertEquals("int", p.toSchema().get("type"));
		assertEquals(1.0, p.toSchema().get("min"));
		assertEquals(100.0, p.toSchema().get("max"));
		assertEquals(List.of("1", "42"), p.toSchema().get("enum"));
		assertEquals("7", p.withDefault(7).effectiveDefault());
		assertEquals(null, AbstractCreateVertexCommand.VertexParameter
				.optional("y", net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes.String, "no default")
				.effectiveDefault());
		assertEquals("'abc'".replace("'", ""), AbstractCreateVertexCommand.VertexParameter
				.optional("z", net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes.String,
						"Text (default 'abc').")
				.effectiveDefault());
	}
}
