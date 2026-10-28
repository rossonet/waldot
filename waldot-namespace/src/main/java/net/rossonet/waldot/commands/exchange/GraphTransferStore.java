package net.rossonet.waldot.commands.exchange;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;

/**
 * Pending chunked transfers of graph snapshots, shared by the export, read
 * block and import commands of a namespace. A transfer not completed within
 * the time to live ({@value DataExchangeProtocol#TRANSFER_TTL_PROPERTY}) is
 * discarded.
 */
public final class GraphTransferStore {

	/** A snapshot ready to be downloaded block by block. */
	public static final class Download {
		private final int blockSize;
		private final byte[] data;
		private volatile long expiresAt;

		Download(final byte[] data, final int blockSize, final long expiresAt) {
			this.data = data;
			this.blockSize = blockSize;
			this.expiresAt = expiresAt;
		}

		public byte[] block(final int index) {
			if (index < 0 || index >= blocks()) {
				throw new IllegalArgumentException("block index " + index + " out of range [0, " + blocks() + ")");
			}
			final int from = index * blockSize;
			return Arrays.copyOfRange(data, from, Math.min(data.length, from + blockSize));
		}

		public int blocks() {
			return Math.max(1, (data.length + blockSize - 1) / blockSize);
		}

		public int size() {
			return data.length;
		}
	}

	/** A snapshot being uploaded block by block. */
	public static final class Upload {
		private final ByteArrayOutputStream data = new ByteArrayOutputStream();
		private volatile long expiresAt;

		synchronized void append(final byte[] block, final long maxSize) {
			if ((long) data.size() + block.length > maxSize) {
				throw new IllegalArgumentException("snapshot bigger than " + maxSize + " bytes");
			}
			data.writeBytes(block);
		}

		synchronized byte[] toByteArray() {
			return data.toByteArray();
		}
	}

	private static long maxImportSize() {
		return Long.getLong(DataExchangeProtocol.MAX_IMPORT_SIZE_PROPERTY, 256L * 1024L * 1024L);
	}

	private static long ttl() {
		return Long.getLong(DataExchangeProtocol.TRANSFER_TTL_PROPERTY, 10L * 60L * 1000L);
	}

	private final Map<String, Download> downloads = new ConcurrentHashMap<>();

	private final Map<String, Upload> uploads = new ConcurrentHashMap<>();

	/**
	 * Appends a block to an upload.
	 *
	 * @param transferId the upload id, null or blank to start a new upload
	 * @return the id of the upload
	 */
	public String appendUpload(final String transferId, final byte[] block) {
		purgeExpired();
		final String id;
		final Upload upload;
		if (transferId == null || transferId.isBlank()) {
			id = UUID.randomUUID().toString();
			upload = new Upload();
			uploads.put(id, upload);
		} else {
			id = transferId.trim();
			upload = uploads.get(id);
			if (upload == null) {
				throw new IllegalArgumentException("unknown or expired transfer " + id);
			}
		}
		upload.expiresAt = System.currentTimeMillis() + ttl();
		try {
			upload.append(block, maxImportSize());
		} catch (final RuntimeException e) {
			uploads.remove(id);
			throw e;
		}
		return id;
	}

	/** Removes every pending transfer. */
	public void clear() {
		downloads.clear();
		uploads.clear();
	}

	/** Completes an upload and returns its content. */
	public byte[] completeUpload(final String transferId) {
		final Upload upload = uploads.remove(transferId);
		if (upload == null) {
			throw new IllegalArgumentException("unknown or expired transfer " + transferId);
		}
		return upload.toByteArray();
	}

	/** @return the number of pending transfers (downloads and uploads) */
	public int pendingTransfers() {
		purgeExpired();
		return downloads.size() + uploads.size();
	}

	private void purgeExpired() {
		final long now = System.currentTimeMillis();
		downloads.values().removeIf(d -> d.expiresAt < now);
		uploads.values().removeIf(u -> u.expiresAt < now);
	}

	/**
	 * Reads a block of a download. The download is removed after the last
	 * block.
	 */
	public byte[] readBlock(final String transferId, final int index) {
		purgeExpired();
		final Download download = transferId == null ? null : downloads.get(transferId.trim());
		if (download == null) {
			throw new IllegalArgumentException("unknown or expired transfer " + transferId);
		}
		final byte[] block = download.block(index);
		if (index == download.blocks() - 1) {
			downloads.remove(transferId.trim());
		} else {
			download.expiresAt = System.currentTimeMillis() + ttl();
		}
		return block;
	}

	/** Registers a download and returns its id. */
	public String registerDownload(final byte[] data, final int blockSize) {
		purgeExpired();
		final String id = UUID.randomUUID().toString();
		downloads.put(id, new Download(data, blockSize, System.currentTimeMillis() + ttl()));
		return id;
	}

	/** @return the download with the given id, or null */
	public Download download(final String transferId) {
		return transferId == null ? null : downloads.get(transferId.trim());
	}
}
