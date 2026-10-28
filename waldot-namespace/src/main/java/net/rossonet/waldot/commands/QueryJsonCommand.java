package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_JSON;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.INFORMATION_DIRECTORY;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.QUERY_JSON_COMMAND;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.commands.exchange.GraphSnapshotCodec;

/**
 * Runs a JEXL expression like {@code query} but returns the result serialized
 * as typed GraphSON v3 (vertices, edges, paths, maps, lists, NodeIds...), so
 * that the clients can rebuild structured objects instead of parsing
 * {@code toString()}. Traversals and iterators are drained into lists; a null
 * result is the JSON {@code null}.
 */
public class QueryJsonCommand extends AbstractExchangeCommand {

	public QueryJsonCommand(final WaldotNamespace namespace) {
		super(namespace, QUERY_JSON_COMMAND, "run a JEXL expression and return the result as typed GraphSON v3",
				INFORMATION_DIRECTORY, new String[] { ARG_COMMAND }, new String[] { ARG_JSON, ARG_ERROR });
	}

	@Override
	public Object clone() {
		return new QueryJsonCommand(waldotNamespace);
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		final String[] output = newOutput();
		try {
			final Object result = getNamespace().runExpression(CommandArguments.requireFirst(inputValues, ARG_COMMAND));
			output[0] = GraphSnapshotCodec.toGraphSon(result);
		} catch (final Throwable e) {
			output[1] = CommandArguments.errorMessage(e);
		}
		return output;
	}
}
