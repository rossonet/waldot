package net.rossonet.waldot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.api.exchange.GraphRenderOptions;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.commands.exchange.GraphSnapshotCodec;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * The {@value DataExchangeProtocol#RENDER_GRAPH_COMMAND} command on a real
 * namespace: selection, properties, SVG and DOT output, chunked transfer. The
 * drawings are also written in {@code build/render-graph} to be looked at.
 */
@TestInstance(Lifecycle.PER_CLASS)
class RenderGraphCommandTest {

	/** Output of a call. */
	private record Rendering(String text, String[] outputs) {

		int edges() {
			return Integer.parseInt(outputs[6]);
		}

		int vertices() {
			return Integer.parseInt(outputs[5]);
		}

		String warnings() {
			return outputs[7];
		}
	}

	private static final Path OUTPUT = Path.of("build", "render-graph");

	private WaldotGraph graph;

	private Object[] call(final String... inputs) {
		return command().runCommand(null, inputs);
	}

	private WaldotCommand command() {
		return graph.getWaldotNamespace().getConsoleStrategy().getCommands().stream()
				.filter(c -> c.getBrowseName() != null
						&& DataExchangeProtocol.RENDER_GRAPH_COMMAND.equals(c.getBrowseName().getName()))
				.findFirst().orElseThrow(() -> new AssertionError("render graph not registered"));
	}

	/**
	 * A small plant: two lines with a controller each, sensors, an ephemeral
	 * session vertex and runtime data.
	 */
	private void createPlant() {
		final Vertex plc1 = graph.addVertex(T.id, "r-plc1", T.label, "controller", "name", "PLC line 1",
				"directory", "plant/line1", "model", "S7-1500", "run!connected", "true");
		final Vertex plc2 = graph.addVertex(T.id, "r-plc2", T.label, "controller", "name", "PLC line 2",
				"directory", "plant/line2", "model", "S7-1200", "run!connected", "false");
		final Vertex t1 = graph.addVertex(T.id, "r-t1", T.label, "sensor", "name", "temperature 1", "directory",
				"plant/line1", "unit", "°C", "status", "ok");
		final Vertex t2 = graph.addVertex(T.id, "r-t2", T.label, "sensor", "name", "temperature 2", "directory",
				"plant/line2", "unit", "°C", "status", "alarm");
		final Vertex p1 = graph.addVertex(T.id, "r-p1", T.label, "sensor", "name", "pressure <1> & \"co\"",
				"directory", "plant/line1", "unit", "bar", "status", "ok");
		final Vertex session = graph.addVertex(T.id, "r-session", T.label, "session", "name", "operator session",
				"ephemeral", "true");
		final Vertex scada = graph.addVertex(T.id, "r-scada", T.label, "server", "name", "SCADA", "directory",
				"plant");
		graph.addVertex(T.id, "r-lonely", T.label, "note", "name", "lonely note");
		plc1.addEdge("reads", t1, T.id, "r-e1", "period", 1000);
		plc1.addEdge("reads", p1, T.id, "r-e2", "period", 500);
		plc2.addEdge("reads", t2, T.id, "r-e3", "period", 1000);
		scada.addEdge("polls", plc1, T.id, "r-e4");
		scada.addEdge("polls", plc2, T.id, "r-e5");
		session.addEdge("watches", t2, T.id, "r-e6", "ephemeral", "true");
	}

	private Rendering render(final GraphRenderOptions options, final String name) throws Exception {
		final Object[] outputs = call(options.toJson(), "true", "");
		assertEquals("", outputs[8], java.util.Arrays.toString(outputs));
		assertEquals("1", outputs[2]);
		final byte[] data = GraphSnapshotCodec
				.decompressIfNeeded(Base64.getDecoder().decode((String) outputs[0]));
		final String text = new String(data, StandardCharsets.UTF_8);
		Files.createDirectories(OUTPUT);
		Files.writeString(OUTPUT.resolve(name + "." + outputs[4]), text);
		final String[] strings = new String[outputs.length];
		for (int i = 0; i < outputs.length; i++) {
			strings[i] = (String) outputs[i];
		}
		return new Rendering(text, strings);
	}

	@BeforeEach
	void resetGraph() {
		graph.getWaldotNamespace().resetNameSpace();
		createPlant();
	}

	@BeforeAll
	void startServer() throws Exception {
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
	}

	@AfterAll
	void stopServer() {
		WaldotTestEnvironment.cleanup(graph, null);
	}

	private static Document parse(final String svg) throws Exception {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		return factory.newDocumentBuilder().parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
	}

	private static Set<String> nodeIds(final Document svg, final String cssClass) {
		final Set<String> ids = new TreeSet<>();
		final NodeList groups = svg.getElementsByTagName("g");
		for (int i = 0; i < groups.getLength(); i++) {
			final Element group = (Element) groups.item(i);
			if (List.of(group.getAttribute("class").split(" ")).contains(cssClass)) {
				ids.add(group.getAttribute("data-node-id"));
			}
		}
		return ids;
	}

	private static boolean hasId(final Set<String> nodeIds, final String id) {
		return nodeIds.stream().anyMatch(n -> n.endsWith(id));
	}

	@Test
	void commandIsRegisteredInTheInformationFolder() {
		assertEquals(DataExchangeProtocol.INFORMATION_DIRECTORY, command().getDirectory());
	}

	@Test
	void defaultDrawingExcludesEphemeralAndRuntime() throws Exception {
		final Rendering rendering = render(new GraphRenderOptions(), "default");
		final Document svg = parse(rendering.text());
		final Set<String> vertices = nodeIds(svg, "vertex");
		assertEquals(7, rendering.vertices(), vertices.toString());
		assertEquals(5, rendering.edges());
		assertFalse(hasId(vertices, "r-session"), "ephemeral vertex drawn");
		assertTrue(hasId(vertices, "r-lonely"), "isolated vertex missing");
		assertFalse(rendering.text().contains("run!connected"), "runtime property drawn");
		assertTrue(rendering.text().contains("S7-1500"));
		assertTrue(rendering.text().contains("pressure &lt;1&gt; &amp; &quot;co&quot;"));
		assertEquals(nodeIds(svg, "edge").size(), rendering.edges());
	}

	@Test
	void ephemeralAndRuntimeOnRequest() throws Exception {
		final GraphRenderOptions options = new GraphRenderOptions();
		options.vertices().includeEphemeral(true);
		options.properties().includeRuntime(true);
		final Rendering rendering = render(options, "ephemeral-runtime");
		final Document svg = parse(rendering.text());
		assertTrue(hasId(nodeIds(svg, "ephemeral"), "r-session"));
		assertEquals(6, rendering.edges());
		assertTrue(rendering.text().contains("run!connected"));
	}

	@Test
	void neighbourhoodOfARoot() throws Exception {
		final GraphRenderOptions options = new GraphRenderOptions();
		options.vertices().roots(List.of("r-plc1")).depth(1);
		final Rendering rendering = render(options, "root-plc1");
		final Set<String> vertices = nodeIds(parse(rendering.text()), "vertex");
		assertEquals(4, vertices.size(), vertices.toString());
		assertTrue(hasId(nodeIds(parse(rendering.text()), "highlight"), "r-plc1"));
		assertTrue(rendering.warnings().contains("omitted"), rendering.warnings());

		options.vertices().direction(GraphRenderOptions.Direction.OUT).depth(5);
		assertEquals(3, render(options, "root-plc1-out").vertices());
	}

	@Test
	void attributeFilters() throws Exception {
		final GraphRenderOptions byLabel = new GraphRenderOptions();
		byLabel.vertices().labels(List.of("sensor"));
		final Rendering sensors = render(byLabel, "sensors");
		assertEquals(3, sensors.vertices());
		assertEquals(0, sensors.edges());

		final GraphRenderOptions byDirectory = new GraphRenderOptions();
		byDirectory.vertices().directories(List.of("/plant/line1/"));
		assertEquals(3, render(byDirectory, "line1").vertices());

		final GraphRenderOptions byWhere = new GraphRenderOptions();
		byWhere.vertices().where(java.util.Map.of("status", "^alarm$"));
		assertEquals(1, render(byWhere, "alarm").vertices());

		final GraphRenderOptions byMatch = new GraphRenderOptions();
		byMatch.vertices().match("temperature");
		assertEquals(2, render(byMatch, "match").vertices());

		final GraphRenderOptions byEdge = new GraphRenderOptions();
		byEdge.edges().labels(List.of("polls"));
		byEdge.vertices().includeIsolated(false);
		final Rendering polls = render(byEdge, "polls");
		assertEquals(3, polls.vertices());
		assertEquals(2, polls.edges());
	}

	@Test
	void expressionSelectsTheCandidates() throws Exception {
		final GraphRenderOptions options = new GraphRenderOptions();
		options.vertices().expression("g.V().hasLabel('controller')");
		final Rendering rendering = render(options, "expression");
		assertEquals(2, rendering.vertices());
	}

	@Test
	void groupsAndLayouts() throws Exception {
		for (final GraphRenderOptions.Algorithm algorithm : GraphRenderOptions.Algorithm.values()) {
			for (final GraphRenderOptions.GroupBy groupBy : GraphRenderOptions.GroupBy.values()) {
				final GraphRenderOptions options = new GraphRenderOptions();
				options.layout().algorithm(algorithm).groupBy(groupBy);
				options.style().title("plant (" + algorithm + ", " + groupBy + ")");
				final Rendering rendering = render(options,
						("layout-" + algorithm + "-" + groupBy).toLowerCase());
				final Document svg = parse(rendering.text());
				assertEquals(7, nodeIds(svg, "vertex").size(), algorithm + " " + groupBy);
				assertFalse(rendering.text().contains("NaN"), algorithm + " " + groupBy);
			}
		}
		final GraphRenderOptions dark = new GraphRenderOptions();
		dark.style().theme(GraphRenderOptions.Theme.DARK).colorBy(GraphRenderOptions.ColorBy.DIRECTORY);
		dark.layout().direction(GraphRenderOptions.LayoutDirection.DOWN);
		dark.properties().edge(List.of("*"));
		render(dark, "dark-down");
	}

	@Test
	void dotOutput() throws Exception {
		final GraphRenderOptions options = new GraphRenderOptions().format(GraphRenderOptions.Format.DOT);
		options.layout().groupBy(GraphRenderOptions.GroupBy.DIRECTORY);
		final Rendering rendering = render(options, "plant");
		assertEquals("dot", rendering.outputs()[4]);
		assertTrue(rendering.text().startsWith("digraph waldot {"));
		assertTrue(rendering.text().contains("subgraph cluster_"));
		assertTrue(rendering.text().contains(" -> "));
		assertTrue(rendering.text().contains("\\\"co\\\""));
	}

	@Test
	void maxTruncatesWithAWarning() throws Exception {
		final GraphRenderOptions options = new GraphRenderOptions();
		options.vertices().max(3);
		final Rendering rendering = render(options, "max");
		assertEquals(3, rendering.vertices());
		assertTrue(rendering.warnings().contains("vertices.max"), rendering.warnings());
	}

	@Test
	void chunkedTransfer() throws Exception {
		for (int i = 0; i < 60; i++) {
			graph.addVertex(T.id, "r-bulk-" + i, T.label, "bulk", "name", "bulk vertex " + i, "payload",
					"text " + i);
		}
		final Object[] outputs = call("", "false", "1024");
		assertEquals("", outputs[8]);
		final String transferId = (String) outputs[1];
		final int blocks = Integer.parseInt((String) outputs[2]);
		assertTrue(blocks > 1);
		final WaldotCommand read = graph.getWaldotNamespace().getConsoleStrategy().getCommands().stream()
				.filter(c -> DataExchangeProtocol.READ_GRAPH_BLOCK_COMMAND.equals(c.getBrowseName().getName()))
				.findFirst().orElseThrow();
		final ByteArrayOutputStream data = new ByteArrayOutputStream();
		for (int i = 0; i < blocks; i++) {
			final Object[] block = read.runCommand(null, new String[] { transferId, String.valueOf(i) });
			assertEquals("", block[2]);
			data.writeBytes(Base64.getDecoder().decode((String) block[0]));
		}
		assertEquals(Integer.parseInt((String) outputs[3]), data.size());
		final Document svg = parse(data.toString(StandardCharsets.UTF_8));
		assertEquals(67, nodeIds(svg, "vertex").size());
	}

	@Test
	void invalidOptionsAreReported() {
		final List<String> errors = new ArrayList<>();
		for (final String options : List.of("{\"vertices\":{\"rootz\":[]}}", "{\"format\":\"png\"}", "not json",
				"{\"vertices\":{\"match\":\"(\"}}", "{\"vertices\":{\"max\":0}}")) {
			final Object[] outputs = call(options, "", "");
			assertFalse(((String) outputs[8]).isBlank(), options);
			errors.add((String) outputs[8]);
		}
		assertTrue(errors.get(0).contains("vertices.rootz"), errors.get(0));
		final Object[] unknownRoot = call("{\"vertices\":{\"roots\":[\"missing\"]}}", "", "");
		assertTrue(((String) unknownRoot[8]).contains("missing"));
		assertNotNull(assertThrows(IllegalArgumentException.class,
				() -> GraphRenderOptions.fromJson("{\"layout\":{\"spacing\":100}}")));
	}

	@Test
	void optionsJsonRoundTrip() {
		final GraphRenderOptions options = new GraphRenderOptions().format(GraphRenderOptions.Format.DOT);
		options.vertices().roots(List.of("a", "b")).depth(3).direction(GraphRenderOptions.Direction.IN)
				.where(java.util.Map.of("k", "v.*")).expression("g.V()").includeEphemeral(true).max(42);
		options.edges().excludeLabels(List.of("x"));
		options.properties().vertex(List.of("unit", "st*")).includeRuntime(true).maxValueLength(10);
		options.layout().algorithm(GraphRenderOptions.Algorithm.RADIAL).spacing(2.5);
		options.style().theme(GraphRenderOptions.Theme.LIGHT).title("t").highlight(List.of("a"));
		assertEquals(options.toJson(), GraphRenderOptions.fromJson(options.toJson()).toJson());
		assertEquals(new GraphRenderOptions().toJson(), GraphRenderOptions.fromJson("").toJson());
		assertEquals(GraphRenderOptions.Algorithm.MRTREE,
				GraphRenderOptions.fromJson("{\"layout\":{\"algorithm\":\"MrTree\"}}").layout().algorithm());
	}
}
