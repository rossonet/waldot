package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_BLOCKS;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_BLOCK_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_COMPRESS;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_DATA;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_EDGES;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_INCLUDE_EPHEMERAL;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_TRANSFER_ID;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_VERTICES;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.DEFAULT_BLOCK_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.EXPORT_GRAPH_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MANAGE_DIRECTORY;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MAX_BLOCK_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MIN_BLOCK_SIZE;

import java.util.Base64;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.commands.exchange.GraphSnapshotCodec;
import net.rossonet.waldot.commands.exchange.GraphTransferStore;

/**
 * Serializes the runtime graph (typed GraphSON v3, adjacency list, optional
 * gzip). A snapshot not bigger than {@code blockSize} is returned in
 * {@code data} (base64); a bigger one is kept on the server and its
 * {@code transferId} is returned, to be downloaded with
 * {@value net.rossonet.waldot.api.exchange.DataExchangeProtocol#READ_GRAPH_BLOCK_COMMAND}.
 */
public class ExportGraphCommand extends AbstractExchangeCommand {

	private final GraphTransferStore transfers;

	public ExportGraphCommand(final WaldotNamespace namespace, final GraphTransferStore transfers) {
		super(namespace, EXPORT_GRAPH_COMMAND, "serialize the graph (GraphSON v3) for a backup or a restore",
				MANAGE_DIRECTORY, new String[] { ARG_COMPRESS, ARG_INCLUDE_EPHEMERAL, ARG_BLOCK_SIZE },
				new String[] { ARG_DATA, ARG_TRANSFER_ID, ARG_BLOCKS, ARG_SIZE, ARG_VERTICES, ARG_EDGES, ARG_ERROR });
		this.transfers = transfers;
	}

	@Override
	public Object clone() {
		return new ExportGraphCommand(waldotNamespace, transfers);
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		final String[] output = newOutput();
		try {
			final boolean compress = booleanArgument(inputValues, 0, true);
			final boolean includeEphemeral = booleanArgument(inputValues, 1, false);
			final int blockSize = intArgument(inputValues, 2, DEFAULT_BLOCK_SIZE, ARG_BLOCK_SIZE);
			if (blockSize < MIN_BLOCK_SIZE || blockSize > MAX_BLOCK_SIZE) {
				throw new IllegalArgumentException(
						"blockSize must be between " + MIN_BLOCK_SIZE + " and " + MAX_BLOCK_SIZE);
			}
			final GraphSnapshotCodec.ExportResult result = GraphSnapshotCodec
					.exportGraph(getNamespace().getGremlinGraph(), includeEphemeral);
			final byte[] payload = compress ? GraphSnapshotCodec.compress(result.data()) : result.data();
			if (payload.length <= blockSize) {
				output[0] = Base64.getEncoder().encodeToString(payload);
				output[2] = "1";
			} else {
				final String transferId = transfers.registerDownload(payload, blockSize);
				output[1] = transferId;
				output[2] = String.valueOf(transfers.download(transferId).blocks());
			}
			output[3] = String.valueOf(payload.length);
			output[4] = String.valueOf(result.vertices());
			output[5] = String.valueOf(result.edges());
		} catch (final Throwable e) {
			output[6] = CommandArguments.errorMessage(e);
		}
		return output;
	}
}
