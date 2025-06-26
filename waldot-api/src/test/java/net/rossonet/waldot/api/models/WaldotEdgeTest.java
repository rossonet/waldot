package net.rossonet.waldot.api.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

class WaldotEdgeTest {

	private WaldotEdge mockEdge;

	@BeforeEach
	void setUp() {
		mockEdge = mock(WaldotEdge.class);
	}

	@Test
	void testGetProperties() {
		final ImmutableList<WaldotProperty<Object>> properties = mock(ImmutableList.class);
		when(mockEdge.getProperties()).thenReturn(properties);

		final ImmutableList<WaldotProperty<Object>> result = mockEdge.getProperties();

		assertEquals(properties, result);
		verify(mockEdge, times(1)).getProperties();
	}

	@Test
	void testGetProperty() {
		final QualifiedProperty<String> property = mock(QualifiedProperty.class);
		final Optional<String> expectedValue = Optional.of("testValue");
		when(mockEdge.getProperty(property)).thenReturn(expectedValue);

		final Optional<String> result = mockEdge.getProperty(property);

		assertEquals(expectedValue, result);
		verify(mockEdge, times(1)).getProperty(property);
	}
}