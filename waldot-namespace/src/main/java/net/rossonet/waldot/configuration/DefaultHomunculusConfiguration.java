package net.rossonet.waldot.configuration;

import java.net.URL;
import java.util.HashMap;

import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public class DefaultHomunculusConfiguration extends HashMap<String, String> implements WaldotConfiguration {

	private static final long serialVersionUID = 4132363257864835403L;

	public static WaldotConfiguration getDefault() {
		return new DefaultHomunculusConfiguration();
	}

	private DefaultHomunculusConfiguration() {
	}

	@Override
	public String getAboutCommandDescription() {
		return "info about this software";
	}

	@Override
	public Boolean getAboutCommandExecutable() {
		return true;
	}

	@Override
	public String getAboutCommandLabel() {
		return "about";
	}

	@Override
	public Boolean getAboutCommandUserExecutable() {
		return true;
	}

	@Override
	public UInteger getAboutCommandUserWriteMask() {
		return UInteger.valueOf(WriteMask.Executable.getValue());
	}

	@Override
	public UInteger getAboutCommandWriteMask() {
		return UInteger.valueOf(WriteMask.Executable.getValue());
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
		return "Asset Administration";
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
		return UInteger.valueOf(WriteMask.Executable.getValue());
	}

	@Override
	public UInteger getExecCommandWriteMask() {
		return UInteger.valueOf(WriteMask.Executable.getValue());
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
		return UInteger.valueOf(WriteMask.Executable.getValue());
	}

	@Override
	public UInteger getHelpCommandWriteMask() {
		return UInteger.valueOf(WriteMask.Executable.getValue());
	}

	@Override
	public String getHelpDirectoryPath() {
		return "/app/help";
	}

	@Override
	public String getInterfaceRootNodeBrowseName() {
		return "WaldOT API & Commands Interface";
	}

	@Override
	public String getInterfaceRootNodeDisplayName() {
		return "WaldOT API & Commands Interface";
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
		return UInteger.valueOf(WriteMask.Executable.getValue());
	}

	@Override
	public UInteger getOsCheckDelayCommandWriteMask() {
		return UInteger.valueOf(WriteMask.Executable.getValue());
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
		return UInteger.valueOf(WriteMask.Executable.getValue());
	}

	@Override
	public UInteger getWaldotCommandWriteMask() {
		return UInteger.valueOf(WriteMask.Executable.getValue());
	}

	@Override
	public String getZenohConfiguration() {
		return null;
	}

}
