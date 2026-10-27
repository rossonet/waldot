package net.rossonet.waldot.commands;

/**
 * Small helpers shared by the OPC UA console commands to validate input
 * arguments and to produce non-null error messages.
 */
final class CommandArguments {

	/**
	 * Returns a non-null, human readable message for the given throwable: the
	 * exception message when present, otherwise {@link Throwable#toString()}.
	 */
	static String errorMessage(final Throwable e) {
		if (e == null) {
			return "unknown error";
		}
		final String message = e.getMessage();
		return message != null && !message.isBlank() ? message : e.toString();
	}

	/**
	 * Returns the first input argument.
	 *
	 * @throws IllegalArgumentException if the array is null/empty or the first
	 *                                  value is null or blank
	 */
	static String requireFirst(final String[] inputValues, final String argumentName) {
		if (inputValues == null || inputValues.length == 0 || inputValues[0] == null
				|| inputValues[0].isBlank()) {
			throw new IllegalArgumentException("missing required argument '" + argumentName + "'");
		}
		return inputValues[0];
	}

	private CommandArguments() {
		// utility class
	}
}
