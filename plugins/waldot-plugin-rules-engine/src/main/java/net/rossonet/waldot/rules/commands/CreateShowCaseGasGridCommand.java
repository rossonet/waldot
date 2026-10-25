package net.rossonet.waldot.rules.commands;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * Show case "Gas Grid": a gas distribution network whose pressure reducing
 * stations (PRS/REMI) are regulated by PI loop controllers written as rules.
 * <p>
 * The graph is organized like a plant diagram, under
 * <code>rules/showcase/gas-grid</code>: <code>supply</code> (city gate and
 * clock), <code>transmission</code> (high pressure trunk),
 * <code>stations/station-N</code> (sensor, valve, controller and process of
 * each station), <code>consumers</code>, <code>safety</code>,
 * <code>supervisory</code>, <code>rules/*</code>, <code>compute</code>,
 * <code>links</code> and <code>triggers</code>.
 * </p>
 * <p>
 * The links show the properties of <code>link-to</code> and
 * <code>link-from</code>: the <code>deadband-type</code> and
 * <code>deadband-value</code> filter the small changes (transmitter
 * resolution, actuator dead zone, measurement noise), the <code>delay</code>
 * is the dead time of the pipe and the rename of the property maps the value
 * of a vertex to the right property of the next one. The last station has no
 * deadband when <code>compare</code> is true, so its loop runs much more
 * often than the others.
 * </p>
 * <p>
 * The process model is deliberately simple (a first order lag), it is not a
 * hydraulic simulation. The values of the two gas profiles are indicative.
 * </p>
 * Input arguments: <code>stations</code> (2-20, default 4),
 * <code>delay-ms</code> (clock of the simulation, default 200),
 * <code>setpoint</code> (mbar, default 25), <code>pipe-delay-ms</code> (dead
 * time of the pipes, default 300), <code>gas-type</code> (<code>metano</code>
 * or <code>metano-blu</code>) and <code>compare</code> (default true).
 */
public class CreateShowCaseGasGridCommand extends AbstractShowCaseCommand {

	/** Indicative values of a gas profile. */
	public static final class GasProfile {
		public final double deadbandDemandPercent;
		public final double deadbandSensorMbar;
		public final double deadbandSupplyBar;
		public final double demandScale;
		public final double esdMarginPercent;
		public final double flowFactor;
		public final double hydrogenPercent;
		public final String name;
		public final double pipeDelayScale;
		public final double relativeDensity;

		private GasProfile(String name, double hydrogenPercent,
				double relativeDensity, double flowFactor, double demandScale,
				double esdMarginPercent, double deadbandSensorMbar,
				double deadbandDemandPercent, double deadbandSupplyBar,
				double pipeDelayScale) {
			this.name = name;
			this.hydrogenPercent = hydrogenPercent;
			this.relativeDensity = relativeDensity;
			this.flowFactor = flowFactor;
			this.demandScale = demandScale;
			this.esdMarginPercent = esdMarginPercent;
			this.deadbandSensorMbar = deadbandSensorMbar;
			this.deadbandDemandPercent = deadbandDemandPercent;
			this.deadbandSupplyBar = deadbandSupplyBar;
			this.pipeDelayScale = pipeDelayScale;
		}
	}

	public static final String CLOCK_ID = "gas-clock";
	public static final String COMPARE_LABEL = "compare";
	public static final String CONTROL_COMPUTE_ID = "gas-control-loop";
	public static final long DEFAULT_DELAY_MS = 200;
	public static final long DEFAULT_PIPE_DELAY_MS = 300;
	public static final double DEFAULT_SETPOINT_MBAR = 25.0;
	public static final int DEFAULT_STATIONS = 4;
	public static final String DELAY_LABEL = "delay-ms";
	public static final String GAS_TYPE_LABEL = "gas-type";
	public static final String GATE_ID = "gas-gate-gen";
	public static final String LEAK_ID = "gas-leak-monitor";
	/** The existence of this vertex marks the scenario as already created. */
	public static final String NETWORK_ID = "gas-network";
	public static final String PIPE_DELAY_LABEL = "pipe-delay-ms";
	public static final String PROCESS_COMPUTE_ID = "gas-process";
	public static final GasProfile PROFILE_METANO = new GasProfile("metano",
			0.0, 0.60, 1.0, 1.0, 30.0, 2.0, 5.0, 0.3, 1.0);
	public static final GasProfile PROFILE_METANO_BLU = new GasProfile(
			"metano-blu", 20.0, 0.48, 1.12, 1.15, 25.0, 1.2, 3.0, 0.2, 1.2);
	public static final String SCAN_ID = "gas-scan";
	public static final String SETPOINT_LABEL = "setpoint";
	public static final String STATIONS_LABEL = "stations";
	public static final String STATS_ID = "gas-stats";
	public static final String SUPERVISORY_COMPUTE_ID = "gas-supervisory";
	public static final String TRUNK_ID = "gas-trunk";
	public static final double SUPPLY_NOMINAL_BAR = 24.0;

	private static final String DIR = BASE_DIRECTORY + "/gas-grid";
	private static final String LINKS = DIR + "/links";
	private static final String TRIGGERS = DIR + "/triggers";

	public static String consumerId(int station) {
		return "gas-consumer-" + station;
	}

	public static String controllerId(int station) {
		return "gas-ctrl-" + station;
	}

	public static String plantId(int station) {
		return "gas-plant-" + station;
	}

	public static String sensorId(int station) {
		return "gas-sensor-" + station;
	}

	public static String valveId(int station) {
		return "gas-valve-" + station;
	}

	public static String ruleId(String kind, int station) {
		return "gas-rule-" + kind + "-" + station;
	}

	/** Number of vertices created for a number of stations. */
	public static int vertexCount(int stations) {
		return 14 + 11 * stations;
	}

	public CreateShowCaseGasGridCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace, "create show case gas grid",
				"creates the Gas Grid example: a gas distribution network with PI loop controllers on the valves");
		addStringArgument(STATIONS_LABEL, "number of pressure reducing stations (2-20, default "
				+ DEFAULT_STATIONS + ")");
		addStringArgument(DELAY_LABEL,
				"clock of the simulation in milliseconds (default "
						+ DEFAULT_DELAY_MS + ", minimum 10)");
		addStringArgument(SETPOINT_LABEL,
				"downstream pressure setpoint in mbar (5-100, default "
						+ DEFAULT_SETPOINT_MBAR + ")");
		addStringArgument(PIPE_DELAY_LABEL,
				"dead time of the pipes in milliseconds (0-10000, default "
						+ DEFAULT_PIPE_DELAY_MS + ")");
		addStringArgument(GAS_TYPE_LABEL,
				"gas profile: metano (default) or metano-blu");
		addStringArgument(COMPARE_LABEL,
				"true (default) = the last station has no deadband, to compare");
	}

	@Override
	public Object clone() {
		return new CreateShowCaseGasGridCommand(this.waldotNamespace);
	}

	private static String pv(int s) {
		return val(sensorId(s), "pressure");
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext,
			final String[] inputValues) {
		if (exists(NETWORK_ID)) {
			return new String[]{
					"show case gas grid already exists: remove its vertices before creating it again"};
		}
		final long stationsArg;
		final long delay;
		final double setpoint;
		final long pipeDelayArg;
		try {
			stationsArg = longArgument(inputValues, 0, DEFAULT_STATIONS,
					STATIONS_LABEL);
			delay = longArgument(inputValues, 1, DEFAULT_DELAY_MS,
					DELAY_LABEL);
			setpoint = doubleArgument(inputValues, 2, DEFAULT_SETPOINT_MBAR,
					SETPOINT_LABEL);
			pipeDelayArg = longArgument(inputValues, 3,
					DEFAULT_PIPE_DELAY_MS, PIPE_DELAY_LABEL);
		} catch (final IllegalArgumentException e) {
			return new String[]{e.getMessage()};
		}
		final String gasType = stringArgument(inputValues, 4, "metano")
				.toLowerCase();
		final String compareText = stringArgument(inputValues, 5, "true")
				.toLowerCase();
		if (stationsArg < 2 || stationsArg > 20) {
			return new String[]{"argument '" + STATIONS_LABEL
					+ "' must be between 2 and 20"};
		}
		if (delay < 10) {
			return new String[]{
					"argument '" + DELAY_LABEL + "' must be at least 10"};
		}
		if (setpoint < 5 || setpoint > 100) {
			return new String[]{"argument '" + SETPOINT_LABEL
					+ "' must be between 5 and 100"};
		}
		if (pipeDelayArg < 0 || pipeDelayArg > 10000) {
			return new String[]{"argument '" + PIPE_DELAY_LABEL
					+ "' must be between 0 and 10000"};
		}
		final GasProfile profile;
		if (PROFILE_METANO.name.equals(gasType)) {
			profile = PROFILE_METANO;
		} else if (PROFILE_METANO_BLU.name.equals(gasType)) {
			profile = PROFILE_METANO_BLU;
		} else {
			return new String[]{"argument '" + GAS_TYPE_LABEL
					+ "' must be 'metano' or 'metano-blu'"};
		}
		if (!"true".equals(compareText) && !"false".equals(compareText)) {
			return new String[]{"argument '" + COMPARE_LABEL
					+ "' must be 'true' or 'false'"};
		}
		final boolean compare = "true".equals(compareText);
		final int n = (int) stationsArg;
		final long pipeDelay = Math
				.round(pipeDelayArg * profile.pipeDelayScale);
		final double nominalDemand = 50.0 * profile.demandScale;
		final double bias = Math.min(100.0,
				setpoint / (0.9 * profile.flowFactor));

		// network: marker of the scenario, gas profile and operator setpoint
		final Vertex network = vertex(NETWORK_ID, null, "Gas network",
				DIR + "/supervisory", "gas-type", profile.name,
				"h2-percent", profile.hydrogenPercent, "relative-density",
				profile.relativeDensity, "flow-factor", profile.flowFactor,
				"supply-bar", SUPPLY_NOMINAL_BAR, "nominal-demand",
				nominalDemand, "esd-margin-pct", profile.esdMarginPercent,
				"base-setpoint", setpoint, "setpoint", setpoint, "load",
				nominalDemand, "stations", (double) n);
		final Vertex stats = vertex(STATS_ID, null, "Network statistics",
				DIR + "/supervisory", "mean-abs-error", 0.0, "mean-opening",
				bias, "esd-trips", 0.0);
		final Vertex leak = vertex(LEAK_ID, null, "Leak monitor",
				DIR + "/safety", "leak", 0.0, "alarm", "none", "alarms", 0.0);
		final Vertex trunk = vertex(TRUNK_ID, null, "High pressure trunk",
				DIR + "/transmission", "pressure", SUPPLY_NOMINAL_BAR);

		// supply: city gate and clock of the simulation
		final Vertex gate = generator(GATE_ID, "City gate pressure",
				DIR + "/supply", "sinusoidal", 22, 26, delay * 5);
		final Vertex clock = generator(CLOCK_ID, "Simulation clock",
				DIR + "/supply", "incremental", 0, 1000000, delay);
		linkTo(gate, trunk, "gate-to-trunk", "data", "pressure",
				"absolute", profile.deadbandSupplyBar, null, LINKS);
		// cyclic scan of the loops: a transmitter inside its deadband is silent, so
		// the controllers also run on a slow clock (like the scan of a PLC)
		final Vertex scan = generator(SCAN_ID, "Controller scan clock",
				DIR + "/supply", "incremental", 0, 1000000, delay * 4);

		// compute vertices
		final Vertex control = compute(CONTROL_COMPUTE_ID,
				"Control loop compute", DIR + "/compute", 1);
		final Vertex process = compute(PROCESS_COMPUTE_ID,
				"Process compute", DIR + "/compute", 4);
		final Vertex supervisory = compute(SUPERVISORY_COMPUTE_ID,
				"Supervisory compute", DIR + "/compute", 1);

		final String supply = val(NETWORK_ID, "supply-bar");
		final String flow = val(NETWORK_ID, "flow-factor");
		final String nominal = val(NETWORK_ID, "nominal-demand");
		final String margin = val(NETWORK_ID, "esd-margin-pct");

		for (int i = 0; i < n; i++) {
			final boolean noDeadband = compare && i == n - 1;
			final String dir = DIR + "/stations/station-" + i;
			final String sid = sensorId(i);
			final String cid = controllerId(i);
			final String vid = valveId(i);
			final String pid = plantId(i);
			final String kid = consumerId(i);

			final Vertex sensor = vertex(sid, null,
					"Pressure transmitter " + i, dir, "pressure", setpoint);
			final Vertex valve = vertex(vid, null, "Regulating valve " + i,
					dir, "opening", bias);
			final Vertex controller = vertex(cid, null,
					"Loop controller " + i, dir, "setpoint", setpoint, "mode",
					"AUTO", "fault", "none", "integral", 0.0, "last-error",
					0.0, "output", bias, "bias", bias, "kp", 0.4, "ki", 0.1,
					"esd-trips", 0.0);
			final Vertex plant = vertex(pid, null, "Process " + i, dir,
					"pressure", setpoint, "upstream", SUPPLY_NOMINAL_BAR);
			final Vertex consumer = vertex(kid, null,
					"Consumers of station " + i, DIR + "/consumers", "demand",
					nominalDemand);
			final Vertex demandGenerator = generator("gas-demand-gen-" + i,
					"Demand profile " + i, DIR + "/consumers", "random",
					Math.round(nominalDemand * 0.8),
					Math.round(nominalDemand * 1.2), delay * 3);

			// links with deadband and delay
			linkTo(demandGenerator, consumer, "demand-to-consumer-" + i,
					"data", "demand", "percentage",
					noDeadband ? null : profile.deadbandDemandPercent, null,
					LINKS);
			linkTo(trunk, plant, "trunk-to-process-" + i, "pressure",
					"upstream", "percentage", noDeadband ? null : 1.0, null,
					LINKS);
			linkTo(plant, sensor, "pipe-to-transmitter-" + i, "pressure",
					"pressure", "absolute",
					noDeadband ? null : profile.deadbandSensorMbar, pipeDelay,
					LINKS);
			linkTo(network, controller, "setpoint-to-controller-" + i,
					"setpoint", null, null, null, null, LINKS);
			linkFrom(valve, controller, "valve-from-controller-" + i,
					"output", "opening", "absolute", 0.5, null, LINKS);

			// rules
			final String sp = val(cid, "setpoint");
			final String mode = val(cid, "mode");
			final Vertex pi = rule(ruleId("pi", i), "PI loop " + i,
					DIR + "/rules/control", mode + " == 'AUTO'",
					"var err = " + sp + " - " + pv(i) + "; " + "var i0 = "
							+ val(cid, "integral") + "; "
							+ "var i1 = i0 + err; " + "var bias = "
							+ val(cid, "bias") + "; var kp = "
							+ val(cid, "kp") + "; var ki = " + val(cid, "ki")
							+ "; " + "var raw = bias + kp * err + ki * i1; "
							+ "var sat = (raw > 100.0 && err > 0.0) || (raw < 0.0 && err < 0.0); "
							+ "var i2 = sat ? i0 : i1; "
							+ "var outv = math.max(0.0, math.min(100.0, bias + kp * err + ki * i2)); "
							+ set(cid, "integral", "i2") + "; "
							+ set(cid, "last-error", "err") + "; "
							+ set(cid, "output", "outv"),
					0, 0);
			final Vertex guard = rule(ruleId("mode-guard", i),
					"Transmitter guard " + i, DIR + "/rules/control",
					"(" + pv(i) + " < 0.0 || " + pv(i) + " > 200.0) == ("
							+ val(cid, "fault") + " == 'none')",
					"var bad = " + pv(i) + " < 0.0 || " + pv(i) + " > 200.0; "
							+ set(cid, "fault", "bad ? 'sensor' : 'none'")
							+ "; " + "if (bad) { if (" + mode
							+ " == 'AUTO') { " + set(cid, "mode", "'MANUAL'")
							+ "; } } else { if (" + mode + " == 'MANUAL') { "
							+ set(cid, "mode", "'AUTO'") + "; } }",
					0, 0);
			// no hysteresis on safety rules: a discarded notification could hide the overpressure
			final Vertex esdTrip = rule(ruleId("esd-trip", i),
					"Overpressure ESD " + i, DIR + "/rules/safety",
					pv(i) + " > " + sp + " * (1.0 + " + margin
							+ " / 100.0) && " + pv(i) + " <= 200.0 && " + mode
							+ " != 'ESD'",
					set(cid, "mode", "'ESD'") + "; "
							+ set(cid, "integral", "0.0") + "; "
							+ set(cid, "output", "0.0") + "; "
							+ set(cid, "esd-trips",
									val(cid, "esd-trips") + " + 1.0"),
					0, 0);
			final Vertex esdReset = rule(ruleId("esd-reset", i),
					"ESD reset " + i, DIR + "/rules/safety",
					mode + " == 'ESD' && " + pv(i) + " < " + sp + " * 0.5",
					set(cid, "mode", "'AUTO'") + "; "
							+ set(cid, "integral", "0.0"),
					0, 0);
			final Vertex plantRule = rule(ruleId("plant", i),
					"Process model " + i, DIR + "/rules/process", "true",
					"var up = " + val(pid, "upstream") + "; var dem = "
							+ val(kid, "demand") + "; var open = "
							+ val(vid, "opening") + "; var p = "
							+ val(pid, "pressure") + "; "
							+ "var target = 0.9 * open * (up / " + supply
							+ ") * " + flow + " - 0.25 * (dem - " + nominal
							+ ") - 0.25 * " + val(LEAK_ID, "leak") + " / " + n
							+ "; "
							+ set(pid, "pressure",
									"math.max(0.0, p + 0.3 * (target - p))"),
					0, 0);

			execute(control, esdTrip, "runs-esd-trip-" + i, 300,
					DIR + "/compute");
			execute(control, guard, "runs-mode-guard-" + i, 250,
					DIR + "/compute");
			execute(control, esdReset, "runs-esd-reset-" + i, 200,
					DIR + "/compute");
			execute(control, pi, "runs-pi-" + i, 100, DIR + "/compute");
			execute(process, plantRule, "runs-process-" + i, 100,
					DIR + "/compute");

			fire(sensor, pi, "transmitter-drives-pi-" + i, "pressure",
					TRIGGERS);
			fire(scan, pi, "scan-drives-pi-" + i, "data", TRIGGERS);
			fire(sensor, guard, "transmitter-guarded-" + i, "pressure",
					TRIGGERS);
			fire(sensor, esdTrip, "transmitter-trips-esd-" + i, "pressure",
					TRIGGERS);
			fire(sensor, esdReset, "transmitter-resets-esd-" + i, "pressure",
					TRIGGERS);
			fire(clock, plantRule, "clock-steps-process-" + i, "data",
					TRIGGERS);
		}

		// supervisory rules
		final StringBuilder sumDemand = new StringBuilder();
		sumDemand.append("var sum = 0.0; for (var i = 0; i < ").append(n)
				.append("; i = i + 1) { sum = sum + g.V().has('id', 'gas-consumer-' + i).next().property('demand').value(); } ");
		final Vertex balancer = rule("gas-rule-balancer", "Demand balancer",
				DIR + "/rules/supervisory", "true",
				sumDemand + "var avg = sum / " + n + "; "
						+ set(NETWORK_ID, "load", "avg") + "; "
						+ set(NETWORK_ID, "setpoint",
								val(NETWORK_ID, "base-setpoint")
										+ " * (1.0 + 0.5 * (avg / " + nominal
										+ " - 1.0))"),
				1000, 0);
		final Vertex statsRule = rule("gas-rule-stats", "Network statistics",
				DIR + "/rules/supervisory", "true",
				"var sumErr = 0.0; var sumOpen = 0.0; var trips = 0.0; "
						+ "for (var i = 0; i < " + n + "; i = i + 1) { "
						+ "var c = g.V().has('id', 'gas-ctrl-' + i).next(); "
						+ "var s = g.V().has('id', 'gas-sensor-' + i).next(); "
						+ "var v = g.V().has('id', 'gas-valve-' + i).next(); "
						+ "sumErr = sumErr + math.abs(c.property('setpoint').value() - s.property('pressure').value()); "
						+ "sumOpen = sumOpen + v.property('opening').value(); "
						+ "trips = trips + c.property('esd-trips').value(); } "
						+ set(STATS_ID, "mean-abs-error", "sumErr / " + n)
						+ "; " + set(STATS_ID, "esd-trips", "trips") + "; "
						+ set(STATS_ID, "mean-opening", "sumOpen / " + n),
				1000, 0);
		final String saturated = val(STATS_ID, "mean-opening") + " > 90.0";
		final Vertex leakRule = rule("gas-rule-leak-detect", "Leak detection",
				DIR + "/rules/safety",
				"(" + saturated + ") == (" + val(LEAK_ID, "alarm")
						+ " == 'none')",
				"var bad = " + saturated + "; "
						+ set(LEAK_ID, "alarm", "bad ? 'leak' : 'none'")
						+ "; if (bad) { "
						+ set(LEAK_ID, "alarms", val(LEAK_ID, "alarms")
								+ " + 1.0")
						+ "; }",
				0, 0);
		// broken on purpose: arithmetic exception, 'errors' grows
		final Vertex broken = rule("gas-rule-bad-sensor",
				"Broken rule (demo)", DIR + "/rules/supervisory",
				"1 / 0 > 0.0",
				"log.info('never executed')", 0, 0);

		execute(supervisory, leakRule, "runs-leak-detect", 60,
				DIR + "/compute");
		execute(supervisory, balancer, "runs-balancer", 50, DIR + "/compute");
		execute(supervisory, statsRule, "runs-stats", 40, DIR + "/compute");
		execute(supervisory, broken, "runs-bad-sensor", 10, DIR + "/compute");
		fire(clock, balancer, "clock-balances", "data", TRIGGERS);
		fire(clock, statsRule, "clock-collects-stats", "data", TRIGGERS);
		fire(clock, broken, "clock-breaks", "data", TRIGGERS);
		fire(stats, leakRule, "stats-detect-leak", "mean-opening", TRIGGERS);

		return new String[]{"show case gas grid created: " + n
				+ " stations, gas " + profile.name + ", " + vertexCount(n)
				+ " vertices"};
	}
}
