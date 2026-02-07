package net.rossonet.waldot.api.models;

import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseObjectType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.shaded.com.google.common.collect.ImmutableMap;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.PropertyObserver;

/**
 * WaldotVertex is an interface that extends Vertex, WaldotElement,
 * BaseObjectType, AttributeObserver, and PropertyObserver. It represents a
 * vertex in the Waldot graph model, providing methods to manage attributes,
 * properties, and events.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotVertex extends Vertex, WaldotElement, BaseObjectType {

	void addEventObserver(EventObserver observer);

	void addPropertyObserver(PropertyObserver observer);

	void addRelatedProperty(WaldotProperty<?> property);

	public UaMethodNode findMethodNode(NodeId methodId);

	List<EventObserver> getEventObservers();

	WaldotGraphComputerView getGraphComputerView();

	public List<UaMethodNode> getMethodNodes();

	String[] getPropertiesAsStringArray();

	List<PropertyObserver> getPropertyObservers();

	ImmutableMap<String, WaldotVertexProperty<Object>> getVertexProperties();

	boolean inComputerMode();

	/**
	 * called before a property value is changed in the graph database
	 */
	void notifyPropertyValueChanging(String label, DataValue value);

	/**
	 * called before the vertex is removed from the graph database
	 */
	default void notifyRemoveVertex() {
	};

	void postEvent(BaseEventTypeNode event);

	void removeEventObserver(EventObserver observer);

	void removePropertyObserver(PropertyObserver observer);

	void removeRelatedOpcUaNodes();

}
