package net.rossonet.waldot.api.models;

import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseObjectType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
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
 * <p>WaldotVertex represents a node in the WaldOT graph that corresponds to an
 * OPC UA Object node in the address space. It combines TinkerPop's Vertex interface
 * with OPC UA's BaseObjectType to provide a unified abstraction for graph vertices
 * that are also OPC UA server nodes.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a vertex
 * WaldotVertex vertex = graph.addVertex("my:device");
 * 
 * // Add property observer
 * vertex.addPropertyObserver(new MyPropertyObserver());
 * 
 * // Add event observer
 * vertex.addEventObserver(new MyEventObserver());
 * 
 * // Set property
 * vertex.property("temperature", 25.5);
 * 
 * // Traverse edges
 * for (Edge edge : vertex.edges(Direction.OUT)) {
 *     System.out.println(edge.label());
 * }
 * 
 * // Get all properties
 * for (WaldotVertexProperty<Object> prop : vertex.getVertexProperties().values()) {
 *     System.out.println(prop.key() + "=" + prop.value());
 * }
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see Vertex
 * @see WaldotElement
 * @see BaseObjectType
 */
public interface WaldotVertex extends Vertex, WaldotElement, BaseObjectType {

	/**
	 * Adds an event observer to receive notifications about OPC UA events.
	 * 
	 * <p>Event observers are notified when events are fired on or near this vertex.
	 * Multiple observers can be registered on a single vertex.</p>
	 * 
	 * @param observer the EventObserver to add
	 * @see EventObserver
	 * @see #removeEventObserver(EventObserver)
	 * @see #getEventObservers()
	 */
	void addEventObserver(EventObserver observer);

	/**
	 * Adds a property observer to receive notifications about property changes.
	 * 
	 * <p>Property observers are notified when any property value changes on this
	 * vertex. This is useful for monitoring and reacting to data changes in
	 * the graph.</p>
	 * 
	 * @param observer the PropertyObserver to add
	 * @see PropertyObserver
	 * @see #removePropertyObserver(PropertyObserver)
	 * @see #getPropertyObservers()
	 */
	void addPropertyObserver(PropertyObserver observer);

	/**
	 * Adds a related property to this vertex.
	 * 
	 * <p>This associates a WaldotVertexProperty with this vertex. The property
	 * is tracked for cleanup when the vertex is removed.</p>
	 * 
	 * @param property the WaldotVertexProperty to add
	 * @see WaldotVertexProperty
	 */
	void addRelatedProperty(WaldotVertexProperty<?> property);

	/**
	 * Finds a method node by its NodeId.
	 * 
	 * <p>Searches for a method node (OPC UA Method) that is a component of this
	 * vertex. Returns null if no method with the given ID exists.</p>
	 * 
	 * @param methodId the NodeId of the method to find
	 * @return the UaMethodNode if found, null otherwise
	 * @see UaMethodNode
	 * @see NodeId
	 */
	public UaMethodNode findMethodNode(NodeId methodId);

	/**
	 * Fires an OPC UA event from this vertex.
	 * 
	 * <p>This method triggers an event notification that can be received by
	 * subscribed clients. The event is fired with the specified priority.</p>
	 * 
	 * <p>The default implementation is empty. Override to provide custom
	 * event firing behavior.</p>
	 * 
	 * @param node the source node for the event
	 * @param event the BaseEventType event to fire
	 * @param calcolatedPriority the priority for the event (higher = more important)
	 * @see BaseEventType
	 * @see UaNode
	 */
	default void fireEvent(final UaNode node, final BaseEventType event, final int calcolatedPriority) {
	}

	/**
	 * Fires a property change notification.
	 * 
	 * <p>This method triggers a property change notification that can be received
	 * by subscribed clients. The notification is sent with the specified priority.</p>
	 * 
	 * <p>The default implementation is empty. Override to provide custom
	 * property notification behavior.</p>
	 * 
	 * @param node the source node containing the property
	 * @param propertyLabel the label of the property that changed
	 * @param value the new DataValue of the property
	 * @param calcolatedPriority the priority for the notification
	 * @see DataValue
	 */
	default void fireProperty(final UaNode node, final String propertyLabel, final DataValue value, final int calcolatedPriority) {
	}

	/**
	 * Returns all event observers registered on this vertex.
	 * 
	 * @return list of EventObserver instances
	 * @see #addEventObserver(EventObserver)
	 */
	List<EventObserver> getEventObservers();

	/**
	 * Returns the GraphComputer view for OLAP processing.
	 * 
	 * <p>When the graph is in computer mode (OLAP), this returns the view
	 * that allows accessing vertices and edges within the compute context.</p>
	 * 
	 * @return the WaldotGraphComputerView, or null if not in computer mode
	 * @see WaldotGraphComputerView
	 */
	WaldotGraphComputerView getGraphComputerView();

	/**
	 * Returns all method nodes associated with this vertex.
	 * 
	 * <p>These are OPC UA Methods that are components of this vertex (Object).
	 * Each method can be invoked by OPC UA clients.</p>
	 * 
	 * @return list of UaMethodNode instances
	 * @see UaMethodNode
	 * @see #findMethodNode(NodeId)
	 */
	public List<UaMethodNode> getMethodNodes();

	/**
	 * Returns all vertex properties as a string array.
	 * 
	 * <p>This provides a simple representation of all properties for debugging
	 * or logging purposes. The format is "key=value" for each property.</p>
	 * 
	 * @return array of property strings
	 */
	String[] getPropertiesAsStringArray();

	/**
	 * Returns all property observers registered on this vertex.
	 * 
	 * @return list of PropertyObserver instances
	 * @see #addPropertyObserver(PropertyObserver)
	 */
	List<PropertyObserver> getPropertyObservers();

	/**
	 * Returns an immutable map of all vertex properties.
	 * 
	 * <p>The keys are property names and the values are WaldotVertexProperty
	 * instances containing the property data.</p>
	 * 
	 * @return immutable map of property name to WaldotVertexProperty
	 * @see WaldotVertexProperty
	 */
	ImmutableMap<String, WaldotVertexProperty<Object>> getVertexProperties();

	/**
	 * Checks if the graph is in computer mode (OLAP processing).
	 * 
	 * <p>When in computer mode, certain graph operations behave differently
	 * as they operate within a GraphComputer context rather than transactional
	 * (OLTP) context.</p>
	 * 
	 * @return true if in computer mode, false otherwise
	 * @see #getGraphComputerView()
	 */
	boolean inComputerMode();;

	/**
	 * Called before a property value is changed in the graph database.
	 * 
	 * <p>This callback is invoked before a property value is modified, allowing
	 * the implementation to validate, transform, or reject the change.</p>
	 * 
	 * <p>Example use cases:</p>
	 * <ul>
	 *   <li>Validate value constraints</li>
	 *   <li>Transform values (e.g., unit conversion)</li>
	 *   <li>Log changes for audit</li>
	 *   <li>Trigger side effects</li>
	 * </ul>
	 * 
	 * @param label the property label
	 * @param value the new DataValue being set
	 */
	void notifyPropertyValueChanging(String label, DataValue value);

	/**
	 * Called before the vertex is removed from the graph database.
	 * 
	 * <p>This callback is invoked before the vertex is deleted, allowing the
	 * implementation to perform cleanup operations or prevent deletion if needed.</p>
	 * 
	 * <p>The default implementation does nothing. Override to implement
	 * custom pre-removal logic.</p>
	 */
	default void notifyRemoveVertex() {
	}

	/**
	 * Posts an event to the OPC UA event system.
	 * 
	 * <p>This method adds an event to the OPC UA notification queue for
	 * delivery to subscribed clients.</p>
	 * 
	 * @param event the BaseEventTypeNode event to post
	 * @see BaseEventTypeNode
	 */
	void postEvent(BaseEventTypeNode event);

	/**
	 * Removes an event observer from this vertex.
	 * 
	 * @param observer the EventObserver to remove
	 * @see #addEventObserver(EventObserver)
	 */
	void removeEventObserver(EventObserver observer);

	/**
	 * Removes a property observer from this vertex.
	 * 
	 * @param observer the PropertyObserver to remove
	 * @see #addPropertyObserver(PropertyObserver)
	 */
	void removePropertyObserver(PropertyObserver observer);

	/**
	 * Removes all related OPC UA nodes associated with this vertex.
	 * 
	 * <p>This includes property nodes, method nodes, and other components
	 * that were created as part of this vertex. Called during vertex removal
	 * to clean up the OPC UA address space.</p>
	 */
	void removeRelatedOpcUaNodes();

}
