package net.rossonet.waldot.client.v2;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_BLOCKS;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_EDGES;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_TRANSFER_ID;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_VERTICES;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.CLEAR_GRAPH_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.EXPORT_GRAPH_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.IMPORT_GRAPH_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MAX_BLOCK_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MIN_BLOCK_SIZE;

import java.util.Arrays;
import java.util.Base64;

import net.rossonet.waldot.client.api.WaldotClientException;
import net.rossonet.waldot.client.api.command.CommandResult;
import net.rossonet.waldot.client.api.graph.GraphChangeResult;
import net.rossonet.waldot.client.api.graph.GraphSnapshot;
import net.rossonet.waldot.client.api.graph.GraphSnapshotClient;

/** Export, import and clear of the graph for the client v2. */
final class GraphSnapshotClientV2 implements GraphSnapshotClient {

	static void checkBlockSize(final int blockSize) {
		if (blockSize < MIN_BLOCK_SIZE || blockSize > MAX_BLOCK_SIZE) {
			throw new IllegalArgumentException(
					"blockSize must be between " + MIN_BLOCK_SIZE + " and " + MAX_BLOCK_SIZE + ": " + blockSize);
		}
	}

	private static int intOutput(final CommandResult result, final String name) {
		return BlockDownloads.intOutput(result, name);
	}

	private final WaldotClientImplV2 owner;

	GraphSnapshotClientV2(final WaldotClientImplV2 owner) {
		this.owner = owner;
	}

	@Override
	public GraphChangeResult clearGraph() {
		final CommandResult result = owner.commandClient().invokeGlobal(CLEAR_GRAPH_COMMAND).orThrow();
		return new GraphChangeResult(intOutput(result, ARG_VERTICES), intOutput(result, ARG_EDGES), "");
	}

	@Override
	public GraphSnapshot exportGraph(final boolean compress, final boolean includeEphemeral, final int blockSize) {
		checkBlockSize(blockSize);
		final CommandResult result = owner.commandClient()
				.invokeGlobal(EXPORT_GRAPH_COMMAND, String.valueOf(compress), String.valueOf(includeEphemeral),
						String.valueOf(blockSize))
				.orThrow();
		final int vertices = intOutput(result, ARG_VERTICES);
		final int edges = intOutput(result, ARG_EDGES);
		final int blocks = Math.max(1, intOutput(result, ARG_BLOCKS));
		return new GraphSnapshot(BlockDownloads.payload(owner, result, blockSize), compress, vertices, edges,
				blocks);
	}

	@Override
	public GraphChangeResult importGraph(final GraphSnapshot snapshot, final boolean clearBefore,
			final int blockSize) {
		checkBlockSize(blockSize);
		final byte[] data = snapshot.data();
		String transferId = "";
		int offset = 0;
		CommandResult result;
		do {
			final int end = Math.min(data.length, offset + blockSize);
			final boolean last = end >= data.length;
			final String block = Base64.getEncoder().encodeToString(Arrays.copyOfRange(data, offset, end));
			result = owner.commandClient().invokeGlobal(IMPORT_GRAPH_COMMAND, transferId, block,
					String.valueOf(last), String.valueOf(clearBefore));
			if (result.status() != null && result.status().isBad()) {
				result.orThrow();
			}
			final String error = result.getString(ARG_ERROR);
			if (!last && !error.isBlank()) {
				throw new WaldotClientException("upload of the snapshot failed: " + error);
			}
			transferId = result.getString(ARG_TRANSFER_ID).trim();
			offset = end;
			if (last) {
				break;
			}
		} while (true);
		final String error = result.getString(ARG_ERROR);
		final int vertices = intOutput(result, ARG_VERTICES);
		final int edges = intOutput(result, ARG_EDGES);
		if (!error.isBlank() && vertices == 0 && edges == 0) {
			throw new WaldotClientException("import of the snapshot failed: " + error);
		}
		return new GraphChangeResult(vertices, edges, error);
	}
}
