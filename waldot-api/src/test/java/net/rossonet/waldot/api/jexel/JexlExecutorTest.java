package net.rossonet.waldot.api.jexel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.jexl.JexlExecutor;

class JexlExecutorTest {

	@Test
	void testExecuteSimpleExpression() {
		final JexlExecutor executor = new JexlExecutor("test");
		assertEquals(3, executor.execute("1 + 2"));
	}

	@Test
	void testFunctionObjectExecution() {
		final JexlExecutor executor = new JexlExecutor("test");
		executor.setFunctionObject("math", new MathFacade());
		assertEquals(9, executor.execute("math.square(3)"));
	}

	@Test
	void testClosedExecutorFailsFast() throws Exception {
		final JexlExecutor executor = new JexlExecutor("test");
		executor.close();
		assertThrows(IllegalStateException.class, () -> executor.execute("1 + 2"));
	}

	public static class MathFacade {
		public int square(int value) {
			return value * value;
		}
	}
}
