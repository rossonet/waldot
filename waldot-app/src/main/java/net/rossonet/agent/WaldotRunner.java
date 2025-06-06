package net.rossonet.agent;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.auth.DefaultAnonymousValidator;
import net.rossonet.waldot.auth.DefaultIdentityValidator;
import net.rossonet.waldot.auth.DefaultX509IdentityValidator;
import net.rossonet.waldot.configuration.DefaultHomunculusConfiguration;
import net.rossonet.waldot.configuration.DefaultOpcUaConfiguration;
import net.rossonet.waldot.gremlin.opcgraph.strategies.boot.SingleFileWithStagesBootstrapStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.console.ConsoleV0Strategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerBaseV0Strategy;
import net.rossonet.waldot.namespaces.HomunculusNamespace;
import net.rossonet.waldot.opc.WaldotOpcUaServer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "WaldOT", mixinStandardHelpOptions = true, version = { "${COMMAND-NAME} 1.0",
		"JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
		"OS: ${os.name} ${os.version} ${os.arch}" }, description = "WaldOT OPCUA server", footer = "powered by Rossonet s.c.a r.l.", showEndOfOptionsDelimiterInUsageHelp = true, showAtFileInUsageHelp = true)
public class WaldotRunner implements Callable<Integer>, AutoCloseable {
// TODO: completare con annotazioni Picocli
	public static void main(String[] args) {
		final WaldotRunner waldotRunner = new WaldotRunner();
		try {
			waldotRunner.runWaldot();
			System.out.println("bye, bye from WaldOT");
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

	protected URL bootUrl;

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

	protected String fileConfigurationPath = MainAgent.DEFAULT_FILE_CONFIGURATION_PATH;

	protected String helpCommandDescription;

	protected Boolean helpCommandExecutable;

	protected String helpCommandLabel;

	protected Boolean helpCommandUserExecutable;

	protected UInteger helpCommandUserWriteMask;

	protected UInteger helpCommandWriteMask;

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

	protected String versionCommandDescription;

	protected Boolean versionCommandExecutable;

	protected String versionCommandLabel;

	protected Boolean versionCommandUserExecutable;

	protected UInteger versionCommandUserWriteMask;

	protected UInteger versionCommandWriteMask;

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

	public URL getBootUrl() {

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

	public String getFileConfigurationPath() {
		return fileConfigurationPath;
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

	public String getVersionCommandDescription() {

		return versionCommandDescription;
	}

	public Boolean getVersionCommandExecutable() {

		return versionCommandExecutable;
	}

	public String getVersionCommandLabel() {

		return versionCommandLabel;
	}

	public Boolean getVersionCommandUserExecutable() {

		return versionCommandUserExecutable;
	}

	public UInteger getVersionCommandUserWriteMask() {

		return versionCommandUserWriteMask;
	}

	public UInteger getVersionCommandWriteMask() {

		return versionCommandWriteMask;
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

	private void runBootConfiguration(List<String> configuration) {
		int num = 0;
		for (final String line : configuration) {
			if (line.startsWith("#") || line.isEmpty()) {
				continue;
			}
			num++;
			try {
				final Object runExpression = waldot.runExpression(line);
				if (runExpression != null) {
					System.out.println("[" + num + "]: " + line + " ->\n" + runExpression + "\n");
				} else {
					System.out.println("[" + num + "]: " + line + " -> no result\n");
				}
			} catch (final Exception e) {
				System.err.println("Error executing command '" + line + "': " + e.getMessage());
			}
		}

	}

	public void runWaldot() throws InterruptedException, ExecutionException {
		final DefaultHomunculusConfiguration configuration = DefaultHomunculusConfiguration.getDefault();
		final DefaultOpcUaConfiguration serverConfiguration = DefaultOpcUaConfiguration.getDefault();
		waldot = new WaldotOpcUaServer(configuration, serverConfiguration, new DefaultAnonymousValidator(configuration),
				new DefaultIdentityValidator(configuration), new DefaultX509IdentityValidator(configuration));
		final HomunculusNamespace namespace = new HomunculusNamespace(waldot, new MiloSingleServerBaseV0Strategy(),
				new ConsoleV0Strategy(), configuration, new SingleFileWithStagesBootstrapStrategy(), bootUrl);
		waldot.startup(namespace).get();
		final Path target = Path.of(getFileConfigurationPath());
		if (Files.exists(target)) {
			try {
				System.out.println("\n\nBoot configuration file found at " + target.toAbsolutePath() + "\n\n");
				runBootConfiguration(Files.readAllLines(target));
			} catch (final Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("No boot configuration file found at " + target.toAbsolutePath());
		}
		waldot.waitCompletion();
	}

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

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
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

	public void setBindAddresses(String bindAddresses) {
		this.bindAddresses = bindAddresses;
	}

	public void setBindHostname(String bindHostname) {
		this.bindHostname = bindHostname;
	}

	public void setBootUrl(URL bootUrl) {
		this.bootUrl = bootUrl;
	}

	public void setDnsAddressCertificateGenerator(String dnsAddressCertificateGenerator) {
		this.dnsAddressCertificateGenerator = dnsAddressCertificateGenerator;
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

	public void setFileConfigurationPath(String fileConfigurationPath) {
		this.fileConfigurationPath = fileConfigurationPath;
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

	public void setHttpsBindPort(int httpsBindPort) {
		this.httpsBindPort = httpsBindPort;
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

	public void setManufacturerName(String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public void setProductUri(String productUri) {
		this.productUri = productUri;
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

	public void setTcpBindPort(int tcpBindPort) {
		this.tcpBindPort = tcpBindPort;
	}

	public void setVersionCommandDescription(String versionCommandDescription) {
		this.versionCommandDescription = versionCommandDescription;
	}

	public void setVersionCommandExecutable(Boolean versionCommandExecutable) {
		this.versionCommandExecutable = versionCommandExecutable;
	}

	public void setVersionCommandLabel(String versionCommandLabel) {
		this.versionCommandLabel = versionCommandLabel;
	}

	public void setVersionCommandUserExecutable(Boolean versionCommandUserExecutable) {
		this.versionCommandUserExecutable = versionCommandUserExecutable;
	}

	public void setVersionCommandUserWriteMask(UInteger versionCommandUserWriteMask) {
		this.versionCommandUserWriteMask = versionCommandUserWriteMask;
	}

	public void setVersionCommandWriteMask(UInteger versionCommandWriteMask) {
		this.versionCommandWriteMask = versionCommandWriteMask;
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

}