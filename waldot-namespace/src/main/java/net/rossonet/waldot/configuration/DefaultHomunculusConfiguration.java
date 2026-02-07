package net.rossonet.waldot.configuration;

import java.net.URL;
import java.util.HashMap;

import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public class DefaultHomunculusConfiguration extends HashMap<String, String> implements WaldotConfiguration {

	private static final String DEFAULT_ABOUT_COMMAND_DESCRIPTION = "info about this software";
	private static final boolean DEFAULT_ABOUT_COMMAND_EXECUTABLE = true;
	private static final String DEFAULT_ABOUT_COMMAND_LABEL = "about";
	private static final boolean DEFAULT_ABOUT_COMMAND_USER_EXECUTABLE = true;
	public static String DEFAULT_HELP_DIRECTORY = "/app/help";
	private static final UInteger DEFAULT_WRITE_MASK = UInteger.valueOf(WriteMask.Executable.getValue());
	private static final long serialVersionUID = 4132363257864835403L;

	public static WaldotConfiguration getDefault() {
		return new DefaultHomunculusConfiguration();
	}

	private DefaultHomunculusConfiguration() {
	}

	@Override
	public String getAboutCommandDescription() {
		return DEFAULT_ABOUT_COMMAND_DESCRIPTION;
	}

	@Override
	public Boolean getAboutCommandExecutable() {
		return DEFAULT_ABOUT_COMMAND_EXECUTABLE;
	}

	@Override
	public String getAboutCommandLabel() {
		return DEFAULT_ABOUT_COMMAND_LABEL;
	}

	@Override
	public Boolean getAboutCommandUserExecutable() {
		return DEFAULT_ABOUT_COMMAND_USER_EXECUTABLE;
	}

	@Override
	public UInteger getAboutCommandUserWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public UInteger getAboutCommandWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public boolean getAnonymousAccessAllowed() {
		return true;
	}

	@Override
	public String getAssetRootNodeBrowseName() {
		return "aas";
	}

	@Override
	public String getAssetRootNodeDisplayName() {
		return "WaldOT Administration";
	}

	@Override
	public String getAssetRootNodeId() {
		return "aas";
	}

	@Override
	public URL getBootUrl() {
		return null;
	}

	@Override
	public long getDefaultFactsValidDelayMs() {
		return 0;
	}

	@Override
	public long getDefaultFactsValidUntilMs() {
		return 0;
	}

	@Override
	public String getDeleteDirectoryDescription() {
		return "delete directory by NodeId";
	}

	@Override
	public Boolean getDeleteDirectoryExecutable() {
		return true;
	}

	@Override
	public String getDeleteDirectoryLabel() {
		return "delete directory";
	}

	@Override
	public UInteger getDeleteDirectoryUserWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public UInteger getDeleteDirectoryWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public String getExecCommandDescription() {
		return "run system command";
	}

	@Override
	public Boolean getExecCommandExecutable() {
		return true;
	}

	@Override
	public String getExecCommandLabel() {
		return "exec";
	}

	@Override
	public Boolean getExecCommandUserExecutable() {
		return true;
	}

	@Override
	public UInteger getExecCommandUserWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public UInteger getExecCommandWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public String getFactoryPassword() {
		return "password123";
	}

	@Override
	public String getFactoryUsername() {
		return "admin";
	}

	@Override
	public String getHelpCommandDescription() {
		return "list available commands";

	}

	@Override
	public Boolean getHelpCommandExecutable() {
		return true;
	}

	@Override
	public String getHelpCommandLabel() {
		return "help";
	}

	@Override
	public Boolean getHelpCommandUserExecutable() {
		return true;

	}

	@Override
	public UInteger getHelpCommandUserWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public UInteger getHelpCommandWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public String getHelpDirectory() {
		return DEFAULT_HELP_DIRECTORY;
	}

	@Override
	public String getInterfaceRootNodeBrowseName() {
		return "WaldOT Commands";
	}

	@Override
	public String getInterfaceRootNodeDisplayName() {
		return "WaldOT Commands";
	}

	@Override
	public String getInterfaceRootNodeId() {
		return "cmd";
	}

	@Override
	public String getManagerNamespaceUri() {
		return "urn:rossonet:waldot:engine";
	}

	@Override
	public String getOsCheckDelayCommandDescription() {
		return "manage system delay on OS data updates";
	}

	@Override
	public Boolean getOsCheckDelayCommandExecutable() {
		return true;
	}

	@Override
	public String getOsCheckDelayCommandLabel() {
		return "os_check_delay";
	}

	@Override
	public Boolean getOsCheckDelayCommandUserExecutable() {
		return true;
	}

	@Override
	public UInteger getOsCheckDelayCommandUserWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public UInteger getOsCheckDelayCommandWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public String getRootNodeBrowseName() {
		return "WaldOT";
	}

	@Override
	public String getRootNodeDisplayName() {
		return "WaldOT Engine";
	}

	@Override
	public String getRootNodeId() {
		return "waldot";
	}

	@Override
	public String getWaldotCommandDescription() {
		return "run Gremlin query";
	}

	@Override
	public Boolean getWaldotCommandExecutable() {
		return true;
	}

	@Override
	public String getWaldotCommandLabel() {
		return "query";
	}

	@Override
	public Boolean getWaldotCommandUserExecutable() {
		return true;
	}

	@Override
	public UInteger getWaldotCommandUserWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public UInteger getWaldotCommandWriteMask() {
		return DEFAULT_WRITE_MASK;
	}

	@Override
	public String getZenohConfiguration() {
		return null;
	}

}
