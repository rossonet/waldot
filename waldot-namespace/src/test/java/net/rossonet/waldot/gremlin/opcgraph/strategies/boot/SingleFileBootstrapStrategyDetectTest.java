package net.rossonet.waldot.gremlin.opcgraph.strategies.boot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SingleFileBootstrapStrategy#detectScriptMode(String)}.
 *
 * Regression: a file containing only {@code var x = ...} statements (as
 * documented in BOOTSTRAP_CONFIGURATION.md) was not detected as script mode.
 */
class SingleFileBootstrapStrategyDetectTest {

	@Test
	void assignmentWithoutVarIsScriptMode() {
		assertTrue(SingleFileBootstrapStrategy.detectScriptMode("x = 1;\n"));
	}

	@Test
	void blankOrNullIsLineMode() {
		assertFalse(SingleFileBootstrapStrategy.detectScriptMode(null));
		assertFalse(SingleFileBootstrapStrategy.detectScriptMode(""));
		assertFalse(SingleFileBootstrapStrategy.detectScriptMode("   \n\n"));
	}

	@Test
	void equalityComparisonIsNotAnAssignment() {
		assertFalse(SingleFileBootstrapStrategy.detectScriptMode("x == 1\n"));
	}

	@Test
	void hashCommentOnLastLineWithoutNewlineIsIgnored() {
		assertFalse(SingleFileBootstrapStrategy.detectScriptMode("graph.addVertex('id', 'a')\n# x = 1"));
	}

	@Test
	void hashInsideStringDoesNotTruncateLine() {
		// before the fix, everything after '#' was removed, hiding the assignment
		assertTrue(SingleFileBootstrapStrategy.detectScriptMode("label = 'a#b';\n"));
	}

	@Test
	void quotedUrlDoesNotForceScriptMode() {
		assertFalse(SingleFileBootstrapStrategy.detectScriptMode(
				"graph.addVertex('id', 'a', 'source', 'https://example.com/boot.conf')\n"));
		assertFalse(SingleFileBootstrapStrategy
				.detectScriptMode("graph.addVertex('note', 'see // docs and def foo(')\n"));
	}

	@Test
	void slashCommentOutsideStringIsScriptMode() {
		assertTrue(SingleFileBootstrapStrategy.detectScriptMode("// comment\ngraph.addVertex('id', 'a');\n"));
	}

	@Test
	void simpleCommandsAreLineMode() {
		final String content = "# legacy comment\n" + "graph.addVertex('id', 'sensor1', 'label', 'sensor1')\n"
				+ "graph.addVertex('id', 'sensor2', 'label', 'sensor2')\n";
		assertFalse(SingleFileBootstrapStrategy.detectScriptMode(content));
	}

	@Test
	void varDeclarationIsScriptMode() {
		assertTrue(SingleFileBootstrapStrategy.detectScriptMode("var x = 1;\n"));
		assertTrue(SingleFileBootstrapStrategy
				.detectScriptMode("var v = graph.addVertex('id', 'temp-1', 'label', 'temp-1');\n"));
	}
}