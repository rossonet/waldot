package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;

public interface WaldotProperty<DATA_TYPE> extends Property<DATA_TYPE>, UaServerNode {

	@Override
	QualifiedName getBrowseName();

	WaldotNamespace getNamespace();

	WaldotEdge getPropertyReference();

}
