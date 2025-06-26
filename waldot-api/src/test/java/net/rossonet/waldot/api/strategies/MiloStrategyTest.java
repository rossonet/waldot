package net.rossonet.waldot.api.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotVertex;

class MiloStrategyTest {

	private MiloStrategy mockStrategy;

	@BeforeEach
	void setUp() {
		mockStrategy = mock(MiloStrategy.class);
	}

	@Test
	void testAddEdge() {
		final WaldotVertex sourceVertex = mock(WaldotVertex.class);
		final WaldotVertex targetVertex = mock(WaldotVertex.class);
		final Edge edge = mock(Edge.class);
		when(mockStrategy.addEdge(sourceVertex, targetVertex, "label", new Object[] {})).thenReturn(edge);

		final Edge result = mockStrategy.addEdge(sourceVertex, targetVertex, "label", new Object[] {});

		assertEquals(edge, result);
		verify(mockStrategy, times(1)).addEdge(sourceVertex, targetVertex, "label", new Object[] {});
	}

	@Test
	void testAddVertex() {
		final NodeId nodeId = mock(NodeId.class);
		final WaldotVertex vertex = mock(WaldotVertex.class);
		when(mockStrategy.addVertex(nodeId, new Object[] {})).thenReturn(vertex);

		final WaldotVertex result = mockStrategy.addVertex(nodeId, new Object[] {});

		assertEquals(vertex, result);
		verify(mockStrategy, times(1)).addVertex(nodeId, new Object[] {});
	}

	@Test
	void testGetEdges() {
		final Map<NodeId, WaldotEdge> edges = mock(Map.class);
		when(mockStrategy.getEdges()).thenReturn(edges);

		final Map<NodeId, WaldotEdge> result = mockStrategy.getEdges();

		assertEquals(edges, result);
		verify(mockStrategy, times(1)).getEdges();
	}

	@Test
	void testGetVertices() {
		final Map<NodeId, WaldotVertex> vertices = mock(Map.class);
		when(mockStrategy.getVertices()).thenReturn(vertices);

		final Map<NodeId, WaldotVertex> result = mockStrategy.getVertices();

		assertEquals(vertices, result);
		verify(mockStrategy, times(1)).getVertices();
	}

	@Test
	void testNamespaceParametersGet() {
		final String key = "testKey";
		final Object value = new Object();
		when(mockStrategy.namespaceParametersGet(key)).thenReturn(value);

		final Object result = mockStrategy.namespaceParametersGet(key);

		assertEquals(value, result);
		verify(mockStrategy, times(1)).namespaceParametersGet(key);
	}

	@Test
	void testNamespaceParametersKeySet() {
		final Set<String> keys = mock(Set.class);
		when(mockStrategy.namespaceParametersKeySet()).thenReturn(keys);

		final Set<String> result = mockStrategy.namespaceParametersKeySet();

		assertEquals(keys, result);
		verify(mockStrategy, times(1)).namespaceParametersKeySet();
	}

	@Test
	void testNamespaceParametersPut() {
		final String key = "testKey";
		final Object value = new Object();
		doNothing().when(mockStrategy).namespaceParametersPut(key, value);

		mockStrategy.namespaceParametersPut(key, value);

		verify(mockStrategy, times(1)).namespaceParametersPut(key, value);
	}

	@Test
	void testNamespaceParametersRemove() {
		final String key = "testKey";
		doNothing().when(mockStrategy).namespaceParametersRemove(key);

		mockStrategy.namespaceParametersRemove(key);

		verify(mockStrategy, times(1)).namespaceParametersRemove(key);
	}
}