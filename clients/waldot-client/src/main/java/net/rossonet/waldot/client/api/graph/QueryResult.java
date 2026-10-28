package net.rossonet.waldot.client.api.graph;

/**
 * Result of the text {@code query} command.
 *
 * @param output result as text ({@code toString()} on the server)
 * @param error  error message, empty on success
 */
public record QueryResult(String output, String error) {

	/** @return true if the error is empty */
	public boolean isSuccess() {
		return error == null || error.isBlank();
	}
}
