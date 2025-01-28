package net.rossonet.waldot.api;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.core.nodes.ObjectNode;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;

public interface WaldotCommand extends Vertex, ObjectNode, AttributeObserver {

	String[] runCommand(InvocationContext invocationContext, String[] inputValues);

}
