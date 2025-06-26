package net.rossonet.waldot.api.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.PropertyObserver;

class WaldotVertexTest {

	private WaldotVertex mockVertex;

	@BeforeEach
	void setUp() {
		mockVertex = mock(WaldotVertex.class);
	}

	@Test
	void testAddAttributeObserver() {
		final AttributeObserver observer = mock(AttributeObserver.class);
		doNothing().when(mockVertex).addAttributeObserver(observer);

		mockVertex.addAttributeObserver(observer);

		verify(mockVertex, times(1)).addAttributeObserver(observer);
	}

	@Test
	void testAddEventObserver() {
		final EventObserver observer = mock(EventObserver.class);
		doNothing().when(mockVertex).addEventObserver(observer);

		mockVertex.addEventObserver(observer);

		verify(mockVertex, times(1)).addEventObserver(observer);
	}

	@Test
	void testAddPropertyObserver() {
		final PropertyObserver observer = mock(PropertyObserver.class);
		doNothing().when(mockVertex).addPropertyObserver(observer);

		mockVertex.addPropertyObserver(observer);

		verify(mockVertex, times(1)).addPropertyObserver(observer);
	}

	@Test
	void testFindMethodNode() {
		final NodeId methodId = mock(NodeId.class);
		final UaMethodNode methodNode = mock(UaMethodNode.class);
		when(mockVertex.findMethodNode(methodId)).thenReturn(methodNode);

		final UaMethodNode result = mockVertex.findMethodNode(methodId);

		assertEquals(methodNode, result);
		verify(mockVertex, times(1)).findMethodNode(methodId);
	}

	@Test
	void testFireAttributeChanged() {
		final AttributeId attributeId = mock(AttributeId.class);
		final Object attributeValue = new Object();
		doNothing().when(mockVertex).fireAttributeChanged(attributeId, attributeValue);

		mockVertex.fireAttributeChanged(attributeId, attributeValue);

		verify(mockVertex, times(1)).fireAttributeChanged(attributeId, attributeValue);
	}

	@Test
	void testGetEventObservers() {
		final List<EventObserver> observers = mock(List.class);
		when(mockVertex.getEventObservers()).thenReturn(observers);

		final List<EventObserver> result = mockVertex.getEventObservers();

		assertEquals(observers, result);
		verify(mockVertex, times(1)).getEventObservers();
	}

	@Test
	void testGetGraphComputerView() {
		final WaldotGraphComputerView view = mock(WaldotGraphComputerView.class);
		when(mockVertex.getGraphComputerView()).thenReturn(view);

		final WaldotGraphComputerView result = mockVertex.getGraphComputerView();

		assertEquals(view, result);
		verify(mockVertex, times(1)).getGraphComputerView();
	}

	@Test
	void testGetMethodNodes() {
		final List<UaMethodNode> methodNodes = mock(List.class);
		when(mockVertex.getMethodNodes()).thenReturn(methodNodes);

		final List<UaMethodNode> result = mockVertex.getMethodNodes();

		assertEquals(methodNodes, result);
		verify(mockVertex, times(1)).getMethodNodes();
	}

	@Test
	void testGetPropertyObservers() {
		final List<PropertyObserver> observers = mock(List.class);
		when(mockVertex.getPropertyObservers()).thenReturn(observers);

		final List<PropertyObserver> result = mockVertex.getPropertyObservers();

		assertEquals(observers, result);
		verify(mockVertex, times(1)).getPropertyObservers();
	}

	@Test
	void testGetVertexProperties() {
		final ImmutableMap<String, WaldotVertexProperty<Object>> properties = mock(ImmutableMap.class);
		when(mockVertex.getVertexProperties()).thenReturn(properties);

		final ImmutableMap<String, WaldotVertexProperty<Object>> result = mockVertex.getVertexProperties();

		assertEquals(properties, result);
		verify(mockVertex, times(1)).getVertexProperties();
	}

	@Test
	void testInComputerMode() {
		when(mockVertex.inComputerMode()).thenReturn(true);

		final boolean result = mockVertex.inComputerMode();

		assertTrue(result);
		verify(mockVertex, times(1)).inComputerMode();
	}

	@Test
	void testPostEvent() {
		final BaseEventTypeNode event = mock(BaseEventTypeNode.class);
		doNothing().when(mockVertex).postEvent(event);

		mockVertex.postEvent(event);

		verify(mockVertex, times(1)).postEvent(event);
	}

	@Test
	void testRemoveAttributeObserver() {
		final AttributeObserver observer = mock(AttributeObserver.class);
		doNothing().when(mockVertex).removeAttributeObserver(observer);

		mockVertex.removeAttributeObserver(observer);

		verify(mockVertex, times(1)).removeAttributeObserver(observer);
	}

	@Test
	void testRemoveEventObserver() {
		final EventObserver observer = mock(EventObserver.class);
		doNothing().when(mockVertex).removeEventObserver(observer);

		mockVertex.removeEventObserver(observer);

		verify(mockVertex, times(1)).removeEventObserver(observer);
	}

	@Test
	void testRemovePropertyObserver() {
		final PropertyObserver observer = mock(PropertyObserver.class);
		doNothing().when(mockVertex).removePropertyObserver(observer);

		mockVertex.removePropertyObserver(observer);

		verify(mockVertex, times(1)).removePropertyObserver(observer);
	}
}