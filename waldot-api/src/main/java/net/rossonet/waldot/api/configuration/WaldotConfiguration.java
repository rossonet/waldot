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
 * <p>WaldotConfiguration provides access to all configuration settings for
 * the WaldOT agent, including command definitions, node identifiers,
 * authentication settings, and feature flags. It extends Map to allow
 * generic configuration access while providing typed getters for
 * specific settings.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Get configuration
 * WaldotConfiguration config = namespace.getConfiguration();
 * 
 * // Get specific values
 * String namespaceUri = config.getManagerNamespaceUri();
 * String rootNodeId = config.getRootNodeId();
 * boolean anonymousAllowed = config.getAnonymousAccessAllowed();
 * 
 * // Generic map access
 * String customValue = config.get("my.custom.key");
 * 
 * // Iterate over all settings
 * for (String key : config.keySet()) {
 *     System.out.println(key + "=" + config.get(key));
 * }
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 *
 */
public interface WaldotConfiguration extends Serializable, Map<String, String> {

	/**
	 * Gets the description for the About command.
	 * 
	 * @return the description string
	 */
	String getAboutCommandDescription();

	/**
	 * Checks if the About command is executable.
	 * 
	 * @return true if executable
	 */
	Boolean getAboutCommandExecutable();

	/**
	 * Gets the label for the About command.
	 * 
	 * @return the label string
	 */
	String getAboutCommandLabel();

	/**
	 * Checks if the About command is executable by regular users.
	 * 
	 * @return true if user executable
	 */
	Boolean getAboutCommandUserExecutable();

	/**
	 * Gets the user write mask for the About command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getAboutCommandUserWriteMask();

	/**
	 * Gets the write mask for the About command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getAboutCommandWriteMask();

	/**
	 * Checks if anonymous access is allowed.
	 * 
	 * @return true if anonymous access is allowed
	 */
	boolean getAnonymousAccessAllowed();

	/**
	 * Gets the browse name for the asset root node.
	 * 
	 * @return the browse name string
	 */
	String getAssetRootNodeBrowseName();

	/**
	 * Gets the display name for the asset root node.
	 * 
	 * @return the display name string
	 */
	String getAssetRootNodeDisplayName();

	/**
	 * Gets the NodeId for the asset root node.
	 * 
	 * @return the NodeId string
	 */
	String getAssetRootNodeId();

	/**
	 * Gets the bootstrap URL for configuration loading.
	 * 
	 * @return the URL
	 * @see URL
	 */
	URL getBootUrl();

	/**
	 * Gets the default delay for facts validity in milliseconds.
	 * 
	 * @return the delay in milliseconds
	 */
	long getDefaultFactsValidDelayMs();

	/**
	 * Gets the default expiration time for facts in milliseconds.
	 * 
	 * @return the time in milliseconds
	 */
	long getDefaultFactsValidUntilMs();

	/**
	 * Gets the description for the DeleteDirectory command.
	 * 
	 * @return the description string
	 */
	String getDeleteDirectoryDescription();

	/**
	 * Checks if the DeleteDirectory command is executable.
	 * 
	 * @return true if executable
	 */
	Boolean getDeleteDirectoryExecutable();

	/**
	 * Gets the label for the DeleteDirectory command.
	 * 
	 * @return the label string
	 */
	String getDeleteDirectoryLabel();

	/**
	 * Gets the user write mask for the DeleteDirectory command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getDeleteDirectoryUserWriteMask();

	/**
	 * Gets the write mask for the DeleteDirectory command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getDeleteDirectoryWriteMask();

	/**
	 * Gets the description for the Exec command.
	 * 
	 * @return the description string
	 */
	String getExecCommandDescription();

	/**
	 * Checks if the Exec command is executable.
	 * 
	 * @return true if executable
	 */
	Boolean getExecCommandExecutable();

	/**
	 * Gets the label for the Exec command.
	 * 
	 * @return the label string
	 */
	String getExecCommandLabel();

	/**
	 * Checks if the Exec command is executable by regular users.
	 * 
	 * @return true if user executable
	 */
	Boolean getExecCommandUserExecutable();

	/**
	 * Gets the user write mask for the Exec command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getExecCommandUserWriteMask();

	/**
	 * Gets the write mask for the Exec command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getExecCommandWriteMask();

	/**
	 * Gets the factory password for default authentication.
	 * 
	 * @return the password string
	 */
	String getFactoryPassword();

	/**
	 * Gets the factory username for default authentication.
	 * 
	 * @return the username string
	 */
	String getFactoryUsername();

	/**
	 * Gets the description for the Help command.
	 * 
	 * @return the description string
	 */
	String getHelpCommandDescription();

	/**
	 * Checks if the Help command is executable.
	 * 
	 * @return true if executable
	 */
	Boolean getHelpCommandExecutable();

	/**
	 * Gets the label for the Help command.
	 * 
	 * @return the label string
	 */
	String getHelpCommandLabel();

	/**
	 * Checks if the Help command is executable by regular users.
	 * 
	 * @return true if user executable
	 */
	Boolean getHelpCommandUserExecutable();

	/**
	 * Gets the user write mask for the Help command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getHelpCommandUserWriteMask();

	/**
	 * Gets the write mask for the Help command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getHelpCommandWriteMask();

	/**
	 * Gets the help directory path.
	 * 
	 * @return the directory path string
	 */
	String getHelpDirectory();

	/**
	 * Gets the browse name for the interface root node.
	 * 
	 * @return the browse name string
	 */
	String getInterfaceRootNodeBrowseName();

	/**
	 * Gets the display name for the interface root node.
	 * 
	 * @return the display name string
	 */
	String getInterfaceRootNodeDisplayName();

	/**
	 * Gets the NodeId for the interface root node.
	 * 
	 * @return the NodeId string
	 */
	String getInterfaceRootNodeId();

	/**
	 * Gets the namespace URI for the manager.
	 * 
	 * @return the namespace URI string
	 */
	String getManagerNamespaceUri();

	/**
	 * Gets the description for the OsCheckDelay command.
	 * 
	 * @return the description string
	 */
	String getOsCheckDelayCommandDescription();

	/**
	 * Checks if the OsCheckDelay command is executable.
	 * 
	 * @return true if executable
	 */
	Boolean getOsCheckDelayCommandExecutable();

	/**
	 * Gets the label for the OsCheckDelay command.
	 * 
	 * @return the label string
	 */
	String getOsCheckDelayCommandLabel();

	/**
	 * Checks if the OsCheckDelay command is executable by regular users.
	 * 
	 * @return true if user executable
	 */
	Boolean getOsCheckDelayCommandUserExecutable();

	/**
	 * Gets the user write mask for the OsCheckDelay command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getOsCheckDelayCommandUserWriteMask();

	/**
	 * Gets the write mask for the OsCheckDelay command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getOsCheckDelayCommandWriteMask();

	/**
	 * Gets the browse name for the root node.
	 * 
	 * @return the browse name string
	 */
	String getRootNodeBrowseName();

	/**
	 * Gets the display name for the root node.
	 * 
	 * @return the display name string
	 */
	String getRootNodeDisplayName();

	/**
	 * Gets the NodeId for the root node.
	 * 
	 * @return the NodeId string
	 */
	String getRootNodeId();

	/**
	 * Gets the description for the Waldot command.
	 * 
	 * @return the description string
	 */
	String getWaldotCommandDescription();

	/**
	 * Checks if the Waldot command is executable.
	 * 
	 * @return true if executable
	 */
	Boolean getWaldotCommandExecutable();

	/**
	 * Gets the label for the Waldot command.
	 * 
	 * @return the label string
	 */
	String getWaldotCommandLabel();

	/**
	 * Checks if the Waldot command is executable by regular users.
	 * 
	 * @return true if user executable
	 */
	Boolean getWaldotCommandUserExecutable();

	/**
	 * Gets the user write mask for the Waldot command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getWaldotCommandUserWriteMask();

	/**
	 * Gets the write mask for the Waldot command.
	 * 
	 * @return the write mask
	 * @see UInteger
	 */
	UInteger getWaldotCommandWriteMask();

	/**
	 * Gets the Zenoh configuration string.
	 * 
	 * @return the configuration string
	 */
	String getZenohConfiguration();

}
