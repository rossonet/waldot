package net.rossonet.waldot.api.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FactTest {

    private Fact mockFact;

    @BeforeEach
    void setUp() {
        mockFact = mock(Fact.class);

        when(mockFact.getAttribute()).thenReturn("testAttribute");
        when(mockFact.getType()).thenReturn(Fact.FactType.EVENT);
        when(mockFact.getValue()).thenReturn("testValue");
    }

    @Test
    void testGetAttribute() {
        assertEquals("testAttribute", mockFact.getAttribute());
        verify(mockFact, times(1)).getAttribute();
    }

    @Test
    void testGetType() {
        assertEquals(Fact.FactType.EVENT, mockFact.getType());
        verify(mockFact, times(1)).getType();
    }

    @Test
    void testGetValue() {
        assertEquals("testValue", mockFact.getValue());
        verify(mockFact, times(1)).getValue();
    }
}