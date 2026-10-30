package net.rossonet.waldot.api;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.ObjectTypeManager.ObjectNodeConstructor;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.AccessRestrictionType;
import org.eclipse.milo.opcua.stack.core.types.structured.RolePermissionType;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.exception.PluginBlockingException;

/**
 * PluginListener interface for handling events related to Waldot plugins.
 * Implementations of this interface should define how to handle plugin
 * initialization, vertex creation, command retrieval, and rule functions.
 * 
 * <p>This is the main extension point for WaldOT plugins. Plugins that implement
 * this interface can register custom vertex types, edge types, commands, and
 * event handlers within the WaldOT OPC UA address space and TinkerPop graph.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * @WaldotPlugin
 * public class MyPlugin implements AutoCloseable, PluginListener {
 *     @Override
 *     public void initialize(WaldotNamespace waldotNamespace) {
 *         // Register custom vertex types using addParameterToTypeNode()
 *         // Initialize plugin resources
 *     }
 *     
 *     @Override
 *     public boolean containsVertexType(String typeDefinitionLabel) {
 *         return "my:customType".equals(typeDefinitionLabel);
 *     }
 *     
 *     @Override
 *     public WaldotVertex createVertex(NodeId typeDefinitionNodeId, ...) {
 *         // Create and return custom vertex instances
 *         return new MyCustomVertex(...);
 *     }
 * }
 * }</pre>
 *
 * <h2>Lifecycle</h2>
 * <p>The namespace drives each plugin instance (one instance per namespace)
 * through these calls:</p>
 * <ol>
 *   <li>{@link #initialize(WaldotNamespace)}: once, when the plugin is
 *   registered;</li>
 *   <li>{@link #start()}: when the bootstrap procedure runs, at startup and
 *   again after every reset;</li>
 *   <li>{@link #stop()}: a <b>restartable pause</b>, called before
 *   {@link #reset()} (by {@code WaldotNamespace.resetNameSpace()}) and by
 *   {@code WaldotNamespace.unregisterPlugin(...)}. The plugin must stop its
 *   background work but keep its resources (executors, connections), because
 *   {@link #start()} may follow;</li>
 *   <li>{@code close()}: the <b>definitive release</b>. A plugin that owns
 *   resources (threads, executors, sockets, static references) implements
 *   {@link AutoCloseable}; the namespace calls {@code close()} exactly once,
 *   when the namespace itself is closed, always <i>after</i> {@link #stop()}
 *   and after the {@link AutoCloseable} vertices of the graph have been
 *   closed. After {@code close()} the plugin is never started again.</li>
 * </ol>
 * <p>Both {@link #stop()} and {@code close()} must not throw when called
 * after a failed startup and must tolerate a second call: the namespace logs
 * any exception and goes on with the shutdown of the other plugins.</p>
 *
 * @author Andrea Ambrosini - Zona Autonoma srl
 */
public interface PluginListener {

	public final class MetaDataTypeNode {

		private UaVariableNode uaVariableNode;

		public UaVariableNode getUaVariableNode() {
			return uaVariableNode;
		}

		void setUaVariableNode(UaVariableNode uaVariableNode) {
			this.uaVariableNode = uaVariableNode;

		}
	}
	public final static String OBJECT_TYPES = "ObjectTypes/";

	public final static ObjectNodeConstructor objectNodeConstructor = new ObjectNodeConstructor() {

		@Override
		public UaObjectNode apply(UaNodeContext context, NodeId nodeId,
				QualifiedName browseName, LocalizedText displayName,
				LocalizedText description, UInteger writeMask,
				UInteger userWriteMask, RolePermissionType[] rolePermissions,
				RolePermissionType[] userRolePermissions,
				AccessRestrictionType accessRestrictions) {
			return new UaObjectNode(context, nodeId, browseName, displayName,
					description, writeMask, userWriteMask, rolePermissions,
					userRolePermissions, accessRestrictions);
		}

	};

	private static <DATA_TYPE> MetaDataTypeNode addParameter(
			WaldotNamespace waldotNamespace, UaObjectTypeNode typeNode,
			String variableId, NodeId dataType) {
		final MetaDataTypeNode metaDataVariableNode = new MetaDataTypeNode();
		final UaVariableNode variable = new UaVariableNode.UaVariableNodeBuilder(
				waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace
						.generateNodeId(typeNode.getNodeId().toParseableString()
								+ "." + variableId))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(
						waldotNamespace.generateQualifiedName(variableId))
				.setDisplayName(LocalizedText.english(variableId))
				.setDataType(dataType)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		variable.addReference(
				new Reference(variable.getNodeId(), NodeIds.HasModellingRule,
						NodeIds.ModellingRule_Mandatory.expanded(), true));
		variable.setValue(new DataValue(new Variant("NaN")));
		typeNode.addComponent(variable);
		waldotNamespace.getStorageManager().addNode(variable);
		metaDataVariableNode.setUaVariableNode(variable);
		return metaDataVariableNode;
	}

	private static <DATA_TYPE> MetaDataTypeNode addParameter(
			WaldotNamespace waldotNamespace, UaObjectTypeNode typeNode,
			String variableId, NodeId dataType, String description) {
		final MetaDataTypeNode metaDataVariableNode = new MetaDataTypeNode();
		final UaVariableNode variable = new UaVariableNode.UaVariableNodeBuilder(
				waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace
						.generateNodeId(typeNode.getNodeId().toParseableString()
								+ "." + variableId))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(
						waldotNamespace.generateQualifiedName(variableId))
				.setDisplayName(LocalizedText.english(variableId))
				.setDescription(LocalizedText.english(description))
				.setDataType(dataType)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		variable.addReference(
				new Reference(variable.getNodeId(), NodeIds.HasModellingRule,
						NodeIds.ModellingRule_Mandatory.expanded(), true));
		variable.setValue(new DataValue(new Variant("NaN")));
		typeNode.addComponent(variable);
		waldotNamespace.getStorageManager().addNode(variable);
		metaDataVariableNode.setUaVariableNode(variable);
		return metaDataVariableNode;
	}

	private static <DATA_TYPE> MetaDataTypeNode addParameter(
			WaldotNamespace waldotNamespace, UaObjectTypeNode typeNode,
			String variableId, NodeId dataType, UInteger[] arrayDimensions) {
		final MetaDataTypeNode metaDataVariableNode = new MetaDataTypeNode();
		final UaVariableNode variable = new UaVariableNode.UaVariableNodeBuilder(
				waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace
						.generateNodeId(typeNode.getNodeId().toParseableString()
								+ "." + variableId))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(
						waldotNamespace.generateQualifiedName(variableId))
				.setDisplayName(LocalizedText.english(variableId))
				.setDataType(dataType)
				.setTypeDefinition(NodeIds.BaseDataVariableType)
				.setArrayDimensions(arrayDimensions).build();
		variable.addReference(
				new Reference(variable.getNodeId(), NodeIds.HasModellingRule,
						NodeIds.ModellingRule_Mandatory.expanded(), true));
		variable.setValue(new DataValue(new Variant("NaN")));
		typeNode.addComponent(variable);
		waldotNamespace.getStorageManager().addNode(variable);
		metaDataVariableNode.setUaVariableNode(variable);
		return metaDataVariableNode;
	}

	public static MetaDataTypeNode addParameterToTypeNode(
			WaldotNamespace waldotNamespace, UaObjectTypeNode typeNode,
			String variableId, NodeId dataType) {
		return addParameter(waldotNamespace, typeNode, variableId, dataType);
	}

	public static <DATA_TYPE> MetaDataTypeNode addParameterToTypeNode(
			WaldotNamespace waldotNamespace, UaObjectTypeNode typeNode,
			String variableId, NodeId dataType, String description) {
		return addParameter(waldotNamespace, typeNode, variableId, dataType,
				description);
	}

	public static <DATA_TYPE> MetaDataTypeNode addParameterToTypeNode(
			WaldotNamespace waldotNamespace, UaObjectTypeNode typeNode,
			String variableId, NodeId dataType, UInteger[] arrayDimensions) {
		return addParameter(waldotNamespace, typeNode, variableId, dataType,
				arrayDimensions);
	}

	/**
	 * Checks if this plugin handles the given edge type label.
	 * 
	 * <p>This method is called during graph traversal to determine if this plugin
	 * can process edges of the specified type. Plugins should return true if they
	 * recognize and can handle the given type definition label.</p>
	 * 
	 * @param typeDefinitionLabel the edge type label to check (e.g., "my:customEdge")
	 * @return true if this plugin handles the edge type, false otherwise
	 * @see WaldotEdge
	 */
	default boolean containsEdgeType(String typeDefinitionLabel) {
		return false;
	}

	/**
	 * Checks if this plugin handles the given vertex type label.
	 * 
	 * <p>This method is called during vertex creation to determine if this plugin
	 * can create vertices of the specified type. Plugins should return true if they
	 * recognize the type definition label and can instantiate the corresponding vertex.</p>
	 * 
	 * @param typeDefinitionLabel the vertex type label to check (e.g., "os:processor")
	 * @return true if this plugin handles the vertex type, false otherwise
	 * @see WaldotVertex
	 */
	default boolean containsVertexType(final String typeDefinitionLabel) {
		return false;
	}

	/**
	 * Checks if this plugin handles the given vertex type node ID.
	 * 
	 * <p>This method is called during OPC UA node instantiation to determine if 
	 * this plugin can create vertices of the specified type node. The NodeId typically
	 * corresponds to an ObjectType node in the OPC UA address space.</p>
	 * 
	 * @param typeDefinition the NodeId of the type definition to check
	 * @return true if this plugin handles the vertex type node, false otherwise
	 * @see NodeId
	 */
	default boolean containsVertexTypeNode(final NodeId typeDefinition) {
		return false;
	}

	/**
	 * Creates a new vertex instance of the specified type.
	 * 
	 * <p>This method is called when the system needs to instantiate a new vertex in
	 * the graph. The plugin should check the typeDefinitionNodeId and create an
	 * appropriate vertex implementation. This is the core method for plugin-based
	 * vertex creation in WaldOT.</p>
	 * 
	 * <p>Example implementation from waldot-plugin-os:</p>
	 * <pre>{@code
	 * @Override
	 * public WaldotVertex createVertex(NodeId typeDefinitionNodeId, WaldotGraph graph,
	 *         UaNodeContext context, NodeId nodeId, QualifiedName browseName,
	 *         LocalizedText displayName, LocalizedText description, UInteger writeMask,
	 *         UInteger userWriteMask, UByte eventNotifier, long version,
	 *         Object[] propertyKeyValues) {
	 *     if (oshiSensorsTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
	 *         return createOshiSensorsVertexObject(graph, context, nodeId, browseName,
	 *                 displayName, description, writeMask, userWriteMask, eventNotifier,
	 *                 version, propertyKeyValues);
	 *     }
	 *     // ... handle other types
	 *     return null;
	 * }
	 * }</pre>
	 * 
	 * @param typeDefinitionNodeId the NodeId of the type definition (ObjectType)
	 * @param graph the WaldotGraph instance
	 * @param context the OPC UA node context
	 * @param nodeId the NodeId for the new vertex
	 * @param browseName the browse name for the vertex
	 * @param displayName the display name for the vertex
	 * @param description the description for the vertex
	 * @param writeMask the write mask for access control
	 * @param userWriteMask the user-specific write mask
	 * @param eventNotifier the event notifier flag
	 * @param version the version of the vertex
	 * @param propertyKeyValues initial property key-value pairs
	 * @return a new WaldotVertex instance, or null if type not handled
	 * @see WaldotVertex
	 * @see WaldotGraph
	 */
	default WaldotVertex createVertex(final NodeId typeDefinitionNodeId,
			final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName,
			final LocalizedText displayName, final LocalizedText description,
			final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version,
			Object[] propertyKeyValues) {
		return null;
	}

	/**
	 * Returns the collection of commands provided by this plugin.
	 * 
	 * <p>Commands are executable operations that can be invoked through the
	 * WaldOT console or OPC UA method calls. Each WaldotCommand represents a
	 * callable method with input/output arguments.</p>
	 * 
	 * @return a collection of WaldotCommand instances, empty by default
	 * @see WaldotCommand
	 */
	default Collection<WaldotCommand> getCommands() {
		return Collections.emptyList();
	}

	/**
	 * Gets the NodeId for a given vertex type label.
	 * 
	 * <p>This method maps a human-readable type label (like "os:processor") to
	 * the corresponding OPC UA NodeId. Used during vertex instantiation to
	 * determine which plugin should handle the creation.</p>
	 * 
	 * @param typeDefinitionLabel the vertex type label
	 * @return the NodeId for the type, or null if not found
	 * @see NodeId
	 */
	default NodeId getVertexTypeNode(final String typeDefinitionLabel) {
		return null;
	}

	/**
	 * Initializes the plugin with the given WaldotNamespace.
	 * 
	 * <p>This is the primary initialization method called when the plugin is
	 * registered with the WaldOT system. Implementations should:</p>
	 * <ul>
	 *   <li>Store reference to the WaldotNamespace for later use</li>
	 *   <li>Register custom OPC UA type nodes using addParameterToTypeNode()</li>
	 *   <li>Initialize any required resources</li>
	 *   <li>Register any custom commands via waldotNamespace.registerCommand()</li>
	 * </ul>
	 * 
	 * <p>Example from waldot-plugin-os:</p>
	 * <pre>{@code
	 * @Override
	 * public void initialize(WaldotNamespace waldotNamespace) {
	 *     this.waldotNamespace = waldotNamespace;
	 *     generateOshiSensorsTypeNode();
	 *     generateOshiCentralProcessorTypeNode();
	 *     // ... generate other type nodes
	 *     waldotNamespace.getOpcuaServer().updateReferenceTypeTree();
	 * }
	 * }</pre>
	 * 
	 * @param waldotNamespace the WaldotNamespace instance to use for registration
	 * @see WaldotNamespace
	 */
	void initialize(WaldotNamespace waldotNamespace);

	/**
	 * Notifies the plugin that a new edge has been added to the graph.
	 * 
	 * <p>This callback is invoked after an edge is created in the graph.
	 * Plugins can use this to perform additional setup, validation, or
	 * monitoring of edge creation.</p>
	 *
	 * @param edge the newly created WaldotEdge
	 * @param sourceVertex the source vertex of the edge
	 * @param targetVertex the target vertex of the edge
	 * @param label the edge label
	 * @param type the edge type
	 * @param propertyKeyValues initial property key-value pairs
	 * @see WaldotEdge
	 * @see WaldotVertex
	 */
	default void notifyAddEdge(WaldotEdge edge, WaldotVertex sourceVertex,
			WaldotVertex targetVertex, String label, String type,
			Object[] propertyKeyValues) throws PluginBlockingException {
	}

	/**
	 * Notifies the plugin that an edge has been removed from the graph.
	 * 
	 * <p>This callback is invoked after an edge is removed from the graph.
	 * Plugins can use this to perform cleanup or update internal state.</p>
	 * 
	 * @param edge the removed WaldotEdge
	 * @see WaldotEdge
	 */
	default void notifyRemoveEdge(WaldotEdge edge) {
	}

	/**
	 * Resets the plugin to its initial state.
	 * 
	 * <p>This method is called to reset all plugin state without fully
	 * closing and reinitializing. Useful for testing or recovery scenarios.</p>
	 */
	default void reset() {

	}

	/**
	 * Starts the plugin after initialization.
	 * 
	 * <p>This method is called after initialize() to begin plugin operation.
	 * Implementations should start any background tasks, monitoring, or
	 * event processing.</p>
	 */
	default void start() {

	}

	/**
	 * Pauses the plugin.
	 *
	 * <p>Called before {@link #reset()}, when the plugin is unregistered and,
	 * as first step, when the namespace is closed. Implementations stop their
	 * background tasks but keep the resources needed by a following
	 * {@link #start()}: the definitive release belongs to {@code close()} of
	 * the plugins that implement {@link AutoCloseable} (see the lifecycle in
	 * the class documentation). Must be idempotent and must not throw if the
	 * plugin was never started.</p>
	 *
	 * <p>The default implementation does nothing.</p>
	 *
	 * @see #start()
	 */
	default void stop() {

	};

}
