package net.rossonet.zenoh.acme;

import net.rossonet.zenoh.annotation.ExportedObject;
import net.rossonet.zenoh.annotation.ExportedParameter;

@ExportedObject(name = "FireNewData", description = "create a new telemetry data", unique = false)
public class FireNewData {

	@ExportedParameter(description = "Maximum value for the simulated sensors", mandatary = true, name = "Max Simulation Value", viewOrder = 6)
	private final int maxSimulationValue = 100;
	@ExportedParameter(description = "Minimum value for the simulated sensors", mandatary = true, name = "Min Simulation Value", viewOrder = 5)
	private final int minSimulationValue = 0;

}
