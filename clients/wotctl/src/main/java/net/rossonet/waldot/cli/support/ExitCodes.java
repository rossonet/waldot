package net.rossonet.waldot.cli.support;

/**
 * Exit codes of the {@code waldot} command line interface.
 */
public final class ExitCodes {

	/** Success (also when the downstream pipe was closed). */
	public static final int OK = 0;

	/** The server refused or failed the operation (command error). */
	public static final int SERVER_ERROR = 1;

	/** Wrong usage: unknown option, missing or invalid argument. */
	public static final int USAGE = 2;

	/** Connection failed, lost or timed out. */
	public static final int CONNECTION = 3;

	/** Interrupted by SIGINT (Ctrl-C), 128 + 2. */
	public static final int INTERRUPTED = 130;

	private ExitCodes() {
	}
}
