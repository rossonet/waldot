
package net.rossonet.cli;

import java.nio.file.Path;
import java.nio.file.Paths;

import picocli.CommandLine;

/**
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class MainCli {

	private static WaldotCommandRunner commandRunner;
	public static boolean running = true;
	public final static Path WORKING_DIR = Path.of(System.getProperty("user.home"), ".waldot");

	static {
		if (!WORKING_DIR.toFile().exists()) {
			WORKING_DIR.toFile().mkdirs();
			System.out.println("working directory " + WORKING_DIR.toAbsolutePath().toString() + " created");
		}
		final String logPath = Paths.get(WORKING_DIR.toAbsolutePath().toAbsolutePath().toString(), "client.log")
				.toString();
		System.setProperty("org.slf4j.simpleLogger.logFile", logPath);
	}

	public static void main(final String... args) {
		while (running) {
			try {
				commandRunner = new WaldotCommandRunner();
				final CommandLine commandLine = new CommandLine(commandRunner);
				commandRunner.setCommandLine(commandLine);
				commandLine.execute(args);
			} catch (final Throwable e) {
				System.err.println("error in main loop: " + e.getMessage());
				if (commandRunner != null) {
					try {
						commandRunner.close();
					} catch (final Exception e1) {
						System.err.println("error closing command runner: " + e1.getMessage());
					}
				}
			}
		}
		System.exit(0);
	}
}
