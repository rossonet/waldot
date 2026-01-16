package net.rossonet.agent;

import java.io.IOException;
import java.nio.file.Path;

import org.jline.console.CommandRegistry;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import net.rossonet.cli.jline.JLineHistory;
import net.rossonet.cli.jline.history.HistoryStorage;
import net.rossonet.waldot.utils.SystemCommandHelper.QuotedStringTokenizer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliJLineCompleter;

@Command(name = "", description = "@|bold,underline Demo Client Interactive Console :|@%n", footer = {
		"by Rossonet s.c.a r.l. - https://rossonet.net%n" }, subcommands = {
				net.rossonet.agent.PicocliShellJline3Test.VersionCommand.class }, customSynopsis = {
						"" }, synopsisHeading = "")
public class PicocliShellJline3Test implements Runnable {

	@Command(name = "add", description = "Stampa un saluto")
	static class AddCommand implements Runnable {
		@Option(names = { "-x", "--x_val" }, description = "primo addendo")
		int x = 0;

		@Option(names = { "-y", "--y_val" }, description = "secondo addendo")
		int y = 0;

		@Override
		public void run() {
			System.out.println(x + y);
		}
	}

	@Command(name = "hello", description = "Stampa un saluto")
	static class HelloCommand implements Runnable {
		@Option(names = { "-n", "--name" }, description = "Nome da salutare")
		String name = "mondo";

		@Override
		public void run() {
			System.out.println("Ciao, " + name + "!");
		}
	}

	@Command(name = "version", description = "versione del client")
	static class VersionCommand implements Runnable {

		@Override
		public void run() {
			System.out.println(11.0);
		}
	}

	@Override
	public void run() {
		System.out.println("Root command executed");

	}

	@Test
	public void testShell() throws IOException {
		final Path workDir = Path.of(System.getProperty("user.dir"), ".waldot");
		final Path historyPath = Path.of(workDir.toString(), "history.enc");
		if (!workDir.toFile().exists()) {
			workDir.toFile().mkdirs();
		}
		final CommandLine hello = new CommandLine(new HelloCommand());
		final CommandLine add = new CommandLine(new AddCommand());
		final CommandLine rootCmd = new CommandLine(new PicocliShellJline3Test());
		rootCmd.addSubcommand("hello", hello);
		rootCmd.addSubcommand("add", add);
		final PicocliCommands commands = new PicocliCommands(rootCmd);
		final PicocliJLineCompleter completer = new PicocliJLineCompleter(rootCmd.getCommandSpec());
		final HistoryStorage storage = new HistoryStorage(historyPath);
		final JLineHistory historyStorage = new JLineHistory(storage, "test_user", "tty_X");
		final Terminal terminal = TerminalBuilder.builder().system(true).build();
		final LineReader reader = LineReaderBuilder.builder().terminal(terminal).completer(completer).appName("WaldOT")
				.history(historyStorage).build();
		final CommandRegistry.CommandSession session = new CommandRegistry.CommandSession(terminal);
		String line;
		while ((line = reader.readLine("shell> ")) != null) {
			if ("exit".equalsIgnoreCase(line.trim())) {
				break;
			}
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
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}
}
