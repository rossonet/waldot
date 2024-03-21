package net.rossonet.cmd;

import java.io.PrintWriter;

import net.rossonet.cmd.commands.SystemInfoCommand;
import picocli.CommandLine;

public class BasePicocliRunner {

	public static BasePicocliRunner getNewInstance() {
		return new BasePicocliRunner();
	}

	private final LoggerWrapper loggerWrapper;

	private final CommandLine picocliCommandLine;

	private int returnCode = 11;

	private BasePicocliRunner() {
		loggerWrapper = new LoggerWrapper();
		loggerWrapper.info("runner started");
		picocliCommandLine = new CommandLine(new SystemInfoCommand());
	}

	public BasePicocliRunner execute(final String[] args) {
		returnCode = picocliCommandLine.execute(args);
		return this;
	}

	public BasePicocliRunner setErrorWriter(PrintWriter errorWriter) {
		picocliCommandLine.setErr(errorWriter);
		return this;
	}

	public BasePicocliRunner setOutputWriter(PrintWriter outputWriter) {
		picocliCommandLine.setOut(outputWriter);
		return this;
	}

	public int waitCompletionAndGetReturnCode() {
		return returnCode;
	}

}
