package net.rossonet.waldot.configuration;

import java.net.URL;
import java.util.HashMap;

import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public class HomunculusConfiguration extends HashMap<String, String> implements WaldotConfiguration {

	private static final long serialVersionUID = 4132363257864835403L;

	public static HomunculusConfiguration getDefault() {
		return new HomunculusConfiguration();
	}

	private HomunculusConfiguration() {
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
		return UInteger.MIN;
	}

	@Override
	public UInteger getAboutCommandWriteMask() {
		return UInteger.MIN;
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
		return "Asset Administration Shell & I4.0 API";
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
		return UInteger.MIN;
	}

	@Override
	public UInteger getExecCommandWriteMask() {
		return UInteger.MIN;
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
		return UInteger.MIN;
	}

	@Override
	public UInteger getHelpCommandWriteMask() {
		return UInteger.MIN;
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
	public String getVersionCommandDescription() {
		return "show software version";
	}

	@Override
	public Boolean getVersionCommandExecutable() {
		return true;
	}

	@Override
	public String getVersionCommandLabel() {
		return "version";
	}

	@Override
	public Boolean getVersionCommandUserExecutable() {
		return true;
	}

	@Override
	public UInteger getVersionCommandUserWriteMask() {
		return UInteger.MIN;
	}

	@Override
	public UInteger getVersionCommandWriteMask() {
		return UInteger.MIN;
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
		return UInteger.MIN;
	}

	@Override
	public UInteger getWaldotCommandWriteMask() {
		return UInteger.MIN;
	}

}
