package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_CLEAR_BEFORE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_DATA;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_EDGES;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_LAST;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_TRANSFER_ID;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_VERTICES;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.IMPORT_GRAPH_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MANAGE_DIRECTORY;

import java.util.Base64;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.commands.exchange.GraphSnapshotCodec;
import net.rossonet.waldot.commands.exchange.GraphTransferStore;

/**
 * Restores a snapshot written by
 * {@value net.rossonet.waldot.api.exchange.DataExchangeProtocol#EXPORT_GRAPH_COMMAND}.
 * The snapshot is uploaded in one or more blocks (base64, gzip detected
 * automatically): the first call has an empty {@code transferId} and returns
 * the id to use for the following blocks; the call with {@code last=true}
 * imports the whole snapshot (removing the current graph first when
 * {@code clearBefore=true}). Elements that cannot be created are skipped and
 * reported in {@code error}, the others are kept.
 */
public class ImportGraphCommand extends AbstractExchangeCommand {

	private final GraphTransferStore transfers;

	public ImportGraphCommand(final WaldotNamespace namespace, final GraphTransferStore transfers) {
		super(namespace, IMPORT_GRAPH_COMMAND, "restore a graph serialized with 'export graph'", MANAGE_DIRECTORY,
				new String[] { ARG_TRANSFER_ID, ARG_DATA, ARG_LAST, ARG_CLEAR_BEFORE },
				new String[] { ARG_TRANSFER_ID, ARG_VERTICES, ARG_EDGES, ARG_ERROR });
		this.transfers = transfers;
	}

	@Override
	public Object clone() {
		return new ImportGraphCommand(waldotNamespace, transfers);
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		final String[] output = newOutput();
		try {
			final String data = stringArgument(inputValues, 1);
			final boolean last = booleanArgument(inputValues, 2, true);
			final boolean clearBefore = booleanArgument(inputValues, 3, false);
			final String transferId = transfers.appendUpload(stringArgument(inputValues, 0),
					Base64.getDecoder().decode(data.trim()));
			output[0] = transferId;
			if (last) {
				final GraphSnapshotCodec.ImportResult result = GraphSnapshotCodec
						.importGraph(getNamespace().getGremlinGraph(), transfers.completeUpload(transferId), clearBefore);
				output[1] = String.valueOf(result.vertices());
				output[2] = String.valueOf(result.edges());
				if (!result.errors().isEmpty()) {
					output[3] = "not restored: " + String.join("; ", result.errors());
				}
			}
		} catch (final Throwable e) {
			output[3] = CommandArguments.errorMessage(e);
		}
		return output;
	}
}
