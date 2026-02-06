package net.rossonet.waldot;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.EnumUtils;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex.Algorithm;
import net.rossonet.waldot.utils.ThreadHelper;

/**
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotGeneratorPlugin implements AutoCloseable, PluginListener {
	public static final String ALGORITHM_FIELD = "Algorithm";
	private static final String CRONTAB_FIELD = "scheduling";
	public static final String DATA_GENERATOR_OBJECT_TYPE_LABEL = "generator";
	private static final String DEFAULT_ALGORITHM_FIELD = Algorithm.incremental.toString();
	private static final String DEFAULT_CRONTAB_FIELD = "0 0/5 * * *"; // Every 5 minutes
	private static final Long DEFAULT_DELAY_FIELD = 1000L;
	private static final Long DEFAULT_MAX_VALUE_FIELD = 20000L;
	private static final Long DEFAULT_MIN_VALUE_FIELD = 0L;
	public static final String DELAY_FIELD = "Delay";
	public static boolean ENABLE_EXEC_COMMAND = false;
	private final static ExecutorService executor = ThreadHelper.newVirtualThreadExecutor();
	private final static Logger logger = LoggerFactory.getLogger(WaldotGeneratorPlugin.class);
	public static final String MAX_VALUE_FIELD = "Max";

	public static final String MIN_VALUE_FIELD = "Min";
	public static final String TIMER_OBJECT_TYPE_LABEL = "timer";

	private UaObjectTypeNode dataGeneratorTypeNode;

	private UaObjectTypeNode timerTypeNode;

	protected WaldotNamespace waldotNamespace;

	@Override
	public void close() throws Exception {
		executor.shutdownNow();
	}

	@Override
	public boolean containsVertexType(String typeDefinitionLabel) {
		return TIMER_OBJECT_TYPE_LABEL.equals(typeDefinitionLabel)
				|| DATA_GENERATOR_OBJECT_TYPE_LABEL.equals(typeDefinitionLabel);
	}

	@Override
	public boolean containsVertexTypeNode(NodeId typeDefinitionNodeId) {
		return timerTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| dataGeneratorTypeNode.getNodeId().equals(typeDefinitionNodeId);
	}

	private WaldotVertex createDataGeneratorObject(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		final String keyValuesPropertyDelay = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				DELAY_FIELD.toLowerCase());
		long delay = DEFAULT_DELAY_FIELD;
		if (keyValuesPropertyDelay != null && !keyValuesPropertyDelay.isEmpty()) {
			if (delay < 100L) {
				delay = Long.valueOf(keyValuesPropertyDelay);
			} else {
				logger.info(DELAY_FIELD.toLowerCase() + " is less than 100ms, using default {} '{}'", DELAY_FIELD,
						DEFAULT_DELAY_FIELD);
			}
		} else {
			logger.info(DELAY_FIELD.toLowerCase() + " not found in propertyKeyValues, using default {} '{}'",
					DELAY_FIELD, DEFAULT_DELAY_FIELD);
		}

		final String keyValuesPropertyMin = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				MIN_VALUE_FIELD.toLowerCase());
		long min = DEFAULT_MIN_VALUE_FIELD;
		if (keyValuesPropertyMin != null && !keyValuesPropertyMin.isEmpty()) {
			min = Long.valueOf(keyValuesPropertyMin);
		} else {
			logger.info(MIN_VALUE_FIELD.toLowerCase() + " not found in propertyKeyValues, using default {} '{}'",
					MIN_VALUE_FIELD, DEFAULT_MIN_VALUE_FIELD);
		}

		final String keyValuesPropertyMax = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				MAX_VALUE_FIELD.toLowerCase());
		long max = DEFAULT_MAX_VALUE_FIELD;
		if (keyValuesPropertyMax != null && !keyValuesPropertyMax.isEmpty()) {
			max = Long.valueOf(keyValuesPropertyMax);
		} else {
			logger.info(MAX_VALUE_FIELD.toLowerCase() + " not found in propertyKeyValues, using default {} '{}'",
					MAX_VALUE_FIELD, DEFAULT_MAX_VALUE_FIELD);
		}

		final String keyValuesPropertyAlgorithm = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				ALGORITHM_FIELD.toLowerCase());
		Algorithm algorithm = Algorithm.valueOf(DEFAULT_ALGORITHM_FIELD);
		if (keyValuesPropertyAlgorithm != null && !keyValuesPropertyAlgorithm.isEmpty()) {
			if (EnumUtils.isValidEnum(Algorithm.class, keyValuesPropertyAlgorithm)) {
				algorithm = Algorithm.valueOf(keyValuesPropertyAlgorithm);
			} else {
				logger.info("Algorithm {} not found, using default {} '{}'", keyValuesPropertyAlgorithm,
						ALGORITHM_FIELD, DEFAULT_ALGORITHM_FIELD);
				logger.info("Available algorithms are: {}", EnumUtils.getEnumList(Algorithm.class).toString());
			}
		} else {
			logger.info(ALGORITHM_FIELD.toLowerCase() + " not found in propertyKeyValues, using default {} '{}'",
					ALGORITHM_FIELD, DEFAULT_ALGORITHM_FIELD);
		}
		return new DataGeneratorVertex(executor, graph, context, nodeId, browseName, displayName, description,
				writeMask, userWriteMask, eventNotifier, version, delay, min, max, algorithm);
	}

	private void createDataGeneratorTypeNode() {
		dataGeneratorTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId("ObjectTypes/WaldOTDataGeneratorObjectType"))
				.setBrowseName(waldotNamespace.generateQualifiedName("WaldOTDataGeneratorObjectType"))
				.setDisplayName(LocalizedText.english("WaldOT Data Generator")).setIsAbstract(false).build();
		final UaVariableNode labelTimerTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace
						.generateNodeId("ObjectTypes/WaldOTDataGeneratorObjectType." + MiloStrategy.LABEL_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(MiloStrategy.LABEL_FIELD))
				.setDisplayName(LocalizedText.english(MiloStrategy.LABEL_FIELD)).setDataType(NodeIds.String)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		labelTimerTypeNode.addReference(new Reference(labelTimerTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		labelTimerTypeNode.setValue(new DataValue(new Variant("NaN")));
		dataGeneratorTypeNode.addComponent(labelTimerTypeNode);
		waldotNamespace.getStorageManager().addNode(labelTimerTypeNode);
		// variables
		final UaVariableNode delayMsTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId("ObjectTypes/WaldOTDataGeneratorObjectType." + DELAY_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(DELAY_FIELD))
				.setDisplayName(LocalizedText.english(DELAY_FIELD)).setDataType(NodeIds.UInt64)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		delayMsTypeNode.addReference(new Reference(delayMsTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		delayMsTypeNode.setValue(new DataValue(new Variant(DEFAULT_DELAY_FIELD)));
		dataGeneratorTypeNode.addComponent(delayMsTypeNode);
		waldotNamespace.getStorageManager().addNode(delayMsTypeNode);

		final UaVariableNode maxValueTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				waldotNamespace.getOpcUaNodeContext())
				.setNodeId(
						waldotNamespace.generateNodeId("ObjectTypes/WaldOTDataGeneratorObjectType." + MAX_VALUE_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(MAX_VALUE_FIELD))
				.setDisplayName(LocalizedText.english(MAX_VALUE_FIELD)).setDataType(NodeIds.UInt64)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		maxValueTypeNode.addReference(new Reference(maxValueTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		maxValueTypeNode.setValue(new DataValue(new Variant(DEFAULT_MAX_VALUE_FIELD)));
		dataGeneratorTypeNode.addComponent(maxValueTypeNode);
		waldotNamespace.getStorageManager().addNode(maxValueTypeNode);

		final UaVariableNode minValueTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				waldotNamespace.getOpcUaNodeContext())
				.setNodeId(
						waldotNamespace.generateNodeId("ObjectTypes/WaldOTDataGeneratorObjectType." + MIN_VALUE_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(MIN_VALUE_FIELD))
				.setDisplayName(LocalizedText.english(MIN_VALUE_FIELD)).setDataType(NodeIds.UInt64)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		minValueTypeNode.addReference(new Reference(minValueTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		minValueTypeNode.setValue(new DataValue(new Variant(DEFAULT_MIN_VALUE_FIELD)));
		dataGeneratorTypeNode.addComponent(minValueTypeNode);
		waldotNamespace.getStorageManager().addNode(minValueTypeNode);

		final UaVariableNode algorithmTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				waldotNamespace.getOpcUaNodeContext())
				.setNodeId(
						waldotNamespace.generateNodeId("ObjectTypes/WaldOTDataGeneratorObjectType." + ALGORITHM_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(ALGORITHM_FIELD))
				.setDisplayName(LocalizedText.english(ALGORITHM_FIELD)).setDataType(NodeIds.String)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		algorithmTypeNode.addReference(new Reference(algorithmTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		algorithmTypeNode.setValue(new DataValue(new Variant(DEFAULT_ALGORITHM_FIELD)));
		dataGeneratorTypeNode.addComponent(algorithmTypeNode);
		waldotNamespace.getStorageManager().addNode(algorithmTypeNode);

		// add definition to the address space
		waldotNamespace.getStorageManager().addNode(dataGeneratorTypeNode);
		dataGeneratorTypeNode.addReference(new Reference(dataGeneratorTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(dataGeneratorTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);
	}

	private WaldotVertex createTimerVertexObject(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version) {
		// TODO creare oggetto di tipo timer
		return null;
	}

	@Override
	public WaldotVertex createVertex(NodeId typeDefinitionNodeId, WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		if (!containsVertexTypeNode(typeDefinitionNodeId)) {
			return null;
		}
		if (timerTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			return createTimerVertexObject(graph, context, nodeId, browseName, displayName, description, writeMask,
					userWriteMask, eventNotifier, version);
		} else if (dataGeneratorTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			return createDataGeneratorObject(graph, context, nodeId, browseName, displayName, description, writeMask,
					userWriteMask, eventNotifier, version, propertyKeyValues);
		} else {
			return null;
		}
	}

	private void generateTimerTypeNode() {
		timerTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId("ObjectTypes/WaldOTTimerObjectType"))
				.setBrowseName(waldotNamespace.generateQualifiedName("WaldOTTimerObjectType"))
				.setDisplayName(LocalizedText.english("WaldOT Timer Node")).setIsAbstract(false).build();
		final UaVariableNode labelTimerTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				waldotNamespace.getOpcUaNodeContext())
				.setNodeId(
						waldotNamespace.generateNodeId("ObjectTypes/WaldOTTimerObjectType." + MiloStrategy.LABEL_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(MiloStrategy.LABEL_FIELD))
				.setDisplayName(LocalizedText.english(MiloStrategy.LABEL_FIELD)).setDataType(NodeIds.String)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		labelTimerTypeNode.addReference(new Reference(labelTimerTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		labelTimerTypeNode.setValue(new DataValue(new Variant("NaN")));
		timerTypeNode.addComponent(labelTimerTypeNode);
		final UaVariableNode schedulingTimerTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId("ObjectTypes/WaldOTTimerObjectType." + CRONTAB_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(CRONTAB_FIELD))
				.setDisplayName(LocalizedText.english(CRONTAB_FIELD)).setDataType(NodeIds.String)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		schedulingTimerTypeNode.addReference(new Reference(schedulingTimerTypeNode.getNodeId(),
				NodeIds.HasModellingRule, NodeIds.ModellingRule_Mandatory.expanded(), true));
		schedulingTimerTypeNode.setValue(new DataValue(new Variant(DEFAULT_CRONTAB_FIELD)));
		timerTypeNode.addComponent(schedulingTimerTypeNode);
		waldotNamespace.getStorageManager().addNode(labelTimerTypeNode);
		waldotNamespace.getStorageManager().addNode(schedulingTimerTypeNode);
		waldotNamespace.getStorageManager().addNode(timerTypeNode);
		timerTypeNode.addReference(
				new Reference(timerTypeNode.getNodeId(), NodeIds.HasSubtype, NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(timerTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);
	}

	@Override
	public Collection<WaldotCommand> getCommands() {
		return Arrays.asList();
	}

	@Override
	public NodeId getVertexTypeNode(String typeDefinitionLabel) {
		if (TIMER_OBJECT_TYPE_LABEL.equals(typeDefinitionLabel)) {
			return timerTypeNode.getNodeId();
		} else if (DATA_GENERATOR_OBJECT_TYPE_LABEL.equals(typeDefinitionLabel)) {
			return dataGeneratorTypeNode.getNodeId();
		} else {
			return null;
		}
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		generateTimerTypeNode();
		createDataGeneratorTypeNode();
	}

}
