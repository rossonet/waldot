package net.rossonet.waldot.api.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WaldotVertexPropertyTest {

	private WaldotVertexProperty<Object> mockVertexProperty;
	private WaldotVertex mockVertex;

	@BeforeEach
	void setUp() {
		mockVertexProperty = mock(WaldotVertexProperty.class);
		mockVertex = mock(WaldotVertex.class);
	}

	@Test
	void testGetVertexPropertyReference() {
		when(mockVertexProperty.getVertexPropertyReference()).thenReturn(mockVertex);

		final WaldotVertex result = mockVertexProperty.getVertexPropertyReference();

		assertEquals(mockVertex, result);
		verify(mockVertexProperty, times(1)).getVertexPropertyReference();
	}

}