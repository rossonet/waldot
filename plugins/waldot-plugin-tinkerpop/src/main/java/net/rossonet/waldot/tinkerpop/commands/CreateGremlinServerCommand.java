package net.rossonet.waldot.tinkerpop.commands;

import java.util.List;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;
import net.rossonet.waldot.tinkerpop.TinkerPopConstants;

/**
 * OPC UA command that creates a Gremlin Server vertex (
 * {@value TinkerPopConstants#GREMLIN_TYPE_LABEL}).
 */
public class CreateGremlinServerCommand extends AbstractCreateVertexCommand {

	private static final String COMMAND_DESCRIPTION = "Create a vertex that starts an embedded Gremlin Server exposing "
			+ "the WaldOT graph to the native TinkerPop clients (Gremlin Console, drivers, Graph-Explorer)";
	private static final String COMMAND_NAME = "create gremlin server";

	public CreateGremlinServerCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace, COMMAND_NAME, COMMAND_DESCRIPTION,
				TinkerPopConstants.BASE_CMD_DIRECTORY,
				TinkerPopConstants.GREMLIN_TYPE_LABEL, Language.ENGLISH,
				List.of(VertexParameter.optional(TinkerPopConstants.PORT_FIELD,
						VariableNodeTypes.UInt16,
						TinkerPopConstants.PORT_FIELD_DESCRIPTION),
						VertexParameter.optional(
								TinkerPopConstants.BIND_HOST_FIELD,
								VariableNodeTypes.String,
								TinkerPopConstants.BIND_HOST_FIELD_DESCRIPTION)));
	}

	@Override
	protected String validate(final List<Object> keyValues) {
		final String port = findValue(keyValues, TinkerPopConstants.PORT_FIELD);
		if (port != null) {
			final long value = Long.parseLong(port);
			if (value < 1 || value > 65535) {
				return "the port must be between 1 and 65535";
			}
		}
		return null;
	}

}
