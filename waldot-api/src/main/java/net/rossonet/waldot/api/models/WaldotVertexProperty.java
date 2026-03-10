package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.eclipse.milo.opcua.sdk.server.model.variables.BaseVariableType;

/**
 * WaldotVertexProperty is an interface that extends VertexProperty,
 * BaseVariableType, WaldotElement, AttributeObserver, and PropertyObserver. It
 * represents a property associated with a vertex in the Waldot graph model,
 * providing methods to access the vertex property reference and manage
 * attributes.
 * 
 * <p>WaldotVertexProperty represents a property on a vertex in the WaldOT graph.
 * It combines TinkerPop's VertexProperty interface with OPC UA variable node
 * functionality and supports both graph properties and OPC UA data changes.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Get vertex property
 * WaldotVertexProperty<Double> prop = vertex.property("temperature");
 * 
 * // Get property value
 * Double value = prop.value();
 * 
 * // Get property key
 * String key = prop.key();
 * 
 * // Get owning vertex
 * WaldotVertex owner = prop.getVertexPropertyReference();
 * 
 * // Check if single or multi-value
 * boolean isSingle = prop.isSingle();
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see VertexProperty
 * @see BaseVariableType
 * @see WaldotElement
 */
public interface WaldotVertexProperty<DATA_TYPE> extends VertexProperty<DATA_TYPE>, BaseVariableType, WaldotElement {

	/**
	 * Returns the vertex that owns this property.
	 * 
	 * @return the owning WaldotVertex
	 * @see WaldotVertex
	 */
	WaldotVertex getVertexPropertyReference();

}
