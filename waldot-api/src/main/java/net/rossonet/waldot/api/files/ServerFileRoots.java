package net.rossonet.waldot.api.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Directories of the server whose files the clients can read with
 * {@code file list} and {@code file get} (for example the DTML dumps and the
 * saved configurations of Clarabella). Only the registered roots are readable,
 * and a path can never leave its root, not even through a symbolic link.
 */
public final class ServerFileRoots {

	private static final ConcurrentSkipListMap<String, Path> ROOTS = new ConcurrentSkipListMap<>();

	/** The registered roots, sorted by name. */
	public static Map<String, Path> all() {
		return Map.copyOf(ROOTS);
	}

	/**
	 * Registers (or replaces) a root.
	 *
	 * @param name      name used by the clients, for example {@code dtml}
	 * @param directory the directory
	 */
	public static void register(final String name, final Path directory) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(directory, "directory");
		ROOTS.put(name, directory.toAbsolutePath().normalize());
	}

	/**
	 * Resolves a path inside a root.
	 *
	 * @param root name of the root
	 * @param path relative path, empty for the root itself
	 * @return the absolute real path
	 * @throws IllegalArgumentException if the root is unknown or the path leaves
	 *                                  it
	 * @throws IOException              if the path does not exist
	 */
	public static Path resolve(final String root, final String path) throws IOException {
		final Path base = ROOTS.get(root);
		if (base == null) {
			throw new IllegalArgumentException("radice sconosciuta '" + root + "'; radici: " + ROOTS.keySet());
		}
		final String relative = path == null ? "" : path.strip();
		if (Path.of(relative).isAbsolute()) {
			throw new IllegalArgumentException("il percorso deve essere relativo alla radice '" + root + "'");
		}
		final Path candidate = base.resolve(relative).normalize();
		if (!candidate.startsWith(base)) {
			throw new IllegalArgumentException("il percorso '" + path + "' esce dalla radice '" + root + "'");
		}
		if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS) && !Files.exists(candidate)) {
			throw new IOException("il file '" + relative + "' non esiste nella radice '" + root + "' (" + base + ")");
		}
		final Path real = candidate.toRealPath();
		final Path realBase = base.toRealPath();
		if (!real.startsWith(realBase)) {
			throw new IllegalArgumentException(
					"il percorso '" + path + "' porta fuori dalla radice '" + root + "' (link simbolico)");
		}
		return real;
	}

	/** Removes a root. */
	public static void unregister(final String name) {
		if (name != null) {
			ROOTS.remove(name);
		}
	}

	private ServerFileRoots() {
	}
}
