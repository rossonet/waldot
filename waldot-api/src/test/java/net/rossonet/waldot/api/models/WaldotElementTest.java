package net.rossonet.waldot.api.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WaldotElementTest {

	private WaldotElement mockElement;

	@BeforeEach
	void setUp() {
		mockElement = mock(WaldotElement.class);
	}

	@Test
	void testAddComponent() {
		final WaldotElement component = mock(WaldotElement.class);
		doNothing().when(mockElement).addComponent(component);

		mockElement.addComponent(component);

		verify(mockElement, times(1)).addComponent(component);
	}

	@Test
	void testGetIcon() {
		final ByteString icon = mock(ByteString.class);
		when(mockElement.getIcon()).thenReturn(icon);

		assertEquals(icon, mockElement.getIcon());
		verify(mockElement, times(1)).getIcon();
	}

	@Test
	void testGetNamespace() {
		final WaldotNamespace namespace = mock(WaldotNamespace.class);
		when(mockElement.getNamespace()).thenReturn(namespace);

		assertEquals(namespace, mockElement.getNamespace());
		verify(mockElement, times(1)).getNamespace();
	}

	@Test
	void testGetNodeVersion() {
		final String version = "1.0.0";
		when(mockElement.getNodeVersion()).thenReturn(version);

		assertEquals(version, mockElement.getNodeVersion());
		verify(mockElement, times(1)).getNodeVersion();
	}

	@Test
	void testIsRemoved() {
		when(mockElement.isRemoved()).thenReturn(true);

		assertTrue(mockElement.isRemoved());
		verify(mockElement, times(1)).isRemoved();
	}

	@Test
	void testRemoveComponent() {
		final WaldotElement component = mock(WaldotElement.class);
		doNothing().when(mockElement).removeComponent(component);

		mockElement.removeComponent(component);

		verify(mockElement, times(1)).removeComponent(component);
	}

	@Test
	void testSetIcon() {
		final ByteString icon = mock(ByteString.class);
		doNothing().when(mockElement).setIcon(icon);

		mockElement.setIcon(icon);

		verify(mockElement, times(1)).setIcon(icon);
	}

	@Test
	void testSetNodeVersion() {
		final String version = "1.0.0";
		doNothing().when(mockElement).setNodeVersion(version);

		mockElement.setNodeVersion(version);

		verify(mockElement, times(1)).setNodeVersion(version);
	}

	@Test
	void testVersion() {
		final long version = 12345L;
		when(mockElement.version()).thenReturn(version);

		assertEquals(version, mockElement.version());
		verify(mockElement, times(1)).version();
	}
}