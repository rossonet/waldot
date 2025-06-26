package net.rossonet.waldot.api.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotNamespace;

class BootstrapStrategyTest {

	private BootstrapStrategy mockBootstrapStrategy;
	private WaldotNamespace mockNamespace;

	@BeforeEach
	void setUp() {
		mockBootstrapStrategy = mock(BootstrapStrategy.class);
		mockNamespace = mock(WaldotNamespace.class);
	}

	@Test
	void testGetAgentStatus() {
		when(mockBootstrapStrategy.getAgentStatus()).thenReturn(BootstrapStrategy.AgentStatus.READY);

		final BootstrapStrategy.AgentStatus status = mockBootstrapStrategy.getAgentStatus();

		assertEquals(BootstrapStrategy.AgentStatus.READY, status);
		verify(mockBootstrapStrategy, times(1)).getAgentStatus();
	}

	@Test
	void testInitialize() {
		doNothing().when(mockBootstrapStrategy).initialize(mockNamespace);

		mockBootstrapStrategy.initialize(mockNamespace);

		verify(mockBootstrapStrategy, times(1)).initialize(mockNamespace);
	}

	@Test
	void testRunBootstrapProcedure() {
		doNothing().when(mockBootstrapStrategy).runBootstrapProcedure();

		mockBootstrapStrategy.runBootstrapProcedure();

		verify(mockBootstrapStrategy, times(1)).runBootstrapProcedure();
	}
}