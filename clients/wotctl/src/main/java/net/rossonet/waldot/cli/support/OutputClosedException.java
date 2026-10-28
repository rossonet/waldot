package net.rossonet.waldot.cli.support;

/**
 * The standard output was closed by the downstream process of the pipe (e.g.
 * {@code waldot subscribe ... | head -n 3}): the command stops cleanly with
 * exit code {@value ExitCodes#OK}.
 */
public final class OutputClosedException extends CliException {

	private static final long serialVersionUID = 2939447135565304093L;

	public OutputClosedException() {
		super(ExitCodes.OK, "standard output closed", null);
	}
}
