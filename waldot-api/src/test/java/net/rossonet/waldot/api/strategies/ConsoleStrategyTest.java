package net.rossonet.waldot.api.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.jexl3.JexlContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotNamespace;

class ConsoleStrategyTest {

	private ConsoleStrategy mockConsoleStrategy;
	private WaldotNamespace mockNamespace;
	private JexlContext mockJexlContext;

	@BeforeEach
	void setUp() {
		mockConsoleStrategy = mock(ConsoleStrategy.class);
		mockNamespace = mock(WaldotNamespace.class);
		mockJexlContext = mock(JexlContext.class);
	}

	@Test
	void testGetWaldotNamespace() {
		when(mockConsoleStrategy.getWaldotNamespace()).thenReturn(mockNamespace);

		final WaldotNamespace result = mockConsoleStrategy.getWaldotNamespace();

		assertEquals(mockNamespace, result);
		verify(mockConsoleStrategy, times(1)).getWaldotNamespace();
	}

	@Test
	void testInitialize() {
		doNothing().when(mockConsoleStrategy).initialize(mockNamespace);

		mockConsoleStrategy.initialize(mockNamespace);

		verify(mockConsoleStrategy, times(1)).initialize(mockNamespace);
	}

	@Test
	void testReset() {
		doNothing().when(mockConsoleStrategy).reset();

		mockConsoleStrategy.reset();

		verify(mockConsoleStrategy, times(1)).reset();
	}

	@Test
	void testRunExpressionWithContext() {
		final String expression = "testExpression";
		final Object expectedResult = "result";
		when(mockConsoleStrategy.runExpression(expression, mockJexlContext)).thenReturn(expectedResult);

		final Object result = mockConsoleStrategy.runExpression(expression, mockJexlContext);

		assertEquals(expectedResult, result);
		verify(mockConsoleStrategy, times(1)).runExpression(expression, mockJexlContext);
	}

	@Test
	void testRunExpressionWithoutContext() {
		final String expression = "testExpression";
		final Object expectedResult = "result";
		when(mockConsoleStrategy.runExpression(expression)).thenReturn(expectedResult);

		final Object result = mockConsoleStrategy.runExpression(expression);

		assertEquals(expectedResult, result);
		verify(mockConsoleStrategy, times(1)).runExpression(expression);
	}
}