package net.rossonet.waldot.api.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdManagerTest {

	private IdManager<String> mockIdManager;
	private WaldotGraph mockGraph;

	@BeforeEach
	void setUp() {
		mockIdManager = mock(IdManager.class);
		mockGraph = mock(WaldotGraph.class);
	}

	@Test
	void testAllow() {
		final Object id = "testId";
		when(mockIdManager.allow(mockGraph, id)).thenReturn(true);

		final boolean result = mockIdManager.allow(mockGraph, id);

		assertTrue(result);
		verify(mockIdManager, times(1)).allow(mockGraph, id);
	}

	@Test
	void testConvert() {
		final Object id = "testId";
		final String expectedId = "convertedId";
		when(mockIdManager.convert(mockGraph, id)).thenReturn(expectedId);

		final String result = mockIdManager.convert(mockGraph, id);

		assertEquals(expectedId, result);
		verify(mockIdManager, times(1)).convert(mockGraph, id);
	}

	@Test
	void testGetNextId() {
		final String nextId = "nextId";
		when(mockIdManager.getNextId(mockGraph)).thenReturn(nextId);

		final String result = mockIdManager.getNextId(mockGraph);

		assertEquals(nextId, result);
		verify(mockIdManager, times(1)).getNextId(mockGraph);
	}
}