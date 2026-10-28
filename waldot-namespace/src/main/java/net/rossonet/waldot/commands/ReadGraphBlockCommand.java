package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_DATA;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_INDEX;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_LAST;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_TRANSFER_ID;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MANAGE_DIRECTORY;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.READ_GRAPH_BLOCK_COMMAND;

import java.util.Base64;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.commands.exchange.GraphTransferStore;

/**
 * Downloads a block of a snapshot prepared by
 * {@value net.rossonet.waldot.api.exchange.DataExchangeProtocol#EXPORT_GRAPH_COMMAND}.
 * The transfer is released after the last block.
 */
public class ReadGraphBlockCommand extends AbstractExchangeCommand {

	private final GraphTransferStore transfers;

	public ReadGraphBlockCommand(final WaldotNamespace namespace, final GraphTransferStore transfers) {
		super(namespace, READ_GRAPH_BLOCK_COMMAND, "read a block of an exported graph", MANAGE_DIRECTORY,
				new String[] { ARG_TRANSFER_ID, ARG_INDEX }, new String[] { ARG_DATA, ARG_LAST, ARG_ERROR });
		this.transfers = transfers;
	}

	@Override
	public Object clone() {
		return new ReadGraphBlockCommand(waldotNamespace, transfers);
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		final String[] output = newOutput();
		try {
			final String transferId = CommandArguments.requireFirst(inputValues, ARG_TRANSFER_ID).trim();
			final int index = intArgument(inputValues, 1, 0, ARG_INDEX);
			final GraphTransferStore.Download download = transfers.download(transferId);
			final int blocks = download == null ? -1 : download.blocks();
			output[0] = Base64.getEncoder().encodeToString(transfers.readBlock(transferId, index));
			output[1] = String.valueOf(index == blocks - 1);
		} catch (final Throwable e) {
			output[2] = CommandArguments.errorMessage(e);
		}
		return output;
	}
}
