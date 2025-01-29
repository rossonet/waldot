package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.eclipse.milo.opcua.sdk.core.nodes.MethodNode;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.api.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

public interface WaldotCommand extends Element, MethodNode, UaServerNode {
	void addInputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description);

	void addOutputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description);

	public ByteString getIcon();

	Argument[] getInputArguments();

	MethodInvocationHandler getInvocationHandler();

	WaldotNamespace getNamespace();

	public String getNodeVersion();

	Argument[] getOutputArguments();

	boolean isRemoved();

	Object[] runCommand(InvocationContext invocationContext, String[] inputValues);

	Object[] runCommand(String[] methodInputs);

	public void setIcon(ByteString icon);

	void setInputArguments(Argument[] array);

	public void setNodeVersion(String nodeVersion);

	void setOutputArguments(Argument[] array);

	long version();
}
