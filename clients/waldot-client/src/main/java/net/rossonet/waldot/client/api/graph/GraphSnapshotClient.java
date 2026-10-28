package net.rossonet.waldot.client.api.graph;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;

/**
 * Backup and restore of the whole graph with the commands {@code export graph},
 * {@code read graph block}, {@code import graph} and {@code clear graph}. A
 * payload not bigger than the block size travels in a single call, a bigger
 * one in blocks. The errors are reported with
 * {@link net.rossonet.waldot.client.api.WaldotClientException}.
 */
public interface GraphSnapshotClient {

	/** Removes every vertex and edge of the graph. */
	GraphChangeResult clearGraph();

	/** Exports the graph compressed, without ephemeral elements. */
	default GraphSnapshot exportGraph() {
		return exportGraph(true, false, DataExchangeProtocol.DEFAULT_BLOCK_SIZE);
	}

	/**
	 * Exports the graph.
	 *
	 * @param compress         gzip the payload
	 * @param includeEphemeral include the ephemeral elements
	 * @param blockSize        block size of the transfer (between
	 *                         {@link DataExchangeProtocol#MIN_BLOCK_SIZE} and
	 *                         {@link DataExchangeProtocol#MAX_BLOCK_SIZE})
	 */
	GraphSnapshot exportGraph(boolean compress, boolean includeEphemeral, int blockSize);

	/**
	 * Imports a snapshot (GraphSON, plain or gzip).
	 *
	 * @param snapshot    the snapshot
	 * @param clearBefore remove the current graph first
	 * @param blockSize   size of the uploaded blocks (bytes of payload)
	 */
	GraphChangeResult importGraph(GraphSnapshot snapshot, boolean clearBefore, int blockSize);

	/** Imports a snapshot with the default block size. */
	default GraphChangeResult importGraph(final GraphSnapshot snapshot, final boolean clearBefore) {
		return importGraph(snapshot, clearBefore, DataExchangeProtocol.DEFAULT_BLOCK_SIZE);
	}

	/** Replaces the current graph with a snapshot. */
	default GraphChangeResult restore(final GraphSnapshot snapshot) {
		return importGraph(snapshot, true);
	}
}
