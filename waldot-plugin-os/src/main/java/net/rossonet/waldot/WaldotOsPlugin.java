package net.rossonet.waldot;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.commands.ExecCommand;
import net.rossonet.waldot.rules.SysCommandExecutor;

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

	}

}
