package net.rossonet.waldot.cli.support;

/**
 * Error of a command with the exit code of the process.
 */
public class CliException extends RuntimeException {

	private static final long serialVersionUID = -1735024961012449587L;

	/** Connection failed or lost (exit code {@value ExitCodes#CONNECTION}). */
	public static CliException connection(final String message,
			final Throwable cause) {
		return new CliException(ExitCodes.CONNECTION, message, cause);
	}

	/** The server refused the operation (exit code {@value ExitCodes#SERVER_ERROR}). */
	public static CliException server(final String message) {
		return new CliException(ExitCodes.SERVER_ERROR, message, null);
	}

	private final int exitCode;

	public CliException(final int exitCode, final String message,
			final Throwable cause) {
		super(message, cause);
		this.exitCode = exitCode;
	}

	public int exitCode() {
		return exitCode;
	}
}
