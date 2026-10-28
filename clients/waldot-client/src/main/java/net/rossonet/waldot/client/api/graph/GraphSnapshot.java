package net.rossonet.waldot.client.api.graph;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;

/**
 * A serialized graph downloaded with {@code export graph}: typed GraphSON v3,
 * adjacency list (one vertex with its edges per line), optionally gzip
 * compressed.
 *
 * @param data       payload as returned by the server
 * @param compressed true if the payload is gzip compressed
 * @param vertices   number of vertices
 * @param edges      number of edges
 * @param blocks     number of blocks used by the transfer
 */
public record GraphSnapshot(byte[] data, boolean compressed, int vertices, int edges, int blocks) {

	private static boolean isGzip(final byte[] bytes) {
		return bytes.length >= 2 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b;
	}

	/**
	 * Wraps a payload (for example read from a file); the compression is
	 * detected from the gzip header. The counters are unknown (-1).
	 */
	public static GraphSnapshot of(final byte[] data) {
		return new GraphSnapshot(data, isGzip(data), -1, -1, 1);
	}

	public GraphSnapshot {
		data = data == null ? new byte[0] : data.clone();
	}

	/** @return the payload as returned by the server (a copy) */
	@Override
	public byte[] data() {
		return data.clone();
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof GraphSnapshot snapshot && compressed == snapshot.compressed
				&& vertices == snapshot.vertices && edges == snapshot.edges && blocks == snapshot.blocks
				&& Arrays.equals(data, snapshot.data);
	}

	/** @return serialization format */
	public String format() {
		return DataExchangeProtocol.SNAPSHOT_FORMAT;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(data) * 31 + vertices * 7 + edges;
	}

	/** @return size of the payload in bytes */
	public int size() {
		return data.length;
	}

	/** @return the GraphSON text (decompressed if needed) */
	public String text() {
		return new String(uncompressed(), StandardCharsets.UTF_8);
	}

	@Override
	public String toString() {
		return "GraphSnapshot[size=" + data.length + ", compressed=" + compressed + ", vertices=" + vertices
				+ ", edges=" + edges + ", blocks=" + blocks + "]";
	}

	/** @return the GraphSON bytes (decompressed if needed) */
	public byte[] uncompressed() {
		if (!isGzip(data)) {
			return data.clone();
		}
		try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data));
				ByteArrayOutputStream out = new ByteArrayOutputStream(data.length * 4)) {
			in.transferTo(out);
			return out.toByteArray();
		} catch (final IOException e) {
			throw new UncheckedIOException("invalid gzip snapshot", e);
		}
	}
}
