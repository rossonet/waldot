package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;

/**
 * WaldotElement is an interface that extends Element and UaServerNode. It
 * represents a node in the Waldot graph model, providing methods to manage
 * components, icons, namespaces, and versioning.
 * 
 * <p>WaldotElement is the base interface for all graph elements (vertices and edges)
 * in WaldOT. It combines TinkerPop's Element interface with OPC UA's UaServerNode
 * to provide unified access to both graph properties and OPC UA node attributes.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Get element properties (from TinkerPop)
 * for (String key : element.keys()) {
 *     Object value = element.property(key).value();
 * }
 * 
 * // Get OPC UA node attributes (from Milo)
 * NodeId nodeId = element.getNodeId();
 * QualifiedName browseName = element.getBrowseName();
 * LocalizedText displayName = element.getDisplayName();
 * 
 * // Get version
 * long version = element.version();
 * 
 * // Get namespace
 * WaldotNamespace ns = element.getNamespace();
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see Element
 * @see org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode
 */
public interface WaldotElement extends Element, UaServerNode {

	/**
	 * Adds a component to this element.
	 * 
	 * <p>Components are child nodes in the OPC UA address space hierarchy.
	 * This is used to build the node tree structure.</p>
	 * 
	 * @param waldotElement the component to add
	 * @see #removeComponent(WaldotElement)
	 */
	void addComponent(WaldotElement waldotElement);

	/**
	 * Returns the icon associated with this element.
	 * 
	 * <p>Icons are stored as ByteString and can be used for UI representation.</p>
	 * 
	 * @return the icon ByteString, or null if not set
	 * @see ByteString
	 * @see #setIcon(ByteString)
	 */
	public ByteString getIcon();

	/**
	 * Returns the WaldotNamespace that contains this element.
	 * 
	 * @return the owning WaldotNamespace
	 * @see WaldotNamespace
	 */
	WaldotNamespace getNamespace();

	/**
	 * Returns the node version string.
	 * 
	 * <p>The version is used for caching and change detection in the
	 * OPC UA address space.</p>
	 * 
	 * @return the version string, or null if not set
	 * @see #setNodeVersion(String)
	 */
	public String getNodeVersion();

	/**
	 * Checks if this element has been removed from the graph.
	 * 
	 * @return true if the element is removed, false otherwise
	 */
	boolean isRemoved();

	/**
	 * Removes a component from this element.
	 * 
	 * @param waldotElement the component to remove
	 * @see #addComponent(WaldotElement)
	 */
	void removeComponent(WaldotElement waldotElement);

	/**
	 * Sets the icon for this element.
	 * 
	 * @param icon the ByteString containing icon data
	 * @see #getIcon()
	 */
	public void setIcon(ByteString icon);

	/**
	 * Sets the node version.
	 * 
	 * <p>The version should be incremented whenever the element's
	 * structure or properties change significantly.</p>
	 * 
	 * @param nodeVersion the version string
	 * @see #getNodeVersion()
	 */
	public void setNodeVersion(String nodeVersion);

	/**
	 * Returns the version number of this element.
	 * 
	 * <p>The version is a monotonically increasing number that changes
	 * whenever the element is modified. Useful for caching and
	 * concurrency control.</p>
	 * 
	 * @return the current version number
	 */
	long version();

}
