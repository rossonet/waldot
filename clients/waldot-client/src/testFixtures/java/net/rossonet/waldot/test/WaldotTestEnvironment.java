package net.rossonet.waldot.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.utils.NetworkHelper;

/**
 * Per-JVM isolated environment for WaldOT tests.
 * <p>
 * On first use (normally at launcher session start, see
 * {@link WaldotTestLauncherSessionListener}) it allocates free OPC UA ports and
 * a unique temporary directory and publishes them as system properties read by
 * the default server/client configurations. In this way several test JVMs
 * (Gradle forks and parallel modules) can run at the same time without
 * conflicting on port 12686 or on files in /tmp.
 * </p>
 * <p>
 * Properties already set (for example from the command line) are respected.
 * </p>
 */
public final class WaldotTestEnvironment {

	public static final String BOOT_FILE_PROPERTY = "waldot.boot.file";
	public static final String CLIENT_KEYSTORE_PROPERTY = "waldot.client.keystore";
	public static final String HTTPS_PORT_PROPERTY = "waldot.opc.https.port";
	/** Maximum wait (ms) for the release of the OPC UA port after a server shutdown. */
	public static final long PORT_RELEASE_TIMEOUT_MSEC = Long.getLong("waldot.test.port.release.timeout.msec",
			30_000L);
	public static final String SECURITY_DIR_PROPERTY = "waldot.security.dir";
	public static final String TCP_PORT_PROPERTY = "waldot.opc.tcp.port";
	public static final String TMP_DIR_PROPERTY = "waldot.test.tmp.dir";

	private static volatile boolean initialized = false;
	private static Path tmpDir;

	private static int allocateFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			return socket.getLocalPort();
		} catch (final IOException e) {
			throw new UncheckedIOException("unable to allocate a free port", e);
		}
	}

	/**
	 * Waits (at most {@link #PORT_RELEASE_TIMEOUT_MSEC}) until the OPC UA port is
	 * released.
	 *
	 * @return true if the port is free, false on timeout
	 */
	public static boolean awaitOpcPortReleased() {
		return awaitPortReleased(opcTcpPort(), PORT_RELEASE_TIMEOUT_MSEC);
	}

	/**
	 * Waits until the given local port is free, at most timeoutMsec.
	 *
	 * @return true if the port is free, false on timeout
	 */
	public static boolean awaitPortReleased(final int port, final long timeoutMsec) {
		final long deadline = System.currentTimeMillis() + timeoutMsec;
		try {
			while (!NetworkHelper.checkLocalPortAvailable(port)) {
				if (System.currentTimeMillis() > deadline) {
					System.err.println("port " + port + " still busy after " + timeoutMsec + " ms");
					return false;
				}
				Thread.sleep(20);
			}
			return true;
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/** Path of the default bootstrap file used by OpcFactory.getOpcGraph(). */
	public static Path bootFile() {
		init();
		return Path.of(System.getProperty(BOOT_FILE_PROPERTY));
	}

	/** URL (file://) of the default bootstrap file. */
	public static String bootUrl() {
		return bootFile().toUri().toString();
	}

	/**
	 * Always closes the client and the namespace (ignoring errors), waits for
	 * the release of the OPC UA port and deletes the default bootstrap file and
	 * the client keystore.
	 */
	public static void cleanup(final WaldotGraph graph, final WaldotTestClientHandler clientHandler) {
		if (clientHandler != null) {
			try {
				clientHandler.disconnect();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		if (graph != null && graph.getWaldotNamespace() != null) {
			try {
				graph.getWaldotNamespace().close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		awaitOpcPortReleased();
		deleteDefaultFiles();
	}

	/** Path of the client keystore. */
	public static Path clientKeystore() {
		init();
		return Path.of(System.getProperty(CLIENT_KEYSTORE_PROPERTY));
	}

	/** Deletes the default bootstrap file and the client keystore. */
	public static void deleteDefaultFiles() {
		try {
			Files.deleteIfExists(bootFile());
			Files.deleteIfExists(clientKeystore());
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private static void deleteRecursively(final Path dir) {
		if (dir == null || !Files.exists(dir)) {
			return;
		}
		try (Stream<Path> walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				} catch (final IOException e) {
					// best effort
				}
			});
		} catch (final IOException e) {
			// best effort
		}
	}

	/**
	 * Initializes the environment (idempotent). Must run before the loading of
	 * DefaultOpcUaConfiguration and DefaultWaldOTAgentClientConfigurationV1.
	 */
	public static synchronized void init() {
		if (initialized) {
			return;
		}
		try {
			final String configuredTmp = System.getProperty(TMP_DIR_PROPERTY);
			if (configuredTmp != null && !configuredTmp.isBlank()) {
				tmpDir = Files.createDirectories(Path.of(configuredTmp));
			} else {
				tmpDir = Files.createTempDirectory("waldot-test-");
				final Path toDelete = tmpDir;
				Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteRecursively(toDelete)));
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("unable to create the test temporary directory", e);
		}
		setIfAbsent(TCP_PORT_PROPERTY, String.valueOf(allocateFreePort()));
		setIfAbsent(HTTPS_PORT_PROPERTY, String.valueOf(allocateFreePort()));
		setIfAbsent(SECURITY_DIR_PROPERTY, tmpDir.resolve(".security").toString());
		setIfAbsent(CLIENT_KEYSTORE_PROPERTY, tmpDir.resolve("waldot-client.ks").toString());
		setIfAbsent(BOOT_FILE_PROPERTY, tmpDir.resolve("boot.conf").toString());
		initialized = true;
		System.out.println("WaldOT test environment: opc.tcp port " + System.getProperty(TCP_PORT_PROPERTY)
				+ ", tmp dir " + tmpDir);
	}

	/** OPC UA TCP port used by the server started in this JVM. */
	public static int opcTcpPort() {
		init();
		return Integer.getInteger(TCP_PORT_PROPERTY);
	}

	/** OPC UA endpoint of the server started in this JVM. */
	public static String opcEndpoint() {
		return "opc.tcp://localhost:" + opcTcpPort() + "/waldot";
	}

	private static void setIfAbsent(final String key, final String value) {
		final String actual = System.getProperty(key);
		if (actual == null || actual.isBlank()) {
			System.setProperty(key, value);
		}
	}

	/** Temporary directory reserved to this JVM. */
	public static Path tmpDir() {
		init();
		return tmpDir;
	}

	/** Path of a file inside the temporary directory reserved to this JVM. */
	public static Path tmpPath(final String name) {
		return tmpDir().resolve(name);
	}

	/** URL (file://) of a file inside the temporary directory of this JVM. */
	public static String tmpUrl(final String name) {
		return tmpPath(name).toUri().toString();
	}

	private WaldotTestEnvironment() {
	}
}
