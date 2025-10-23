package plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.cli.CliPluginListener;
import net.rossonet.cli.MainCli;
import net.rossonet.waldot.api.annotation.WaldotCliPlugin;
import net.rossonet.waldot.utils.TextHelper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@WaldotCliPlugin
public class BaseCommands implements CliPluginListener {
	@Command(name = "about", description = "about this software")
	static class AboutCommand implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			System.out.println("powered by Rossonet s.c.a r.l.");
			return null;
		}

	}

	@Command(name = "exit", description = "exit the cli", aliases = { "quit", "bye" })
	static class ExitCommand implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			MainCli.running = false;
			System.out.println("exiting...");
			System.exit(0);
			return null;
		}

	}

	@Command(name = "info", description = "software info")
	static class InfoCommand implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			System.out.println("info about the software");
			return null;
		}

	}

	@Command(name = "log", description = "write to or read the log file")
	static class LogCommand implements Callable<Void> {
		private final Logger logger = LoggerFactory.getLogger("cmd log");
		final Path logPath = Paths.get(MainCli.WORKING_DIR.toAbsolutePath().toAbsolutePath().toString(), "client.log");

		@Option(names = { "--log-message" }, description = "content of the log line", arity = "1")
		String message;

		@Option(names = { "--log-print" }, description = "print log file content", arity = "0")
		boolean print;

		@Override
		public Void call() throws Exception {
			if (print) {
				if (Files.exists(logPath)) {
					final String noteContent = Files.readString(logPath);
					System.out.println("log content:\n" + noteContent);
				} else {
					System.out.println("no log file found");
				}
				return null;
			}
			if (message != null && !message.isEmpty()) {
				logger.info(message);
				System.out.println("log message: " + message);
			} else {
				System.out.println("log message is empty, nothing to do");
			}
			return null;
		}

	}

	@Command(name = "note", description = "write the note")
	static class NoteCommand implements Callable<Void> {

		@Option(names = { "--note-message" }, description = "content of the note", arity = "1")
		String message;

		@Option(names = { "--note-print" }, description = "print note content", arity = "0")
		boolean print;

		@Override
		public Void call() throws Exception {
			if (print) {
				if (Files.exists(NOTE_FILE)) {
					final String noteContent = Files.readString(NOTE_FILE);
					System.out.println("note content:\n" + noteContent);
				} else {
					System.out.println("no note found");
				}
				return null;
			}
			if (message != null && !message.isEmpty()) {
				Files.writeString(NOTE_FILE, message + System.lineSeparator(), java.nio.file.StandardOpenOption.CREATE,
						java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
				System.out.println("note saved");
			} else {
				System.out.println("note message is empty, nothing to save");
			}
			return null;
		}

	}

	protected final static Path NOTE_FILE = Path.of(MainCli.WORKING_DIR.toString(), "note.txt");

	public BaseCommands() {
		if (Files.exists(NOTE_FILE)) {
			try {
				final String noteContent = Files.readString(NOTE_FILE);
				System.out.println(TextHelper.ANSI_RED + "\n--- Note ---\n" + noteContent + "------------\n"
						+ TextHelper.ANSI_RESET);
			} catch (final IOException e) {
				System.err.println("error reading note file: " + e.getMessage());
			}
		}
	}

	@Override
	public Map<String, CommandLine> getCommands() {
		final Map<String, CommandLine> result = new HashMap<>();
		result.put("about", new CommandLine(new AboutCommand()));
		result.put("info", new CommandLine(new InfoCommand()));
		result.put("exit", new CommandLine(new ExitCommand()));
		result.put("note", new CommandLine(new NoteCommand()));
		result.put("log", new CommandLine(new LogCommand()));
		return result;
	}

}
