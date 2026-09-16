package net.rossonet.waldot.dataGenerator.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.WaldotGeneratorPlugin;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcCommand;

public class CreateSimulatorCommand extends AbstractOpcCommand {
	private static final String CONTEXT_DIRECTORY = "simulator";
	private static final String CREATE_SIMULATION_COMMAND_DESCRIPTION = "create data simulator vertex";
	private static final String CREATE_SIMULATION_COMMAND_NAME = "create generator";
	public static final UInteger DEFAULT_WRITE_MASK = UInteger
			.valueOf(WriteMask.Executable.getValue());
	private static final String LABEL_RESULT = "result";
	protected final static Logger logger = LoggerFactory
			.getLogger(CreateSimulatorCommand.class);
	private static final String LONG_LABEL_RESULT = "result of the update operation";
	public CreateSimulatorCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace,
				CREATE_SIMULATION_COMMAND_NAME,
				CREATE_SIMULATION_COMMAND_DESCRIPTION, DEFAULT_WRITE_MASK,
				DEFAULT_WRITE_MASK, true, true);
		addInputArgument(MiloStrategy.LABEL_FIELD,
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("Label for the data simulator vertex"));
		addInputArgument(WaldotGeneratorPlugin.ALGORITHM_FIELD,
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(
						WaldotGeneratorPlugin.ALGORITHM_FIELD_DESCRIPTION));
		addInputArgument(WaldotGeneratorPlugin.DELAY_FIELD,
				VariableNodeTypes.UInt64.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(WaldotGeneratorPlugin.DELAY_DESCRIPTION));
		addInputArgument(WaldotGeneratorPlugin.MIN_VALUE_FIELD,
				VariableNodeTypes.UInt64.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText
						.english(WaldotGeneratorPlugin.MIN_VALUE_DESCRIPTION));
		addInputArgument(WaldotGeneratorPlugin.MAX_VALUE_FIELD,
				VariableNodeTypes.UInt64.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText
						.english(WaldotGeneratorPlugin.MAX_VALUE_DESCRIPTION));
		addOutputArgument(LABEL_RESULT, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null,
				LocalizedText.english(LONG_LABEL_RESULT));
		this.addReference(
				new Reference(this.getNodeId(), NodeIds.HasModellingRule,
						NodeIds.ModellingRule_Mandatory.expanded(), true));

	}

	@Override
	public Object clone() {
		return new CreateSimulatorCommand(this.waldotNamespace);
	}

	private void enrichParameters(final String[] inputValues,
			final List<String> parameters, int i, String field) {
		if (inputValues[i] != null && !inputValues[i].isEmpty()) {
			parameters.add(field);
			parameters.add(inputValues[i]);
		}
	}

	@Override
	public String getDirectory() {
		return CONTEXT_DIRECTORY;
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext,
			final String[] inputValues) {
		final List<String> parameters = new ArrayList<>();
		parameters.add(MiloStrategy.TYPE_FIELD.toLowerCase());
		parameters.add(WaldotGeneratorPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL);
		enrichParameters(inputValues, parameters, 0,
				MiloStrategy.LABEL_FIELD.toLowerCase());
		enrichParameters(inputValues, parameters, 1,
				WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase());
		enrichParameters(inputValues, parameters, 2,
				WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase());
		enrichParameters(inputValues, parameters, 3,
				WaldotGeneratorPlugin.MIN_VALUE_FIELD.toLowerCase());
		enrichParameters(inputValues, parameters, 4,
				WaldotGeneratorPlugin.MAX_VALUE_FIELD.toLowerCase());
		final Object[] arrayParameters = parameters.toArray(new String[0]);
		logger.info("create vertex with parameters: "
				+ Arrays.toString(arrayParameters));
		return new String[]{getGraph().addVertex(arrayParameters).toString()};
	}

}
