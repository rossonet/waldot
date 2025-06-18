
package net.rossonet.agent;

import picocli.CommandLine;

/**
 * Main class for the Waldot Agent. This class serves as the entry point for the
 * Waldot application, initializing and executing the WaldotRunner with command
 * line arguments.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class MainAgent {
	public static final String DEFAULT_FILE_CONFIGURATION_PATH = "file:///waldot/boot.conf";

	public static void main(final String... args) {
		final int exitCode = new CommandLine(new WaldotRunner()).execute(args);
		System.out.println("Exit code: " + exitCode);
		System.exit(exitCode);
	}
}
