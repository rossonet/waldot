package net.rossonet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.WaldotGeneratorPlugin;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.dataGenerator.EventGeneratorVertex;
import net.rossonet.waldot.dataGenerator.GeneratorConstants;
import net.rossonet.waldot.dataGenerator.commands.CreateEventSimulatorCommand;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Verifies the event generator vertex (sim:event-generator).
 */
public class EventGeneratorVertexTest {

	private static final long WAIT_MS = 10000;

	private static void waitFor(String what, BooleanSupplier condition)
			throws InterruptedException {
		final long end = System.currentTimeMillis() + WAIT_MS;
		while (System.currentTimeMillis() < end) {
			if (condition.getAsBoolean()) {
				return;
			}
			Thread.sleep(10);
		}
		throw new AssertionError("timeout waiting for " + what);
	}

	private final List<BaseEventType> events = new CopyOnWriteArrayList<>();

	private WaldotGraph g;

	private WaldotGeneratorPlugin plugin;

	@AfterEach
	public void afterEach() throws Exception {
		WaldotTestEnvironment.cleanup(g, null);
		g = null;
	}

	@Test
	public void allConfiguredEventTypesAreMapped() throws Exception {
		for (final var entry : EventGeneratorVertex.EVENT_TYPES.entrySet()) {
			events.clear();
			final EventGeneratorVertex generator = create("label",
					"type-" + entry.getKey(), "algorithm", "sequential",
					"delay", "20", "messages", "x", "event-type",
					entry.getKey());
			waitFor("event type " + entry.getKey(), () -> events.stream()
					.anyMatch(e -> entry.getValue().equals(e.getEventType())));
			generator.property("algorithm", "stopped");
			generator.close();
		}
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		g = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(),
				new LoggerHistoryStrategy());
		for (final PluginListener p : g.getWaldotNamespace().getPlugins()) {
			if (p instanceof WaldotGeneratorPlugin) {
				plugin = (WaldotGeneratorPlugin) p;
			}
		}
		assertNotNull(plugin);
	}

	@Test
	public void burstWithMaxEventsStops() throws Exception {
		final EventGeneratorVertex generator = create("label", "burst",
				"algorithm", "burst", "burst-size", "4", "max-events", "10",
				"delay", "20", "messages", "a|b");
		waitFor("stop", () -> "stopped"
				.equals(generator.property("algorithm").value().toString()));
		Thread.sleep(100);
		assertEquals(10, events.size());
		assertEquals(10L, generator.getCounter());
		assertEquals("10", generator.property("count").value().toString());
	}

	private CreateEventSimulatorCommand command() {
		for (final WaldotCommand c : plugin.getCommands()) {
			if (c instanceof CreateEventSimulatorCommand) {
				return (CreateEventSimulatorCommand) c;
			}
		}
		throw new AssertionError("command not registered");
	}

	@Test
	public void commandCreatesTheVertexAndValidates() {
		final String[] ok = command().runCommand(null,
				new String[]{"from-command", "tests/events", "random", "100",
						"5", "x|y", "|", "${message}", "cat", "audit", "10",
						"20", "", "3", "0", "0", "10", "7"});
		assertEquals(1, ok.length);
		assertFalse(ok[0].startsWith(AbstractCreateVertexCommand.ERROR_PREFIX),
				ok[0]);
		assertNotNull(
				g.getWaldotNamespace().getVertexNode(NodeId.parse(ok[0])));
		for (final String[] wrong : List.of(new String[]{"x", "", "bad"},
				new String[]{"x", "", "", "5"},
				new String[]{"x", "", "", "", "", "", "", "", "", "unknown"},
				new String[]{"x", "", "", "", "", "", "", "", "", "", "900",
						"100"},
				new String[]{"x", "", "", "", "", "", "", "", "", "", "", "",
						"10|2000"},
				new String[]{"x", "", "", "", "", "", "", "", "", "", "", "",
						"", "0"})) {
			final String[] result = command().runCommand(null, wrong);
			assertTrue(
					result[0].startsWith(
							AbstractCreateVertexCommand.ERROR_PREFIX),
					String.join(",", wrong) + " -> " + result[0]);
		}
	}

	private EventGeneratorVertex create(Object... keyValues) {
		final Object[] all = new Object[keyValues.length + 2];
		all[0] = MiloStrategy.TYPE_FIELD.toLowerCase();
		all[1] = GeneratorConstants.EVENT_GENERATOR_OBJECT_TYPE_LABEL;
		System.arraycopy(keyValues, 0, all, 2, keyValues.length);
		final Vertex vertex = g.addVertex(all);
		final EventGeneratorVertex generator = assertInstanceOf(
				EventGeneratorVertex.class, vertex);
		generator.addEventObserver((node, event) -> events.add(event));
		return generator;
	}

	@Test
	public void linkedPropertyIsUsedInTheTemplate() throws Exception {
		final Vertex source = g.addVertex("label", "temperature-source",
				"temperature", 1.0);
		final EventGeneratorVertex generator = create("label", "linked",
				"delay", "20", "template", "temperature=${p:temperature}");
		source.addEdge("feeds", generator, "type", "link-to",
				"properties-to-monitor", "temperature");
		source.property("temperature", 42.5);
		waitFor("linked value", () -> events.stream().anyMatch(
				e -> e.getMessage().text().equals("temperature=42.5")));
	}

	@Test
	public void placeholdersExpandSourceCategoryTimeAndEpoch()
			throws Exception {
		create("label", "source-a", "algorithm", "sequential", "delay", "20",
				"messages", "m", "category", "cat-${counter}", "template",
				"${source}|${category}|${epoch}|${time}");
		waitFor("placeholder event", () -> !events.isEmpty());
		final BaseEventType event = events.get(0);
		final String text = event.getMessage().text();
		final String[] parts = text.split("\\|");
		assertEquals(4, parts.length, text);
		assertEquals("source-a", parts[0]);
		assertTrue(parts[1].startsWith("cat-"), text);
		assertTrue(parts[2].matches("\\d+"), text);
		assertTrue(parts[3].contains("T"), text);
	}

	@Test
	public void randomChoiceWithSeedUsesTheListAndTheRange() throws Exception {
		create("label", "random", "algorithm", "random", "delay", "10",
				"messages", "alpha|beta|gamma", "template",
				"${message} ${random}", "random-min", "5", "random-max", "6",
				"min-severity", "300", "max-severity", "310", "seed", "42");
		waitFor("events", () -> events.size() >= 20);
		for (final BaseEventType event : events) {
			final String[] parts = event.getMessage().text().split(" ");
			assertTrue(List.of("alpha", "beta", "gamma").contains(parts[0]));
			final int value = Integer.parseInt(parts[1]);
			assertTrue(value >= 5 && value <= 6, "random " + value);
			final int severity = event.getSeverity().intValue();
			assertTrue(severity >= 300 && severity <= 310,
					"severity " + severity);
		}
	}

	@Test
	public void sequentialMessagesWithTemplateAndSeverities() throws Exception {
		final EventGeneratorVertex generator = create("label", "sequential",
				"algorithm", "sequential", "delay", "20", "messages",
				"start;run ${counter};stop", "separator", ";", "severities",
				"100;500;900", "template", "[${severity}] ${message} #${index}",
				"category", "pump-${index}", "event-type", "device-failure");
		waitFor("three events", () -> events.size() >= 3);
		generator.property("algorithm", "stopped");
		assertEquals("[100] start #0", events.get(0).getMessage().text());
		assertEquals("[500] run 2 #1", events.get(1).getMessage().text());
		assertEquals("[900] stop #2", events.get(2).getMessage().text());
		assertEquals(900, events.get(2).getSeverity().intValue());
		assertEquals("pump-1", events.get(1).getBrowseName().getName());
		assertEquals(NodeIds.DeviceFailureEventType,
				events.get(0).getEventType());
		assertEquals(generator.getNodeId(), events.get(0).getSourceNode());
		final int stoppedAt = events.size();
		Thread.sleep(150);
		assertTrue(events.size() <= stoppedAt + 1,
				"the generator did not stop");
		assertTrue(
				generator.property("data").value().toString().startsWith("["));
	}
}
