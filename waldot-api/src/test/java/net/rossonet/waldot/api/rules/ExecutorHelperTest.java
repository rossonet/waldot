package net.rossonet.waldot.api.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.jexl3.JexlContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.jexl.Rule;
import net.rossonet.waldot.jexl.RuleExecutor;
import net.rossonet.waldot.jexl.WaldotStepLogger;

class ExecutorHelperTest {

	private RuleExecutor mockExecutorHelper;
	private JexlContext mockJexlContext;
	private WaldotNamespace mockNamespace;
	private Rule mockRule;
	private WaldotStepLogger mockStepLogger;

	@BeforeEach
	void setUp() {
		mockExecutorHelper = mock(RuleExecutor.class);
		mockNamespace = mock(WaldotNamespace.class);
		mockRule = mock(Rule.class);
		mockStepLogger = mock(WaldotStepLogger.class);
		mockJexlContext = mock(JexlContext.class);
	}

	@Test
	void testEvaluateRule() {
		when(mockExecutorHelper.evaluateRule(mockNamespace, mockRule, mockStepLogger)).thenReturn(true);

		final boolean result = mockExecutorHelper.evaluateRule(mockNamespace, mockRule, mockStepLogger);

		assertTrue(result);
		verify(mockExecutorHelper, times(1)).evaluateRule(mockNamespace, mockRule, mockStepLogger);
	}

	@Test
	void testExecuteExpression() {
		final String expression = "2 + 2";
		when(mockExecutorHelper.execute(expression)).thenReturn(4);

		final Object result = mockExecutorHelper.execute(expression);

		assertEquals(4, result);
		verify(mockExecutorHelper, times(1)).execute(expression);
	}

	@Test
	void testExecuteExpressionWithContext() {
		final String expression = "x + y";
		when(mockExecutorHelper.execute(expression, mockJexlContext)).thenReturn(5);

		final Object result = mockExecutorHelper.execute(expression, mockJexlContext);

		assertEquals(5, result);
		verify(mockExecutorHelper, times(1)).execute(expression, mockJexlContext);
	}

	@Test
	void testExecuteRule() {
		when(mockExecutorHelper.executeRule(mockNamespace, mockRule, mockStepLogger)).thenReturn("Success");

		final Object result = mockExecutorHelper.executeRule(mockNamespace, mockRule, mockStepLogger);

		assertEquals("Success", result);
		verify(mockExecutorHelper, times(1)).executeRule(mockNamespace, mockRule, mockStepLogger);
	}

	@Test
	void testSetContext() {
		final String id = "contextId";
		final Object context = new Object();
		doNothing().when(mockExecutorHelper).setContext(id, context);

		mockExecutorHelper.setContext(id, context);

		verify(mockExecutorHelper, times(1)).setContext(id, context);
	}

	@Test
	void testSetFunctionObject() {
		final String id = "functionId";
		final Object function = new Object();
		doNothing().when(mockExecutorHelper).setFunctionObject(id, function);

		mockExecutorHelper.setFunctionObject(id, function);

		verify(mockExecutorHelper, times(1)).setFunctionObject(id, function);
	}
}