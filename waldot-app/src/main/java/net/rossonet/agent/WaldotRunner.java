package net.rossonet.agent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.configuration.OpcConfiguration;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;
import net.rossonet.waldot.auth.DefaultAnonymousValidator;
import net.rossonet.waldot.auth.DefaultIdentityValidator;
import net.rossonet.waldot.auth.DefaultX509IdentityValidator;
import net.rossonet.waldot.configuration.DefaultHomunculusConfiguration;
import net.rossonet.waldot.configuration.DefaultOpcUaConfiguration;
import net.rossonet.waldot.gremlin.opcgraph.strategies.boot.SingleFileBootstrapStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.client.BaseClientManagementStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.console.BaseConsoleStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerBaseStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.BaseHistoryStrategy;
import net.rossonet.waldot.namespaces.HomunculusNamespace;
import net.rossonet.waldot.opc.WaldotOpcUaServer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * WaldotRunner is the main class for running the WaldOT OPCUA server. It uses
 * Picocli for command-line interface management and provides methods to start
 * the server and execute commands.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a r.l.
 */
@Command(name = "WaldOT", mixinStandardHelpOptions = true, version = { "${COMMAND-NAME} 1.0",
		"JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
		"OS: ${os.name} ${os.version} ${os.arch}" }, description = "WaldOT OPCUA server", footer = "powered by Rossonet s.c.a r.l.", showEndOfOptionsDelimiterInUsageHelp = true, showAtFileInUsageHelp = true)
public class WaldotRunner implements Callable<Integer>, AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger("WaldOT runner");

	// FIXME: completare con annotazioni Picocli
	public static void main(final String[] args) {
		final WaldotRunner waldotRunner = new WaldotRunner();
		try {
			waldotRunner.runWaldot();
			logger.info("bye, bye from WaldOT");
			System.exit(0);
		} catch (final Exception e) {
			if (waldotRunner != null) {
				try {
					waldotRunner.close();
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		}
	}

	protected String aboutCommandDescription;

	protected Boolean aboutCommandExecutable;

	protected String aboutCommandLabel;

	protected Boolean aboutCommandUserExecutable;

	protected UInteger aboutCommandUserWriteMask;

	protected UInteger aboutCommandWriteMask;

	protected boolean anonymousAccessAllowed;

	protected String applicationName;

	protected String assetRootNodeBrowseName;

	protected String assetRootNodeDisplayName;

	protected String assetRootNodeId;

	protected String bindAddresses;

	protected String bindHostname;

	protected String bootUrl = MainAgent.DEFAULT_FILE_CONFIGURATION_PATH;

	protected long defaultFactsValidDelayMs;

	protected long defaultFactsValidUntilMs;

	protected String dnsAddressCertificateGenerator;

	protected String execCommandDescription;

	protected Boolean execCommandExecutable;

	protected String execCommandLabel;

	protected Boolean execCommandUserExecutable;

	protected UInteger execCommandUserWriteMask;

	protected UInteger execCommandWriteMask;

	protected String factoryPassword;

	protected String factoryUsername;

	protected String helpCommandDescription;

	protected Boolean helpCommandExecutable;

	protected String helpCommandLabel;

	protected Boolean helpCommandUserExecutable;

	protected UInteger helpCommandUserWriteMask;

	protected UInteger helpCommandWriteMask;

	protected String helpDirectory;

	protected int httpsBindPort;

	protected String interfaceRootNodeBrowseName;

	protected String interfaceRootNodeDisplayName;

	protected String interfaceRootNodeId;

	protected String managerNamespaceUri;

	protected String manufacturerName;

	protected String path;

	protected String productName;

	protected String productUri;

	protected String rootNodeBrowseName;

	protected String rootNodeDisplayName;

	protected String rootNodeId;

	@Spec
	CommandSpec spec;

	protected int tcpBindPort;

	private WaldotOpcUaServer waldot;

	protected String waldotCommandDescription;

	protected Boolean waldotCommandExecutable;

	protected String waldotCommandLabel;

	protected Boolean waldotCommandUserExecutable;

	protected UInteger waldotCommandUserWriteMask;

	protected UInteger waldotCommandWriteMask;

	@Override
	public Integer call() throws Exception {
		runWaldot();
		return 0;
	}

	@Override
	public void close() throws Exception {
		if (waldot != null) {
			waldot.close();
		}
	}

	public String getAboutCommandDescription() {

		return aboutCommandDescription;
	}

	public Boolean getAboutCommandExecutable() {

		return aboutCommandExecutable;
	}

	public String getAboutCommandLabel() {

		return aboutCommandLabel;
	}

	public Boolean getAboutCommandUserExecutable() {

		return aboutCommandUserExecutable;
	}

	public UInteger getAboutCommandUserWriteMask() {

		return aboutCommandUserWriteMask;
	}

	public UInteger getAboutCommandWriteMask() {

		return aboutCommandWriteMask;
	}

	public boolean getAnonymousAccessAllowed() {

		return anonymousAccessAllowed;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getAssetRootNodeBrowseName() {

		return assetRootNodeBrowseName;
	}

	public String getAssetRootNodeDisplayName() {

		return assetRootNodeDisplayName;
	}

	public String getAssetRootNodeId() {

		return assetRootNodeId;
	}

	public String getBindAddresses() {
		return bindAddresses;
	}

	public String getBindHostname() {
		return bindHostname;
	}

	public String getBootUrl() {
		return bootUrl;
	}

	public long getDefaultFactsValidDelayMs() {
		return defaultFactsValidDelayMs;
	}

	public long getDefaultFactsValidUntilMs() {
		return defaultFactsValidUntilMs;
	}

	public String getDnsAddressCertificateGenerator() {
		return dnsAddressCertificateGenerator;
	}

	public String getExecCommandDescription() {

		return execCommandDescription;
	}

	public Boolean getExecCommandExecutable() {

		return execCommandExecutable;
	}

	public String getExecCommandLabel() {

		return execCommandLabel;
	}

	public Boolean getExecCommandUserExecutable() {

		return execCommandUserExecutable;
	}

	public UInteger getExecCommandUserWriteMask() {

		return execCommandUserWriteMask;
	}

	public UInteger getExecCommandWriteMask() {

		return execCommandWriteMask;
	}

	public String getFactoryPassword() {

		return factoryPassword;
	}

	public String getFactoryUsername() {

		return factoryUsername;
	}

	public String getHelpCommandDescription() {

		return helpCommandDescription;
	}

	public Boolean getHelpCommandExecutable() {

		return helpCommandExecutable;
	}

	public String getHelpCommandLabel() {

		return helpCommandLabel;
	}

	public Boolean getHelpCommandUserExecutable() {

		return helpCommandUserExecutable;
	}

	public UInteger getHelpCommandUserWriteMask() {

		return helpCommandUserWriteMask;
	}

	public UInteger getHelpCommandWriteMask() {

		return helpCommandWriteMask;
	}

	public String getHelpDirectory() {

		return helpDirectory;
	}

	public int getHttpsBindPort() {
		return httpsBindPort;
	}

	public String getInterfaceRootNodeBrowseName() {

		return interfaceRootNodeBrowseName;
	}

	public String getInterfaceRootNodeDisplayName() {

		return interfaceRootNodeDisplayName;
	}

	public String getInterfaceRootNodeId() {

		return interfaceRootNodeId;
	}

	public String getManagerNamespaceUri() {

		return managerNamespaceUri;
	}

	public String getManufacturerName() {
		return manufacturerName;
	}

	public String getPath() {
		return path;
	}

	public String getProductName() {
		return productName;
	}

	public String getProductUri() {
		return productUri;
	}

	public String getRootNodeBrowseName() {

		return rootNodeBrowseName;
	}

	public String getRootNodeDisplayName() {

		return rootNodeDisplayName;
	}

	public String getRootNodeId() {

		return rootNodeId;
	}

	public int getTcpBindPort() {
		return tcpBindPort;
	}

	public WaldotOpcUaServer getWaldot() {
		return waldot;
	}

	public String getWaldotCommandDescription() {

		return waldotCommandDescription;
	}

	public Boolean getWaldotCommandExecutable() {

		return waldotCommandExecutable;
	}

	public String getWaldotCommandLabel() {

		return waldotCommandLabel;
	}

	public Boolean getWaldotCommandUserExecutable() {
		return waldotCommandUserExecutable;
	}

	public UInteger getWaldotCommandUserWriteMask() {
		return waldotCommandUserWriteMask;
	}

	public UInteger getWaldotCommandWriteMask() {
		return waldotCommandWriteMask;
	}

	public void runWaldot() throws InterruptedException, ExecutionException {
		Thread.currentThread().setName("WaldOT_main");
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		final WaldotConfiguration configuration = DefaultHomunculusConfiguration.getDefault();
		final OpcConfiguration serverConfiguration = DefaultOpcUaConfiguration.getDefault();
		waldot = new WaldotOpcUaServer(configuration, serverConfiguration, new DefaultAnonymousValidator(configuration),
				new DefaultIdentityValidator(configuration), new DefaultX509IdentityValidator(configuration));
		final HomunculusNamespace namespace = new HomunculusNamespace(waldot, new MiloSingleServerBaseStrategy(),
				new BaseHistoryStrategy(), new BaseConsoleStrategy(), configuration, new SingleFileBootstrapStrategy(),
				new BaseClientManagementStrategy(), bootUrl);
		waldot.startup(namespace).get();
		waldot.waitCompletion();
	}

	public void setAboutCommandDescription(final String aboutCommandDescription) {
		this.aboutCommandDescription = aboutCommandDescription;
	}

	public void setAboutCommandExecutable(final Boolean aboutCommandExecutable) {
		this.aboutCommandExecutable = aboutCommandExecutable;
	}

	public void setAboutCommandLabel(final String aboutCommandLabel) {
		this.aboutCommandLabel = aboutCommandLabel;
	}

	public void setAboutCommandUserExecutable(final Boolean aboutCommandUserExecutable) {
		this.aboutCommandUserExecutable = aboutCommandUserExecutable;
	}

	public void setAboutCommandUserWriteMask(final UInteger aboutCommandUserWriteMask) {
		this.aboutCommandUserWriteMask = aboutCommandUserWriteMask;
	}

	public void setAboutCommandWriteMask(final UInteger aboutCommandWriteMask) {
		this.aboutCommandWriteMask = aboutCommandWriteMask;
	}

	public void setAnonymousAccessAllowed(final boolean anonymousAccessAllowed) {
		this.anonymousAccessAllowed = anonymousAccessAllowed;
	}

	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}

	public void setAssetRootNodeBrowseName(final String assetRootNodeBrowseName) {
		this.assetRootNodeBrowseName = assetRootNodeBrowseName;
	}

	public void setAssetRootNodeDisplayName(final String assetRootNodeDisplayName) {
		this.assetRootNodeDisplayName = assetRootNodeDisplayName;
	}

	public void setAssetRootNodeId(final String assetRootNodeId) {
		this.assetRootNodeId = assetRootNodeId;
	}

	public void setBindAddresses(final String bindAddresses) {
		this.bindAddresses = bindAddresses;
	}

	public void setBindHostname(final String bindHostname) {
		this.bindHostname = bindHostname;
	}

	public void setBootUrl(final String bootUrl) {
		this.bootUrl = bootUrl;
	}

	public void setDnsAddressCertificateGenerator(final String dnsAddressCertificateGenerator) {
		this.dnsAddressCertificateGenerator = dnsAddressCertificateGenerator;
	}

	public void setExecCommandDescription(final String execCommandDescription) {
		this.execCommandDescription = execCommandDescription;
	}

	public void setExecCommandExecutable(final Boolean execCommandExecutable) {
		this.execCommandExecutable = execCommandExecutable;
	}

	public void setExecCommandLabel(final String execCommandLabel) {
		this.execCommandLabel = execCommandLabel;
	}

	public void setExecCommandUserExecutable(final Boolean execCommandUserExecutable) {
		this.execCommandUserExecutable = execCommandUserExecutable;
	}

	public void setExecCommandUserWriteMask(final UInteger execCommandUserWriteMask) {
		this.execCommandUserWriteMask = execCommandUserWriteMask;
	}

	public void setExecCommandWriteMask(final UInteger execCommandWriteMask) {
		this.execCommandWriteMask = execCommandWriteMask;
	}

	public void setFactoryPassword(final String factoryPassword) {
		this.factoryPassword = factoryPassword;
	}

	public void setFactoryUsername(final String factoryUsername) {
		this.factoryUsername = factoryUsername;
	}

	public void setHelpCommandDescription(final String helpCommandDescription) {
		this.helpCommandDescription = helpCommandDescription;
	}

	public void setHelpCommandExecutable(final Boolean helpCommandExecutable) {
		this.helpCommandExecutable = helpCommandExecutable;
	}

	public void setHelpCommandLabel(final String helpCommandLabel) {
		this.helpCommandLabel = helpCommandLabel;
	}

	public void setHelpCommandUserExecutable(final Boolean helpCommandUserExecutable) {
		this.helpCommandUserExecutable = helpCommandUserExecutable;
	}

	public void setHelpCommandUserWriteMask(final UInteger helpCommandUserWriteMask) {
		this.helpCommandUserWriteMask = helpCommandUserWriteMask;
	}

	public void setHelpCommandWriteMask(final UInteger helpCommandWriteMask) {
		this.helpCommandWriteMask = helpCommandWriteMask;
	}

	public void setHelpDirectory(final String helpDirectory) {
		this.helpDirectory = helpDirectory;
	}

	public void setHttpsBindPort(final int httpsBindPort) {
		this.httpsBindPort = httpsBindPort;
	}

	public void setInterfaceRootNodeBrowseName(final String interfaceRootNodeBrowseName) {
		this.interfaceRootNodeBrowseName = interfaceRootNodeBrowseName;
	}

	public void setInterfaceRootNodeDisplayName(final String interfaceRootNodeDisplayName) {
		this.interfaceRootNodeDisplayName = interfaceRootNodeDisplayName;
	}

	public void setInterfaceRootNodeId(final String interfaceRootNodeId) {
		this.interfaceRootNodeId = interfaceRootNodeId;
	}

	public void setManagerNamespaceUri(final String managerNamespaceUri) {
		this.managerNamespaceUri = managerNamespaceUri;
	}

	public void setManufacturerName(final String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	public void setProductName(final String productName) {
		this.productName = productName;
	}

	public void setProductUri(final String productUri) {
		this.productUri = productUri;
	}

	public void setRootNodeBrowseName(final String rootNodeBrowseName) {
		this.rootNodeBrowseName = rootNodeBrowseName;
	}

	public void setRootNodeDisplayName(final String rootNodeDisplayName) {
		this.rootNodeDisplayName = rootNodeDisplayName;
	}

	public void setRootNodeId(final String rootNodeId) {
		this.rootNodeId = rootNodeId;
	}

	public void setTcpBindPort(final int tcpBindPort) {
		this.tcpBindPort = tcpBindPort;
	}

	public void setWaldotCommandDescription(final String waldotCommandDescription) {
		this.waldotCommandDescription = waldotCommandDescription;
	}

	public void setWaldotCommandExecutable(final Boolean waldotCommandExecutable) {
		this.waldotCommandExecutable = waldotCommandExecutable;
	}

	public void setWaldotCommandLabel(final String waldotCommandLabel) {
		this.waldotCommandLabel = waldotCommandLabel;
	}

	public void setWaldotCommandUserExecutable(final Boolean waldotCommandUserExecutable) {
		this.waldotCommandUserExecutable = waldotCommandUserExecutable;
	}

	public void setWaldotCommandUserWriteMask(final UInteger waldotCommandUserWriteMask) {
		this.waldotCommandUserWriteMask = waldotCommandUserWriteMask;
	}

	public void setWaldotCommandWriteMask(final UInteger waldotCommandWriteMask) {
		this.waldotCommandWriteMask = waldotCommandWriteMask;
	}

}