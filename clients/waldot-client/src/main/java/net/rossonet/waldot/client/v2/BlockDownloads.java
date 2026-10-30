package net.rossonet.waldot.client.v2;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_BLOCKS;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_DATA;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_LAST;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_TRANSFER_ID;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.READ_GRAPH_BLOCK_COMMAND;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import net.rossonet.waldot.client.api.WaldotClientException;
import net.rossonet.waldot.client.api.command.CommandResult;

/**
 * Payload of a command that returns its data inline ({@code data}) or as a
 * chunked transfer ({@code transferId}, {@code blocks}) read with
 * {@code read graph block}.
 */
final class BlockDownloads {

	/** Integer output, 0 when empty. */
	static int intOutput(final CommandResult result, final String name) {
		final String value = result.getString(name).trim();
		if (value.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(value);
		} catch (final NumberFormatException e) {
			throw new WaldotClientException("invalid output '" + name + "': " + value);
		}
	}

	/** @return the payload of the result, downloading its blocks if needed */
	static byte[] payload(final WaldotClientImplV2 owner, final CommandResult result, final int blockSize) {
		final int blocks = Math.max(1, intOutput(result, ARG_BLOCKS));
		final String transferId = result.getString(ARG_TRANSFER_ID).trim();
		if (transferId.isEmpty()) {
			return Base64.getDecoder().decode(result.getString(ARG_DATA).trim());
		}
		final ByteArrayOutputStream data = new ByteArrayOutputStream(blocks * blockSize);
		for (int index = 0; index < blocks; index++) {
			final CommandResult block = owner.commandClient()
					.invokeGlobal(READ_GRAPH_BLOCK_COMMAND, transferId, String.valueOf(index)).orThrow();
			data.writeBytes(Base64.getDecoder().decode(block.getString(ARG_DATA).trim()));
			if (Boolean.parseBoolean(block.getString(ARG_LAST).trim()) && index != blocks - 1) {
				throw new WaldotClientException("unexpected last block " + index + " of " + blocks);
			}
		}
		return data.toByteArray();
	}

	private BlockDownloads() {
	}
}
