package net.rossonet.waldot.api.configuration;

import java.io.Serializable;
import java.net.URL;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

/**
 * WaldotConfiguration is an interface that defines the configuration settings
 * for the Waldot Agent. It extends Serializable and Map<String, String> to
 * allow for easy serialization and access to configuration properties.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 *
 */
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

	long getDefaultFactsValidDelayMs();

	long getDefaultFactsValidUntilMs();

	String getDeleteDirectoryDescription();

	Boolean getDeleteDirectoryExecutable();

	String getDeleteDirectoryLabel();

	UInteger getDeleteDirectoryUserWriteMask();

	UInteger getDeleteDirectoryWriteMask();

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

	String getHelpDirectory();

	String getInterfaceRootNodeBrowseName();

	String getInterfaceRootNodeDisplayName();

	String getInterfaceRootNodeId();

	String getManagerNamespaceUri();

	String getOsCheckDelayCommandDescription();

	Boolean getOsCheckDelayCommandExecutable();

	String getOsCheckDelayCommandLabel();

	Boolean getOsCheckDelayCommandUserExecutable();

	UInteger getOsCheckDelayCommandUserWriteMask();

	UInteger getOsCheckDelayCommandWriteMask();

	String getRootNodeBrowseName();

	String getRootNodeDisplayName();

	String getRootNodeId();

	String getWaldotCommandDescription();

	Boolean getWaldotCommandExecutable();

	String getWaldotCommandLabel();

	Boolean getWaldotCommandUserExecutable();

	UInteger getWaldotCommandUserWriteMask();

	UInteger getWaldotCommandWriteMask();

	String getZenohConfiguration();

}
