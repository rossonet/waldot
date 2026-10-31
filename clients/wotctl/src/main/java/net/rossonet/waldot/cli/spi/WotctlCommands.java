package net.rossonet.waldot.cli.spi;

/**
 * Registration of the commands of a {@link WotctlPlugin}. A command is a class
 * annotated with {@code @Command} or an instance of it.
 */
public interface WotctlCommands {

	/**
	 * Adds a first level command ({@code wotctl NAME}).
	 *
	 * @return this, to chain the calls
	 */
	WotctlCommands add(Object command);

	/**
	 * Adds a subcommand to an existing group, given by the names of the commands
	 * separated by spaces (e.g. {@code "snapshot"} for
	 * {@code wotctl snapshot NAME}, {@code "acme tools"} for a group of a
	 * plugin).
	 *
	 * @return this, to chain the calls
	 */
	WotctlCommands add(String parentPath, Object command);
}
