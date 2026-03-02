package net.rossonet.waldot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

import net.rossonet.oshi.vertex.OshiCentralProcessorVertex;
import net.rossonet.oshi.vertex.OshiComputerSystemVertex;
import net.rossonet.oshi.vertex.OshiDisplayVertex;
import net.rossonet.oshi.vertex.OshiFileSystemVertex;
import net.rossonet.oshi.vertex.OshiGlobalMemoryVertex;
import net.rossonet.oshi.vertex.OshiGraphicsCardVertex;
import net.rossonet.oshi.vertex.OshiHWDiskStoreVertex;
import net.rossonet.oshi.vertex.OshiLogicalVolumeGroupVertex;
import net.rossonet.oshi.vertex.OshiNetworkIFVertex;
import net.rossonet.oshi.vertex.OshiOSVersionInfoVertex;
import net.rossonet.oshi.vertex.OshiPowerSourceVertex;
import net.rossonet.oshi.vertex.OshiSensorsVertex;
import net.rossonet.oshi.vertex.OshiSoundCardVertex;
import net.rossonet.oshi.vertex.OshiUsbDeviceVertex;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.vertex.OshiInternetProtocolStatsVertex;

/**
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotOsPlugin implements AutoCloseable, PluginListener {
	private static final String CENTRAL_PROCESSOR_DISPLAY_NAME = "Central Processor";
	public static final String CENTRAL_PROCESSOR_TYPE_LABEL = "os:processor";
	private static final String CENTRAL_PROCESSOR_TYPE_NODE_ID = "OshiCentralProcessorObjectType";
	private static final String COMPUTER_DISPLAY_NAME = "Computer System";
	private static final String COMPUTER_TYPE_LABEL = "os:computer";
	private static final String COMPUTER_TYPE_NODE_ID = "OshiComputerSystemObjectType";
	private static final String DISK_STORE_DISPLAY_NAME = "Disk Store";
	private static final String DISK_STORE_TYPE_LABEL = "os:disk";
	private static final String DISK_STORE_TYPE_NODE_ID = "OshiHWDiskStoreObjectType";
	private static final String DISPLAY_DISPLAY_NAME = "Display";
	private static final String DISPLAY_TYPE_LABEL = "os:display";
	private static final String DISPLAY_TYPE_NODE_ID = "OshiDisplayObjectType";
	private static final String FILESYSTEM_DISPLAY_NAME = "File System";
	public static final String FILESYSTEM_TYPE_LABEL = "os:filesystem";
	private static final String FILESYSTEM_TYPE_NODE_ID = "OshiFileSystemObjectType";
	private static final String GLOBAL_MEMORY_DISPLAY_NAME = "Memory";
	public static final String GLOBAL_MEMORY_TYPE_LABEL = "os:memory";
	private static final String GLOBAL_MEMORY_TYPE_NODE_ID = "OshiGlobalMemoryObjectType";
	private static final String GRAPHIC_CARD_DISPLAY_NAME = "Graphic Card";
	private static final String GRAPHIC_CARD_TYPE_LABEL = "os:graphic";
	private static final String GRAPHIC_CARD_TYPE_NODE_ID = "OshiGraphicCardObjectType";
	private static final String INTERNET_PROTO_STATS_DISPLAY_NAME = "Internet Protocol Stats";
	private static final String INTERNET_PROTO_STATS_TYPE_LABEL = "os:protocolstats";
	private static final String INTERNET_PROTO_STATS_TYPE_NODE_ID = "OshiInternetProtocolStatsObjectType";
	private final static Logger logger = LoggerFactory.getLogger(WaldotOsPlugin.class);
	private static final String LOGICAL_VOLUMES_DISPLAY_NAME = "Logical Volume";
	public static final String LOGICAL_VOLUMES_TYPE_LABEL = "os:volume";
	private static final String LOGICAL_VOLUMES_TYPE_NODE_ID = "OshiLogicalVolumeObjectType";
	private static final String NETWORK_DISPLAY_NAME = "Network Interface";
	private static final String NETWORK_TYPE_LABEL = "os:network";
	private static final String NETWORK_TYPE_NODE_ID = "OshiNetworkIFObjectType";
	private static final String OS_VERSION_DISPLAY_NAME = "Operating System Version";
	private static final String OS_VERSION_TYPE_LABEL = "os:version";
	private static final String OS_VERSION_TYPE_NODE_ID = "OshiOSVersionInfoObjectType";
	private static final String POWER_SOURCE_DISPLAY_NAME = "Power Source";
	public static final String POWER_SOURCE_TYPE_LABEL = "os:power";
	private static final String POWER_SOURCE_TYPE_NODE_ID = "OshiPowerSourceObjectType";
	private static final String SENSORS_DISPLAY_NAME = "Sensor";
	public static final String SENSORS_TYPE_LABEL = "os:sensor";
	private static final String SENSORS_TYPE_NODE_ID = "OshiSensorObjectType";
	private static final String SOUND_CARD_DISPLAY_NAME = "Sound Card";
	private static final String SOUND_CARD_TYPE_LABEL = "os:soundcard";
	private static final String SOUND_CARD_TYPE_NODE_ID = "OshiSoundCardObjectType";
	public static final String STATUS_LABEL = "status";
	private static final String USB_DISPLAY_NAME = "USB Device";
	private static final String USB_TYPE_LABEL = "os:usb";
	private static final String USB_TYPE_NODE_ID = "OshiUsbDeviceObjectType";
	private final List<WaldotVertex> monitoredVertices = Collections.synchronizedList(new ArrayList<>());
	private UaObjectTypeNode oshiCentralProcessorTypeNode;
	private UaObjectTypeNode oshiComputerSystemTypeNode;
	private UaObjectTypeNode oshiDisplayTypeNode;
	private UaObjectTypeNode oshiFileSystemTypeNode;
	private UaObjectTypeNode oshiGlobalMemoryTypeNode;
	private UaObjectTypeNode oshiGraphicsCardTypeNode;
	private UaObjectTypeNode oshiHWDiskStoreTypeNode;
	private UaObjectTypeNode oshiInternetProtocolStatsTypeNode;
	private UaObjectTypeNode oshiLogicalVolumeGroupTypeNode;
	private UaObjectTypeNode oshiNetworkIFTypeNode;
	private UaObjectTypeNode oshiOSVersionInfoTypeNode;
	private UaObjectTypeNode oshiPowerSourceTypeNode;
	private UaObjectTypeNode oshiSensorsTypeNode;
	private UaObjectTypeNode oshiSoundCardTypeNode;
	private UaObjectTypeNode oshiUsbDeviceTypeNode;
	protected WaldotNamespace waldotNamespace;

	@Override
	public void close() throws Exception {
		logger.info("WaldotOsPlugin closed");
	}

	@Override
	public boolean containsVertexType(final String typeDefinitionLabel) {
		return FILESYSTEM_TYPE_LABEL.equals(typeDefinitionLabel) || SENSORS_TYPE_LABEL.equals(typeDefinitionLabel)
				|| DISK_STORE_TYPE_LABEL.equals(typeDefinitionLabel) || NETWORK_TYPE_LABEL.equals(typeDefinitionLabel)
				|| DISPLAY_TYPE_LABEL.equals(typeDefinitionLabel) || USB_TYPE_LABEL.equals(typeDefinitionLabel)
				|| SOUND_CARD_TYPE_LABEL.equals(typeDefinitionLabel)
				|| GRAPHIC_CARD_TYPE_LABEL.equals(typeDefinitionLabel)
				|| INTERNET_PROTO_STATS_TYPE_LABEL.equals(typeDefinitionLabel)
				|| OS_VERSION_TYPE_LABEL.equals(typeDefinitionLabel) || COMPUTER_TYPE_LABEL.equals(typeDefinitionLabel)
				|| LOGICAL_VOLUMES_TYPE_LABEL.equals(typeDefinitionLabel)
				|| POWER_SOURCE_TYPE_LABEL.equals(typeDefinitionLabel)
				|| GLOBAL_MEMORY_TYPE_LABEL.equals(typeDefinitionLabel)
				|| CENTRAL_PROCESSOR_TYPE_LABEL.equals(typeDefinitionLabel);
	}

	@Override
	public boolean containsVertexTypeNode(final NodeId typeDefinitionNodeId) {
		return oshiGlobalMemoryTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiSensorsTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiPowerSourceTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiCentralProcessorTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiFileSystemTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiLogicalVolumeGroupTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiComputerSystemTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiDisplayTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiGraphicsCardTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiHWDiskStoreTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiInternetProtocolStatsTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiNetworkIFTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiOSVersionInfoTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiSoundCardTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| oshiUsbDeviceTypeNode.getNodeId().equals(typeDefinitionNodeId);
	}

	private WaldotVertex createOshiCentralProcessorVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiCentralProcessorVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiComputerSystemVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiComputerSystemVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiDisplayVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiDisplayVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiFileSystemVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiFileSystemVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiGlobalMemoryVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiGlobalMemoryVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiGraphicsCardVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiGraphicsCardVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiHWDiskStoreVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiHWDiskStoreVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiInternetProtocolStatsVertexObject(final WaldotGraph graph,
			final UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			final LocalizedText displayName, final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask, final UByte eventNotifier, final long version,
			final Object[] propertyKeyValues) {
		return new OshiInternetProtocolStatsVertex(graph, context, nodeId, browseName, displayName, description,
				writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiLogicalVolumeGroupVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiLogicalVolumeGroupVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiNetworkIFVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiNetworkIFVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiOSVersionInfoVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiOSVersionInfoVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiPowerSourceVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiPowerSourceVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiSensorsVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiSensorsVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiSoundCardVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiSoundCardVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	private WaldotVertex createOshiUsbDeviceVertexObject(final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final Object[] propertyKeyValues) {
		return new OshiUsbDeviceVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	@Override
	public WaldotVertex createVertex(final NodeId typeDefinitionNodeId, final WaldotGraph graph,
			final UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			final LocalizedText displayName, final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask, final UByte eventNotifier, final long version,
			final Object[] propertyKeyValues) {
		if (!containsVertexTypeNode(typeDefinitionNodeId)) {
			logger.warn("TypeDefinitionNodeId: {} not managed by WaldotOsPlugin", typeDefinitionNodeId);
			return null;
		}
		WaldotVertex vertexObject = null;
		if (oshiSensorsTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiSensorsVertexObject(graph, context, nodeId, browseName, displayName, description,
					writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiFileSystemTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiFileSystemVertexObject(graph, context, nodeId, browseName, displayName,
					description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiUsbDeviceTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiUsbDeviceVertexObject(graph, context, nodeId, browseName, displayName, description,
					writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiSoundCardTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiSoundCardVertexObject(graph, context, nodeId, browseName, displayName, description,
					writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiOSVersionInfoTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiOSVersionInfoVertexObject(graph, context, nodeId, browseName, displayName,
					description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiNetworkIFTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiNetworkIFVertexObject(graph, context, nodeId, browseName, displayName, description,
					writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiInternetProtocolStatsTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiInternetProtocolStatsVertexObject(graph, context, nodeId, browseName, displayName,
					description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiHWDiskStoreTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiHWDiskStoreVertexObject(graph, context, nodeId, browseName, displayName,
					description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiGraphicsCardTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiGraphicsCardVertexObject(graph, context, nodeId, browseName, displayName,
					description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiComputerSystemTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiComputerSystemVertexObject(graph, context, nodeId, browseName, displayName,
					description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiDisplayTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiDisplayVertexObject(graph, context, nodeId, browseName, displayName, description,
					writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiGlobalMemoryTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiGlobalMemoryVertexObject(graph, context, nodeId, browseName, displayName,
					description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiPowerSourceTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiPowerSourceVertexObject(graph, context, nodeId, browseName, displayName,
					description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiLogicalVolumeGroupTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiLogicalVolumeGroupVertexObject(graph, context, nodeId, browseName, displayName,
					description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (oshiCentralProcessorTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			vertexObject = createOshiCentralProcessorVertexObject(graph, context, nodeId, browseName, displayName,
					description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		}
		if (vertexObject == null) {
			logger.warn("Vertex object is null for typeDefinitionNodeId: {}", typeDefinitionNodeId);
			return null;
		} else {
			logger.trace("Vertex object created: {} for typeDefinitionNodeId: {}", vertexObject, typeDefinitionNodeId);
			monitoredVertices.add(vertexObject);
			return vertexObject;
		}
	}

	private void generateOshiCentralProcessorTypeNode() {
		oshiCentralProcessorTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + CENTRAL_PROCESSOR_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(CENTRAL_PROCESSOR_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(CENTRAL_PROCESSOR_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiCentralProcessorTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiCentralProcessorVertex.generateParameters(waldotNamespace, oshiCentralProcessorTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiCentralProcessorTypeNode);
		oshiCentralProcessorTypeNode.addReference(new Reference(oshiCentralProcessorTypeNode.getNodeId(),
				NodeIds.HasSubtype, NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiCentralProcessorTypeNode.getNodeId(),
				UaObjectNode.class, objectNodeConstructor);

	}

	private void generateOshiComputerSystemTypeNode() {
		oshiComputerSystemTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + COMPUTER_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(COMPUTER_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(COMPUTER_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiComputerSystemTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiComputerSystemVertex.generateParameters(waldotNamespace, oshiComputerSystemTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiComputerSystemTypeNode);
		oshiComputerSystemTypeNode.addReference(new Reference(oshiComputerSystemTypeNode.getNodeId(),
				NodeIds.HasSubtype, NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiComputerSystemTypeNode.getNodeId(),
				UaObjectNode.class, objectNodeConstructor);

	}

	private void generateOshiDisplayTypeNode() {
		oshiDisplayTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + DISPLAY_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(DISPLAY_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(DISPLAY_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiDisplayTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiDisplayVertex.generateParameters(waldotNamespace, oshiDisplayTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiDisplayTypeNode);
		oshiDisplayTypeNode.addReference(new Reference(oshiDisplayTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiDisplayTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);

	}

	private void generateOshiFileSystemTypeNode() {
		oshiFileSystemTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + FILESYSTEM_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(FILESYSTEM_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(FILESYSTEM_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiFileSystemTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiFileSystemVertex.generateParameters(waldotNamespace, oshiFileSystemTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiFileSystemTypeNode);
		oshiFileSystemTypeNode.addReference(new Reference(oshiFileSystemTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiFileSystemTypeNode.getNodeId(),
				UaObjectNode.class, objectNodeConstructor);
	}

	private void generateOshiGlobalMemoryTypeNode() {
		oshiGlobalMemoryTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + GLOBAL_MEMORY_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(GLOBAL_MEMORY_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(GLOBAL_MEMORY_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiGlobalMemoryTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiGlobalMemoryVertex.generateParameters(waldotNamespace, oshiGlobalMemoryTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiGlobalMemoryTypeNode);
		oshiGlobalMemoryTypeNode.addReference(new Reference(oshiGlobalMemoryTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiGlobalMemoryTypeNode.getNodeId(),
				UaObjectNode.class, objectNodeConstructor);

	}

	private void generateOshiGraphicsCardTypeNode() {
		oshiGraphicsCardTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + GRAPHIC_CARD_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(GRAPHIC_CARD_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(GRAPHIC_CARD_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiGraphicsCardTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiGraphicsCardVertex.generateParameters(waldotNamespace, oshiGraphicsCardTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiGraphicsCardTypeNode);
		oshiGraphicsCardTypeNode.addReference(new Reference(oshiGraphicsCardTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiGraphicsCardTypeNode.getNodeId(),
				UaObjectNode.class, objectNodeConstructor);

	}

	private void generateOshiHWDiskStoreTypeNode() {
		oshiHWDiskStoreTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + DISK_STORE_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(DISK_STORE_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(DISK_STORE_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiHWDiskStoreTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiHWDiskStoreVertex.generateParameters(waldotNamespace, oshiHWDiskStoreTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiHWDiskStoreTypeNode);
		oshiHWDiskStoreTypeNode.addReference(new Reference(oshiHWDiskStoreTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiHWDiskStoreTypeNode.getNodeId(),
				UaObjectNode.class, objectNodeConstructor);

	}

	private void generateOshiInternetProtocolStatsTypeNode() {
		oshiInternetProtocolStatsTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + INTERNET_PROTO_STATS_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(INTERNET_PROTO_STATS_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(INTERNET_PROTO_STATS_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiInternetProtocolStatsTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiInternetProtocolStatsVertex.generateParameters(waldotNamespace, oshiInternetProtocolStatsTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiInternetProtocolStatsTypeNode);
		oshiInternetProtocolStatsTypeNode.addReference(new Reference(oshiInternetProtocolStatsTypeNode.getNodeId(),
				NodeIds.HasSubtype, NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiInternetProtocolStatsTypeNode.getNodeId(),
				UaObjectNode.class, objectNodeConstructor);

	}

	private void generateOshiLogicalVolumeGroupTypeNode() {
		oshiLogicalVolumeGroupTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + LOGICAL_VOLUMES_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(LOGICAL_VOLUMES_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(LOGICAL_VOLUMES_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiLogicalVolumeGroupTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiLogicalVolumeGroupVertex.generateParameters(waldotNamespace, oshiLogicalVolumeGroupTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiLogicalVolumeGroupTypeNode);
		oshiLogicalVolumeGroupTypeNode.addReference(new Reference(oshiLogicalVolumeGroupTypeNode.getNodeId(),
				NodeIds.HasSubtype, NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiLogicalVolumeGroupTypeNode.getNodeId(),
				UaObjectNode.class, objectNodeConstructor);
	}

	private void generateOshiNetworkIFTypeNode() {
		oshiNetworkIFTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + NETWORK_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(NETWORK_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(NETWORK_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiNetworkIFTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiNetworkIFVertex.generateParameters(waldotNamespace, oshiNetworkIFTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiNetworkIFTypeNode);
		oshiNetworkIFTypeNode.addReference(new Reference(oshiNetworkIFTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiNetworkIFTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);

	}

	private void generateOshiPowerSourceTypeNode() {
		oshiPowerSourceTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + POWER_SOURCE_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(POWER_SOURCE_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(POWER_SOURCE_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiPowerSourceTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiPowerSourceVertex.generateParameters(waldotNamespace, oshiPowerSourceTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiPowerSourceTypeNode);
		oshiPowerSourceTypeNode.addReference(new Reference(oshiPowerSourceTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiPowerSourceTypeNode.getNodeId(),
				UaObjectNode.class, objectNodeConstructor);

	}

	private void generateOshiSensorsTypeNode() {
		oshiSensorsTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + SENSORS_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(SENSORS_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(SENSORS_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiSensorsTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiSensorsVertex.generateParameters(waldotNamespace, oshiSensorsTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiSensorsTypeNode);
		oshiSensorsTypeNode.addReference(new Reference(oshiSensorsTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiSensorsTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);
	}

	private void generateOshiSoundCardTypeNode() {
		oshiSoundCardTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + SOUND_CARD_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(SOUND_CARD_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(SOUND_CARD_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiSoundCardTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiSoundCardVertex.generateParameters(waldotNamespace, oshiSoundCardTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiSoundCardTypeNode);
		oshiSoundCardTypeNode.addReference(new Reference(oshiSoundCardTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiSoundCardTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);

	}

	private void generateOshiUsbDeviceTypeNode() {
		oshiUsbDeviceTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + USB_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(USB_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(USB_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiUsbDeviceTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiUsbDeviceVertex.generateParameters(waldotNamespace, oshiUsbDeviceTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiUsbDeviceTypeNode);
		oshiUsbDeviceTypeNode.addReference(new Reference(oshiUsbDeviceTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiUsbDeviceTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);

	}

	private void generateOSVersionInfoTypeNode() {
		oshiOSVersionInfoTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + OS_VERSION_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(OS_VERSION_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(OS_VERSION_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, oshiOSVersionInfoTypeNode,
				MiloStrategy.LABEL_FIELD.toLowerCase(), NodeIds.String);
		OshiOSVersionInfoVertex.generateParameters(waldotNamespace, oshiOSVersionInfoTypeNode);
		waldotNamespace.getStorageManager().addNode(oshiOSVersionInfoTypeNode);
		oshiOSVersionInfoTypeNode.addReference(new Reference(oshiOSVersionInfoTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(oshiOSVersionInfoTypeNode.getNodeId(),
				UaObjectNode.class, objectNodeConstructor);

	}

	@Override
	public NodeId getVertexTypeNode(final String typeDefinitionLabel) {
		switch (typeDefinitionLabel) {
		case GLOBAL_MEMORY_TYPE_LABEL:
			return oshiGlobalMemoryTypeNode.getNodeId();
		case SENSORS_TYPE_LABEL:
			return oshiSensorsTypeNode.getNodeId();
		case POWER_SOURCE_TYPE_LABEL:
			return oshiPowerSourceTypeNode.getNodeId();
		case CENTRAL_PROCESSOR_TYPE_LABEL:
			return oshiCentralProcessorTypeNode.getNodeId();
		case FILESYSTEM_TYPE_LABEL:
			return oshiFileSystemTypeNode.getNodeId();
		case LOGICAL_VOLUMES_TYPE_LABEL:
			return oshiLogicalVolumeGroupTypeNode.getNodeId();
		case NETWORK_TYPE_LABEL:
			return oshiNetworkIFTypeNode.getNodeId();
		case OS_VERSION_TYPE_LABEL:
			return oshiOSVersionInfoTypeNode.getNodeId();
		case SOUND_CARD_TYPE_LABEL:
			return oshiSoundCardTypeNode.getNodeId();
		case GRAPHIC_CARD_TYPE_LABEL:
			return oshiGraphicsCardTypeNode.getNodeId();
		case DISK_STORE_TYPE_LABEL:
			return oshiHWDiskStoreTypeNode.getNodeId();
		case COMPUTER_TYPE_LABEL:
			return oshiComputerSystemTypeNode.getNodeId();
		case DISPLAY_TYPE_LABEL:
			return oshiDisplayTypeNode.getNodeId();
		case USB_TYPE_LABEL:
			return oshiUsbDeviceTypeNode.getNodeId();
		case INTERNET_PROTO_STATS_TYPE_LABEL:
			return oshiInternetProtocolStatsTypeNode.getNodeId();
		default:
			logger.warn("TypeDefinitionLabel: {} not managed by WaldotOsPlugin", typeDefinitionLabel);
			return null;
		}
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		generateOshiSensorsTypeNode();
		generateOshiLogicalVolumeGroupTypeNode();
		generateOshiCentralProcessorTypeNode();
		generateOshiPowerSourceTypeNode();
		generateOshiGlobalMemoryTypeNode();
		generateOshiFileSystemTypeNode();
		generateOshiNetworkIFTypeNode();
		generateOshiInternetProtocolStatsTypeNode();
		generateOshiHWDiskStoreTypeNode();
		generateOshiGraphicsCardTypeNode();
		generateOshiComputerSystemTypeNode();
		generateOshiDisplayTypeNode();
		generateOshiSoundCardTypeNode();
		generateOshiUsbDeviceTypeNode();
		generateOSVersionInfoTypeNode();
		waldotNamespace.getOpcuaServer().updateReferenceTypeTree();
	}

}
