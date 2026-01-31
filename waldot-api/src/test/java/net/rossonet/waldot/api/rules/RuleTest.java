package net.rossonet.waldot.api.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.jexl.CachedRuleRecord;
import net.rossonet.waldot.jexl.ClonableMapContext;
import net.rossonet.waldot.jexl.Rule;
import net.rossonet.waldot.jexl.WaldotStepLogger;

class RuleTest {

	private ClonableMapContext mockContext;
	private CachedRuleRecord mockFact;
	private RuleListener mockListener;
	private Rule mockRule;
	private Callable<WaldotStepLogger> mockRunner;

	@BeforeEach
	void setUp() {
		mockRule = mock(Rule.class);
		mockFact = mock(CachedRuleRecord.class);
		mockListener = mock(RuleListener.class);
		mockContext = mock(ClonableMapContext.class);
		mockRunner = mock(Callable.class);

		when(mockRule.getAction()).thenReturn("testAction");
		when(mockRule.getCondition()).thenReturn("testCondition");
		when(mockRule.getDefaultValidDelayMs()).thenReturn(1000L);
		when(mockRule.getDefaultValidUntilMs()).thenReturn(2000L);
		when(mockRule.getDelayBeforeEvaluation()).thenReturn(10);
		when(mockRule.getDelayBeforeExecute()).thenReturn(20);
		when(mockRule.getExecutionTimeout()).thenReturn(30);
		when(mockRule.getFacts()).thenReturn(Collections.singletonList(mockFact));
		when(mockRule.getListeners()).thenReturn(Collections.singletonList(mockListener));
		when(mockRule.getNewRunner()).thenReturn(mockRunner);
		when(mockRule.getPriority()).thenReturn(1);
		when(mockRule.getRefractoryPeriodMs()).thenReturn(500);
		when(mockRule.getThreadName()).thenReturn("testThread");
		when(mockRule.isClearFactsAfterExecution()).thenReturn(true);
		when(mockRule.isDirty()).thenReturn(false);
		when(mockRule.isParallelExecution()).thenReturn(true);
	}

	@Test
	void testGetAction() {
		assertEquals("testAction", mockRule.getAction());
		verify(mockRule, times(1)).getAction();
	}

	@Test
	void testGetCondition() {
		assertEquals("testCondition", mockRule.getCondition());
		verify(mockRule, times(1)).getCondition();
	}

	@Test
	void testGetDefaultValidDelayMs() {
		assertEquals(1000L, mockRule.getDefaultValidDelayMs());
		verify(mockRule, times(1)).getDefaultValidDelayMs();
	}

	@Test
	void testGetDefaultValidUntilMs() {
		assertEquals(2000L, mockRule.getDefaultValidUntilMs());
		verify(mockRule, times(1)).getDefaultValidUntilMs();
	}

	@Test
	void testGetDelayBeforeEvaluation() {
		assertEquals(10, mockRule.getDelayBeforeEvaluation());
		verify(mockRule, times(1)).getDelayBeforeEvaluation();
	}

	@Test
	void testGetDelayBeforeExecute() {
		assertEquals(20, mockRule.getDelayBeforeExecute());
		verify(mockRule, times(1)).getDelayBeforeExecute();
	}

	@Test
	void testGetExecutionTimeout() {
		assertEquals(30, mockRule.getExecutionTimeout());
		verify(mockRule, times(1)).getExecutionTimeout();
	}

	@Test
	void testGetFacts() {
		final Collection<CachedRuleRecord> facts = mockRule.getFacts();
		assertNotNull(facts);
		assertEquals(1, facts.size());
		assertTrue(facts.contains(mockFact));
		verify(mockRule, times(1)).getFacts();
	}

	@Test
	void testGetListeners() {
		final Collection<RuleListener> listeners = mockRule.getListeners();
		assertNotNull(listeners);
		assertEquals(1, listeners.size());
		assertTrue(listeners.contains(mockListener));
		verify(mockRule, times(1)).getListeners();
	}

	@Test
	void testGetNewRunner() {
		assertEquals(mockRunner, mockRule.getNewRunner());
		verify(mockRule, times(1)).getNewRunner();
	}

	@Test
	void testGetPriority() {
		assertEquals(1, mockRule.getPriority());
		verify(mockRule, times(1)).getPriority();
	}

	@Test
	void testGetRefractoryPeriodMs() {
		assertEquals(500, mockRule.getRefractoryPeriodMs());
		verify(mockRule, times(1)).getRefractoryPeriodMs();
	}

	@Test
	void testGetThreadName() {
		assertEquals("testThread", mockRule.getThreadName());
		verify(mockRule, times(1)).getThreadName();
	}

	@Test
	void testIsClearFactsAfterExecution() {
		assertTrue(mockRule.isClearFactsAfterExecution());
		verify(mockRule, times(1)).isClearFactsAfterExecution();
	}

	@Test
	void testIsDirty() {
		assertFalse(mockRule.isDirty());
		verify(mockRule, times(1)).isDirty();
	}

	@Test
	void testIsParallelExecution() {
		assertTrue(mockRule.isParallelExecution());
		verify(mockRule, times(1)).isParallelExecution();
	}
}