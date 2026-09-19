package net.rossonet.waldot.rules.commands;

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

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.rules.WaldotRulesEnginePlugin;

public class CreateRuleCommand extends AbstractOpcCommand {
	private static final String CONTEXT_DIRECTORY = WaldotRulesEnginePlugin.BASE_CMD_DIRECTORY;
	private static final String CREATE_RULE_COMMAND_DESCRIPTION = "create rule vertex";
	private static final String CREATE_RULE_COMMAND_NAME = "create rule";
	public static final UInteger DEFAULT_WRITE_MASK = UInteger
			.valueOf(WriteMask.Executable.getValue());
	private static final String LABEL_RESULT = "result";
	protected final static Logger logger = LoggerFactory
			.getLogger(CreateRuleCommand.class);
	private static final String LONG_LABEL_RESULT = "result of the update operation";
	public CreateRuleCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace,
				CREATE_RULE_COMMAND_NAME, CREATE_RULE_COMMAND_DESCRIPTION,
				DEFAULT_WRITE_MASK, DEFAULT_WRITE_MASK, true, true);
		addInputArgument(MiloStrategy.LABEL_FIELD,
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("Label for the rule"));
		addInputArgument(WaldotRulesEnginePlugin.CONDITION_FIELD,
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(
						WaldotRulesEnginePlugin.CONDITION_FIELD_DESCRIPTION));
		addInputArgument(WaldotRulesEnginePlugin.ACTION_FIELD,
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(
						WaldotRulesEnginePlugin.ACTION_FIELD_DESCRIPTION));
		addInputArgument(WaldotRulesEnginePlugin.DEBUG_LEVEL_LABEL,
				VariableNodeTypes.Int16.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(
						WaldotRulesEnginePlugin.DEBUG_LEVEL_LABEL_DESCRIPTION));
		addOutputArgument(LABEL_RESULT, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null,
				LocalizedText.english(LONG_LABEL_RESULT));
		this.addReference(
				new Reference(this.getNodeId(), NodeIds.HasModellingRule,
						NodeIds.ModellingRule_Mandatory.expanded(), true));

	}

	@Override
	public Object clone() {
		return new CreateRuleCommand(this.waldotNamespace);
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
		parameters
				.add(WaldotRulesEnginePlugin.RULE_NODE_PARAMETER.toLowerCase());
		enrichParameters(inputValues, parameters, 0,
				MiloStrategy.LABEL_FIELD.toLowerCase());
		enrichParameters(inputValues, parameters, 1,
				WaldotRulesEnginePlugin.CONDITION_FIELD.toLowerCase());
		enrichParameters(inputValues, parameters, 2,
				WaldotRulesEnginePlugin.ACTION_FIELD.toLowerCase());
		enrichParameters(inputValues, parameters, 3,
				WaldotRulesEnginePlugin.DEBUG_LEVEL_LABEL.toLowerCase());
		final Object[] arrayParameters = parameters.toArray(new String[0]);
		logger.info("create rule with parameters: "
				+ Arrays.toString(arrayParameters));
		return new String[]{getGraph().addVertex(arrayParameters).toString()};
	}

}
