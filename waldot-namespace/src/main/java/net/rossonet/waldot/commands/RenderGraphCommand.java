package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_BLOCKS;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_BLOCK_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_COMPRESS;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_DATA;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_EDGES;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_FORMAT;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_OPTIONS;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_TRANSFER_ID;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_VERTICES;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_WARNINGS;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.DEFAULT_BLOCK_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.INFORMATION_DIRECTORY;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MAX_BLOCK_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MIN_BLOCK_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.RENDER_GRAPH_COMMAND;

import java.util.Base64;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import net.rossonet.waldot.api.exchange.GraphRenderOptions;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.commands.exchange.GraphSnapshotCodec;
import net.rossonet.waldot.commands.exchange.GraphTransferStore;
import net.rossonet.waldot.commands.render.GraphRenderer;

/**
 * Draws the graph as SVG (layout by Eclipse ELK) or as Graphviz DOT, with the
 * selection, properties, layout and style given as JSON in {@code options}
 * (see {@link GraphRenderOptions}). Like
 * {@value net.rossonet.waldot.api.exchange.DataExchangeProtocol#EXPORT_GRAPH_COMMAND},
 * a drawing not bigger than {@code blockSize} is returned in {@code data}
 * (base64), a bigger one is downloaded with
 * {@value net.rossonet.waldot.api.exchange.DataExchangeProtocol#READ_GRAPH_BLOCK_COMMAND}.
 */
public class RenderGraphCommand extends AbstractExchangeCommand {

	private final GraphTransferStore transfers;

	public RenderGraphCommand(final WaldotNamespace namespace, final GraphTransferStore transfers) {
		super(namespace, RENDER_GRAPH_COMMAND, "draw the graph (or a part of it) as SVG or Graphviz DOT",
				INFORMATION_DIRECTORY, new String[] { ARG_OPTIONS, ARG_COMPRESS, ARG_BLOCK_SIZE },
				new String[] { ARG_DATA, ARG_TRANSFER_ID, ARG_BLOCKS, ARG_SIZE, ARG_FORMAT, ARG_VERTICES, ARG_EDGES,
						ARG_WARNINGS, ARG_ERROR });
		this.transfers = transfers;
	}

	@Override
	public Object clone() {
		return new RenderGraphCommand(waldotNamespace, transfers);
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		final String[] output = newOutput();
		try {
			final GraphRenderOptions options = GraphRenderOptions.fromJson(stringArgument(inputValues, 0));
			final boolean compress = booleanArgument(inputValues, 1, true);
			final int blockSize = intArgument(inputValues, 2, DEFAULT_BLOCK_SIZE, ARG_BLOCK_SIZE);
			if (blockSize < MIN_BLOCK_SIZE || blockSize > MAX_BLOCK_SIZE) {
				throw new IllegalArgumentException(
						"blockSize must be between " + MIN_BLOCK_SIZE + " and " + MAX_BLOCK_SIZE);
			}
			final GraphRenderer.Result result = GraphRenderer.render(getNamespace(), options);
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
			output[4] = result.format();
			output[5] = String.valueOf(result.vertices());
			output[6] = String.valueOf(result.edges());
			output[7] = String.join("\n", result.warnings());
		} catch (final Throwable e) {
			output[8] = CommandArguments.errorMessage(e);
		}
		return output;
	}
}
