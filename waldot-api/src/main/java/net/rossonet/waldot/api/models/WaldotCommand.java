package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.core.nodes.MethodNode;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.api.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

public interface WaldotCommand extends Vertex, MethodNode, AttributeObserver {

	void addInputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description);

	void addOutputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description);

	UByte getEventNotifier();

	Argument[] getInputArguments();

	MethodInvocationHandler getInvocationHandler();

	Argument[] getOutputArguments();

	String[] runCommand(InvocationContext invocationContext, String[] inputValues);

	Object runCommand(String[] methodInputs);

	void setEventNotifier(UByte eventNotifier);

	void setInputArguments(Argument[] array);

	void setOutputArguments(Argument[] array);

}
