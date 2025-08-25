package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.sdk.server.model.variables.BaseVariableType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode;

/**
 * WaldotProperty is an interface that extends Property, BaseVariableType, and
 * UaServerNode. It represents a property in the Waldot graph model, providing
 * methods to access the namespace and property reference.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotProperty<DATA_TYPE> extends Property<DATA_TYPE>, BaseVariableType, UaServerNode {

	WaldotNamespace getNamespace();

	WaldotEdge getPropertyReference();

}
