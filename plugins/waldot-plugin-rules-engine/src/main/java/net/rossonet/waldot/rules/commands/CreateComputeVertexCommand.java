package net.rossonet.waldot.rules.commands;

import java.util.List;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;
import net.rossonet.waldot.rules.RulesEngineConstants;

/**
 * OPC UA command that creates a compute vertex (
 * {@value RulesEngineConstants#COMPUTE_NODE_PARAMETER}).
 */
public class CreateComputeVertexCommand extends AbstractCreateVertexCommand {

	private static final String CREATE_COMPUTE_COMMAND_DESCRIPTION = "Create a compute vertex: the thread manager "
			+ "that executes the actions of the rules connected with "
			+ RulesEngineConstants.EXECUTE_EDGE_LABEL + " edges";
	private static final String CREATE_COMPUTE_COMMAND_NAME = "create compute";

	public CreateComputeVertexCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace, CREATE_COMPUTE_COMMAND_NAME,
				CREATE_COMPUTE_COMMAND_DESCRIPTION,
				RulesEngineConstants.BASE_CMD_DIRECTORY,
				RulesEngineConstants.COMPUTE_NODE_PARAMETER, Language.ENGLISH,
				List.of(VertexParameter.optional(
						RulesEngineConstants.THREAD_POOL_SIZE_FIELD,
						VariableNodeTypes.Int32,
						RulesEngineConstants.THREAD_POOL_SIZE_FIELD_DESCRIPTION),
						VertexParameter.optional(
								RulesEngineConstants.EXECUTION_TIMEOUT_MS_FIELD,
								VariableNodeTypes.Int64,
								RulesEngineConstants.EXECUTION_TIMEOUT_MS_FIELD_DESCRIPTION),
						VertexParameter.optional(
								RulesEngineConstants.PRIORITY_FACTOR_FIELD,
								VariableNodeTypes.Double,
								RulesEngineConstants.PRIORITY_FACTOR_FIELD_DESCRIPTION)));
	}

	@Override
	protected String validate(final List<Object> keyValues) {
		final String threads = findValue(keyValues,
				RulesEngineConstants.THREAD_POOL_SIZE_FIELD);
		if (threads != null && Long.parseLong(threads) < 1) {
			return "the number of threads must be at least 1";
		}
		final String timeout = findValue(keyValues,
				RulesEngineConstants.EXECUTION_TIMEOUT_MS_FIELD);
		if (timeout != null && Long.parseLong(timeout) <= 0) {
			return "the execution timeout must be greater than 0";
		}
		return null;
	}

}
