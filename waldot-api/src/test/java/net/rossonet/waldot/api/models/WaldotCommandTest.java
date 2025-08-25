package net.rossonet.waldot.api.models;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WaldotCommandTest {

	private WaldotCommand mockCommand;

	@BeforeEach
	void setUp() {
		mockCommand = mock(WaldotCommand.class);
	}

	@Test
	void testAddInputArgument() {
		doNothing().when(mockCommand).addInputArgument(anyString(), any(NodeId.class), anyInt(), any(UInteger[].class),
				any(LocalizedText.class));

		mockCommand.addInputArgument("name", mock(NodeId.class), 0, new UInteger[] {}, mock(LocalizedText.class));

		verify(mockCommand, times(1)).addInputArgument(anyString(), any(NodeId.class), anyInt(), any(UInteger[].class),
				any(LocalizedText.class));
	}

	@Test
	void testAddOutputArgument() {
		doNothing().when(mockCommand).addOutputArgument(anyString(), any(NodeId.class), anyInt(), any(UInteger[].class),
				any(LocalizedText.class));

		mockCommand.addOutputArgument("name", mock(NodeId.class), 0, new UInteger[] {}, mock(LocalizedText.class));

		verify(mockCommand, times(1)).addOutputArgument(anyString(), any(NodeId.class), anyInt(), any(UInteger[].class),
				any(LocalizedText.class));
	}

	@Test
	void testGetIcon() {
		final ByteString icon = mock(ByteString.class);
		when(mockCommand.getIcon()).thenReturn(icon);

		assertEquals(icon, mockCommand.getIcon());
		verify(mockCommand, times(1)).getIcon();
	}

	/*
	 * @Test void testRunCommandWithInvocationContext() { final InvocationContext
	 * context = mock(InvocationContext.class); final String[] inputValues = {
	 * "value1", "value2" }; final Object[] expectedOutput = { "output1", "output2"
	 * }; when(mockCommand.runCommand(context,
	 * inputValues)).thenReturn(expectedOutput);
	 * 
	 * assertArrayEquals(expectedOutput, mockCommand.runCommand(context,
	 * inputValues)); verify(mockCommand, times(1)).runCommand(context,
	 * inputValues); }
	 */
	@Test
	void testRunCommandWithMethodInputs() {
		final String[] methodInputs = { "input1", "input2" };
		final Object[] expectedOutput = { "output1", "output2" };
		when(mockCommand.runCommand(methodInputs)).thenReturn(expectedOutput);

		assertArrayEquals(expectedOutput, mockCommand.runCommand(methodInputs));
		verify(mockCommand, times(1)).runCommand(methodInputs);
	}

	@Test
	void testSetIcon() {
		doNothing().when(mockCommand).setIcon(any(ByteString.class));

		mockCommand.setIcon(mock(ByteString.class));

		verify(mockCommand, times(1)).setIcon(any(ByteString.class));
	}

	@Test
	void testVersion() {
		final long version = 12345L;
		when(mockCommand.version()).thenReturn(version);

		assertEquals(version, mockCommand.version());
		verify(mockCommand, times(1)).version();
	}
}