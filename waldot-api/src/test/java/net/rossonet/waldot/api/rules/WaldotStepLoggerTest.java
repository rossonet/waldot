package net.rossonet.waldot.api.rules;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.commons.jexl3.JexlContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.RuleListener;

class WaldotStepLoggerTest {

	private WaldotStepLogger mockLogger;
	private JexlContext mockJexlContext;
	private RuleListener mockListener;

	@BeforeEach
	void setUp() {
		mockLogger = mock(WaldotStepLogger.class);
		mockJexlContext = mock(JexlContext.class);
		mockListener = mock(RuleListener.class);
	}

	@Test
	void testOnActionCompiled() {
		doNothing().when(mockLogger).onActionCompiled(100L);

		mockLogger.onActionCompiled(100L);

		verify(mockLogger, times(1)).onActionCompiled(100L);
	}

	@Test
	void testOnActionExecutionException() {
		Exception exception = new Exception("Test Exception");
		doNothing().when(mockLogger).onActionExecutionException(mockJexlContext, exception);

		mockLogger.onActionExecutionException(mockJexlContext, exception);

		verify(mockLogger, times(1)).onActionExecutionException(mockJexlContext, exception);
	}

	@Test
	void testOnAfterActionExecution() {
		doNothing().when(mockLogger).onAfterActionExecution(200L, "result");

		mockLogger.onAfterActionExecution(200L, "result");

		verify(mockLogger, times(1)).onAfterActionExecution(200L, "result");
	}

	@Test
	void testOnAfterConditionExecution() {
		doNothing().when(mockLogger).onAfterConditionExecution(300L, true);

		mockLogger.onAfterConditionExecution(300L, true);

		verify(mockLogger, times(1)).onAfterConditionExecution(300L, true);
	}

	@Test
	void testOnBeforeActionExecution() {
		doNothing().when(mockLogger).onBeforeActionExecution(mockJexlContext);

		mockLogger.onBeforeActionExecution(mockJexlContext);

		verify(mockLogger, times(1)).onBeforeActionExecution(mockJexlContext);
	}

	@Test
	void testOnBeforeConditionExecution() {
		doNothing().when(mockLogger).onBeforeConditionExecution(mockJexlContext);

		mockLogger.onBeforeConditionExecution(mockJexlContext);

		verify(mockLogger, times(1)).onBeforeConditionExecution(mockJexlContext);
	}

	@Test
	void testOnConditionCompiled() {
		doNothing().when(mockLogger).onConditionCompiled(400L);

		mockLogger.onConditionCompiled(400L);

		verify(mockLogger, times(1)).onConditionCompiled(400L);
	}

	@Test
	void testOnConditionExecutionException() {
		Exception exception = new Exception("Condition Exception");
		doNothing().when(mockLogger).onConditionExecutionException(mockJexlContext, exception);

		mockLogger.onConditionExecutionException(mockJexlContext, exception);

		verify(mockLogger, times(1)).onConditionExecutionException(mockJexlContext, exception);
	}

	@Test
	void testOnEvaluateStoppedByListener() {
		doNothing().when(mockLogger).onEvaluateStoppedByListener(mockListener);

		mockLogger.onEvaluateStoppedByListener(mockListener);

		verify(mockLogger, times(1)).onEvaluateStoppedByListener(mockListener);
	}

	@Test
	void testOnThreadRegistered() {
		doNothing().when(mockLogger).onThreadRegistered("TestThread", 5);

		mockLogger.onThreadRegistered("TestThread", 5);

		verify(mockLogger, times(1)).onThreadRegistered("TestThread", 5);
	}
}