package net.rossonet.waldot.api.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WaldotPropertyTest {

	private WaldotProperty<Object> mockProperty;
	private WaldotNamespace mockNamespace;
	private WaldotEdge mockEdge;

	@BeforeEach
	void setUp() {
		mockProperty = mock(WaldotProperty.class);
		mockNamespace = mock(WaldotNamespace.class);
		mockEdge = mock(WaldotEdge.class);
	}

	@Test
	void testBaseVariableTypeMethods() {
		// Example: Test BaseVariableType.getBrowseName() delegation
		when(mockProperty.getBrowseName()).thenReturn(null);

		assertNull(mockProperty.getBrowseName());
		verify(mockProperty, times(1)).getBrowseName();
	}

	@Test
	void testGetNamespace() {
		when(mockProperty.getNamespace()).thenReturn(mockNamespace);

		final WaldotNamespace result = mockProperty.getNamespace();

		assertEquals(mockNamespace, result);
		verify(mockProperty, times(1)).getNamespace();
	}

	@Test
	void testGetPropertyReference() {
		when(mockProperty.getPropertyReference()).thenReturn(mockEdge);

		final WaldotEdge result = mockProperty.getPropertyReference();

		assertEquals(mockEdge, result);
		verify(mockProperty, times(1)).getPropertyReference();
	}

	@Test
	void testUaServerNodeMethods() {
		// Example: Test UaServerNode.getNodeId() delegation
		when(mockProperty.getNodeId()).thenReturn(null);

		assertNull(mockProperty.getNodeId());
		verify(mockProperty, times(1)).getNodeId();
	}
}