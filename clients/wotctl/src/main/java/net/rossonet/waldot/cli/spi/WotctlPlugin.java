package net.rossonet.waldot.cli.spi;

/**
 * Plugin of {@code wotctl}: adds commands to the command line. The
 * implementations are found with {@link java.util.ServiceLoader} (file
 * {@code META-INF/services/net.rossonet.waldot.cli.spi.WotctlPlugin}), so they
 * must be on the classpath when {@code wotctl} is built: a fat jar or a native
 * binary that contains the plugins is produced by a distribution module that
 * depends on {@code wotctl} and on the plugins.
 * <p>
 * The commands are normal picocli commands: they extend
 * {@link net.rossonet.waldot.cli.support.ClientCommand} (connection options,
 * {@code -o} format, exit codes) or
 * {@link net.rossonet.waldot.cli.support.CommandGroup}, and they get the
 * Italian texts of the help, the exit codes and the shell completion of the
 * core commands.
 *
 * <pre>
 * public class AcmePlugin implements WotctlPlugin {
 * 	public String name() {
 * 		return "acme";
 * 	}
 *
 * 	public void register(final WotctlCommands commands) {
 * 		commands.add(AcmeCommand.class).add("snapshot", AcmeCheckCommand.class);
 * 	}
 * }
 * </pre>
 */
public interface WotctlPlugin {

	/**
	 * @return short name of the plugin, shown by {@code wotctl --version} and in
	 *         the error messages
	 */
	String name();

	/**
	 * Adds the commands of the plugin. A command name already used by
	 * {@code wotctl} or by another plugin is an error.
	 */
	void register(WotctlCommands commands);

	/**
	 * @return version of the plugin shown by {@code wotctl --version}, null if
	 *         unknown; by default the {@code Implementation-Version} of the jar
	 *         (absent in the native binary, see
	 *         {@link PluginVersion#fromResource(Class, String)})
	 */
	default String version() {
		return getClass().getPackage().getImplementationVersion();
	}
}
