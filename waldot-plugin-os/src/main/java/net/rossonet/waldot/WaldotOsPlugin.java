package net.rossonet.waldot;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.commands.ExecCommand;
import net.rossonet.waldot.rules.SysCommandExecutor;
import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import oshi.software.os.OperatingSystem;

/**
 * WaldotOsPlugin is a plugin that provides OS-related commands to the Waldot
 * system. It implements the PluginListener interface to register its commands
 * and initialize itself with the provided WaldotNamespace.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotOsPlugin implements PluginListener {

	protected WaldotNamespace waldotNamespace;

	private ExecCommand execCommand;
	private final SysCommandExecutor sysCommandExecutor = new SysCommandExecutor();

	@Override
	public Collection<WaldotCommand> getCommands() {
		return Collections.singleton(execCommand);
	}

	@Override
	public Map<String, Object> getRuleFunctions() {
		final Map<String, Object> ruleFunctions = new HashMap<>();
		ruleFunctions.put("sys", sysCommandExecutor);
		return ruleFunctions;
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		execCommand = new ExecCommand(waldotNamespace);
		popolateOsData();
	}

	private void popolateOsData() {
		final SystemInfo systemInfo = new SystemInfo();
		final OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
		final PlatformEnum platform = systemInfo.getCurrentPlatform();
		final HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
		final CentralProcessor cpu = hardwareAbstractionLayer.getProcessor();
		final List<GraphicsCard> graphicsCards = hardwareAbstractionLayer.getGraphicsCards();
		final Sensors sensors = hardwareAbstractionLayer.getSensors();

	}

}
