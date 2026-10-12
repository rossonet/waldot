package net.rossonet.waldot.plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcVertex;

public abstract class AbstractPluginListener implements PluginListener {
	protected final List<WaldotCommand> commands = new ArrayList<>();
	protected final Map<String, UaObjectTypeNode> labelTypeNodes = new HashMap<>();
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final Map<UaObjectTypeNode, Class<? extends AbstractOpcVertex>> typeClassNodes = new HashMap<>();
	private WaldotNamespace waldotNamespace;

	@Override
	public boolean containsVertexType(String typeDefinitionLabel) {
		return labelTypeNodes.containsKey(typeDefinitionLabel);
	}

	@Override
	public boolean containsVertexTypeNode(NodeId typeDefinitionNodeId) {
		boolean result = false;
		for (final UaObjectTypeNode typeNode : typeClassNodes.keySet()) {
			if (typeNode.getNodeId().equals(typeDefinitionNodeId)) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public WaldotVertex createVertex(NodeId typeDefinitionNodeId,
			WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName,
			LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version,
			Object[] propertyKeyValues) {
		if (!containsVertexTypeNode(typeDefinitionNodeId)) {
			logger.warn("TypeDefinitionNodeId: {} not managed",
					typeDefinitionNodeId);
			return null;
		}
		for (final Map.Entry<UaObjectTypeNode, Class<? extends AbstractOpcVertex>> entry : typeClassNodes
				.entrySet()) {
			if (entry.getKey().getNodeId().equals(typeDefinitionNodeId)) {
				try {
					final Constructor<? extends AbstractOpcVertex> builder = entry
							.getValue().getConstructor(WaldotGraph.class,
									UaNodeContext.class, NodeId.class,
									QualifiedName.class, LocalizedText.class,
									LocalizedText.class, UInteger.class,
									UInteger.class, UByte.class, long.class,
									Object[].class);
					final AbstractOpcVertex newInstance = builder.newInstance(
							graph, context, nodeId, browseName, displayName,
							description, writeMask, userWriteMask,
							eventNotifier, version, propertyKeyValues);
					// managedObjects.add(newInstance);
					return newInstance;
				} catch (final Throwable e) {
					logger.error(
							"Error creating vertex for typeDefinitionNodeId: {}",
							typeDefinitionNodeId, e);
				}
			}
		}
		logger.warn("TypeDefinitionNodeId: {} not managed",
				typeDefinitionNodeId);
		return null;
	}
	private UaObjectTypeNode generateTypeNode(Class<?> vertexClass,
			String typeNodeId, String typeNodeDisplayName) {
		final UaObjectTypeNode type = UaObjectTypeNode
				.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace
						.generateNodeId(OBJECT_TYPES + typeNodeId))
				.setBrowseName(
						waldotNamespace.generateQualifiedName(typeNodeId))
				.setDisplayName(LocalizedText.english(typeNodeDisplayName))
				.setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, type,
				MiloStrategy.LABEL_FIELD, NodeIds.String);
		try {
			final Method method = vertexClass.getMethod("generateParameters",
					WaldotNamespace.class, UaObjectTypeNode.class);
			method.invoke(null, waldotNamespace, type);
		} catch (final Exception e) {
			logger.error("Error invoking generateParameters for class "
					+ vertexClass.getName(), e);
		}
		waldotNamespace.getStorageManager().addNode(type);
		type.addReference(new Reference(type.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(
				type.getNodeId(), UaObjectNode.class, objectNodeConstructor);
		return type;
	}

	@Override
	public Collection<WaldotCommand> getCommands() {
		return commands;
	}

	@Override
	public NodeId getVertexTypeNode(String typeDefinitionLabel) {
		if (labelTypeNodes.containsKey(typeDefinitionLabel)) {
			return labelTypeNodes.get(typeDefinitionLabel).getNodeId();
		} else {
			logger.warn("TypeDefinitionLabel: {} not managed",
					typeDefinitionLabel);
			return null;
		}
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.typeClassNodes.clear();
		this.labelTypeNodes.clear();
		this.waldotNamespace = waldotNamespace;
	}

	protected void registerCommand(WaldotCommand command) {
		commands.add(command);
	}

	protected void registerTypeNode(
			Class<? extends AbstractOpcVertex> vertexClass, String typeNodeId,
			String typeNodeDisplayName, String label) {
		final UaObjectTypeNode typeNode = generateTypeNode(vertexClass,
				typeNodeId, typeNodeDisplayName);
		labelTypeNodes.put(label, typeNode);
		typeClassNodes.put(typeNode, vertexClass);
	}
}
