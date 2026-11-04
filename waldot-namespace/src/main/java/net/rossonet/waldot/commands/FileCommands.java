package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_BLOCKS;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_BLOCK_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_DATA;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_JSON;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_PATH;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ROOT;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_TRANSFER_ID;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.DEFAULT_BLOCK_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.FILE_GET_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.FILE_LIST_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.INFORMATION_DIRECTORY;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MANAGE_DIRECTORY;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MAX_BLOCK_SIZE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MIN_BLOCK_SIZE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.files.ServerFileRoots;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.commands.exchange.GraphTransferStore;

/**
 * {@code file list} and {@code file get}: read the files of the server
 * directories registered in {@link ServerFileRoots}, with the same block
 * transfer of {@code export graph}.
 */
public final class FileCommands {

	/** {@code file get}: content of a file, inline or by blocks. */
	public static final class Get extends AbstractExchangeCommand {

		private final GraphTransferStore transfers;

		public Get(final WaldotNamespace namespace, final GraphTransferStore transfers) {
			super(namespace, FILE_GET_COMMAND,
					"read a file of a server root (dtml, configurations, ...): base64 data or a transferId to read "
							+ "with 'read graph block'",
					MANAGE_DIRECTORY, new String[] { ARG_ROOT, ARG_PATH, ARG_BLOCK_SIZE },
					new String[] { ARG_DATA, ARG_TRANSFER_ID, ARG_BLOCKS, ARG_SIZE, ARG_PATH, ARG_ERROR });
			this.transfers = transfers;
		}

		@Override
		public Object clone() {
			return new Get(waldotNamespace, transfers);
		}

		@Override
		public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
			final String[] output = newOutput();
			try {
				final String root = CommandArguments.requireFirst(inputValues, ARG_ROOT).strip();
				final int blockSize = intArgument(inputValues, 2, DEFAULT_BLOCK_SIZE, ARG_BLOCK_SIZE);
				if (blockSize < MIN_BLOCK_SIZE || blockSize > MAX_BLOCK_SIZE) {
					throw new IllegalArgumentException(
							"blockSize must be between " + MIN_BLOCK_SIZE + " and " + MAX_BLOCK_SIZE);
				}
				final Path file = ServerFileRoots.resolve(root, stringArgument(inputValues, 1));
				if (!Files.isRegularFile(file)) {
					throw new IllegalArgumentException("'" + file + "' non è un file: usa file list");
				}
				final long size = Files.size(file);
				if (size > MAX_FILE_SIZE) {
					throw new IllegalArgumentException(
							"il file '" + file + "' è di " + size + " byte, oltre il limite di " + MAX_FILE_SIZE);
				}
				final byte[] payload = Files.readAllBytes(file);
				if (payload.length <= blockSize) {
					output[0] = Base64.getEncoder().encodeToString(payload);
					output[2] = "1";
				} else {
					final String transferId = transfers.registerDownload(payload, blockSize);
					output[1] = transferId;
					output[2] = String.valueOf(transfers.download(transferId).blocks());
				}
				output[3] = String.valueOf(payload.length);
				output[4] = file.toString();
			} catch (final Throwable e) {
				output[5] = CommandArguments.errorMessage(e);
			}
			return output;
		}
	}

	/** {@code file list}: the roots, or the files of a directory of a root. */
	public static final class ListFiles extends AbstractExchangeCommand {

		public ListFiles(final WaldotNamespace namespace) {
			super(namespace, FILE_LIST_COMMAND,
					"list the server file roots or, with a root, its files as JSON (name, size, modified)",
					INFORMATION_DIRECTORY, new String[] { ARG_ROOT, ARG_PATH }, new String[] { ARG_JSON, ARG_ERROR });
		}

		@Override
		public Object clone() {
			return new ListFiles(waldotNamespace);
		}

		@Override
		public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
			final String[] output = newOutput();
			try {
				output[0] = JSON.writeValueAsString(list(stringArgument(inputValues, 0).strip(),
						stringArgument(inputValues, 1)));
			} catch (final Throwable e) {
				output[1] = CommandArguments.errorMessage(e);
			}
			return output;
		}
	}

	private static final ObjectMapper JSON = new ObjectMapper();

	/** Largest file that can be read (64 MiB). */
	public static final long MAX_FILE_SIZE = 64L * 1024 * 1024;

	/** JSON tree of {@code file list} (also used by the tests). */
	public static Map<String, Object> list(final String root, final String path) throws IOException {
		final Map<String, Object> out = new LinkedHashMap<>();
		if (root == null || root.isBlank()) {
			final List<Map<String, Object>> roots = new ArrayList<>();
			for (final Map.Entry<String, Path> e : ServerFileRoots.all().entrySet()) {
				final Map<String, Object> m = new LinkedHashMap<>();
				m.put("root", e.getKey());
				m.put("path", e.getValue().toString());
				m.put("exists", Files.isDirectory(e.getValue()));
				roots.add(m);
			}
			out.put("roots", roots);
			return out;
		}
		final Path directory = ServerFileRoots.resolve(root, path);
		if (!Files.isDirectory(directory)) {
			throw new IllegalArgumentException("'" + directory + "' non è una directory: usa file get");
		}
		final Path base = ServerFileRoots.resolve(root, "");
		final List<Map<String, Object>> files = new ArrayList<>();
		try (Stream<Path> stream = Files.list(directory)) {
			for (final Path p : stream.sorted().toList()) {
				final Map<String, Object> m = new LinkedHashMap<>();
				m.put("name", base.relativize(p.toAbsolutePath()).toString());
				m.put("directory", Files.isDirectory(p));
				m.put("size", Files.isRegularFile(p) ? Files.size(p) : 0L);
				m.put("modified", Files.getLastModifiedTime(p).toInstant().toString());
				files.add(m);
			}
		}
		out.put("root", root);
		out.put("path", directory.toString());
		out.put("files", files);
		return out;
	}

	private FileCommands() {
	}
}
