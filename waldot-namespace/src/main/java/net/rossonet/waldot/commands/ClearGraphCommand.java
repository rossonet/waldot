package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_EDGES;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_VERTICES;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.CLEAR_GRAPH_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MANAGE_DIRECTORY;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * Removes every vertex and every edge of the graph (same as
 * {@code cmd.reset()} in the console). Outputs the number of removed
 * elements.
 */
public class ClearGraphCommand extends AbstractExchangeCommand {

	public ClearGraphCommand(final WaldotNamespace namespace) {
		super(namespace, CLEAR_GRAPH_COMMAND, "remove every vertex and edge of the graph", MANAGE_DIRECTORY,
				new String[0], new String[] { ARG_VERTICES, ARG_EDGES, ARG_ERROR });
	}

	@Override
	public Object clone() {
		return new ClearGraphCommand(waldotNamespace);
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		final String[] output = newOutput();
		try {
			final int vertices = getNamespace().getVerticesCount();
			final int edges = getNamespace().getEdgesCount();
			getNamespace().resetNameSpace();
			output[0] = String.valueOf(vertices - getNamespace().getVerticesCount());
			output[1] = String.valueOf(edges - getNamespace().getEdgesCount());
		} catch (final Throwable e) {
			output[2] = CommandArguments.errorMessage(e);
		}
		return output;
	}
}
