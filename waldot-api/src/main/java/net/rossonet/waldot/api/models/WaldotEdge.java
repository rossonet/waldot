package net.rossonet.waldot.api.models;

import java.util.List;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseObjectType;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.shaded.com.google.common.collect.ImmutableList;

import net.rossonet.waldot.api.PropertyObserver;

/**
 * WaldotEdge is an interface that extends Edge, WaldotElement, and
 * BaseObjectType. It represents an edge in the Waldot graph model and provides
 * methods to access properties associated with the edge.
 * 
 * <p>WaldotEdge represents a relationship between two vertices in the WaldOT
 * graph. It corresponds to an OPC UA Reference between nodes in the address
 * space. Edges can have properties and can be monitored for changes.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create edge between vertices
 * WaldotEdge edge = graph.addEdge(sourceVertex, targetVertex, "hasSensor");
 * 
 * // Set edge property
 * edge.property("priority", 10);
 * 
 * // Get edge properties
 * for (WaldotProperty<Object> prop : edge.getProperties()) {
 *     System.out.println(prop.key() + "=" + prop.value());
 * }
 * 
 * // Navigate to connected vertices
 * WaldotVertex out = edge.outVertex();
 * WaldotVertex in = edge.inVertex();
 * 
 * // Add property observer
 * edge.addPropertyObserver(new MyPropertyObserver());
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see Edge
 * @see WaldotElement
 * @see BaseObjectType
 */
public interface WaldotEdge extends Edge, WaldotElement, BaseObjectType {

	/**
	 * Sets the monitor for this edge.
	 * 
	 * <p>The monitor tracks changes to the edge and can trigger notifications
	 * or logging when the edge is modified.</p>
	 * 
	 * @param monitoredEdge the MonitoredEdge to use for monitoring
	 * @see MonitoredEdge
	 */
	void setMonitor(MonitoredEdge monitoredEdge);

	/**
	 * Adds a property observer to receive notifications about property changes.
	 * 
	 * <p>Property observers are notified when any property value changes on this
	 * edge. This is useful for monitoring edge modifications.</p>
	 * 
	 * @param propertyObserver the PropertyObserver to add
	 * @see PropertyObserver
	 * @see #removePropertyObserver(PropertyObserver)
	 */
	void addPropertyObserver(PropertyObserver propertyObserver);

	/**
	 * Adds a related property to this edge.
	 * 
	 * <p>This associates a WaldotProperty with this edge. The property
	 * is tracked for cleanup when the edge is removed.</p>
	 * 
	 * @param property the WaldotProperty to add
	 * @see WaldotProperty
	 */
	void addRelatedProperty(WaldotProperty<?> property);

	/**
	 * Adds a related reference to this edge.
	 * 
	 * <p>References are OPC UA references that connect this edge to other nodes.
	 * This is used for tracking and cleanup of OPC UA references.</p>
	 * 
	 * @param reference the Reference to add
	 * @see Reference
	 */
	void addRelatedReference(Reference reference);

	/**
	 * Returns an immutable list of all properties on this edge.
	 * 
	 * @return immutable list of WaldotProperty objects
	 * @see WaldotProperty
	 */
	ImmutableList<WaldotProperty<Object>> getProperties();

	/**
	 * Gets a specific property value using a QualifiedProperty.
	 * 
	 * <p>This method provides type-safe access to OPC UA properties using
	 * the QualifiedProperty descriptor.</p>
	 * 
	 * @param property the QualifiedProperty descriptor
	 * @param <T> the type of the property value
	 * @return Optional containing the value if present, empty otherwise
	 * @see QualifiedProperty
	 */
	public <T> Optional<T> getProperty(QualifiedProperty<T> property);

	/**
	 * Returns all property observers registered on this edge.
	 * 
	 * @return list of PropertyObserver instances
	 * @see #addPropertyObserver(PropertyObserver)
	 */
	List<PropertyObserver> getPropertyObservers();

	/**
	 * Called before a property value is changed on this edge.
	 * 
	 * <p>This callback is invoked before a property value is modified, allowing
	 * the implementation to validate, transform, or reject the change.</p>
	 * 
	 * @param label the property label
	 * @param value the new DataValue being set
	 */
	void notifyPropertyValueChanging(String label, DataValue value);

	/**
	 * Removes a property observer from this edge.
	 * 
	 * @param observer the PropertyObserver to remove
	 * @see #addPropertyObserver(PropertyObserver)
	 */
	void removePropertyObserver(PropertyObserver observer);

	/**
	 * Removes all related OPC UA nodes associated with this edge.
	 * 
	 * <p>This includes property nodes and references that were created as
	 * part of this edge. Called during edge removal to clean up the OPC UA
	 * address space.</p>
	 */
	void removeRelatedOpcUaNodes();

}
