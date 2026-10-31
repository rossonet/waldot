package net.rossonet.waldot.cli.spi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import picocli.CommandLine;

/**
 * Finds the {@link WotctlPlugin} with {@link ServiceLoader} and adds their
 * commands to the command line. The plugins are sorted by name, so the help
 * and the completion script do not depend on the order of the classpath.
 */
public final class PluginLoader {

	/** Registration of the commands of one plugin. */
	private static final class Registration implements WotctlCommands {

		private final WotctlPlugin plugin;
		private final CommandLine root;

		Registration(final CommandLine root, final WotctlPlugin plugin) {
			this.root = root;
			this.plugin = plugin;
		}

		@Override
		public WotctlCommands add(final Object command) {
			return add("", command);
		}

		@Override
		public WotctlCommands add(final String parentPath,
				final Object command) {
			final CommandLine parent = parent(parentPath);
			final CommandLine subcommand;
			try {
				subcommand = command instanceof final CommandLine commandLine
						? commandLine
						: new CommandLine(command, root.getFactory());
			} catch (final CommandLine.InitializationException e) {
				throw error("comando non valido " + command + ": "
						+ e.getMessage(), e);
			}
			final List<String> names = new ArrayList<>();
			names.add(subcommand.getCommandName());
			names.addAll(List.of(subcommand.getCommandSpec().aliases()));
			for (final String name : names) {
				if (parent.getSubcommands().containsKey(name)) {
					throw error("il comando '"
							+ (parent.getCommandSpec().qualifiedName() + " "
									+ name)
							+ "' esiste già", null);
				}
			}
			parent.addSubcommand(subcommand);
			return this;
		}

		private IllegalStateException error(final String message,
				final Throwable cause) {
			return new IllegalStateException(
					"plugin '" + plugin.name() + "': " + message, cause);
		}

		private CommandLine parent(final String parentPath) {
			CommandLine parent = root;
			if (parentPath == null || parentPath.isBlank()) {
				return parent;
			}
			for (final String name : parentPath.trim().split("\\s+")) {
				final CommandLine next = parent.getSubcommands().get(name);
				if (next == null) {
					throw error("il gruppo '" + parentPath.trim()
							+ "' non esiste", null);
				}
				parent = next;
			}
			return parent;
		}
	}

	/**
	 * @return the plugins on the classpath, sorted by name
	 * @throws IllegalStateException if a plugin cannot be loaded
	 */
	public static List<WotctlPlugin> plugins() {
		final List<WotctlPlugin> found = new ArrayList<>();
		try {
			for (final WotctlPlugin plugin : ServiceLoader
					.load(WotctlPlugin.class)) {
				found.add(plugin);
			}
		} catch (final ServiceConfigurationError e) {
			throw new IllegalStateException(
					"plugin non caricabile: " + e.getMessage(), e);
		}
		found.sort(Comparator.comparing(WotctlPlugin::name));
		return List.copyOf(found);
	}

	/**
	 * Adds the commands of the plugins to the command line, in the given order.
	 * It must be called before the configuration of the command line (resource
	 * bundle, usage), that applies only to the subcommands already present.
	 *
	 * @throws IllegalStateException if a plugin uses a name already taken or a
	 *                               group that does not exist
	 */
	public static void register(final CommandLine root,
			final List<WotctlPlugin> plugins) {
		for (final WotctlPlugin plugin : plugins) {
			plugin.register(new Registration(root, plugin));
		}
	}

	private PluginLoader() {
	}
}
