package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.sdk.server.model.variables.BaseVariableType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode;

/**
 * WaldotProperty is an interface that extends Property, BaseVariableType, and
 * UaServerNode. It represents a property in the Waldot graph model, providing
 * methods to access the namespace and property reference.
 * 
 * <p>WaldotProperty represents a property on an edge in the WaldOT graph.
 * It combines TinkerPop's Property interface with OPC UA variable node
 * functionality.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Get property from edge
 * WaldotProperty<String> prop = edge.property("priority");
 * 
 * // Get property value
 * String value = prop.value();
 * 
 * // Get property key
 * String key = prop.key();
 * 
 * // Get namespace
 * WaldotNamespace ns = prop.getNamespace();
 * 
 * // Get owning edge
 * WaldotEdge owner = prop.getPropertyReference();
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see Property
 * @see BaseVariableType
 */
public interface WaldotProperty<DATA_TYPE> extends Property<DATA_TYPE>, BaseVariableType, UaServerNode {

	/**
	 * Returns the namespace containing this property.
	 * 
	 * @return the WaldotNamespace
	 * @see WaldotNamespace
	 */
	WaldotNamespace getNamespace();

	/**
	 * Returns the edge that owns this property.
	 * 
	 * @return the owning WaldotEdge
	 * @see WaldotEdge
	 */
	WaldotEdge getPropertyReference();

}
