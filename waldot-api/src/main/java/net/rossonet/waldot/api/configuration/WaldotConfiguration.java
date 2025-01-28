package net.rossonet.waldot.api.configuration;

import java.io.Serializable;
import java.net.URL;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

public interface WaldotConfiguration extends Serializable, Map<String, String> {

	String getAboutCommandDescription();

	Boolean getAboutCommandExecutable();

	String getAboutCommandLabel();

	Boolean getAboutCommandUserExecutable();

	UInteger getAboutCommandUserWriteMask();

	UInteger getAboutCommandWriteMask();

	boolean getAnonymousAccessAllowed();

	String getAssetRootNodeBrowseName();

	String getAssetRootNodeDisplayName();

	String getAssetRootNodeId();

	URL getBootUrl();

	String getExecCommandDescription();

	Boolean getExecCommandExecutable();

	String getExecCommandLabel();

	Boolean getExecCommandUserExecutable();

	UInteger getExecCommandUserWriteMask();

	UInteger getExecCommandWriteMask();

	String getFactoryPassword();

	String getFactoryUsername();

	String getHelpCommandDescription();

	Boolean getHelpCommandExecutable();

	String getHelpCommandLabel();

	Boolean getHelpCommandUserExecutable();

	UInteger getHelpCommandUserWriteMask();

	UInteger getHelpCommandWriteMask();

	String getInterfaceRootNodeBrowseName();

	String getInterfaceRootNodeDisplayName();

	String getInterfaceRootNodeId();

	String getManagerNamespaceUri();

	String getRootNodeBrowseName();

	String getRootNodeDisplayName();

	String getRootNodeId();

	String getVersionCommandDescription();

	Boolean getVersionCommandExecutable();

	String getVersionCommandLabel();

	Boolean getVersionCommandUserExecutable();

	UInteger getVersionCommandUserWriteMask();

	UInteger getVersionCommandWriteMask();

	String getWaldotCommandDescription();

	Boolean getWaldotCommandExecutable();

	String getWaldotCommandLabel();

	Boolean getWaldotCommandUserExecutable();

	UInteger getWaldotCommandUserWriteMask();

	UInteger getWaldotCommandWriteMask();

}
