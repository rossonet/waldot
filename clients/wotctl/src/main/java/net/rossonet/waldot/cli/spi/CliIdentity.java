package net.rossonet.waldot.cli.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Identity of the executable: the core builds {@code wotctl}, a distribution
 * with plugins (for example {@code wotctl-clarabella}) builds an executable
 * with another name. The distribution writes the resource {@link #RESOURCE}
 * at build time (keys {@code name}, {@code description}, {@code mcp-name},
 * and the commit of the repository of the distribution:
 * {@code git.commit.id.abbrev}, {@code git.commit.time}); without the resource
 * the identity is the one of {@code wotctl}.
 *
 * <p>
 * The texts of the commands use the picocli variable
 * {@code ${ROOT-COMMAND-NAME}}, the messages written at runtime
 * {@code CliIdentity.get().name()}. The connection variables {@code WALDOT_*} are common to
 * every executable; the variables and the directories of the executable
 * itself take its name ({@link #env(String)}, {@link #cacheDir()}).
 * </p>
 */
public final class CliIdentity {

	/** Name of the core executable. */
	public static final String DEFAULT_NAME = "wotctl";

	/** Resource written by the build of a distribution. */
	public static final String RESOURCE = "/net/rossonet/waldot/cli/cli-identity.properties";

	private static volatile CliIdentity current;

	/**
	 * Identity of this executable: the resource of the distribution if present,
	 * otherwise {@code wotctl}.
	 */
	public static CliIdentity get() {
		CliIdentity identity = current;
		if (identity == null) {
			identity = load();
			current = identity;
		}
		return identity;
	}

	private static CliIdentity load() {
		final Properties properties = new Properties();
		try (InputStream input = CliIdentity.class
				.getResourceAsStream(RESOURCE)) {
			if (input != null) {
				properties.load(
						new InputStreamReader(input, StandardCharsets.UTF_8));
			}
		} catch (final IOException e) {
			// identity of the core
		}
		return of(properties.getProperty("name"),
				properties.getProperty("description"),
				properties.getProperty("mcp-name"),
				properties.getProperty("git.commit.id.abbrev"),
				properties.getProperty("git.commit.time"));
	}

	/**
	 * Identity with the given values (blank = the default); for the tests and
	 * the embedded uses.
	 */
	public static CliIdentity of(final String name, final String description,
			final String mcpName) {
		return of(name, description, mcpName, null, null);
	}

	/**
	 * Identity with the given values and the commit of the build (blank =
	 * unknown).
	 */
	public static CliIdentity of(final String name, final String description,
			final String mcpName, final String commit,
			final String commitTime) {
		final String cleanName = name == null || name.isBlank()
				? DEFAULT_NAME
				: name.trim();
		return new CliIdentity(cleanName,
				description == null ? "" : description.trim(),
				mcpName == null || mcpName.isBlank()
						? cleanName
						: mcpName.trim(),
				commit == null ? "" : commit.trim(),
				commitTime == null ? "" : commitTime.trim());
	}

	/**
	 * Replaces the identity of the process (tests); null goes back to the
	 * resource.
	 */
	public static void set(final CliIdentity identity) {
		current = identity;
	}

	private final String commit;

	private final String commitTime;

	private final String description;

	private final String mcpName;

	private final String name;

	private CliIdentity(final String name, final String description,
			final String mcpName, final String commit,
			final String commitTime) {
		this.name = name;
		this.description = description;
		this.mcpName = mcpName;
		this.commit = commit;
		this.commitTime = commitTime;
	}

	/**
	 * Directory of the local state of the executable:
	 * {@code ~/.cache/<name>}.
	 */
	public Path cacheDir() {
		return Path.of(System.getProperty("user.home"), ".cache", name);
	}

	/**
	 * Short commit of the repository of the distribution at build time, empty
	 * if unknown (the core, builds without git).
	 */
	public String commit() {
		return commit;
	}

	/** Time of the commit (ISO 8601), empty if unknown. */
	public String commitTime() {
		return commitTime;
	}

	/** One-line description of the distribution, empty for the core. */
	public String description() {
		return description;
	}

	/**
	 * Environment variable of the executable: {@code <PREFIX>_<suffix>}, for
	 * example {@code MUUH_MCP_ARGS}; null if not set.
	 */
	public String env(final String suffix) {
		return System.getenv(envName(suffix));
	}

	/** Name of an environment variable of the executable. */
	public String envName(final String suffix) {
		return envPrefix() + "_" + suffix;
	}

	/**
	 * Prefix of the environment variables of the executable: the name in
	 * capitals, other characters as '_' ({@code wotctl} &rarr; {@code WOTCTL}).
	 */
	public String envPrefix() {
		return name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
	}

	/** Name of the MCP server ({@code wotctl mcp}): by default the name. */
	public String mcpName() {
		return mcpName;
	}

	/** Name of the executable ({@code wotctl} for the core). */
	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
