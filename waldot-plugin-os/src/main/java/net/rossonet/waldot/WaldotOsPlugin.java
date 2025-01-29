package net.rossonet.waldot;

import java.util.Collection;
import java.util.Collections;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.commands.ExecCommand;

@WaldotPlugin
public class WaldotOsPlugin implements PluginListener {

	protected WaldotNamespace waldotNamespace;
	private ExecCommand execCommand;

	@Override
	public Collection<WaldotCommand> getCommands() {
		return Collections.singleton(execCommand);
	}

	@Override
	public void initialize(WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		execCommand = new ExecCommand(waldotNamespace);

	}

}
