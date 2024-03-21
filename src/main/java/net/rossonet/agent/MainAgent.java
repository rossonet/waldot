
package net.rossonet.agent;

import org.rossonet.ext.picocli.CommandLine;

/**
 * Classe main per avvio applicazione
 *
 * @author Andrea Ambrosini
 */
public class MainAgent {
	public static void main(final String... args) {
		final int exitCode = new CommandLine(new BotEngine()).execute(args);
		System.exit(exitCode);
	}
}
