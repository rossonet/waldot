package net.rossonet.waldot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class SystemCommandHelperTest {

	@Test
	void testQuotedStringTokenizer() {
		final SystemCommandHelper.QuotedStringTokenizer tokenizer = new SystemCommandHelper.QuotedStringTokenizer(
				"echo \"hello world\" sample");
		assertEquals(List.of("echo", "hello world", "sample"), tokenizer.getTokens());
		assertTrue(tokenizer.hasMoreTokens());
		assertEquals("echo", tokenizer.nextToken());
		assertTrue(tokenizer.hasMoreTokens());
	}

	@Test
	void testExecuteSystemCommandAndWait() throws Exception {
		final List<String> output = new ArrayList<>();
		SystemCommandHelper.executeSystemCommandAndWait(new File("."), new String[] { "bash", "-lc", "echo ok" },
				output::add, 3_000);
		assertTrue(output.stream().anyMatch(line -> line.contains("ok")));
	}

	@Test
	void testExecuteSystemCommandAndWaitTimeout() {
		assertThrows(TimeoutException.class,
				() -> SystemCommandHelper.executeSystemCommandAndWait(new File("."),
						new String[] { "bash", "-lc", "sleep 2" }, line -> {
						}, 100));
	}

	@Test
	void testTokenizerWithoutMoreTokens() {
		final SystemCommandHelper.QuotedStringTokenizer tokenizer = new SystemCommandHelper.QuotedStringTokenizer("echo");
		tokenizer.nextToken();
		assertFalse(tokenizer.hasMoreTokens());
	}
}
