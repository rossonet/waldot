package net.rossonet.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.jline.console.CommandRegistry;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.shaded.com.google.common.reflect.ClassPath;

import net.rossonet.cli.jline.JLineHistory;
import net.rossonet.cli.jline.history.HistoryStorage;
import net.rossonet.waldot.api.annotation.WaldotCliPlugin;
import net.rossonet.waldot.utils.SystemCommandHelper.QuotedStringTokenizer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliJLineCompleter;

/**
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a r.l.
 */
@Command(name = "walctl", mixinStandardHelpOptions = true, showDefaultValues = true, version = { "${COMMAND-NAME} 1.0",
		"JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
		"OS: ${os.name} ${os.version} ${os.arch}" }, description = "WaldOT Command Line CLI", footer = "\n@ only for internal use", showEndOfOptionsDelimiterInUsageHelp = true, showAtFileInUsageHelp = true)
public class WaldotCommandRunner implements Callable<Void>, AutoCloseable {

	private static final String[] PLUGINS_BASE_SEARCH_PACKAGE = new String[] { "net.rossonet.waldot", "plugin",
			"plugins" };

	private final CommandRegister commandRegister = new CommandRegister();

	@Option(names = { "-i", "--interactive" }, description = "run in interactive mode", arity = "0")
	protected boolean interactive = false;

	private final Logger logger = LoggerFactory.getLogger("walctl");

	private CommandLine rootCmd;

	private final SignalHandler signalHandler = new SignalHandler() {

		@Override
		public void handle(Signal signal) {
			logger.info("received signal: " + signal.name());

		}
	};

	@Spec
	CommandSpec spec;

	private Terminal terminal;

	public WaldotCommandRunner() {
		try {
			loadPlugins();
		} catch (final IOException e) {
			logger.error("error loading plugins", e);
		}
	}

	@Override
	public Void call() throws Exception {
		runWaldotCtl();
		return null;
	}

	@Override
	public void close() throws Exception {
		if (terminal != null) {
			terminal.close();
		}
		System.out.println("\nbye, bye");
	}

	public CommandRegister getCommandRegister() {
		return commandRegister;
	}

	private String getprompt() {
		return "walctl> ";
	}

	public boolean isInteractive() {
		return interactive;
	}

	public void loadPlugins() throws IOException {
		final ClassPath cp = ClassPath.from(Thread.currentThread().getContextClassLoader());
		for (final String basePackage : PLUGINS_BASE_SEARCH_PACKAGE) {
			logger.info("Searching plugins in base package: {}", basePackage);
			for (final ClassPath.ClassInfo classInfo : cp.getTopLevelClassesRecursive(basePackage)) {
				final Class<?> clazz = classInfo.load();
				if (clazz.isAnnotationPresent(WaldotCliPlugin.class)) {
					logger.info("Found plugin: {}", clazz.getName());
					try {
						final CliPluginListener candidate = (CliPluginListener) clazz.getConstructor().newInstance();
						for (final Entry<String, CommandLine> c : candidate.getCommands().entrySet()) {
							commandRegister.registerCommand(c.getKey(), c.getValue());
						}
					} catch (final Exception e) {
						logger.error("Error creating plugin", e);
					}

				}
			}
		}
	}

	public void runWaldotCtl() throws InterruptedException, ExecutionException, IOException {
		Thread.currentThread().setName("WaldOT client");
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

		final Path historyPath = Path.of(MainCli.WORKING_DIR.toString(), "history.enc");

		final PicocliCommands commands = new PicocliCommands(rootCmd);
		final PicocliJLineCompleter completer = new PicocliJLineCompleter(rootCmd.getCommandSpec());
		final HistoryStorage storage = new HistoryStorage(historyPath);
		final JLineHistory historyStorage = new JLineHistory(storage, "test_user", "tty_X");
		terminal = TerminalBuilder.builder().system(true).color(true).signalHandler(signalHandler).build();
		final LineReader reader = LineReaderBuilder.builder().terminal(terminal).completer(completer).appName("WaldOT")
				.history(historyStorage).build();
		final CommandRegistry.CommandSession session = new CommandRegistry.CommandSession(terminal);
		String line;
		if (interactive) {
			terminal.writer().flush();
			while ((line = reader.readLine(getprompt())) != null) {
				try {
					final QuotedStringTokenizer tokenizer = new QuotedStringTokenizer(line);
					final String command = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
					Object[] args = tokenizer.getTokens().toArray(new String[0]);
					if (args.length > 0) {
						final String[] newArgs = new String[args.length - 1];
						System.arraycopy(args, 1, newArgs, 0, args.length - 1);
						args = newArgs;
					}
					commands.invoke(session, command, args);
					line = "";
					terminal.writer().flush();
				} catch (final Exception e) {
					logger.error("interacting with the console", e.getMessage());
				}
			}
		} else {
			MainCli.running = false;
		}
	}

	public void setCommandLine(CommandLine commandLine) {
		rootCmd = commandLine;
		for (final Entry<String, CommandLine> c : commandRegister.getCommands().entrySet()) {
			rootCmd.addSubcommand(c.getKey(), c.getValue());
		}

	}

}