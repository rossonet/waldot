package net.rossonet.waldot.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.rules.commands.CreateShowCaseGasGridCommand;
import net.rossonet.waldot.utils.NetworkHelper;

/**
 * Tests of the Gas Grid show case: the graph created by the command, the
 * properties of the link edges and the behavior of the loop controllers.
 */
public class WaldotRulesEngineGasGridTest {

	private static final long WAIT_MS = 40000;

	private WaldotGraph g;

	@AfterEach
	public void afterEach() throws Exception {
		clean();
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		clean();
		g = OpcFactory.getOpcGraph("file:///tmp/boot.conf", new LoggerHistoryStrategy());
	}

	private void clean() throws Exception {
		if (g != null && g.getWaldotNamespace() != null) {
			g.getWaldotNamespace().close();
			g = null;
		}
		Files.deleteIfExists(Path.of("/tmp/boot.conf"));
		Files.deleteIfExists(Path.of("/tmp/index.txt"));
		while (!NetworkHelper.checkLocalPortAvailable(12686)) {
			Thread.sleep(10);
		}
		Files.deleteIfExists(Path.of("/tmp/waldot-client.ks"));
	}

	/** Arguments: stations, delay-ms, setpoint, pipe-delay-ms, gas-type, compare. */
	private String gas(String... args) {
		return new CreateShowCaseGasGridCommand(g.getWaldotNamespace()).runCommand(null, args)[0];
	}

	private Vertex v(String id) {
		return g.traversal().V().has("id", id).next();
	}

	private List<Vertex> vertices(String idPrefix) {
		final List<Vertex> result = new ArrayList<>();
		g.vertices().forEachRemaining(vertex -> {
			if (vertex.property("id").isPresent() && vertex.property("id").value().toString().startsWith(idPrefix)) {
				result.add(vertex);
			}
		});
		return result;
	}

	private static Edge outEdge(Vertex vertex, String label) {
		final Iterator<Edge> edges = vertex.edges(Direction.OUT);
		while (edges.hasNext()) {
			final Edge edge = edges.next();
			if (label.equals(edge.label())) {
				return edge;
			}
		}
		throw new AssertionError("edge not found: " + label);
	}

	private static double num(Vertex v, String label) {
		return Double.parseDouble(v.property(label).value().toString());
	}

	private static void waitFor(String what, BooleanSupplier condition) throws InterruptedException {
		final long end = System.currentTimeMillis() + WAIT_MS;
		while (System.currentTimeMillis() < end) {
			try {
				if (condition.getAsBoolean()) {
					return;
				}
			} catch (final RuntimeException e) {
				// the graph is moving: retry
			}
			Thread.sleep(20);
		}
		throw new AssertionError("Timeout waiting for " + what);
	}

	@Test
	public void commandIsRegistered() {
		final List<String> names = new ArrayList<>();
		g.getWaldotNamespace().getConsoleStrategy().getCommands().forEach(c -> names.add(c.getConsoleCommand()));
		assertTrue(names.stream().anyMatch(n -> n.contains("show") && n.contains("gas")), names.toString());
	}

	@Test
	public void createsTheExpectedGraph() {
		final String result = gas("4", "50", "25", "100", "metano", "true");
		assertTrue(result.contains("created"), result);
		assertEquals(CreateShowCaseGasGridCommand.vertexCount(4), vertices("gas-").size());
		assertEquals(58, vertices("gas-").size());
		// the graph is organized like a plant diagram
		assertEquals("rules/showcase/gas-grid/supply", v("gas-gate-gen").property("directory").value());
		assertEquals("rules/showcase/gas-grid/transmission", v("gas-trunk").property("directory").value());
		assertEquals("rules/showcase/gas-grid/stations/station-2", v("gas-valve-2").property("directory").value());
		assertEquals("rules/showcase/gas-grid/stations/station-2", v("gas-ctrl-2").property("directory").value());
		assertEquals("rules/showcase/gas-grid/consumers", v("gas-consumer-1").property("directory").value());
		assertEquals("rules/showcase/gas-grid/safety", v("gas-leak-monitor").property("directory").value());
		assertEquals("rules/showcase/gas-grid/supervisory", v("gas-network").property("directory").value());
		assertEquals("rules/showcase/gas-grid/rules/control", v("gas-rule-pi-0").property("directory").value());
		assertEquals("rules/showcase/gas-grid/rules/safety",
				v("gas-rule-esd-trip-0").property("directory").value());
		assertEquals("rules/showcase/gas-grid/rules/process", v("gas-rule-plant-0").property("directory").value());
		assertEquals("rules/showcase/gas-grid/compute", v("gas-control-loop").property("directory").value());
		assertEquals("sim:generator", v("gas-clock").property("type").value().toString());
		assertEquals("rules:rule", v("gas-rule-pi-0").property("type").value().toString());
		assertEquals(1L, (long) num(v("gas-control-loop"), "threads"));
		assertEquals(4L, (long) num(v("gas-process"), "threads"));
		assertEquals(1L, (long) num(v("gas-supervisory"), "threads"));
		assertEquals("AUTO", v("gas-ctrl-0").property("mode").value());
	}

	@Test
	public void linkEdgesCarryDeadbandDelayAndRename() {
		gas("4", "50", "25", "100", "metano", "true");
		// transmitter: deadband and delay (dead time of the pipe)
		final Edge pipe = outEdge(v("gas-plant-0"), "pipe-to-transmitter-0");
		assertEquals("link-to", pipe.property("type").value());
		assertEquals(2.0, ((Number) pipe.property("deadband-value").value()).doubleValue(), 1e-9);
		assertEquals("absolute", pipe.property("deadband-type").value());
		assertEquals(100L, ((Number) pipe.property("delay").value()).longValue());
		assertEquals("pressure", pipe.property("pressure").value());
		// demand: percentage deadband and rename data -> demand
		final Edge demand = outEdge(v("gas-demand-gen-0"), "demand-to-consumer-0");
		assertEquals("percentage", demand.property("deadband-type").value());
		assertEquals(5.0, ((Number) demand.property("deadband-value").value()).doubleValue(), 1e-9);
		assertEquals("demand", demand.property("data").value());
		// city gate: rename data -> pressure
		final Edge gate = outEdge(v("gas-gate-gen"), "gate-to-trunk");
		assertEquals("pressure", gate.property("data").value());
		assertEquals("absolute", gate.property("deadband-type").value());
		// valve: link-from the output of the controller, renamed to opening
		final Edge valve = outEdge(v("gas-valve-0"), "valve-from-controller-0");
		assertEquals("link-from", valve.property("type").value());
		assertEquals("opening", valve.property("output").value());
		assertEquals(0.5, ((Number) valve.property("deadband-value").value()).doubleValue(), 1e-9);
		// the last station is the comparison: no deadband, same delay
		final Edge comparePipe = outEdge(v("gas-plant-3"), "pipe-to-transmitter-3");
		assertFalse(comparePipe.property("deadband-value").isPresent());
		assertEquals(100L, ((Number) comparePipe.property("delay").value()).longValue());
		assertFalse(outEdge(v("gas-demand-gen-3"), "demand-to-consumer-3").property("deadband-value").isPresent());
	}

	@Test
	public void compareFalseKeepsTheDeadbandOnEveryStation() {
		gas("3", "50", "25", "100", "metano", "false");
		assertTrue(outEdge(v("gas-plant-2"), "pipe-to-transmitter-2").property("deadband-value").isPresent());
	}

	@Test
	public void gasProfilesAreSelectable() {
		gas("2", "50", "25", "100", "metano-blu", "true");
		final Vertex network = v("gas-network");
		assertEquals("metano-blu", network.property("gas-type").value());
		assertEquals(20.0, num(network, "h2-percent"), 1e-9);
		assertEquals(0.48, num(network, "relative-density"), 1e-9);
		assertEquals(1.12, num(network, "flow-factor"), 1e-9);
		// the blue profile has tighter deadbands and a longer dead time
		final Edge pipe = outEdge(v("gas-plant-0"), "pipe-to-transmitter-0");
		assertEquals(1.2, ((Number) pipe.property("deadband-value").value()).doubleValue(), 1e-9);
		assertEquals(120L, ((Number) pipe.property("delay").value()).longValue());
		assertEquals(3.0,
				((Number) outEdge(v("gas-demand-gen-0"), "demand-to-consumer-0").property("deadband-value").value())
						.doubleValue(),
				1e-9);
		assertEquals(0.0, num(v("gas-trunk"), "pressure") - 24.0, 1e-9);
	}

	@Test
	public void refusesToBeCreatedTwice() {
		assertTrue(gas("3", "50").contains("created"));
		final int before = vertices("gas-").size();
		assertTrue(gas("3", "50").contains("already exists"));
		assertEquals(before, vertices("gas-").size());
	}

	@Test
	public void rejectsInvalidArguments() {
		assertTrue(gas("1").contains("stations"));
		assertTrue(gas("21").contains("stations"));
		assertTrue(gas("x").contains("not a number"));
		assertTrue(gas("4", "5").contains("delay-ms"));
		assertTrue(gas("4", "50", "1").contains("setpoint"));
		assertTrue(gas("4", "50", "abc").contains("not a number"));
		assertTrue(gas("4", "50", "25", "-1").contains("pipe-delay-ms"));
		assertTrue(gas("4", "50", "25", "100", "idrogeno").contains("gas-type"));
		assertTrue(gas("4", "50", "25", "100", "metano", "forse").contains("compare"));
		assertEquals(0, vertices("gas-").size());
	}

	@Test
	public void loopsFollowTheSetpoint() throws Exception {
		gas("3", "50", "25", "100", "metano", "true");
		// the operator moves the setpoint of the whole network
		v("gas-network").property("base-setpoint", 40.0);
		waitFor("setpoint distributed", () -> num(v("gas-ctrl-0"), "setpoint") > 36);
		for (int i = 0; i < 3; i++) {
			final int station = i;
			waitFor("pressure of station " + station + " near the setpoint",
					() -> Math.abs(num(v("gas-plant-" + station), "pressure") - 40.0) < 5.0);
		}
		// the controller drives the valve through link-from
		waitFor("valve opening follows the controller", () -> num(v("gas-valve-0"), "opening") > 30.0
				&& Math.abs(num(v("gas-valve-0"), "opening") - num(v("gas-ctrl-0"), "output")) <= 0.5);
		waitFor("statistics", () -> num(v("gas-stats"), "mean-abs-error") > 0 && num(v("gas-stats"), "mean-opening") > 30);
		waitFor("PI executed", () -> num(v("gas-rule-pi-0"), "executed") > 5);
		assertTrue(num(v("gas-ctrl-0"), "output") >= 0 && num(v("gas-ctrl-0"), "output") <= 100);
	}

	@Test
	public void deadbandReducesTheExecutionsOfTheLoop() throws Exception {
		gas("3", "50", "25", "100", "metano", "true");
		waitFor("loops running", () -> num(v("gas-rule-pi-2"), "total") > 20);
		// station 0 has the deadband, station 2 (the last) has none: measure the
		// notifications of the loops in the same window
		final double filteredStart = num(v("gas-rule-pi-0"), "total");
		final double unfilteredStart = num(v("gas-rule-pi-2"), "total");
		Thread.sleep(8000);
		final double filtered = num(v("gas-rule-pi-0"), "total") - filteredStart;
		final double unfiltered = num(v("gas-rule-pi-2"), "total") - unfilteredStart;
		assertTrue(unfiltered > filtered * 1.5, "unfiltered=" + unfiltered + " filtered=" + filtered);
	}

	@Test
	public void delayIsTheDeadTimeOfThePipe() throws Exception {
		gas("2", "50", "25", "1500", "metano", "false");
		waitFor("running", () -> num(v("gas-rule-plant-0"), "executed") > 3);
		// the process jumps, the transmitter sees it only after the dead time
		v("gas-plant-0").property("pressure", 80.0);
		Thread.sleep(500);
		assertTrue(num(v("gas-sensor-0"), "pressure") < 60.0, "sensor=" + num(v("gas-sensor-0"), "pressure"));
		waitFor("the transmitter sees the jump", () -> num(v("gas-sensor-0"), "pressure") > 70.0);
	}

	@Test
	public void overpressureClosesTheValveAndTheLoopRecovers() throws Exception {
		gas("2", "50", "25", "100", "metano", "false");
		final Vertex controller = v("gas-ctrl-0");
		waitFor("loops running", () -> num(v("gas-rule-pi-0"), "executed") > 3);
		// 90 mbar is far above the limit (25 * 1.3 = 32.5)
		waitFor("ESD trip", () -> {
			v("gas-sensor-0").property("pressure", 90.0);
			return num(controller, "esd-trips") > 0;
		});
		assertTrue(num(v("gas-rule-esd-trip-0"), "executed") > 0);
		// the valve is closed and the loop recovers when the pressure falls
		waitFor("ESD reset", () -> "AUTO".equals(controller.property("mode").value())
				&& num(v("gas-rule-esd-reset-0"), "executed") > 0);
		waitFor("statistics see the trip", () -> num(v("gas-stats"), "esd-trips") > 0);
		// the other station is not affected
		assertEquals(0.0, num(v("gas-ctrl-1"), "esd-trips"), 0.0);
	}

	@Test
	public void faultyTransmitterPutsTheLoopInManual() throws Exception {
		gas("2", "50", "25", "100", "metano", "false");
		final Vertex controller = v("gas-ctrl-0");
		waitFor("loops running", () -> num(v("gas-rule-pi-0"), "executed") > 3);
		waitFor("manual mode", () -> {
			v("gas-sensor-0").property("pressure", -5.0);
			return "sensor".equals(controller.property("fault").value());
		});
		// the frozen output stays inside the valid range
		assertTrue(num(controller, "output") >= 0 && num(controller, "output") <= 100);
		// the next valid measure restores the automatic mode
		waitFor("automatic mode", () -> "none".equals(controller.property("fault").value())
				&& "AUTO".equals(controller.property("mode").value()));
	}

	@Test
	public void leakSaturatesTheValvesAndRaisesTheAlarm() throws Exception {
		gas("2", "50", "25", "100", "metano", "false");
		final Vertex monitor = v("gas-leak-monitor");
		waitFor("statistics", () -> num(v("gas-rule-stats"), "executed") > 1);
		assertEquals("none", monitor.property("alarm").value());
		// the leak removes more gas than the valves can supply, but the pressure stays
		// above zero: the transmitter keeps changing and the loop keeps running
		monitor.property("leak", 600.0);
		try {
			waitFor("leak alarm", () -> "leak".equals(monitor.property("alarm").value()));
		} catch (final AssertionError e) {
			throw new AssertionError(e.getMessage() + " stats=" + v("gas-stats").property("mean-opening").value()
					+ " p0=" + v("gas-plant-0").property("pressure").value() + " s0="
					+ v("gas-sensor-0").property("pressure").value() + " open0="
					+ v("gas-valve-0").property("opening").value() + " out0="
					+ v("gas-ctrl-0").property("output").value() + " int0="
					+ v("gas-ctrl-0").property("integral").value() + " mode=" + v("gas-ctrl-0").property("mode").value()
					+ " leak=" + monitor.property("leak").value() + " leakRule.total="
					+ v("gas-rule-leak-detect").property("total").value(), e);
		}
		assertTrue(num(v("gas-stats"), "mean-opening") > 90.0);
		assertTrue(num(monitor, "alarms") >= 1);
		monitor.property("leak", 0.0);
		waitFor("alarm cleared", () -> "none".equals(monitor.property("alarm").value()));
	}

	@Test
	public void gridMovesAndTheBrokenRuleCountsErrors() throws Exception {
		gas("3", "50", "25", "100", "metano-blu", "true");
		// the generators feed the network through the links
		waitFor("clock", () -> num(v("gas-clock"), "data") > 10);
		waitFor("demand", () -> num(v("gas-consumer-0"), "demand") > 0 && num(v("gas-consumer-0"), "demand") != 57.5);
		waitFor("trunk pressure", () -> num(v("gas-trunk"), "pressure") != 24.0);
		waitFor("upstream pressure", () -> num(v("gas-plant-0"), "upstream") != 24.0);
		// the process runs in parallel and the supervisory rules run
		waitFor("process", () -> num(v("gas-rule-plant-1"), "executed") > 5);
		waitFor("balancer", () -> num(v("gas-rule-balancer"), "executed") > 0);
		assertNotNull(v("gas-network").property("load").value());
		// the supervisory broken rule never executes
		waitFor("errors", () -> num(v("gas-rule-bad-sensor"), "errors") > 0);
		assertEquals(0.0, num(v("gas-rule-bad-sensor"), "executed"), 0.0);
		// no safety trip during the normal operation
		assertEquals(0.0, num(v("gas-stats"), "esd-trips"), 0.0);
		assertEquals("none", v("gas-leak-monitor").property("alarm").value());
	}
}
