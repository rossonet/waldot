package net.rossonet.waldot.rules.commands;

import java.util.List;

import net.rossonet.waldot.rules.RulesEngineConstants;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;

/**
 * OPC UA command that creates a rule vertex (
 * {@value RulesEngineConstants#RULE_NODE_PARAMETER}).
 */
public class CreateRuleCommand extends AbstractCreateVertexCommand {

	private static final String CREATE_RULE_COMMAND_DESCRIPTION = "Create a rule vertex: an IF-THEN rule with a JEXL "
			+ "condition and action, fired by 'fire' edges and executed by a compute vertex through a "
			+ RulesEngineConstants.EXECUTE_EDGE_LABEL + " edge";
	private static final String CREATE_RULE_COMMAND_NAME = "create rule";

	public CreateRuleCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace, CREATE_RULE_COMMAND_NAME, CREATE_RULE_COMMAND_DESCRIPTION,
				RulesEngineConstants.BASE_CMD_DIRECTORY, RulesEngineConstants.RULE_NODE_PARAMETER,
				Language.ENGLISH,
				List.of(VertexParameter.optional(RulesEngineConstants.CONDITION_FIELD, VariableNodeTypes.String,
						RulesEngineConstants.CONDITION_FIELD_DESCRIPTION),
						VertexParameter.optional(RulesEngineConstants.ACTION_FIELD, VariableNodeTypes.String,
								RulesEngineConstants.ACTION_FIELD_DESCRIPTION),
						VertexParameter.optional(RulesEngineConstants.DEBUG_LEVEL_LABEL, VariableNodeTypes.Int32,
								RulesEngineConstants.DEBUG_LEVEL_LABEL_DESCRIPTION),
						VertexParameter.optional(RulesEngineConstants.HYSTERESIS_LABEL, VariableNodeTypes.Int64,
								RulesEngineConstants.HYSTERESIS_LABEL_DESCRIPTION)));
	}

	@Override
	protected String validate(final List<Object> keyValues) {
		final String debug = findValue(keyValues, RulesEngineConstants.DEBUG_LEVEL_LABEL);
		if (debug != null && Long.parseLong(debug) < 0) {
			return "the debug level must not be negative";
		}
		final String hysteresis = findValue(keyValues, RulesEngineConstants.HYSTERESIS_LABEL);
		if (hysteresis != null && Long.parseLong(hysteresis) < 0) {
			return "the hysteresis must not be negative";
		}
		return null;
	}

}
