package net.rossonet.waldot.api.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WaldotThreadFactoryTest {

	private WaldotThreadFactory mockThreadFactory;
	private Runnable mockRunnable;

	@BeforeEach
	void setUp() {
		mockThreadFactory = mock(WaldotThreadFactory.class);
		mockRunnable = mock(Runnable.class);
	}

	@Test
	void testNewThread() {
		final Thread mockThread = new Thread(mockRunnable);
		when(mockThreadFactory.newThread(mockRunnable)).thenReturn(mockThread);

		final Thread result = mockThreadFactory.newThread(mockRunnable);

		assertNotNull(result);
		assertEquals(mockThread, result);
		verify(mockThreadFactory, times(1)).newThread(mockRunnable);
	}

}