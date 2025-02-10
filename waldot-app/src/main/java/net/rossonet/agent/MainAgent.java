
package net.rossonet.agent;

import picocli.CommandLine;

/**
 * Avvio WaldOT da linea di comando
 *
 * @author Andrea Ambrosini
 */
public class MainAgent {
	public static void main(final String... args) {
		final int exitCode = new CommandLine(new WaldotRunner()).execute(args);
		System.out.println("Exit code: " + exitCode);
		System.exit(exitCode);
	}
}
