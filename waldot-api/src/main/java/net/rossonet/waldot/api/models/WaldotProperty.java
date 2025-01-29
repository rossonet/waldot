package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.sdk.server.model.types.variables.BaseVariableType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode;

public interface WaldotProperty<DATA_TYPE> extends Property<DATA_TYPE>, BaseVariableType, UaServerNode {

	WaldotNamespace getNamespace();

	WaldotEdge getPropertyReference();

}
