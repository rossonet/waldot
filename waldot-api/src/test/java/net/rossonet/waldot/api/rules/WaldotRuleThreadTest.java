package net.rossonet.waldot.api.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WaldotRuleThreadTest {

	private WaldotStepLogger mockStepLogger;
	private Rule mockRule;
	private Runnable mockRunnable;
	private WaldotRuleThread ruleThread;

	@BeforeEach
	void setUp() {
		mockStepLogger = mock(WaldotStepLogger.class);
		mockRule = mock(Rule.class);
		mockRunnable = mock(Runnable.class);
		ruleThread = new WaldotRuleThread(mockRunnable, mockStepLogger);
	}

	@Test
	void testConstructor() {
		assertNotNull(ruleThread);
		assertEquals(mockStepLogger, ruleThread.getStepRegister());
	}

	@Test
	void testGetRule() {
		assertNull(ruleThread.getRule());
		when(mockRule.getPriority()).thenReturn(Thread.NORM_PRIORITY);
		when(mockRule.getThreadName()).thenReturn("TestThread");
		when(mockRule.label()).thenReturn("TestRule");
		ruleThread.setRule(mockRule);
		assertEquals(mockRule, ruleThread.getRule());
	}

	@Test
	void testGetStepRegister() {
		assertEquals(mockStepLogger, ruleThread.getStepRegister());
	}

	@Test
	void testSetRule() {
		when(mockRule.getPriority()).thenReturn(Thread.NORM_PRIORITY);
		when(mockRule.getThreadName()).thenReturn("TestThread");
		when(mockRule.label()).thenReturn("TestRule");

		ruleThread.setRule(mockRule);

		assertEquals(mockRule, ruleThread.getRule());
		assertEquals("TestThread", ruleThread.getName());
		assertEquals(Thread.NORM_PRIORITY, ruleThread.getPriority());

		verify(mockStepLogger, times(1)).onThreadRegistered("TestThread", Thread.NORM_PRIORITY);
	}
}