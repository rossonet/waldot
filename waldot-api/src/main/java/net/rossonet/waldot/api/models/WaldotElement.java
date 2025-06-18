package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;

/**
 * WaldotElement is an interface that extends Element and UaServerNode. It
 * represents a node in the Waldot graph model, providing methods to manage
 * components, icons, namespaces, and versioning.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
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
