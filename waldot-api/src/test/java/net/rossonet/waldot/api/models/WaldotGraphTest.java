package net.rossonet.waldot.api.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WaldotGraphTest {

	private WaldotGraph mockGraph;
	private WaldotNamespace mockNamespace;
	private WaldotGraphComputerView mockGraphComputerView;

	@BeforeEach
	void setUp() {
		mockGraph = mock(WaldotGraph.class);
		mockNamespace = mock(WaldotNamespace.class);
		mockGraphComputerView = mock(WaldotGraphComputerView.class);

		when(mockGraph.getWaldotNamespace()).thenReturn(mockNamespace);
		when(mockGraph.getGraphComputerView()).thenReturn(mockGraphComputerView);
	}

	@Test
	void testCreateElementIterator() {
		final Map<NodeId, WaldotElement> mockElements = mock(Map.class);
		final IdManager<?> mockIdManager = mock(IdManager.class);
		final NodeId[] mockIds = new NodeId[0];
		final Iterator<Element> mockIterator = mock(Iterator.class);

		when(mockGraph.createElementIterator(Element.class, WaldotElement.class, mockElements, mockIdManager, mockIds))
				.thenReturn(mockIterator);

		assertEquals(mockIterator, mockGraph.createElementIterator(Element.class, WaldotElement.class, mockElements,
				mockIdManager, mockIds));
		verify(mockGraph, times(1)).createElementIterator(Element.class, WaldotElement.class, mockElements,
				mockIdManager, mockIds);
	}

	@Test
	void testGetEdgesCount() {
		when(mockGraph.getEdgesCount()).thenReturn(10);

		final int edgesCount = mockGraph.getEdgesCount();

		assertEquals(10, edgesCount);
		verify(mockGraph, times(1)).getEdgesCount();
	}

	@Test
	void testGetGeneratedId() {
		final Long generatedId = 12345L;
		when(mockGraph.getGeneratedId()).thenReturn(generatedId);

		assertEquals(generatedId, mockGraph.getGeneratedId());
		verify(mockGraph, times(1)).getGeneratedId();
	}

	@Test
	void testGetVerticesCount() {
		when(mockGraph.getVerticesCount()).thenReturn(20);

		final int verticesCount = mockGraph.getVerticesCount();

		assertEquals(20, verticesCount);
		verify(mockGraph, times(1)).getVerticesCount();
	}

	@Test
	void testRemoveVertex() {
		final NodeId mockNodeId = mock(NodeId.class);
		doNothing().when(mockGraph).removeVertex(mockNodeId);

		mockGraph.removeVertex(mockNodeId);

		verify(mockGraph, times(1)).removeVertex(mockNodeId);
	}

	@Test
	void testSetNamespace() {
		doNothing().when(mockGraph).setNamespace(mockNamespace);

		mockGraph.setNamespace(mockNamespace);

		verify(mockGraph, times(1)).setNamespace(mockNamespace);
	}

	@Test
	void testVertex() {
		final NodeId mockNodeId = mock(NodeId.class);
		final Vertex mockVertex = mock(Vertex.class);
		when(mockGraph.vertex(mockNodeId)).thenReturn(mockVertex);

		assertEquals(mockVertex, mockGraph.vertex(mockNodeId));
		verify(mockGraph, times(1)).vertex(mockNodeId);
	}
}