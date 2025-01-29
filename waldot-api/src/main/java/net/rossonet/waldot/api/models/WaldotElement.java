package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;

public interface WaldotElement extends Element, UaServerNode {

	void addComponent(WaldotElement waldotElement);

	public ByteString getIcon();

	WaldotNamespace getNamespace();

	public String getNodeVersion();

	boolean isRemoved();

	void removeComponent(WaldotElement waldotElement);

	public void setIcon(ByteString icon);

	public void setNodeVersion(String nodeVersion);

	long version();

}
