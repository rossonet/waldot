package net.rossonet.waldot.cli;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;

import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

import net.rossonet.waldot.cli.commands.AboutCommand;
import net.rossonet.waldot.cli.commands.BrowseCommand;
import net.rossonet.waldot.cli.commands.CallCommand;
import net.rossonet.waldot.cli.commands.CommandsCommand;
import net.rossonet.waldot.cli.commands.CompletionCommand;
import net.rossonet.waldot.cli.commands.DescribeCommand;
import net.rossonet.waldot.cli.commands.EdgeCommand;
import net.rossonet.waldot.cli.commands.PropertyCommand;
import net.rossonet.waldot.cli.commands.QueryCommand;
import net.rossonet.waldot.cli.commands.ReadCommand;
import net.rossonet.waldot.cli.commands.RenderCommand;
import net.rossonet.waldot.cli.commands.ResolveCommand;
import net.rossonet.waldot.cli.commands.SnapshotCommand;
import net.rossonet.waldot.cli.commands.SubscribeCommand;
import net.rossonet.waldot.cli.commands.VertexCommand;
import net.rossonet.waldot.cli.commands.WriteCommand;
import net.rossonet.waldot.cli.spi.PluginLoader;
import net.rossonet.waldot.cli.spi.PluginVersion;
import net.rossonet.waldot.cli.spi.WotctlPlugin;
import net.rossonet.waldot.cli.support.CliException;
import net.rossonet.waldot.cli.support.ExitCodes;
import net.rossonet.waldot.client.api.WaldotClientException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;
import picocli.CommandLine.UnmatchedArgumentException;

/**
 * {@code wotctl}: command line interface of WaldOT, not interactive and
 * scriptable. The data go to stdout (a line at a time), logs and errors to
 * stderr.
 */
@Command(name = "wotctl", mixinStandardHelpOptions = true, versionProvider = WaldotCli.Version.class, description = {
		"Interfaccia a riga di comando di WaldOT: non interattiva, pensata per gli script bash,",
		"le pipe e i container. Si collega a un server WaldOT via OPC UA (client v2) ed espone",
		"tutte le funzioni del client: interrogazioni del grafo, comandi del server e degli",
		"elementi, modifica di vertici, archi e proprietà, backup e ripristino del grafo,",
		"accesso generico allo spazio degli indirizzi OPC UA e sottoscrizioni a valori ed eventi.",
		"",
		"Ogni invocazione apre una connessione, esegue un comando e chiude la connessione",
		"(subscribe invece resta in esecuzione e scrive i valori man mano che arrivano).",
		"", "Regole per l'uso negli script:",
		"  * stdout contiene solo i dati, una riga alla volta, con flush immediato;",
		"  * log, avvisi ed errori vanno su stderr (predefinito: solo avvisi ed errori;",
		"    -v, -vv, -vvv aumentano il dettaglio);",
		"  * il formato si sceglie con -o text|json|jsonl|value (topic solo per subscribe);",
		"  * il codice di uscita distingue errore del server, uso errato e problemi di",
		"    connessione (vedi sotto);",
		"  * se il processo a valle chiude la pipe (es. head -n 1) il comando termina in modo",
		"    pulito con codice 0.", "",
		"Le opzioni di connessione (--endpoint, sicurezza, identità, timeout, riconnessione)",
		"si indicano dopo il nome del comando e hanno tutte una variabile d'ambiente",
		"WALDOT_* equivalente: in un container basta 'docker run -e WALDOT_ENDPOINT=...'.",
		"Per la guida completa di un comando: wotctl help COMANDO oppure wotctl COMANDO --help."}, footer = {
				"Identificatori dei nodi:",
				"  Dove è richiesto un vertice, un arco o un nodo si può usare la forma OPC UA",
				"  (ns=2;s=sensor-1, i=85, ns=0;i=2253) oppure il solo identificatore stringa",
				"  nel namespace di WaldOT (sensor-1). Nella shell la forma OPC UA va tra apici",
				"  perché contiene ';' (wotctl read 'ns=2;s=sensor-1/value').",
				"", "Durate:",
				"  500ms, 10s, 2m, 1h, 1d, combinazioni come 1m30s, ISO-8601 (PT10S) oppure un",
				"  numero di secondi (10, 2.5).", "", "Variabili d'ambiente:",
				"  WALDOT_ENDPOINT, WALDOT_SECURITY_POLICY, WALDOT_SECURITY_MODE, WALDOT_USERNAME,",
				"  WALDOT_PASSWORD, WALDOT_PASSWORD_FILE, WALDOT_IDENTITY_CERT, WALDOT_IDENTITY_KEY,",
				"  WALDOT_KEYSTORE, WALDOT_KEYSTORE_PASSWORD, WALDOT_TRUST_ALL,",
				"  WALDOT_FORCE_ENDPOINT_URL, WALDOT_APPLICATION_URI, WALDOT_AGENT_NAME,",
				"  WALDOT_CONNECT_TIMEOUT, WALDOT_REQUEST_TIMEOUT, WALDOT_SESSION_TIMEOUT,",
				"  WALDOT_KEEPALIVE_TIMEOUT, WALDOT_RECONNECT_INITIAL, WALDOT_RECONNECT_MAX,",
				"  WALDOT_RECONNECT_ATTEMPTS, WALDOT_NAMESPACE_URI, WALDOT_COMMANDS_ROOT,",
				"  WALDOT_OUTPUT (formato predefinito), WALDOT_LOG_LEVEL (livello di log).",
				"  Un'opzione indicata sulla riga di comando prevale sulla variabile.",
				"", "Esempi:", "  # informazioni sul server",
				"  wotctl about --endpoint opc.tcp://server:12686/waldot", "",
				"  # vertici di tipo sensor in JSON Lines, filtrati con jq",
				"  wotctl query -o jsonl \"g.V().hasLabel('sensor')\" | jq -r .id",
				"", "  # valore di una proprietà in una variabile di shell",
				"  T=$(wotctl property get sensor-1 temperature -o value)", "",
				"  # backup e ripristino del grafo",
				"  wotctl snapshot export -f grafo.json.gz",
				"  wotctl snapshot import --clear -f grafo.json.gz", "",
				"  # disegno SVG dell'impianto, raggruppato per cartella",
				"  wotctl render -f impianto.svg --group-by directory", "",
				"  # inoltro dei valori a un broker MQTT (mosquitto_pub -l legge le righe da stdin)",
				"  wotctl subscribe -p sensor-1:value -o value | mosquitto_pub -t waldot/sensor-1 -l",
				"", "  # più proprietà ed eventi, un topic MQTT per ogni riga",
				"  wotctl subscribe -p s1:value -p s2:value -e s1 -o topic --topic-prefix waldot \\",
				"    | while read -r t p; do mosquitto_pub -t \"$t\" -m \"$p\"; done",
				"", "  # dal container Docker",
				"  docker run --rm -e WALDOT_ENDPOINT=opc.tcp://server:12686/waldot \\",
				"    rossonet/waldot-client subscribe -p s1:value",
				""}, subcommands = {HelpCommand.class, AboutCommand.class,
						QueryCommand.class, DescribeCommand.class,
						CommandsCommand.class, VertexCommand.class,
						EdgeCommand.class, PropertyCommand.class,
						SnapshotCommand.class, RenderCommand.class,
						BrowseCommand.class, ReadCommand.class,
						WriteCommand.class, CallCommand.class,
						ResolveCommand.class, SubscribeCommand.class,
						CompletionCommand.class})
public class WaldotCli implements Callable<Integer> {

	/**
	 * Version from the resource written by the build (also in the native
	 * binary, that has no manifest), otherwise from the manifest of the jar.
	 */
	public static final class Version implements IVersionProvider {

		/** Resource with the property {@code version}. */
		static final String RESOURCE = "/net/rossonet/waldot/cli/wotctl-version.properties";

		private final List<WotctlPlugin> plugins;

		/** Without plugins (instance created by picocli). */
		public Version() {
			this(List.of());
		}

		/** With a line for each plugin. */
		Version(final List<WotctlPlugin> plugins) {
			this.plugins = plugins;
		}

		/** @return the version of wotctl, null if unknown */
		static String version() {
			final String version = PluginVersion
					.fromResource(WaldotCli.class, RESOURCE);
			return version != null
					? version
					: WaldotCli.class.getPackage().getImplementationVersion();
		}

		@Override
		public String[] getVersion() {
			final String version = version();
			final List<String> lines = new ArrayList<>();
			lines.add("wotctl " + (version == null ? "(sviluppo)" : version));
			for (final WotctlPlugin plugin : plugins) {
				final String pluginVersion = plugin.version();
				lines.add("plugin " + plugin.name() + " "
						+ (pluginVersion == null ? "(sviluppo)" : pluginVersion));
			}
			lines.add("Java " + System.getProperty("java.version") + " ("
					+ System.getProperty("java.vendor")
					+ (isNativeImage() ? ", binario nativo GraalVM" : "")
					+ ")");
			lines.add("OS " + System.getProperty("os.name") + " "
					+ System.getProperty("os.version") + " "
					+ System.getProperty("os.arch"));
			return lines.toArray(String[]::new);
		}
	}

	/** Status codes reported as connection problems (exit code 3). */
	private static final Set<Long> CONNECTION_STATUS = Set.of(
			StatusCodes.Bad_Timeout, StatusCodes.Bad_RequestTimeout,
			StatusCodes.Bad_ConnectionClosed,
			StatusCodes.Bad_ServerNotConnected, StatusCodes.Bad_SessionClosed,
			StatusCodes.Bad_SessionIdInvalid,
			StatusCodes.Bad_CommunicationError, StatusCodes.Bad_NoCommunication,
			StatusCodes.Bad_SecureChannelClosed, StatusCodes.Bad_NotConnected,
			StatusCodes.Bad_TcpEndpointUrlInvalid, StatusCodes.Bad_ServerHalted,
			StatusCodes.Bad_SecureChannelIdInvalid);

	/** Exit codes shown in the help of every command. */
	private static final Map<String, String> EXIT_CODES = new LinkedHashMap<>();

	private static int verbosity = 0;

	static {
		EXIT_CODES.put("0",
				"successo (anche se il processo a valle ha chiuso la pipe)");
		EXIT_CODES.put("1",
				"errore del comando sul server (comando rifiutato o fallito)");
		EXIT_CODES.put("2",
				"uso errato (opzione sconosciuta, argomento mancante o non valido)");
		EXIT_CODES.put("3", "connessione fallita, persa o scaduta (timeout)");
		EXIT_CODES.put("130", "interrotto con SIGINT (Ctrl-C)");
	}

	/** Applies the logging configuration of slf4j-simple (before any logger). */
	static void configureLogging(final String[] args) {
		verbosity = verbosity(args);
		String level = switch (verbosity) {
			case 0 -> null;
			case 1 -> "info";
			case 2 -> "debug";
			default -> "trace";
		};
		if (level == null) {
			final String env = System.getenv("WALDOT_LOG_LEVEL");
			if (env == null || env.isBlank()) {
				level = "warn";
				// the failed connection attempts are reported by the command
				// itself
				setIfAbsent(
						"org.slf4j.simpleLogger.log.net.rossonet.waldot.client",
						"error");
			} else {
				level = env.trim().toLowerCase(Locale.ROOT);
				if ("debug".equals(level) || "trace".equals(level)) {
					verbosity = 2;
				}
			}
		}
		// Netty without sun.misc.Unsafe: no warnings of the JDK on stderr
		setIfAbsent("io.netty.noUnsafe", "true");
		setIfAbsent("org.slf4j.simpleLogger.defaultLogLevel", level);
		setIfAbsent("org.slf4j.simpleLogger.logFile", "System.err");
		setIfAbsent("org.slf4j.simpleLogger.showDateTime", "true");
		setIfAbsent("org.slf4j.simpleLogger.dateTimeFormat",
				"yyyy-MM-dd HH:mm:ss.SSS");
		if (verbosity < 3) {
			// the network stack is very verbose: only with -vvv
			setIfAbsent("org.slf4j.simpleLogger.log.io.netty", "warn");
		}
	}

	private static void configureUsage(final CommandLine commandLine) {
		final CommandSpec spec = commandLine.getCommandSpec();
		spec.usageMessage().sortOptions(false)
				.exitCodeListHeading("%nCodici di uscita:%n")
				.exitCodeList(EXIT_CODES).customSynopsis(synopsis(spec));
		for (final CommandLine subcommand : commandLine.getSubcommands()
				.values()) {
			configureUsage(subcommand);
		}
	}

	/** Exit code of an exception of a command. */
	static int exitCode(final Throwable exception) {
		if (exception instanceof final CliException cliException) {
			return cliException.exitCode();
		}
		if (exception instanceof final WaldotClientException clientException) {
			return isConnectionProblem(clientException.getStatusCode())
					? ExitCodes.CONNECTION
					: ExitCodes.SERVER_ERROR;
		}
		if (exception instanceof InterruptedException) {
			return ExitCodes.INTERRUPTED;
		}
		return ExitCodes.SERVER_ERROR;
	}

	/** @return true when running as a GraalVM native image */
	static boolean isNativeImage() {
		return "runtime".equals(
				System.getProperty("org.graalvm.nativeimage.imagecode"));
	}

	private static boolean isConnectionProblem(final StatusCode status) {
		return status != null && CONNECTION_STATUS.contains(status.getValue());
	}

	public static void main(final String... args) {
		configureLogging(args);
		// stdout only for the data: any other print of the libraries goes to
		// stderr a raw stream (not a PrintStream, which swallows the
		// IOException): when the downstream process closes the pipe the
		// PrintWriter sees the error (checkError) and the command stops cleanly
		final FileOutputStream stdout = new FileOutputStream(
				FileDescriptor.out);
		System.setOut(System.err);
		final PrintWriter out = new PrintWriter(
				new OutputStreamWriter(stdout, StandardCharsets.UTF_8), false);
		final PrintWriter err = new PrintWriter(
				new OutputStreamWriter(System.err, StandardCharsets.UTF_8),
				true);
		final CommandLine commandLine;
		try {
			commandLine = newCommandLine(out, err);
		} catch (final IllegalStateException e) {
			// a plugin that cannot be loaded or uses a name already taken
			err.println("wotctl: errore: " + message(e));
			err.flush();
			System.exit(ExitCodes.SERVER_ERROR);
			return;
		}
		final int exitCode = commandLine.execute(args);
		out.flush();
		err.flush();
		System.exit(exitCode);
	}

	private static String message(final Throwable exception) {
		final String message = exception.getMessage();
		return message == null || message.isBlank()
				? exception.getClass().getSimpleName()
				: message;
	}

	/**
	 * Creates the command line with the commands of the plugins on the
	 * classpath, the Italian texts, the exception handlers and the given output
	 * streams (used also by the tests).
	 *
	 * @throws IllegalStateException if a plugin cannot be loaded or uses a name
	 *                               already taken
	 */
	public static CommandLine newCommandLine(final PrintWriter out,
			final PrintWriter err) {
		return newCommandLine(out, err, PluginLoader.plugins());
	}

	/**
	 * Creates the command line with the commands of the given plugins.
	 *
	 * @throws IllegalStateException if a plugin uses a name already taken
	 */
	public static CommandLine newCommandLine(final PrintWriter out,
			final PrintWriter err, final List<WotctlPlugin> plugins) {
		final CommandLine commandLine = new CommandLine(new WaldotCli());
		// before the settings below, that apply only to the subcommands already
		// present
		PluginLoader.register(commandLine, plugins);
		// created here, not by picocli: it lists the plugins
		commandLine.getCommandSpec().versionProvider(new Version(plugins));
		commandLine.setResourceBundle(ResourceBundle
				.getBundle("net.rossonet.waldot.cli.messages", Locale.ROOT));
		commandLine.setCaseInsensitiveEnumValuesAllowed(true);
		// width of the terminal when detected, otherwise (pipes, files) 100
		// columns: the descriptions and the examples are written for at least
		// 90 columns
		commandLine.setUsageHelpWidth(100);
		commandLine.setUsageHelpAutoWidth(true);
		commandLine.setOut(out);
		commandLine.setErr(err);
		configureUsage(commandLine);
		commandLine.setParameterExceptionHandler((exception, args) -> {
			final CommandLine failed = exception.getCommandLine();
			final PrintWriter writer = failed.getErr();
			writer.println("wotctl: uso errato: " + exception.getMessage());
			UnmatchedArgumentException.printSuggestions(exception, writer);
			writer.println("Guida: " + failed.getCommandSpec().qualifiedName()
					+ " --help");
			writer.flush();
			return ExitCodes.USAGE;
		});
		commandLine.setExecutionExceptionHandler(
				(exception, failed, parseResult) -> {
					final int code = exitCode(exception);
					if (!(exception instanceof net.rossonet.waldot.cli.support.OutputClosedException)) {
						final PrintWriter writer = failed.getErr();
						writer.println(
								"wotctl: "
										+ (code == ExitCodes.CONNECTION
												? "errore di connessione: "
												: "errore: ")
										+ message(exception));
						if (verbosity >= 2) {
							exception.printStackTrace(writer);
						}
						writer.flush();
					}
					return code;
				});
		return commandLine;
	}

	private static void setIfAbsent(final String key, final String value) {
		if (System.getProperty(key) == null) {
			System.setProperty(key, value);
		}
	}

	/** Short synopsis: {@code wotctl comando [OPZIONI] PARAMETRI}. */
	private static String synopsis(final CommandSpec spec) {
		final StringBuilder synopsis = new StringBuilder(spec.qualifiedName())
				.append(" [OPZIONI]");
		if (!spec.subcommands().isEmpty()) {
			synopsis.append(" COMANDO");
		}
		for (final PositionalParamSpec parameter : spec
				.positionalParameters()) {
			if (parameter.hidden()) {
				continue;
			}
			final String label = parameter.paramLabel()
					+ (parameter.arity().max() > 1 ? "..." : "");
			synopsis.append(' ').append(
					parameter.arity().min() == 0 ? "[" + label + "]" : label);
		}
		return synopsis.toString();
	}

	/** Counts {@code -v}, {@code -vv}, {@code --verbose} before {@code --}. */
	static int verbosity(final String[] args) {
		int count = 0;
		for (final String arg : args) {
			if ("--".equals(arg)) {
				break;
			}
			if ("--verbose".equals(arg)) {
				count++;
			} else if (arg.length() > 1 && arg.matches("-v+")) {
				count += arg.length() - 1;
			}
		}
		return count;
	}

	@Spec
	private CommandSpec spec;

	@Option(names = {"-v", "--verbose"}, description = {
			"Aumenta i messaggi di log su stderr (ripetibile, vale anche dopo il comando)."})
	private final boolean[] verbose = new boolean[0];

	@Override
	public Integer call() {
		throw new ParameterException(spec.commandLine(),
				"specificare un comando (comandi: "
						+ String.join(", ", spec.subcommands().keySet()) + ")");
	}
}
