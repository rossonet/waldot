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

	// Campi privati per configurazione mutabile
	private String aboutCommandDescription = DEFAULT_ABOUT_COMMAND_DESCRIPTION;
	private Boolean aboutCommandExecutable = DEFAULT_ABOUT_COMMAND_EXECUTABLE;
	private String aboutCommandLabel = DEFAULT_ABOUT_COMMAND_LABEL;
	private Boolean aboutCommandUserExecutable = DEFAULT_ABOUT_COMMAND_USER_EXECUTABLE;
	private UInteger aboutCommandUserWriteMask = DEFAULT_WRITE_MASK;
	private UInteger aboutCommandWriteMask = DEFAULT_WRITE_MASK;
	private boolean anonymousAccessAllowed = true;
	private String assetRootNodeBrowseName = "Administration";
	private String assetRootNodeDisplayName = "Administration";
	private String assetRootNodeId = "aas";
	private URL bootUrl = null;
	private long defaultFactsValidDelayMs = 0;
	private long defaultFactsValidUntilMs = 0;
	private String deleteDirectoryDescription = "delete directory by NodeId";
	private Boolean deleteDirectoryExecutable = true;
	private String deleteDirectoryLabel = "delete directory";
	private UInteger deleteDirectoryUserWriteMask = DEFAULT_WRITE_MASK;
	private UInteger deleteDirectoryWriteMask = DEFAULT_WRITE_MASK;
	private String execCommandDescription = "run system command";
	private Boolean execCommandExecutable = true;
	private String execCommandLabel = "exec";
	private Boolean execCommandUserExecutable = true;
	private UInteger execCommandUserWriteMask = DEFAULT_WRITE_MASK;
	private UInteger execCommandWriteMask = DEFAULT_WRITE_MASK;
	private String factoryPassword = "password123";
	private String factoryUsername = "admin";
	private String helpCommandDescription = "list available commands";
	private Boolean helpCommandExecutable = true;
	private String helpCommandLabel = "help";
	private Boolean helpCommandUserExecutable = true;
	private UInteger helpCommandUserWriteMask = DEFAULT_WRITE_MASK;
	private UInteger helpCommandWriteMask = DEFAULT_WRITE_MASK;
	private String helpDirectory = DEFAULT_HELP_DIRECTORY;
	private String interfaceRootNodeBrowseName = "Commands";
	private String interfaceRootNodeDisplayName = "Commands";
	private String interfaceRootNodeId = "cmd";
	private String managerNamespaceUri = "urn:rossonet:waldot:engine";
	private String osCheckDelayCommandDescription = "manage system delay on OS data updates";
	private Boolean osCheckDelayCommandExecutable = true;
	private String osCheckDelayCommandLabel = "os_check_delay";
	private Boolean osCheckDelayCommandUserExecutable = true;
	private UInteger osCheckDelayCommandUserWriteMask = DEFAULT_WRITE_MASK;
	private UInteger osCheckDelayCommandWriteMask = DEFAULT_WRITE_MASK;
	private String rootNodeBrowseName = "Gremlin Engine";
	private String rootNodeDisplayName = "Gremlin Engine";
	private String rootNodeId = "waldot";
	private String waldotCommandDescription = "run Gremlin query";
	private Boolean waldotCommandExecutable = true;
	private String waldotCommandLabel = "query";
	private Boolean waldotCommandUserExecutable = true;
	private UInteger waldotCommandUserWriteMask = DEFAULT_WRITE_MASK;
	private UInteger waldotCommandWriteMask = DEFAULT_WRITE_MASK;
	private String zenohConfiguration = null;

	private DefaultHomunculusConfiguration() {
	}

	@Override
	public String getAboutCommandDescription() {
		return aboutCommandDescription;
	}

	@Override
	public Boolean getAboutCommandExecutable() {
		return aboutCommandExecutable;
	}

	@Override
	public String getAboutCommandLabel() {
		return aboutCommandLabel;
	}

	@Override
	public Boolean getAboutCommandUserExecutable() {
		return aboutCommandUserExecutable;
	}

	@Override
	public UInteger getAboutCommandUserWriteMask() {
		return aboutCommandUserWriteMask;
	}

	@Override
	public UInteger getAboutCommandWriteMask() {
		return aboutCommandWriteMask;
	}

	@Override
	public boolean getAnonymousAccessAllowed() {
		return anonymousAccessAllowed;
	}

	@Override
	public String getAssetRootNodeBrowseName() {
		return assetRootNodeBrowseName;
	}

	@Override
	public String getAssetRootNodeDisplayName() {
		return assetRootNodeDisplayName;
	}

	@Override
	public String getAssetRootNodeId() {
		return assetRootNodeId;
	}

	@Override
	public URL getBootUrl() {
		return bootUrl;
	}

	@Override
	public long getDefaultFactsValidDelayMs() {
		return defaultFactsValidDelayMs;
	}

	@Override
	public long getDefaultFactsValidUntilMs() {
		return defaultFactsValidUntilMs;
	}

	@Override
	public String getDeleteDirectoryDescription() {
		return deleteDirectoryDescription;
	}

	@Override
	public Boolean getDeleteDirectoryExecutable() {
		return deleteDirectoryExecutable;
	}

	@Override
	public String getDeleteDirectoryLabel() {
		return deleteDirectoryLabel;
	}

	@Override
	public UInteger getDeleteDirectoryUserWriteMask() {
		return deleteDirectoryUserWriteMask;
	}

	@Override
	public UInteger getDeleteDirectoryWriteMask() {
		return deleteDirectoryWriteMask;
	}

	@Override
	public String getExecCommandDescription() {
		return execCommandDescription;
	}

	@Override
	public Boolean getExecCommandExecutable() {
		return execCommandExecutable;
	}

	@Override
	public String getExecCommandLabel() {
		return execCommandLabel;
	}

	@Override
	public Boolean getExecCommandUserExecutable() {
		return execCommandUserExecutable;
	}

	@Override
	public UInteger getExecCommandUserWriteMask() {
		return execCommandUserWriteMask;
	}

	@Override
	public UInteger getExecCommandWriteMask() {
		return execCommandWriteMask;
	}

	@Override
	public String getFactoryPassword() {
		return factoryPassword;
	}

	@Override
	public String getFactoryUsername() {
		return factoryUsername;
	}

	@Override
	public String getHelpCommandDescription() {
		return helpCommandDescription;
	}

	@Override
	public Boolean getHelpCommandExecutable() {
		return helpCommandExecutable;
	}

	@Override
	public String getHelpCommandLabel() {
		return helpCommandLabel;
	}

	@Override
	public Boolean getHelpCommandUserExecutable() {
		return helpCommandUserExecutable;
	}

	@Override
	public UInteger getHelpCommandUserWriteMask() {
		return helpCommandUserWriteMask;
	}

	@Override
	public UInteger getHelpCommandWriteMask() {
		return helpCommandWriteMask;
	}

	@Override
	public String getHelpDirectory() {
		return helpDirectory;
	}

	@Override
	public String getInterfaceRootNodeBrowseName() {
		return interfaceRootNodeBrowseName;
	}

	@Override
	public String getInterfaceRootNodeDisplayName() {
		return interfaceRootNodeDisplayName;
	}

	@Override
	public String getInterfaceRootNodeId() {
		return interfaceRootNodeId;
	}

	@Override
	public String getManagerNamespaceUri() {
		return managerNamespaceUri;
	}

	@Override
	public String getOsCheckDelayCommandDescription() {
		return osCheckDelayCommandDescription;
	}

	@Override
	public Boolean getOsCheckDelayCommandExecutable() {
		return osCheckDelayCommandExecutable;
	}

	@Override
	public String getOsCheckDelayCommandLabel() {
		return osCheckDelayCommandLabel;
	}

	@Override
	public Boolean getOsCheckDelayCommandUserExecutable() {
		return osCheckDelayCommandUserExecutable;
	}

	@Override
	public UInteger getOsCheckDelayCommandUserWriteMask() {
		return osCheckDelayCommandUserWriteMask;
	}

	@Override
	public UInteger getOsCheckDelayCommandWriteMask() {
		return osCheckDelayCommandWriteMask;
	}

	@Override
	public String getRootNodeBrowseName() {
		return rootNodeBrowseName;
	}

	@Override
	public String getRootNodeDisplayName() {
		return rootNodeDisplayName;
	}

	@Override
	public String getRootNodeId() {
		return rootNodeId;
	}

	@Override
	public String getWaldotCommandDescription() {
		return waldotCommandDescription;
	}

	@Override
	public Boolean getWaldotCommandExecutable() {
		return waldotCommandExecutable;
	}

	@Override
	public String getWaldotCommandLabel() {
		return waldotCommandLabel;
	}

	@Override
	public Boolean getWaldotCommandUserExecutable() {
		return waldotCommandUserExecutable;
	}

	@Override
	public UInteger getWaldotCommandUserWriteMask() {
		return waldotCommandUserWriteMask;
	}

	@Override
	public UInteger getWaldotCommandWriteMask() {
		return waldotCommandWriteMask;
	}

	@Override
	public String getZenohConfiguration() {
		return zenohConfiguration;
	}

	// Setter methods
	public void setAboutCommandDescription(String aboutCommandDescription) {
		this.aboutCommandDescription = aboutCommandDescription;
	}

	public void setAboutCommandExecutable(Boolean aboutCommandExecutable) {
		this.aboutCommandExecutable = aboutCommandExecutable;
	}

	public void setAboutCommandLabel(String aboutCommandLabel) {
		this.aboutCommandLabel = aboutCommandLabel;
	}

	public void setAboutCommandUserExecutable(Boolean aboutCommandUserExecutable) {
		this.aboutCommandUserExecutable = aboutCommandUserExecutable;
	}

	public void setAboutCommandUserWriteMask(UInteger aboutCommandUserWriteMask) {
		this.aboutCommandUserWriteMask = aboutCommandUserWriteMask;
	}

	public void setAboutCommandWriteMask(UInteger aboutCommandWriteMask) {
		this.aboutCommandWriteMask = aboutCommandWriteMask;
	}

	public void setAnonymousAccessAllowed(boolean anonymousAccessAllowed) {
		this.anonymousAccessAllowed = anonymousAccessAllowed;
	}

	public void setAssetRootNodeBrowseName(String assetRootNodeBrowseName) {
		this.assetRootNodeBrowseName = assetRootNodeBrowseName;
	}

	public void setAssetRootNodeDisplayName(String assetRootNodeDisplayName) {
		this.assetRootNodeDisplayName = assetRootNodeDisplayName;
	}

	public void setAssetRootNodeId(String assetRootNodeId) {
		this.assetRootNodeId = assetRootNodeId;
	}

	public void setBootUrl(URL bootUrl) {
		this.bootUrl = bootUrl;
	}

	public void setDefaultFactsValidDelayMs(long defaultFactsValidDelayMs) {
		this.defaultFactsValidDelayMs = defaultFactsValidDelayMs;
	}

	public void setDefaultFactsValidUntilMs(long defaultFactsValidUntilMs) {
		this.defaultFactsValidUntilMs = defaultFactsValidUntilMs;
	}

	public void setDeleteDirectoryDescription(String deleteDirectoryDescription) {
		this.deleteDirectoryDescription = deleteDirectoryDescription;
	}

	public void setDeleteDirectoryExecutable(Boolean deleteDirectoryExecutable) {
		this.deleteDirectoryExecutable = deleteDirectoryExecutable;
	}

	public void setDeleteDirectoryLabel(String deleteDirectoryLabel) {
		this.deleteDirectoryLabel = deleteDirectoryLabel;
	}

	public void setDeleteDirectoryUserWriteMask(UInteger deleteDirectoryUserWriteMask) {
		this.deleteDirectoryUserWriteMask = deleteDirectoryUserWriteMask;
	}

	public void setDeleteDirectoryWriteMask(UInteger deleteDirectoryWriteMask) {
		this.deleteDirectoryWriteMask = deleteDirectoryWriteMask;
	}

	public void setExecCommandDescription(String execCommandDescription) {
		this.execCommandDescription = execCommandDescription;
	}

	public void setExecCommandExecutable(Boolean execCommandExecutable) {
		this.execCommandExecutable = execCommandExecutable;
	}

	public void setExecCommandLabel(String execCommandLabel) {
		this.execCommandLabel = execCommandLabel;
	}

	public void setExecCommandUserExecutable(Boolean execCommandUserExecutable) {
		this.execCommandUserExecutable = execCommandUserExecutable;
	}

	public void setExecCommandUserWriteMask(UInteger execCommandUserWriteMask) {
		this.execCommandUserWriteMask = execCommandUserWriteMask;
	}

	public void setExecCommandWriteMask(UInteger execCommandWriteMask) {
		this.execCommandWriteMask = execCommandWriteMask;
	}

	public void setFactoryPassword(String factoryPassword) {
		this.factoryPassword = factoryPassword;
	}

	public void setFactoryUsername(String factoryUsername) {
		this.factoryUsername = factoryUsername;
	}

	public void setHelpCommandDescription(String helpCommandDescription) {
		this.helpCommandDescription = helpCommandDescription;
	}

	public void setHelpCommandExecutable(Boolean helpCommandExecutable) {
		this.helpCommandExecutable = helpCommandExecutable;
	}

	public void setHelpCommandLabel(String helpCommandLabel) {
		this.helpCommandLabel = helpCommandLabel;
	}

	public void setHelpCommandUserExecutable(Boolean helpCommandUserExecutable) {
		this.helpCommandUserExecutable = helpCommandUserExecutable;
	}

	public void setHelpCommandUserWriteMask(UInteger helpCommandUserWriteMask) {
		this.helpCommandUserWriteMask = helpCommandUserWriteMask;
	}

	public void setHelpCommandWriteMask(UInteger helpCommandWriteMask) {
		this.helpCommandWriteMask = helpCommandWriteMask;
	}

	public void setHelpDirectory(String helpDirectory) {
		this.helpDirectory = helpDirectory;
	}

	public void setInterfaceRootNodeBrowseName(String interfaceRootNodeBrowseName) {
		this.interfaceRootNodeBrowseName = interfaceRootNodeBrowseName;
	}

	public void setInterfaceRootNodeDisplayName(String interfaceRootNodeDisplayName) {
		this.interfaceRootNodeDisplayName = interfaceRootNodeDisplayName;
	}

	public void setInterfaceRootNodeId(String interfaceRootNodeId) {
		this.interfaceRootNodeId = interfaceRootNodeId;
	}

	public void setManagerNamespaceUri(String managerNamespaceUri) {
		this.managerNamespaceUri = managerNamespaceUri;
	}

	public void setOsCheckDelayCommandDescription(String osCheckDelayCommandDescription) {
		this.osCheckDelayCommandDescription = osCheckDelayCommandDescription;
	}

	public void setOsCheckDelayCommandExecutable(Boolean osCheckDelayCommandExecutable) {
		this.osCheckDelayCommandExecutable = osCheckDelayCommandExecutable;
	}

	public void setOsCheckDelayCommandLabel(String osCheckDelayCommandLabel) {
		this.osCheckDelayCommandLabel = osCheckDelayCommandLabel;
	}

	public void setOsCheckDelayCommandUserExecutable(Boolean osCheckDelayCommandUserExecutable) {
		this.osCheckDelayCommandUserExecutable = osCheckDelayCommandUserExecutable;
	}

	public void setOsCheckDelayCommandUserWriteMask(UInteger osCheckDelayCommandUserWriteMask) {
		this.osCheckDelayCommandUserWriteMask = osCheckDelayCommandUserWriteMask;
	}

	public void setOsCheckDelayCommandWriteMask(UInteger osCheckDelayCommandWriteMask) {
		this.osCheckDelayCommandWriteMask = osCheckDelayCommandWriteMask;
	}

	public void setRootNodeBrowseName(String rootNodeBrowseName) {
		this.rootNodeBrowseName = rootNodeBrowseName;
	}

	public void setRootNodeDisplayName(String rootNodeDisplayName) {
		this.rootNodeDisplayName = rootNodeDisplayName;
	}

	public void setRootNodeId(String rootNodeId) {
		this.rootNodeId = rootNodeId;
	}

	public void setWaldotCommandDescription(String waldotCommandDescription) {
		this.waldotCommandDescription = waldotCommandDescription;
	}

	public void setWaldotCommandExecutable(Boolean waldotCommandExecutable) {
		this.waldotCommandExecutable = waldotCommandExecutable;
	}

	public void setWaldotCommandLabel(String waldotCommandLabel) {
		this.waldotCommandLabel = waldotCommandLabel;
	}

	public void setWaldotCommandUserExecutable(Boolean waldotCommandUserExecutable) {
		this.waldotCommandUserExecutable = waldotCommandUserExecutable;
	}

	public void setWaldotCommandUserWriteMask(UInteger waldotCommandUserWriteMask) {
		this.waldotCommandUserWriteMask = waldotCommandUserWriteMask;
	}

	public void setWaldotCommandWriteMask(UInteger waldotCommandWriteMask) {
		this.waldotCommandWriteMask = waldotCommandWriteMask;
	}

	public void setZenohConfiguration(String zenohConfiguration) {
		this.zenohConfiguration = zenohConfiguration;
	}

}
