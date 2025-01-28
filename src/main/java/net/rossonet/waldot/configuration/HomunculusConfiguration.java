package net.rossonet.waldot.configuration;

import java.net.URL;
import java.util.HashMap;

import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.Configuration;

public class HomunculusConfiguration extends HashMap<String, String> implements Configuration {

	private static final long serialVersionUID = 4132363257864835403L;

	public static HomunculusConfiguration getDefault() {
		return new HomunculusConfiguration();
	}

	private HomunculusConfiguration() {
	}

	public String getAboutCommandDescription() {
		return "info about this software";
	}

	public Boolean getAboutCommandExecutable() {
		return true;
	}

	public String getAboutCommandLabel() {
		return "about";
	}

	public Boolean getAboutCommandUserExecutable() {
		return true;
	}

	public UInteger getAboutCommandUserWriteMask() {
		return UInteger.MIN;
	}

	public UInteger getAboutCommandWriteMask() {
		return UInteger.MIN;
	}

	public String getAssetRootNodeBrowseName() {
		return "aas";
	}

	public String getAssetRootNodeDisplayName() {
		return "Asset Administration Shell & I4.0 API";
	}

	public String getAssetRootNodeId() {
		return "aas";
	}

	public URL getBootUrl() {
		return null;
	}

	public String getExecCommandDescription() {
		return "run system command";
	}

	public Boolean getExecCommandExecutable() {
		return true;
	}

	public String getExecCommandLabel() {
		return "exec";
	}

	public Boolean getExecCommandUserExecutable() {
		return true;
	}

	public UInteger getExecCommandUserWriteMask() {
		return UInteger.MIN;
	}

	public UInteger getExecCommandWriteMask() {
		return UInteger.MIN;
	}

	public String getHelpCommandDescription() {
		return "list available commands";

	}

	public Boolean getHelpCommandExecutable() {
		return true;
	}

	public String getHelpCommandLabel() {
		return "help";
	}

	public Boolean getHelpCommandUserExecutable() {
		return true;

	}

	public UInteger getHelpCommandUserWriteMask() {
		return UInteger.MIN;
	}

	public UInteger getHelpCommandWriteMask() {
		return UInteger.MIN;
	}

	public String getInterfaceRootNodeBrowseName() {
		return "WaldOT API & Commands Interface";
	}

	public String getInterfaceRootNodeDisplayName() {
		return "WaldOT API & Commands Interface";
	}

	public String getInterfaceRootNodeId() {
		return "cmd";
	}

	public String getManagerNamespaceUri() {
		return "urn:rossonet:waldot:engine";
	}

	public String getRootNodeBrowseName() {
		return "WaldOT";
	}

	public String getRootNodeDisplayName() {
		return "WaldOT Engine";
	}

	public String getRootNodeId() {
		return "waldot";
	}

	public String getVersionCommandDescription() {
		return "show software version";
	}

	public Boolean getVersionCommandExecutable() {
		return true;
	}

	public String getVersionCommandLabel() {
		return "version";
	}

	public Boolean getVersionCommandUserExecutable() {
		return true;
	}

	public UInteger getVersionCommandUserWriteMask() {
		return UInteger.MIN;
	}

	public UInteger getVersionCommandWriteMask() {
		return UInteger.MIN;
	}

	public String getWaldotCommandDescription() {
		return "run Gremlin query";
	}

	public Boolean getWaldotCommandExecutable() {
		return true;
	}

	public String getWaldotCommandLabel() {
		return "query";
	}

	public Boolean getWaldotCommandUserExecutable() {
		return true;
	}

	public UInteger getWaldotCommandUserWriteMask() {
		return UInteger.MIN;
	}

	public UInteger getWaldotCommandWriteMask() {
		return UInteger.MIN;
	}

}
