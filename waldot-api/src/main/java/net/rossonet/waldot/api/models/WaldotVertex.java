package net.rossonet.waldot.api.models;

import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.types.objects.BaseObjectType;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import com.google.common.collect.ImmutableMap;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.PropertyObserver;

public interface WaldotVertex extends Vertex, WaldotElement, BaseObjectType, AttributeObserver, PropertyObserver {

	void addAttributeObserver(AttributeObserver observer);

	void addEventObserver(EventObserver observer);

	void addPropertyObserver(PropertyObserver observer);

	public UaMethodNode findMethodNode(NodeId methodId);

	void fireAttributeChanged(AttributeId attributeId, Object attributeValue);

	List<EventObserver> getEventObservers();

	WaldotGraphComputerView getGraphComputerView();

	public List<UaMethodNode> getMethodNodes();

	List<PropertyObserver> getPropertyObservers();

	ImmutableMap<String, WaldotVertexProperty<Object>> getVertexProperties();

	boolean inComputerMode();

	void postEvent(BaseEventTypeNode event);

	void removeAttributeObserver(AttributeObserver observer);

	void removeEventObserver(EventObserver observer);

	void removePropertyObserver(PropertyObserver observer);

}
