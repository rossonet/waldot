package net.rossonet.waldot.rules.oshi;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.WaldotOsPlugin;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.utils.GremlinHelper;
import net.rossonet.waldot.utils.gremlin.OshiMethodWrapper;
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
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystem.OSVersionInfo;

public class OsDataWrapper implements AutoCloseable {
	private final static Logger logger = LoggerFactory.getLogger(OsDataWrapper.class);

	private boolean active = true;
	private final List<OshiMethodWrapper> oshiDataMethods = new ArrayList<>();

	private final HashMap<Object, Method> oshiObjectsToRefresh = new HashMap<>();
	private final Thread systemDataThread = Thread.ofVirtual().unstarted(() -> {
		while (active) {
			try {
				Thread.sleep(getUpdateDelay());
				updateSystemData();
			} catch (final Throwable e) {
				logger.error("oshi data refresh thread error", e);
				break;
			}
		}
	});

	private SystemInfo systemInfo;

	protected WaldotNamespace waldotNamespace;

	private final WaldotOsPlugin waldotOsPlugin;

	public OsDataWrapper(WaldotNamespace waldotNamespace, WaldotOsPlugin waldotOsPlugin) {
		this.waldotNamespace = waldotNamespace;
		this.waldotOsPlugin = waldotOsPlugin;
	}

	@Override
	public void close() throws Exception {
		active = false;
	}

	private long getUpdateDelay() {
		return waldotOsPlugin.getUpdateDelay();
	}

	public void popolateOsData() {
		systemInfo = new SystemInfo();
		// Operating system
		final OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
		try {
			final FileSystem fileSystem = operatingSystem.getFileSystem();
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), fileSystem,
					"operatingSystem/FileSystem"));
		} catch (final Throwable e) {
			logger.error("Error elaborating operating system data", e);
		}
		/*
		 * try { final OSProcess javaProcess = operatingSystem.getCurrentProcess();
		 * oshiDataMethods.addAll(
		 * GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(),
		 * javaProcess, "javaProcess")); } catch (final Throwable e) {
		 * logger.error("Error elaborating operating system data", e); }
		 */
		try {
			final InternetProtocolStats iStats = operatingSystem.getInternetProtocolStats();
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), iStats,
					"operatingSystem/InternetProtocolStats"));
		} catch (final Throwable e) {
			logger.error("Error elaborating operating system data", e);
		}
		try {
			final OSVersionInfo vInfo = operatingSystem.getVersionInfo();
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), vInfo,
					"operatingSystem/VersionInfo"));
		} catch (final Throwable e) {
			logger.error("Error elaborating operating system data", e);
		}

		// HW abstraction layer
		final HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
		try {
			final List<PowerSource> powerSources = hardwareAbstractionLayer.getPowerSources();
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), powerSources,
					"hardware/powerSources"));
		} catch (final Throwable e) {
			logger.error("Error elaborating power sources data", e);
		}
		try {
			final ComputerSystem computerSystem = hardwareAbstractionLayer.getComputerSystem();
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), computerSystem,
					"hardware/computerSystem"));
		} catch (final Throwable e) {
			logger.error("Error elaborating computer system data", e);
		}
		try {
			final GlobalMemory memory = hardwareAbstractionLayer.getMemory();
			oshiDataMethods.addAll(
					GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), memory, "hardware/memory"));
		} catch (final Throwable e) {
			logger.error("Error elaborating memory data", e);
		}
		try {
			final CentralProcessor processor = hardwareAbstractionLayer.getProcessor();
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), processor,
					"hardware/processor"));
		} catch (final Throwable e) {
			logger.error("Error elaborating processor data", e);
		}
		try {
			final Sensors sensors = hardwareAbstractionLayer.getSensors();
			oshiDataMethods.addAll(
					GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), sensors, "hardware/sensors"));
		} catch (final Throwable e) {
			logger.error("Error elaborating sensors data", e);
		}
		try {
			final List<HWDiskStore> diskStores = hardwareAbstractionLayer.getDiskStores();
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), diskStores,
					"hardware/diskStores"));
		} catch (final Throwable e) {
			logger.error("Error elaborating disk stores data", e);
		}
		try {
			final List<LogicalVolumeGroup> logicalVolumeGroups = hardwareAbstractionLayer.getLogicalVolumeGroups();
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(),
					logicalVolumeGroups, "hardware/logicalVolumeGroups"));
		} catch (final Throwable e) {
			logger.error("Error elaborating logical volume groups data", e);
		}
		try {
			final List<NetworkIF> networkIFs = hardwareAbstractionLayer.getNetworkIFs(true);
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), networkIFs,
					"hardware/networkIFs"));
		} catch (final Throwable e) {
			logger.error("Error elaborating network interfaces data", e);
		}
		try {
			final List<Display> displays = hardwareAbstractionLayer.getDisplays();
			oshiDataMethods.addAll(
					GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), displays, "hardware/displays"));
		} catch (final Throwable e) {
			logger.error("Error elaborating display data", e);
		}
		try {
			final List<UsbDevice> usbDevices = hardwareAbstractionLayer.getUsbDevices(true);
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), usbDevices,
					"hardware/usbDevices"));
		} catch (final Throwable e) {
			logger.error("Error elaborating USB devices data", e);
		}
		try {
			final List<SoundCard> soundCards = hardwareAbstractionLayer.getSoundCards();
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), soundCards,
					"hardware/soundCards"));
		} catch (final Throwable e) {
			logger.error("Error elaborating sound cards data", e);
		}
		try {
			final List<GraphicsCard> graphicsCards = hardwareAbstractionLayer.getGraphicsCards();
			oshiDataMethods.addAll(GremlinHelper.elaborateInstance(waldotNamespace.getGremlinGraph(), graphicsCards,
					"hardware/graphicsCards"));
		} catch (final Throwable e) {
			logger.error("Error elaborating graphics cards data", e);
		}
		for (final OshiMethodWrapper subSystem : oshiDataMethods) {
			try {
				final List<Method> methods = Arrays.stream(subSystem.getAnalizedObject().getClass().getMethods())
						.filter(m -> Modifier.isPublic(m.getModifiers()))
						.filter(m -> !Modifier.isStatic(m.getModifiers())).collect(Collectors.toList());
				for (final Method method : methods) {
					final String name = method.getName();
					if (name.equals("updateAttributes")) {
						oshiObjectsToRefresh.put(subSystem.getAnalizedObject(), method);
						break;
					}
				}
			} catch (final Throwable e) {
				logger.error("Error invoking update trigger for " + subSystem, e);
			}
		}
		for (final Object entry : oshiObjectsToRefresh.keySet()) {
			logger.debug("Object to refresh: " + entry);
		}

		systemDataThread.setPriority(Thread.MIN_PRIORITY);
		systemDataThread.start();
		logger.info("os check thread started with delay of " + getUpdateDelay());
	}

	private void updateSystemData() {
		// FIXME verificare il funzionamento di questo metodo e aggiungere la capacita
		// di aggiungere e togliere i nodi dinamicamente
		for (final Map.Entry<Object, Method> entry : oshiObjectsToRefresh.entrySet()) {
			final Object analizedObject = entry.getKey();
			final Method method = entry.getValue();
			try {
				method.invoke(analizedObject);
				logger.debug("Update method invoked: " + method.getName() + " on " + analizedObject);
			} catch (final Throwable e) {
				logger.error("Error invoking update method " + method.getName() + " on " + analizedObject, e);
			}
		}

		for (final OshiMethodWrapper trigger : oshiDataMethods) {
			try {
				trigger.invoke();
				logger.debug("Update trigger invoked: " + trigger);
			} catch (final Throwable e) {
				logger.error("Error invoking update trigger for " + trigger, e);
			}
		}

	}
}
