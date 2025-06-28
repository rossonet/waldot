package net.rossonet.waldot.api.rules;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CachedRuleRecordTest {

    private CachedRuleRecord mockRecord;
    private Fact mockFact;
    private NodeId mockNodeId;

    @BeforeEach
    void setUp() {
        mockRecord = mock(CachedRuleRecord.class);
        mockFact = mock(Fact.class);
        mockNodeId = mock(NodeId.class);

        when(mockRecord.getCreatedAt()).thenReturn(1620000000000L);
        when(mockRecord.getFact()).thenReturn(mockFact);
        when(mockRecord.getFactSource()).thenReturn(mockNodeId);
        when(mockRecord.getValidDelayMs()).thenReturn(60000L);
        when(mockRecord.getValidUntilMs()).thenReturn(1620000060000L);
        when(mockRecord.isExpired()).thenReturn(false);
        when(mockRecord.isValidNow()).thenReturn(true);
    }

    @Test
    void testGetCreatedAt() {
        assertEquals(1620000000000L, mockRecord.getCreatedAt());
        verify(mockRecord, times(1)).getCreatedAt();
    }

    @Test
    void testGetFact() {
        assertEquals(mockFact, mockRecord.getFact());
        verify(mockRecord, times(1)).getFact();
    }

    @Test
    void testGetFactSource() {
        assertEquals(mockNodeId, mockRecord.getFactSource());
        verify(mockRecord, times(1)).getFactSource();
    }

    @Test
    void testGetValidDelayMs() {
        assertEquals(60000L, mockRecord.getValidDelayMs());
        verify(mockRecord, times(1)).getValidDelayMs();
    }

    @Test
    void testGetValidUntilMs() {
        assertEquals(1620000060000L, mockRecord.getValidUntilMs());
        verify(mockRecord, times(1)).getValidUntilMs();
    }

    @Test
    void testIsExpired() {
        assertFalse(mockRecord.isExpired());
        verify(mockRecord, times(1)).isExpired();
    }

    @Test
    void testIsValidNow() {
        assertTrue(mockRecord.isValidNow());
        verify(mockRecord, times(1)).isValidNow();
    }
}