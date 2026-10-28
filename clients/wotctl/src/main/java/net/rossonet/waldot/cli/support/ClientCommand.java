package net.rossonet.waldot.cli.support;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.WaldotClientException;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/**
 * Base of the commands that connect to the server: builds the configuration
 * from the {@link ConnectionOptions}, connects, runs the command and closes
 * the client (also on SIGINT, with a shutdown hook).
 */
public abstract class ClientCommand implements Callable<Integer> {

	/** Closes the client once (normal end or shutdown hook). */
	private static final class Closer implements Runnable {

		private final WaldotClient client;
		private final AtomicBoolean closed = new AtomicBoolean();

		Closer(final WaldotClient client) {
			this.client = client;
		}

		@Override
		public void run() {
			if (closed.compareAndSet(false, true)) {
				try {
					client.close();
				} catch (final RuntimeException e) {
					// closing anyway
				}
			}
		}
	}

	@ArgGroup(validate = false, order = 100, heading = "%nConnessione (ogni opzione ha anche una variabile d'ambiente WALDOT_*):%n")
	protected ConnectionOptions connection = new ConnectionOptions();

	@ArgGroup(validate = false, order = 200, heading = "%nOutput e diagnostica:%n")
	protected OutputOptions output = new OutputOptions();

	@Spec
	protected CommandSpec spec;

	@Override
	public final Integer call() throws Exception {
		final ConnectionOptions connectionOptions = connection == null ? new ConnectionOptions() : connection;
		final OutputOptions outputOptions = output == null ? new OutputOptions() : output;
		final OutputFormat format = outputOptions.format();
		if (format == OutputFormat.topic && !supportsTopicFormat()) {
			throw usage("il formato 'topic' è disponibile solo con il comando subscribe");
		}
		validate();
		final Printer printer = new Printer(spec.commandLine().getOut(), format);
		final WaldOTAgentClientConfiguration configuration = connectionOptions.toConfiguration(spec.commandLine());
		configure(configuration);
		final WaldotClient client = WaldotClient.create(configuration);
		final Closer closer = new Closer(client);
		final Thread hook = new Thread(closer, "waldot-cli-shutdown");
		Runtime.getRuntime().addShutdownHook(hook);
		try {
			try {
				client.connect(connectionOptions.connectTimeout(spec.commandLine()));
			} catch (final WaldotClientException e) {
				throw CliException.connection("connessione a " + configuration.getBaseWaldOTOpcEndpoint()
						+ " non riuscita: " + e.getMessage(), e);
			}
			return execute(client, printer);
		} finally {
			closer.run();
			try {
				Runtime.getRuntime().removeShutdownHook(hook);
			} catch (final IllegalStateException e) {
				// shutdown in progress
			}
		}
	}

	/** Customizes the configuration before the connection. */
	protected void configure(final WaldOTAgentClientConfiguration configuration) {
		// nothing by default
	}

	/**
	 * Runs the command on a connected client.
	 *
	 * @return the exit code
	 */
	protected abstract int execute(WaldotClient client, Printer printer) throws Exception;

	/** Parses a node id ({@code ns=2;s=x}, {@code i=85} or a WaldOT identifier). */
	protected NodeId nodeId(final WaldotClient client, final String value) {
		try {
			return client.addressSpace().parseNodeId(value);
		} catch (final RuntimeException e) {
			throw usage("node id non valido: '" + value + "'");
		}
	}

	/** @return true if the command supports the {@code topic} format */
	protected boolean supportsTopicFormat() {
		return false;
	}

	/** @return an exception of wrong usage (exit code 2) */
	protected ParameterException usage(final String message) {
		return new ParameterException(spec.commandLine(), message);
	}

	/** Checks the arguments before the connection. */
	protected void validate() {
		// nothing by default
	}
}
