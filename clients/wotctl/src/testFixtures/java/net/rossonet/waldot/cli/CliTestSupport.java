package net.rossonet.waldot.cli;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.rossonet.waldot.cli.spi.PluginLoader;
import net.rossonet.waldot.cli.spi.WotctlPlugin;
import picocli.CommandLine;

/**
 * Runs the command line in the test JVM capturing stdout and stderr (and
 * optionally feeding stdin). Test fixture of wotctl, used also by the tests of
 * the plugins ({@code testImplementation testFixtures(project(':wotctl'))}).
 */
public final class CliTestSupport {

	/** Exit code and output of an execution. */
	public record Result(int exitCode, String out, String err) {

		/** @return the lines of stdout */
		public List<String> lines() {
			return out.lines().toList();
		}
	}

	/** Runs the command line with the plugins on the classpath. */
	public static Result run(final String... args) {
		return run(PluginLoader.plugins(), args);
	}

	/** Runs the command line with the given plugins. */
	public static Result run(final List<WotctlPlugin> plugins,
			final String... args) {
		final StringWriter out = new StringWriter();
		final StringWriter err = new StringWriter();
		final CommandLine commandLine = WaldotCli.newCommandLine(
				new PrintWriter(out), new PrintWriter(err), plugins);
		final int exitCode = commandLine.execute(args);
		return new Result(exitCode, out.toString(), err.toString());
	}

	/** Runs the command line with the given standard input. */
	public static Result runWithStdin(final byte[] stdin,
			final String... args) {
		final InputStream previous = System.in;
		System.setIn(new ByteArrayInputStream(stdin));
		try {
			return run(args);
		} finally {
			System.setIn(previous);
		}
	}

	/** Runs the command line with the given standard input. */
	public static Result runWithStdin(final String stdin,
			final String... args) {
		return runWithStdin(stdin.getBytes(StandardCharsets.UTF_8), args);
	}

	private CliTestSupport() {
	}
}
