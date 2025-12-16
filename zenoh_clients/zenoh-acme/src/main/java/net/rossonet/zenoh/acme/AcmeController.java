package net.rossonet.zenoh.acme;

import java.util.List;

import net.rossonet.zenoh.annotation.AnnotatedAgentController;
import net.rossonet.zenoh.annotation.ExportedCommand;
import net.rossonet.zenoh.annotation.ExportedController;
import net.rossonet.zenoh.annotation.ExportedMethodParameter;
import net.rossonet.zenoh.annotation.ExportedParameter;

@ExportedController(enabled = true)
public class AcmeController implements AnnotatedAgentController {

	public enum SimulationSpeed {
		FAST, NORMAL, SLOW, VERY_FAST, VERY_SLOW
	}

	@ExportedParameter(description = "Enable or disable the ACME sensor simulation", mandatary = true, name = "Enable Simulation", viewOrder = 1)
	private boolean enableSimulation = false;
	@ExportedParameter(description = "Maximum value for the simulated sensors", mandatary = true, name = "Max Simulation Value", viewOrder = 6)
	private int maxSimulationValue = 100;
	@ExportedParameter(description = "Minimum value for the simulated sensors", mandatary = true, name = "Min Simulation Value", viewOrder = 5)
	private int minSimulationValue = 0;
	@ExportedParameter(description = "List of simulated sensors", mandatary = false, name = "Simulated Sensors", viewOrder = 4)
	private final List<String> simulatedSensors = List.of("Temperature", "Pressure", "Humidity");
	@ExportedParameter(description = "Description of the simulation", mandatary = false, name = "Simulation Description", viewOrder = 2)
	private String simulationDescription = "ACME Sensor Simulation";
	@ExportedParameter(description = "Speed of the simulation", mandatary = true, name = "Simulation Speed", viewOrder = 3)
	private SimulationSpeed simulationSpeed = SimulationSpeed.NORMAL;

	public int getMaxSimulationValue() {
		return maxSimulationValue;
	}

	public int getMinSimulationValue() {
		return minSimulationValue;
	}

	@ExportedCommand(description = "Get the list of simulated sensors", name = "Get Simulated Sensors", returnName = "Sensors", returnDescription = "List of simulated sensors that match the filter")
	public List<String> getSimulatedSensors(
			@ExportedMethodParameter(name = "filter", description = "selection filter", mandatary = false) String Filter) {
		System.out.println("Command getSimulatedSensors called with filter: " + Filter);
		return simulatedSensors;
	}

	public String getSimulationDescription() {
		return simulationDescription;
	}

	public SimulationSpeed getSimulationSpeed() {
		return simulationSpeed;
	}

	public boolean isEnableSimulation() {
		return enableSimulation;
	}

	@Override
	public void notifyConfigurationChanged(String objectName, ConfigurationChangeType changeType) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyParameterChanged(String parameterName) {
		// TODO Auto-generated method stub

	}

	public void setEnableSimulation(boolean enableSimulation) {
		this.enableSimulation = enableSimulation;
	}

	public void setMaxSimulationValue(int maxSimulationValue) {
		this.maxSimulationValue = maxSimulationValue;
	}

	public void setMinSimulationValue(int minSimulationValue) {
		this.minSimulationValue = minSimulationValue;
	}

	public void setSimulationDescription(String simulationDescription) {
		this.simulationDescription = simulationDescription;
	}

	public void setSimulationSpeed(SimulationSpeed simulationSpeed) {
		this.simulationSpeed = simulationSpeed;
	}

	@Override
	public void startDataFlow() {
		// TODO avvia la simulazione dei dati

	}

	@Override
	public void stopDataFlow() {
		// TODO ferma la simulazione dei dati

	}

	@Override
	public String toString() {
		final int maxLen = 5;
		final StringBuilder builder = new StringBuilder();
		builder.append("AcmeController [simulationDescription=");
		builder.append(simulationDescription);
		builder.append(", simulationSpeed=");
		builder.append(simulationSpeed);
		builder.append(", simulatedSensors=");
		builder.append(simulatedSensors != null ? simulatedSensors.subList(0, Math.min(simulatedSensors.size(), maxLen))
				: null);
		builder.append(", minSimulationValue=");
		builder.append(minSimulationValue);
		builder.append(", maxSimulationValue=");
		builder.append(maxSimulationValue);
		builder.append(", enableSimulation=");
		builder.append(enableSimulation);
		builder.append("]");
		return builder.toString();
	}

}
