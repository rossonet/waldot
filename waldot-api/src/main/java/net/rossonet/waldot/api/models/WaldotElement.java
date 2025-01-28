package net.rossonet.waldot.api.models;

import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.eclipse.milo.opcua.sdk.core.nodes.ObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public interface WaldotElement extends Element, ObjectNode, UaServerNode {

	public void addComponent(UaNode node);

	public UaMethodNode findMethodNode(NodeId methodId);

	public ByteString getIcon();

	public List<UaMethodNode> getMethodNodes();

	WaldotNamespace getNamespace();

	public String getNodeVersion();

	@Override
	public NodeId id();

	public void removeComponent(UaNode node);

	public void setIcon(ByteString icon);

	public void setNodeVersion(String nodeVersion);

	long version();

}
