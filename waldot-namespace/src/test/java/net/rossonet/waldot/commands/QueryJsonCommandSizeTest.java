package net.rossonet.waldot.commands;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A {@code query json} result larger than a response gives an explicit error. */
class QueryJsonCommandSizeTest {

	@Test
	void resultWithinTheLimitIsReturned() {
		assertNull(QueryJsonCommand.tooLarge(null, 100));
		assertNull(QueryJsonCommand.tooLarge("x".repeat(100), 100));
		// multi-byte characters count as UTF-8 bytes
		assertNull(QueryJsonCommand.tooLarge("è".repeat(50), 100));
	}

	@Test
	void resultBeyondTheLimitIsAnError() {
		final String error = QueryJsonCommand.tooLarge("è".repeat(51), 100);
		assertNotNull(error);
		assertTrue(error.contains("102 bytes") && error.contains("range()"), error);
		assertNotNull(QueryJsonCommand.tooLarge("x".repeat(QueryJsonCommand.MAX_RESPONSE_BYTES + 1),
				QueryJsonCommand.MAX_RESPONSE_BYTES));
	}

}
