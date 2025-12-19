package net.rossonet.waldot;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.commands.ExecCommand;
import net.rossonet.waldot.commands.OsCheckDelayCommand;
import net.rossonet.waldot.rules.SysCommandExecutor;
import net.rossonet.waldot.rules.oshi.OsDataWrapper;

/**
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotOsPlugin implements AutoCloseable, PluginListener {
	public static final long DEFAULT_UPDATE_DELAY = 20000;// 120_000;
	public static final String DELAY_FIELD = "Delay";
	public static boolean ENABLE_EXEC_COMMAND = false;
	private final static Logger logger = LoggerFactory.getLogger(WaldotOsPlugin.class);
	private ExecCommand execCommand;

	private OsCheckDelayCommand osCheckDelayCommand;

	private OsDataWrapper osWrapper;

	private final SysCommandExecutor sysCommandExecutor = new SysCommandExecutor();

	private long updateDelay = DEFAULT_UPDATE_DELAY;
	protected WaldotNamespace waldotNamespace;

	@Override
	public void close() throws Exception {
		if (osWrapper != null) {
			osWrapper.close();
		}
	}

	@Override
	public boolean containsObjectDefinition(NodeId typeDefinitionNodeId) {
		return false;
	}

	@Override
	public boolean containsObjectLabel(String typeDefinitionLabel) {
		return false;
	}

	@Override
	public Collection<WaldotCommand> getCommands() {
		if (ENABLE_EXEC_COMMAND) {
			return Arrays.asList(execCommand, osCheckDelayCommand);
		} else {
			return Arrays.asList(osCheckDelayCommand);
		}
	}

	@Override
	public NodeId getObjectLabel(String typeDefinitionLabel) {
		return null;
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
		if (ENABLE_EXEC_COMMAND) {
			execCommand = new ExecCommand(waldotNamespace);
		}
		osCheckDelayCommand = new OsCheckDelayCommand(waldotNamespace, this);
		osWrapper = new OsDataWrapper(waldotNamespace, this);
		osWrapper.popolateOsData();
	}

	public void setUpdateDelay(long timeout) {
		updateDelay = timeout;
		logger.info("Update delay set to {} ms", updateDelay);
	}

}
