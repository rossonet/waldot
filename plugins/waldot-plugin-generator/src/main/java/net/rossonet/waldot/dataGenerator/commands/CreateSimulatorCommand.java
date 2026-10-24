package net.rossonet.waldot.dataGenerator.commands;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;

import net.rossonet.waldot.dataGenerator.GeneratorConstants;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex.Algorithm;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;

/**
 * OPC UA command that creates a data generator vertex (
 * {@value GeneratorConstants#DATA_GENERATOR_OBJECT_TYPE_LABEL}).
 */
public class CreateSimulatorCommand extends AbstractCreateVertexCommand {

	private static final String CREATE_SIMULATION_COMMAND_DESCRIPTION = "Create a data generator vertex that "
			+ "periodically writes a simulated value in its 'data' property";
	private static final String CREATE_SIMULATION_COMMAND_NAME = "create generator";

	public CreateSimulatorCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace, CREATE_SIMULATION_COMMAND_NAME, CREATE_SIMULATION_COMMAND_DESCRIPTION,
				GeneratorConstants.BASE_CMD_DIRECTORY, GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL, Language.ENGLISH,
				List.of(VertexParameter.optional(GeneratorConstants.ALGORITHM_FIELD.toLowerCase(),
						VariableNodeTypes.String, GeneratorConstants.ALGORITHM_FIELD_DESCRIPTION),
						VertexParameter.optional(GeneratorConstants.DELAY_FIELD.toLowerCase(),
								VariableNodeTypes.UInt64, GeneratorConstants.DELAY_DESCRIPTION),
						VertexParameter.optional(GeneratorConstants.MIN_VALUE_FIELD.toLowerCase(),
								VariableNodeTypes.Int64, GeneratorConstants.MIN_VALUE_DESCRIPTION),
						VertexParameter.optional(GeneratorConstants.MAX_VALUE_FIELD.toLowerCase(),
								VariableNodeTypes.Int64, GeneratorConstants.MAX_VALUE_DESCRIPTION),
						VertexParameter.optional(GeneratorConstants.POST_NODE_ID_DATA_FIELD.toLowerCase(),
								VariableNodeTypes.String, GeneratorConstants.POST_NODE_ID_DATA_DESCRIPTION)));
	}

	@Override
	protected String validate(final List<Object> keyValues) {
		final String algorithm = findValue(keyValues, GeneratorConstants.ALGORITHM_FIELD.toLowerCase());
		if (algorithm != null && !EnumUtils.isValidEnum(Algorithm.class, algorithm)) {
			return "unknown algorithm '" + algorithm + "', valid values are "
					+ Arrays.toString(Algorithm.values());
		}
		final String delay = findValue(keyValues, GeneratorConstants.DELAY_FIELD.toLowerCase());
		if (delay != null && Long.parseLong(delay.trim()) < 10) {
			return "the delay must be at least 10 ms";
		}
		final String min = findValue(keyValues, GeneratorConstants.MIN_VALUE_FIELD.toLowerCase());
		final String max = findValue(keyValues, GeneratorConstants.MAX_VALUE_FIELD.toLowerCase());
		final long minValue = min != null ? Long.parseLong(min.trim()) : GeneratorConstants.DEFAULT_MIN_VALUE;
		final long maxValue = max != null ? Long.parseLong(max.trim()) : GeneratorConstants.DEFAULT_MAX_VALUE;
		if (minValue > maxValue) {
			return "Min (" + minValue + ") must not be greater than Max (" + maxValue + ")";
		}
		return null;
	}

}
