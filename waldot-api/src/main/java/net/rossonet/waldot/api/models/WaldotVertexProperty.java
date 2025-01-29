package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.eclipse.milo.opcua.sdk.server.model.types.variables.BaseVariableType;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;

import net.rossonet.waldot.api.PropertyObserver;

public interface WaldotVertexProperty<DATA_TYPE>
		extends VertexProperty<DATA_TYPE>, BaseVariableType, WaldotElement, AttributeObserver, PropertyObserver {

	WaldotVertex getVertexPropertyReference();

}
