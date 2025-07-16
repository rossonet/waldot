package net.rossonet.waldot;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
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
import net.rossonet.waldot.commands.ExecCommand;
import net.rossonet.waldot.commands.OsCheckDelayCommand;
import net.rossonet.waldot.rules.SysCommandExecutor;
import net.rossonet.waldot.utils.GremlinHelper;
import net.rossonet.waldot.utils.gremlin.UpdateTrigger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.software.os.OperatingSystem;

/**
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotOsPlugin implements AutoCloseable, PluginListener {
	private static final String CRONTAB_FIELD = "scheduling";
	private static final String DEFAULT_CRONTAB_FIELD = "0 0/5 * * *"; // Every 5 minutes
	public static final long DEFAULT_UPDATE_DELAY = 120_000;

	private final static Logger logger = LoggerFactory.getLogger(WaldotOsPlugin.class);

	public static final String TIMER_OBJECT_TYPE_LABEL = "timer";

	private boolean active = true;

	private ExecCommand execCommand;

	private final HashMap<Object, Method> objectsToRefresh = new HashMap<>();

	private OsCheckDelayCommand osCheckDelayCommand;

	private final SysCommandExecutor sysCommandExecutor = new SysCommandExecutor();
	private final Thread systemDataThread = new Thread(() -> {
		while (active) {
			try {
				Thread.sleep(getUpdateDelay());
				updateSystemData();
			} catch (final Throwable e) {
				logger.error("System data thread error", e);
				break;
			}
		}
	}, "WaldotOsPlugin");
	private SystemInfo systemInfo;
	private UaObjectTypeNode timerTypeNode;
	private final List<UpdateTrigger> triggers = new ArrayList<>();
	private long updateDelay = DEFAULT_UPDATE_DELAY;
	protected WaldotNamespace waldotNamespace;

	@Override
	public void close() throws Exception {
		active = false;

	}

	@Override
	public boolean containsObjectDefinition(NodeId typeDefinitionNodeId) {
		return timerTypeNode.getNodeId().equals(typeDefinitionNodeId);
	}

	@Override
	public boolean containsObjectLabel(String typeDefinitionLabel) {
		return TIMER_OBJECT_TYPE_LABEL.equals(typeDefinitionLabel);
	}

	private WaldotVertex createTimerVertexObject(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version) {
		// TODO creare oggetto di tipo timer
		return null;
	}

	@Override
	public WaldotVertex createVertexObject(NodeId typeDefinitionNodeId, WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version) {
		if (!containsObjectDefinition(typeDefinitionNodeId)) {
			return null;
		}
		return createTimerVertexObject(graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version);
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
				.setDisplayName(LocalizedText.english(MiloStrategy.LABEL_FIELD)).setDataType(Identifiers.String)
				.setTypeDefinition(Identifiers.BaseDataVariableType).build();
		labelTimerTypeNode.addReference(new Reference(labelTimerTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		labelTimerTypeNode.setValue(new DataValue(new Variant("NaN")));
		timerTypeNode.addComponent(labelTimerTypeNode);
		final UaVariableNode schedulingTimerTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId("ObjectTypes/WaldOTTimerObjectType." + CRONTAB_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(CRONTAB_FIELD))
				.setDisplayName(LocalizedText.english(CRONTAB_FIELD)).setDataType(Identifiers.String)
				.setTypeDefinition(Identifiers.BaseDataVariableType).build();
		schedulingTimerTypeNode.addReference(new Reference(schedulingTimerTypeNode.getNodeId(),
				Identifiers.HasModellingRule, Identifiers.ModellingRule_Mandatory.expanded(), true));
		schedulingTimerTypeNode.setValue(new DataValue(new Variant(DEFAULT_CRONTAB_FIELD)));
		timerTypeNode.addComponent(schedulingTimerTypeNode);
		waldotNamespace.getStorageManager().addNode(labelTimerTypeNode);
		waldotNamespace.getStorageManager().addNode(schedulingTimerTypeNode);
		waldotNamespace.getStorageManager().addNode(timerTypeNode);
		timerTypeNode.addReference(new Reference(timerTypeNode.getNodeId(), Identifiers.HasSubtype,
				Identifiers.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(timerTypeNode.getNodeId(), UaObjectNode.class,
				UaObjectNode::new);
	}

	@Override
	public Collection<WaldotCommand> getCommands() {
		return Arrays.asList(execCommand, osCheckDelayCommand);
	}

	@Override
	public NodeId getObjectLabel(String typeDefinitionLabel) {
		return containsObjectLabel(typeDefinitionLabel) ? timerTypeNode.getNodeId() : null;
	}

	@Override
	public Map<String, Object> getRuleFunctions() {
		final Map<String, Object> ruleFunctions = new HashMap<>();
		ruleFunctions.put("sys", sysCommandExecutor);
		return ruleFunctions;
	}

	public long getUpdateDelay() {
		return updateDelay;
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		generateTimerTypeNode();
		execCommand = new ExecCommand(waldotNamespace);
		osCheckDelayCommand = new OsCheckDelayCommand(waldotNamespace, this);
		popolateOsData();
	}

	private void popolateOsData() {
		systemInfo = new SystemInfo();
		try {
			final OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
			triggers.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), operatingSystem,
					"operatingSystem"));
		} catch (final Throwable e) {
			logger.error("Error elaborating operating system data", e);
		}
		final HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();

		try {
			final List<PowerSource> powerSources = hardwareAbstractionLayer.getPowerSources();
			triggers.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), powerSources,
					"hardware/powerSources"));
		} catch (final Throwable e) {
			logger.error("Error elaborating power sources data", e);
		}
		try {
			final ComputerSystem computerSystem = hardwareAbstractionLayer.getComputerSystem();
			triggers.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), computerSystem,
					"hardware/computerSystem"));
		} catch (final Throwable e) {
			logger.error("Error elaborating computer system data", e);
		}
		try {
			final GlobalMemory memory = hardwareAbstractionLayer.getMemory();
			triggers.addAll(
					GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), memory, "hardware/memory"));
		} catch (final Throwable e) {
			logger.error("Error elaborating memory data", e);
		}
		try {
			final CentralProcessor processor = hardwareAbstractionLayer.getProcessor();
			triggers.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), processor,
					"hardware/processor"));
		} catch (final Throwable e) {
			logger.error("Error elaborating processor data", e);
		}
		try {
			final Sensors sensors = hardwareAbstractionLayer.getSensors();
			triggers.addAll(
					GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), sensors, "hardware/sensors"));
		} catch (final Throwable e) {
			logger.error("Error elaborating sensors data", e);
		}
		try {
			final List<HWDiskStore> diskStores = hardwareAbstractionLayer.getDiskStores();
			triggers.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), diskStores,
					"hardware/diskStores"));
		} catch (final Throwable e) {
			logger.error("Error elaborating disk stores data", e);
		}
		try {
			final List<LogicalVolumeGroup> logicalVolumeGroups = hardwareAbstractionLayer.getLogicalVolumeGroups();
			triggers.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), logicalVolumeGroups,
					"hardware/logicalVolumeGroups"));
		} catch (final Throwable e) {
			logger.error("Error elaborating logical volume groups data", e);
		}
		try {
			final List<NetworkIF> networkIFs = hardwareAbstractionLayer.getNetworkIFs(true);
			triggers.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), networkIFs,
					"hardware/networkIFs"));
		} catch (final Throwable e) {
			logger.error("Error elaborating network interfaces data", e);
		}
		try {
			final List<Display> displays = hardwareAbstractionLayer.getDisplays();
			triggers.addAll(
					GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), displays, "hardware/displays"));
		} catch (final Throwable e) {
			logger.error("Error elaborating display data", e);
		}
		try {
			final List<UsbDevice> usbDevices = hardwareAbstractionLayer.getUsbDevices(true);
			triggers.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), usbDevices,
					"hardware/usbDevices"));
		} catch (final Throwable e) {
			logger.error("Error elaborating USB devices data", e);
		}
		try {
			final List<SoundCard> soundCards = hardwareAbstractionLayer.getSoundCards();
			triggers.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), soundCards,
					"hardware/soundCards"));
		} catch (final Throwable e) {
			logger.error("Error elaborating sound cards data", e);
		}
		try {
			final List<GraphicsCard> graphicsCards = hardwareAbstractionLayer.getGraphicsCards();
			triggers.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), graphicsCards,
					"hardware/graphicsCards"));
		} catch (final Throwable e) {
			logger.error("Error elaborating graphics cards data", e);
		}
		for (final UpdateTrigger trigger : triggers) {
			try {
				final List<Method> methods = Arrays.stream(trigger.getAnalizedObject().getClass().getMethods())
						.filter(m -> Modifier.isPublic(m.getModifiers()))
						.filter(m -> !Modifier.isStatic(m.getModifiers())).collect(Collectors.toList());
				for (final Method method : methods) {
					final String name = method.getName();
					if (name.equals("updateAttributes")) {
						objectsToRefresh.put(trigger.getAnalizedObject(), method);
						break;
					}
				}
			} catch (final Throwable e) {
				logger.error("Error invoking update trigger for " + trigger, e);
			}
		}
		for (final Object entry : objectsToRefresh.keySet()) {
			logger.debug("Object to refresh: " + entry);
		}

		systemDataThread.setPriority(Thread.MIN_PRIORITY);
		systemDataThread.start();
	}

	public void setUpdateDelay(long timeout) {
		updateDelay = timeout;

	}

	private void updateSystemData() {
		for (final Map.Entry<Object, Method> entry : objectsToRefresh.entrySet()) {
			final Object analizedObject = entry.getKey();
			final Method method = entry.getValue();
			try {
				method.invoke(analizedObject);
				logger.debug("Update method invoked: " + method.getName() + " on " + analizedObject);
			} catch (final Throwable e) {
				logger.error("Error invoking update method " + method.getName() + " on " + analizedObject, e);
			}
		}

		for (final UpdateTrigger trigger : triggers) {
			try {
				trigger.invoke();
				logger.debug("Update trigger invoked: " + trigger);
			} catch (final Throwable e) {
				logger.error("Error invoking update trigger for " + trigger, e);
			}
		}

	}

}
