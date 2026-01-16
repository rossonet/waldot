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
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotVertexProperty<DATA_TYPE> extends VertexProperty<DATA_TYPE>, BaseVariableType, WaldotElement {

	WaldotVertex getVertexPropertyReference();

}
