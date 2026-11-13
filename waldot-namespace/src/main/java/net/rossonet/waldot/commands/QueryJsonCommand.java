package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_JSON;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.INFORMATION_DIRECTORY;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.QUERY_JSON_COMMAND;

import java.nio.charset.StandardCharsets;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.channel.EncodingLimits;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.commands.exchange.GraphSnapshotCodec;

/**
 * Runs a JEXL expression like {@code query} but returns the result serialized
 * as typed GraphSON v3 (vertices, edges, paths, maps, lists, NodeIds...), so
 * that the clients can rebuild structured objects instead of parsing
 * {@code toString()}. Besides the core types it uses the TinkerPop extended
 * ones ({@code gx:Int16}, {@code gx:Byte}, {@code gx:BigDecimal}, ...) and the
 * OPC UA ones ({@code opc:UInt16}, {@code opc:ByteString}, ...). Traversals and iterators are drained into lists; a null
 * result is the JSON {@code null}.
 *
 * <p>
 * The result travels in a single OPC UA message, which cannot exceed the
 * message size of the transport ({@link EncodingLimits#DEFAULT_MAX_MESSAGE_SIZE},
 * 2 MiB). Above {@link #MAX_RESPONSE_BYTES} the command answers with an
 * explicit error instead of the result: Milo would otherwise fail the encoding
 * of the response with a {@code Bad_InternalError} that hides the cause. Large
 * reads are split by the client with {@code range()}.
 */
public class QueryJsonCommand extends AbstractExchangeCommand {

	/** Room left in a message for the envelope of the method call response. */
	static final int RESPONSE_ENVELOPE_BYTES = 64 * 1024;

	/** Largest GraphSON result (UTF-8 bytes) returned by the command. */
	public static final int MAX_RESPONSE_BYTES = EncodingLimits.DEFAULT_MAX_MESSAGE_SIZE - RESPONSE_ENVELOPE_BYTES;

	/**
	 * Null if {@code json} fits in a response of at most {@code maxBytes},
	 * otherwise the error message.
	 */
	static String tooLarge(final String json, final int maxBytes) {
		if (json == null || json.length() <= maxBytes / 3) {
			return null;
		}
		final int bytes = json.getBytes(StandardCharsets.UTF_8).length;
		return bytes <= maxBytes ? null
				: "response too large: " + bytes + " bytes, the limit of a response is " + maxBytes
						+ " bytes; split the query with range()";
	}

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
			final String json = GraphSnapshotCodec.toGraphSon(result);
			final String tooLarge = tooLarge(json, MAX_RESPONSE_BYTES);
			if (tooLarge == null) {
				output[0] = json;
			} else {
				output[1] = tooLarge;
			}
		} catch (final Throwable e) {
			output[1] = CommandArguments.errorMessage(e);
		}
		return output;
	}
}
