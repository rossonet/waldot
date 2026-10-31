package net.rossonet.waldot.cli;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import net.rossonet.waldot.cli.CliTestSupport.Result;
import net.rossonet.waldot.cli.support.ClientCommand;

/**
 * Base of the tests of the commands: every command connects to a
 * {@link MockClient} and the binary data written to the raw stdout are
 * captured in {@link #binaryOut}.
 */
public abstract class CommandTestBase {

	/** Binary data written by the commands on the raw standard output. */
	protected final ByteArrayOutputStream binaryOut = new ByteArrayOutputStream();

	protected MockClient mock;

	@TempDir
	protected Path tmp;

	/** @return the binary standard output as UTF-8 text */
	protected String binaryText() {
		return binaryOut.toString(StandardCharsets.UTF_8);
	}

	@BeforeEach
	void installMockClient() {
		mock = new MockClient();
		ClientCommand.clientFactory(configuration -> {
			mock.configuration = configuration;
			return mock.client;
		});
		// the commands do not close the raw stdout
		ClientCommand.stdout(() -> new FilterOutputStream(binaryOut));
	}

	@AfterEach
	void resetHooks() {
		ClientCommand.resetTestHooks();
	}

	protected Result run(final String... args) {
		return CliTestSupport.run(args);
	}

	protected Result runWithStdin(final byte[] stdin, final String... args) {
		return CliTestSupport.runWithStdin(stdin, args);
	}

	protected Result runWithStdin(final String stdin, final String... args) {
		return CliTestSupport.runWithStdin(stdin, args);
	}
}
