package net.rossonet.waldot.api.models;

import org.eclipse.milo.opcua.sdk.core.nodes.MethodNode;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.api.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

/**
 * WaldotCommand is an interface that extends WaldotVertex, MethodNode, and
 * UaServerNode. It defines the structure and behavior of a command in the
 * Waldot system, including methods for adding input and output arguments,
 * running the command, and managing its properties.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotCommand extends WaldotVertex, MethodNode, UaServerNode {
	void addInputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description);

	void addOutputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description);

	@Override
	public ByteString getIcon();

	Argument[] getInputArguments();

	MethodInvocationHandler getInvocationHandler();

	@Override
	WaldotNamespace getNamespace();

	@Override
	public String getNodeVersion();

	Argument[] getOutputArguments();

	@Override
	boolean isRemoved();

	Object[] runCommand(InvocationContext invocationContext, String[] inputValues);

	Object[] runCommand(String[] methodInputs);

	@Override
	public void setIcon(ByteString icon);

	void setInputArguments(Argument[] array);

	@Override
	public void setNodeVersion(String nodeVersion);

	void setOutputArguments(Argument[] array);

	@Override
	long version();
}
