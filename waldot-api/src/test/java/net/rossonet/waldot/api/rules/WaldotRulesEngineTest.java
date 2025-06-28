package net.rossonet.waldot.api.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.models.WaldotVertex;

class WaldotRulesEngineTest {

	private WaldotRulesEngine rulesEngine;
	private RuleListener mockListener;
	private Rule mockRule;

	@BeforeEach
	void setUp() {
		rulesEngine = mock(WaldotRulesEngine.class);
		mockListener = mock(RuleListener.class);
		mockRule = mock(Rule.class);
	}

	@Test
	void testAddAndRemoveListener() {
		doNothing().when(rulesEngine).addListener(mockListener);
		doNothing().when(rulesEngine).removeListener(mockListener);

		rulesEngine.addListener(mockListener);
		verify(rulesEngine, times(1)).addListener(mockListener);

		rulesEngine.removeListener(mockListener);
		verify(rulesEngine, times(1)).removeListener(mockListener);
	}

	@Test
	void testGetListeners() {
		final Collection<RuleListener> listeners = Collections.singletonList(mockListener);
		when(rulesEngine.getListeners()).thenReturn(listeners);

		final Collection<RuleListener> result = rulesEngine.getListeners();
		assertEquals(listeners, result);
		verify(rulesEngine, times(1)).getListeners();
	}

	@Test
	void testRegisterAndDeregisterRule() {
		final NodeId mockNodeId = mock(NodeId.class);
		doNothing().when(rulesEngine).registerOrUpdateRule(mockRule);
		doNothing().when(rulesEngine).deregisterRule(mockNodeId);

		rulesEngine.registerOrUpdateRule(mockRule);
		verify(rulesEngine, times(1)).registerOrUpdateRule(mockRule);

		rulesEngine.deregisterRule(mockNodeId);
		verify(rulesEngine, times(1)).deregisterRule(mockNodeId);
	}

	@Test
	void testSimulateRuleExecution() {
		final NodeId mockNodeId = mock(NodeId.class);
		final WaldotVertex mockVertex = mock(WaldotVertex.class);

		doNothing().when(rulesEngine).registerObserver(mockVertex, mockNodeId);
		doNothing().when(rulesEngine).registerOrUpdateRule(mockRule);

		rulesEngine.registerObserver(mockVertex, mockNodeId);
		rulesEngine.registerOrUpdateRule(mockRule);

		verify(rulesEngine, times(1)).registerObserver(mockVertex, mockNodeId);
		verify(rulesEngine, times(1)).registerOrUpdateRule(mockRule);
	}
}