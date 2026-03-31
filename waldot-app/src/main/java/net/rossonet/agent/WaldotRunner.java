package net.rossonet.agent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
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
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.namespaces.HomunculusNamespace;
import net.rossonet.waldot.opc.WaldotOpcUaServer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * WaldotRunner is the main class for running the WaldOT OPC UA server.
 * 
 * <p>This class uses Picocli for command-line interface management and provides
 * methods to start and configure the WaldOT server. It accepts configuration
 * parameters both from command-line arguments and environment variables, with
 * command-line arguments taking precedence.</p>
 * 
 * <p>The server can be configured through:</p>
 * <ul>
 *   <li>Command-line options (e.g., --tcp-port 12686)</li>
 *   <li>Environment variables (e.g., WALDOT_TCP_PORT=12686)</li>
 *   <li>Default values from configuration classes</li>
 * </ul>
 * 
 * <p>Configuration parameters are divided into two categories:</p>
 * <ul>
 *   <li><b>OPC UA Configuration</b>: Server identity, network binding, security</li>
 *   <li><b>WaldOT Configuration</b>: Application-specific settings, commands, nodes</li>
 * </ul>
 * 
 * @author Andrea Ambrosini - Rossonet s.c.a r.l.
 * @see OpcConfiguration
 * @see WaldotConfiguration
 */
@Command(name = "WaldOT", mixinStandardHelpOptions = true, version = { "${COMMAND-NAME} 1.0",
		"JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
		"OS: ${os.name} ${os.version} ${os.arch}" }, description = "WaldOT OPC UA server with TinkerPop graph integration", footer = "Powered by Rossonet s.c.a r.l. - https://www.rossonet.net", showEndOfOptionsDelimiterInUsageHelp = true, showAtFileInUsageHelp = true)
public class WaldotRunner implements Callable<Integer>, AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger("WaldOT runner");

	/**
	 * Main entry point for the WaldOT application.
	 * 
	 * <p>This method is called when running the application from the command line
	 * or in a Docker container. It uses Picocli to parse command-line arguments
	 * and execute the WaldotRunner.</p>
	 * 
	 * @param args command-line arguments
	 */
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

	// ========================================
	// OPC UA Server Configuration Parameters
	// ========================================
	
	/**
	 * OPC UA application name that identifies this server instance.
	 * 
	 * <p>This name appears in OPC UA client applications when browsing for servers
	 * and is used in the server's Application Description. It should be descriptive
	 * and unique within your network.</p>
	 * 
	 * <p><b>Default</b>: "WaldOT OPCUA server"</p>
	 */
	@Option(names = { "--application-name", "-an" }, description = "OPC UA application name identifying this server instance", defaultValue = "WaldOT OPCUA server")
	protected String applicationName;

	/**
	 * Comma-separated list of IP addresses where the OPC UA server will bind.
	 * 
	 * <p>Defines the network interfaces on which the server listens for client connections.
	 * Use "0.0.0.0" to bind to all available interfaces, or specify specific IP addresses
	 * to restrict access to certain networks.</p>
	 * 
	 * <p><b>Examples</b>:</p>
	 * <ul>
	 *   <li>"0.0.0.0" - bind to all interfaces (default)</li>
	 *   <li>"127.0.0.1" - localhost only</li>
	 *   <li>"192.168.1.100,10.0.0.50" - specific interfaces</li>
	 * </ul>
	 * 
	 * <p><b>Default</b>: "0.0.0.0"</p>
	 */
	@Option(names = { "--bind-addresses", "-ba" }, description = "Comma-separated list of IP addresses for server binding (e.g., 0.0.0.0 for all interfaces)", defaultValue = "0.0.0.0")
	protected String bindAddresses;

	/**
	 * Hostname used in the OPC UA endpoint URL.
	 * 
	 * <p>This hostname appears in the server's endpoint URLs (e.g., opc.tcp://hostname:port/path)
	 * and must be resolvable by OPC UA clients. It's also used for SSL certificate generation
	 * and validation.</p>
	 * 
	 * <p><b>Important</b>: In Docker deployments, this should match the container's network
	 * hostname or the host machine's address accessible to clients.</p>
	 * 
	 * <p><b>Default</b>: "127.0.0.1"</p>
	 */
	@Option(names = { "--bind-hostname", "-bh" }, description = "Hostname for OPC UA endpoint URLs and SSL certificates", defaultValue = "127.0.0.1")
	protected String bindHostname;

	/**
	 * DNS address or hostname used for SSL certificate generation.
	 * 
	 * <p>When the server generates self-signed SSL certificates, this address is included
	 * in the certificate's Subject Alternative Names (SAN). This allows OPC UA clients
	 * to validate the certificate against the server's DNS name.</p>
	 * 
	 * <p><b>Security Note</b>: Must match the hostname clients use to connect, otherwise
	 * certificate validation will fail.</p>
	 * 
	 * <p><b>Default</b>: "127.0.0.1"</p>
	 */
	@Option(names = { "--dns-address-cert", "-dc" }, description = "DNS address for SSL certificate Subject Alternative Name (SAN)", defaultValue = "127.0.0.1")
	protected String dnsAddressCertificateGenerator;

	/**
	 * TCP port for OPC UA binary protocol endpoint.
	 * 
	 * <p>The OPC UA binary protocol (opc.tcp://) is the primary communication protocol
	 * for industrial automation, offering efficient binary serialization and high performance.
	 * Most OPC UA clients connect via this port.</p>
	 * 
	 * <p><b>Standard Ports</b>:</p>
	 * <ul>
	 *   <li>4840 - OPC UA standard port</li>
	 *   <li>12686 - WaldOT default port</li>
	 * </ul>
	 * 
	 * <p><b>Docker Note</b>: This port must be exposed in the container mapping.</p>
	 * 
	 * <p><b>Default</b>: 12686</p>
	 */
	@Option(names = { "--tcp-port", "-tp" }, description = "TCP port for OPC UA binary protocol endpoint (opc.tcp://)", defaultValue = "12686")
	protected int tcpBindPort;

	/**
	 * HTTPS port for OPC UA web services and REST API.
	 * 
	 * <p>Enables HTTPS access to OPC UA services and the WaldOT REST API. This port
	 * is used for web-based monitoring, GraphQL queries, and HTTP-based integrations.</p>
	 * 
	 * <p><b>Use Cases</b>:</p>
	 * <ul>
	 *   <li>Web browser access to server status</li>
	 *   <li>RESTful API integrations</li>
	 *   <li>GraphQL queries over HTTP</li>
	 * </ul>
	 * 
	 * <p><b>Default</b>: 8443</p>
	 */
	@Option(names = { "--https-port", "-hp" }, description = "HTTPS port for OPC UA web services and REST API", defaultValue = "8443")
	protected int httpsBindPort;

	/**
	 * URL path component for OPC UA endpoints.
	 * 
	 * <p>Defines the path portion of OPC UA endpoint URLs. The complete endpoint URL
	 * follows the format: opc.tcp://hostname:port/path</p>
	 * 
	 * <p><b>Example</b>: With path="/waldot", hostname="localhost", port=12686,
	 * the endpoint becomes: opc.tcp://localhost:12686/waldot</p>
	 * 
	 * <p><b>Default</b>: "/waldot"</p>
	 */
	@Option(names = { "--endpoint-path", "-ep" }, description = "URL path component for OPC UA endpoints (e.g., /waldot)", defaultValue = "/waldot")
	protected String path;

	/**
	 * Product name displayed in OPC UA server identification.
	 * 
	 * <p>This name appears in the server's BuildInfo node and is visible to OPC UA clients
	 * in their server browser. It identifies the product family or software name.</p>
	 * 
	 * <p><b>Default</b>: "WaldOT"</p>
	 */
	@Option(names = { "--product-name", "-pn" }, description = "Product name in OPC UA server BuildInfo", defaultValue = "WaldOT")
	protected String productName;

	/**
	 * Product URI uniquely identifying this OPC UA product.
	 * 
	 * <p>A unique identifier for the OPC UA product, typically in URI format. This is part
	 * of the OPC UA Application Description and helps clients identify the server software.</p>
	 * 
	 * <p><b>Format</b>: Should follow URI syntax (e.g., urn:company:product:version)</p>
	 * 
	 * <p><b>Default</b>: "urn:rossonet:waldot:uaserver"</p>
	 */
	@Option(names = { "--product-uri", "-pu" }, description = "Unique product URI for OPC UA Application Description", defaultValue = "urn:rossonet:waldot:uaserver")
	protected String productUri;

	/**
	 * Manufacturer name for OPC UA server identification.
	 * 
	 * <p>The company or organization name that appears in the server's BuildInfo.
	 * This is displayed in OPC UA client applications and helps identify the server vendor.</p>
	 * 
	 * <p><b>Default</b>: "Rossonet s.c.a r.l."</p>
	 */
	@Option(names = { "--manufacturer-name", "-mn" }, description = "Manufacturer name in OPC UA BuildInfo", defaultValue = "Rossonet s.c.a r.l.")
	protected String manufacturerName;

	/**
	 * Directory path for OPC UA security certificates and keys.
	 * 
	 * <p>Location where the server stores its PKI (Public Key Infrastructure) components:</p>
	 * <ul>
	 *   <li>Server certificate and private key</li>
	 *   <li>Trusted client certificates</li>
	 *   <li>Certificate revocation lists (CRLs)</li>
	 *   <li>Rejected certificates</li>
	 * </ul>
	 * 
	 * <p><b>Docker Note</b>: Should be mounted as a volume to persist security credentials
	 * across container restarts.</p>
	 * 
	 * <p><b>Security Warning</b>: Protect this directory with appropriate file permissions
	 * as it contains private keys.</p>
	 * 
	 * <p><b>Default</b>: ".security"</p>
	 */
	@Option(names = { "--security-dir", "-sd" }, description = "Directory for SSL certificates, private keys, and PKI trust lists", defaultValue = ".security")
	protected String securityTempDir;

	/**
	 * Build number for version tracking and identification.
	 * 
	 * <p>An identifier for the specific build or release of the server software. This appears
	 * in the OPC UA BuildInfo node and helps track deployed versions.</p>
	 * 
	 * <p><b>Default</b>: "w001"</p>
	 */
	@Option(names = { "--build-number", "-bn" }, description = "Build number for version tracking in OPC UA BuildInfo", defaultValue = "w001")
	protected String buildNumber;

	// ========================================
	// WaldOT Application Configuration Parameters
	// ========================================
	
	/**
	 * File URL for bootstrap configuration to load on startup.
	 * 
	 * <p>Path to a configuration file that defines the initial graph structure, nodes,
	 * and connections to create when the server starts. This enables pre-configured
	 * deployments and automated setup.</p>
	 * 
	 * <p><b>Format</b>: file:// URL pointing to configuration file</p>
	 * 
	 * <p><b>Docker Default</b>: file:///waldot/boot.conf</p>
	 * <p><b>Default</b>: file:///waldot/boot.conf</p>
	 */
	@Option(names = { "--boot-url", "-bu" }, description = "File URL for bootstrap configuration to load on startup", defaultValue = MainAgent.DEFAULT_FILE_CONFIGURATION_PATH)
	protected String bootUrl;

	/**
	 * Flag to allow anonymous access to the OPC UA server.
	 * 
	 * <p>When enabled, OPC UA clients can connect without providing credentials.
	 * When disabled, clients must authenticate using username/password or X.509 certificates.</p>
	 * 
	 * <p><b>Security Consideration</b>: Enabling anonymous access in production environments
	 * may pose security risks. Use authentication for sensitive deployments.</p>
	 * 
	 * <p><b>Default</b>: true</p>
	 */
	@Option(names = { "--anonymous-access", "-aa" }, description = "Allow anonymous client connections without authentication", defaultValue = "true")
	protected boolean anonymousAccessAllowed;

	/**
	 * Factory default username for administrative access.
	 * 
	 * <p>The default administrator username created on first startup. This account
	 * has full permissions to execute commands and modify the server configuration.</p>
	 * 
	 * <p><b>Security Warning</b>: Change this password in production deployments!</p>
	 * 
	 * <p><b>Default</b>: "admin"</p>
	 */
	@Option(names = { "--factory-username", "-fu" }, description = "Default administrator username for initial setup", defaultValue = "admin")
	protected String factoryUsername;

	/**
	 * Factory default password for administrative access.
	 * 
	 * <p>The default administrator password created on first startup.</p>
	 * 
	 * <p><b>Security Warning</b>: Change this immediately in production! This password
	 * is widely known and poses a critical security risk if left unchanged.</p>
	 * 
	 * <p><b>Default</b>: "password123"</p>
	 */
	@Option(names = { "--factory-password", "-fp" }, description = "Default administrator password (CHANGE IN PRODUCTION!)", defaultValue = "password123")
	protected String factoryPassword;

	/**
	 * URI for the WaldOT OPC UA namespace.
	 * 
	 * <p>The namespace URI identifies the WaldOT-specific nodes and types in the OPC UA
	 * address space. This separates WaldOT objects from standard OPC UA nodes and other
	 * vendor-specific nodes.</p>
	 * 
	 * <p><b>OPC UA Namespaces</b>:</p>
	 * <ul>
	 *   <li>Namespace 0: OPC UA standard nodes</li>
	 *   <li>Namespace 1+: Vendor-specific (WaldOT uses this)</li>
	 * </ul>
	 * 
	 * <p><b>Default</b>: "urn:rossonet:waldot:engine"</p>
	 */
	@Option(names = { "--namespace-uri", "-nu" }, description = "OPC UA namespace URI for WaldOT-specific nodes and types", defaultValue = "urn:rossonet:waldot:engine")
	protected String managerNamespaceUri;

	/**
	 * Directory path containing help documentation files.
	 * 
	 * <p>Location of markdown or text files that provide command documentation, examples,
	 * and usage information. These files are accessible through the OPC UA "help" command.</p>
	 * 
	 * <p><b>Docker Default</b>: /app/help</p>
	 * 
	 * <p><b>Default</b>: "/app/help"</p>
	 */
	@Option(names = { "--help-directory", "-hd" }, description = "Directory containing help documentation files", defaultValue = "/app/help")
	protected String helpDirectory;

	// Node Configuration - Root Node
	/**
	 * NodeId for the root node in the OPC UA address space.
	 * 
	 * <p>The root node serves as the top-level container for all WaldOT-managed nodes.
	 * All graph vertices and plugin objects appear as children of this node.</p>
	 * 
	 * <p><b>Default</b>: "waldot"</p>
	 */
	@Option(names = { "--root-node-id" }, description = "NodeId for the WaldOT root node in OPC UA address space", defaultValue = "waldot")
	protected String rootNodeId;

	/**
	 * Browse name for the root node visible in OPC UA clients.
	 * 
	 * <p>The programmatic name used when browsing the OPC UA address space. This is
	 * typically a simple identifier without spaces.</p>
	 * 
	 * <p><b>Default</b>: "Gremlin Engine"</p>
	 */
	@Option(names = { "--root-node-browse-name" }, description = "Browse name for the root node (identifier without spaces)", defaultValue = "Gremlin Engine")
	protected String rootNodeBrowseName;

	/**
	 * Display name for the root node shown in OPC UA client GUIs.
	 * 
	 * <p>The human-readable name displayed in OPC UA client applications. This can
	 * include spaces and special characters for better readability.</p>
	 * 
	 * <p><b>Default</b>: "Gremlin Engine"</p>
	 */
	@Option(names = { "--root-node-display-name" }, description = "Human-readable display name for the root node", defaultValue = "Gremlin Engine")
	protected String rootNodeDisplayName;

	// Node Configuration - Asset Root Node
	/**
	 * NodeId for the asset administration root node.
	 * 
	 * <p>The asset node serves as a container for administrative and operational objects.
	 * This follows the ISA-95 standard structure for organizing industrial assets.</p>
	 * 
	 * <p><b>Default</b>: "aas"</p>
	 */
	@Option(names = { "--asset-node-id" }, description = "NodeId for asset administration root node", defaultValue = "aas")
	protected String assetRootNodeId;

	/**
	 * Browse name for the asset root node.
	 * 
	 * <p><b>Default</b>: "Administration"</p>
	 */
	@Option(names = { "--asset-node-browse-name" }, description = "Browse name for asset root node", defaultValue = "Administration")
	protected String assetRootNodeBrowseName;

	/**
	 * Display name for the asset root node.
	 * 
	 * <p><b>Default</b>: "Administration"</p>
	 */
	@Option(names = { "--asset-node-display-name" }, description = "Display name for asset root node", defaultValue = "Administration")
	protected String assetRootNodeDisplayName;

	// Node Configuration - Interface Root Node
	/**
	 * NodeId for the command interface root node.
	 * 
	 * <p>The interface node contains all executable OPC UA methods/commands. Clients
	 * can invoke these commands to control the server and execute operations.</p>
	 * 
	 * <p><b>Default</b>: "cmd"</p>
	 */
	@Option(names = { "--interface-node-id" }, description = "NodeId for command interface root node", defaultValue = "cmd")
	protected String interfaceRootNodeId;

	/**
	 * Browse name for the interface root node.
	 * 
	 * <p><b>Default</b>: "Commands"</p>
	 */
	@Option(names = { "--interface-node-browse-name" }, description = "Browse name for interface root node", defaultValue = "Commands")
	protected String interfaceRootNodeBrowseName;

	/**
	 * Display name for the interface root node.
	 * 
	 * <p><b>Default</b>: "Commands"</p>
	 */
	@Option(names = { "--interface-node-display-name" }, description = "Display name for interface root node", defaultValue = "Commands")
	protected String interfaceRootNodeDisplayName;

	// Command Configuration - About
	/**
	 * Label for the "about" command in OPC UA methods.
	 * 
	 * <p><b>Default</b>: "about"</p>
	 */
	@Option(names = { "--about-command-label" }, description = "Label for the about command", defaultValue = "about")
	protected String aboutCommandLabel;

	/**
	 * Description for the "about" command explaining its purpose.
	 * 
	 * <p><b>Default</b>: "info about this software"</p>
	 */
	@Option(names = { "--about-command-description" }, description = "Description for the about command", defaultValue = "info about this software")
	protected String aboutCommandDescription;

	/**
	 * Flag to make the "about" command executable.
	 * 
	 * <p><b>Default</b>: true</p>
	 */
	@Option(names = { "--about-command-executable" }, description = "Enable about command execution", defaultValue = "true")
	protected Boolean aboutCommandExecutable;

	/**
	 * Flag to allow regular users to execute the "about" command.
	 * 
	 * <p><b>Default</b>: true</p>
	 */
	@Option(names = { "--about-command-user-executable" }, description = "Allow users to execute about command", defaultValue = "true")
	protected Boolean aboutCommandUserExecutable;

	protected UInteger aboutCommandUserWriteMask;

	protected UInteger aboutCommandWriteMask;

	// Command Configuration - Exec
	/**
	 * Label for the "exec" system command execution method.
	 * 
	 * <p><b>Security Warning</b>: This command allows executing system commands.
	 * Restrict access in production environments!</p>
	 * 
	 * <p><b>Default</b>: "exec"</p>
	 */
	@Option(names = { "--exec-command-label" }, description = "Label for the exec command", defaultValue = "exec")
	protected String execCommandLabel;

	/**
	 * Description for the "exec" command.
	 * 
	 * <p><b>Default</b>: "run system command"</p>
	 */
	@Option(names = { "--exec-command-description" }, description = "Description for the exec command", defaultValue = "run system command")
	protected String execCommandDescription;

	/**
	 * Flag to make the "exec" command executable.
	 * 
	 * <p><b>Default</b>: true</p>
	 */
	@Option(names = { "--exec-command-executable" }, description = "Enable exec command execution", defaultValue = "true")
	protected Boolean execCommandExecutable;

	/**
	 * Flag to allow regular users to execute system commands.
	 * 
	 * <p><b>Security Warning</b>: Allowing user execution of system commands can be dangerous!</p>
	 * 
	 * <p><b>Default</b>: true</p>
	 */
	@Option(names = { "--exec-command-user-executable" }, description = "Allow users to execute system commands", defaultValue = "true")
	protected Boolean execCommandUserExecutable;

	protected UInteger execCommandUserWriteMask;

	protected UInteger execCommandWriteMask;

	// Command Configuration - Help
	/**
	 * Label for the "help" command that lists available commands.
	 * 
	 * <p><b>Default</b>: "help"</p>
	 */
	@Option(names = { "--help-command-label" }, description = "Label for the help command", defaultValue = "help")
	protected String helpCommandLabel;

	/**
	 * Description for the "help" command.
	 * 
	 * <p><b>Default</b>: "list available commands"</p>
	 */
	@Option(names = { "--help-command-description" }, description = "Description for the help command", defaultValue = "list available commands")
	protected String helpCommandDescription;

	/**
	 * Flag to make the "help" command executable.
	 * 
	 * <p><b>Default</b>: true</p>
	 */
	@Option(names = { "--help-command-executable" }, description = "Enable help command execution", defaultValue = "true")
	protected Boolean helpCommandExecutable;

	/**
	 * Flag to allow regular users to execute the "help" command.
	 * 
	 * <p><b>Default</b>: true</p>
	 */
	@Option(names = { "--help-command-user-executable" }, description = "Allow users to execute help command", defaultValue = "true")
	protected Boolean helpCommandUserExecutable;

	protected UInteger helpCommandUserWriteMask;

	protected UInteger helpCommandWriteMask;

	// Command Configuration - Waldot (Gremlin Query)
	/**
	 * Label for the Gremlin query execution command.
	 * 
	 * <p>This command allows executing Gremlin queries directly from OPC UA clients,
	 * enabling graph traversal and manipulation through the OPC UA interface.</p>
	 * 
	 * <p><b>Default</b>: "query"</p>
	 */
	@Option(names = { "--waldot-command-label" }, description = "Label for Gremlin query command", defaultValue = "query")
	protected String waldotCommandLabel;

	/**
	 * Description for the Gremlin query command.
	 * 
	 * <p><b>Default</b>: "run Gremlin query"</p>
	 */
	@Option(names = { "--waldot-command-description" }, description = "Description for Gremlin query command", defaultValue = "run Gremlin query")
	protected String waldotCommandDescription;

	/**
	 * Flag to make the Gremlin query command executable.
	 * 
	 * <p><b>Default</b>: true</p>
	 */
	@Option(names = { "--waldot-command-executable" }, description = "Enable Gremlin query execution", defaultValue = "true")
	protected Boolean waldotCommandExecutable;

	/**
	 * Flag to allow regular users to execute Gremlin queries.
	 * 
	 * <p><b>Security Note</b>: Gremlin queries have full graph access. Consider
	 * restricting this in production environments with sensitive data.</p>
	 * 
	 * <p><b>Default</b>: true</p>
	 */
	@Option(names = { "--waldot-command-user-executable" }, description = "Allow users to execute Gremlin queries", defaultValue = "true")
	protected Boolean waldotCommandUserExecutable;

	protected UInteger waldotCommandUserWriteMask;

	protected UInteger waldotCommandWriteMask;

	// Additional Configuration Parameters
	/**
	 * Default delay in milliseconds before facts become valid.
	 * 
	 * <p>Used by the rules engine to introduce a delay before newly created facts
	 * are considered valid for rule evaluation. This can prevent race conditions
	 * and allow for stabilization periods.</p>
	 * 
	 * <p><b>Default</b>: 0 (no delay)</p>
	 */
	@Option(names = { "--facts-valid-delay" }, description = "Delay in ms before facts become valid in rules engine", defaultValue = "0")
	protected long defaultFactsValidDelayMs;

	/**
	 * Default expiration time in milliseconds for facts validity.
	 * 
	 * <p>Defines how long facts remain valid in the rules engine before expiring.
	 * A value of 0 means facts never expire.</p>
	 * 
	 * <p><b>Default</b>: 0 (never expires)</p>
	 */
	@Option(names = { "--facts-valid-until" }, description = "Time in ms until facts expire in rules engine (0=never)", defaultValue = "0")
	protected long defaultFactsValidUntilMs;

	/**
	 * Picocli command specification (injected automatically).
	 */
	@Spec
	CommandSpec spec;

	/**
	 * The WaldOT OPC UA server instance.
	 * 
	 * <p>Istanza del server OPC UA WaldOT.</p>
	 */
	private WaldotOpcUaServer waldot;

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

	public String getSecurityTempDir() {
		return securityTempDir;
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

	/**
	 * Runs the WaldOT OPC UA server with the configured parameters.
	 * 
	 * <p>Questo metodo avvia il server OPC UA WaldOT utilizzando i parametri configurati
	 * tramite Picocli. I parametri possono essere forniti via linea di comando o variabili
	 * d'ambiente. Se non sono forniti, vengono utilizzati i valori di default.</p>
	 * 
	 * <p><b>Processo di avvio</b>:</p>
	 * <ol>
	 *   <li>Crea configurazioni di default</li>
	 *   <li>Sovrascrive con parametri Picocli se presenti</li>
	 *   <li>Inizializza il server OPC UA</li>
	 *   <li>Carica il namespace Homunculus</li>
	 *   <li>Avvia il server e attende il completamento</li>
	 * </ol>
	 * 
	 * @throws InterruptedException if the server thread is interrupted
	 * @throws ExecutionException if the server startup fails
	 */
	public void runWaldot() throws InterruptedException, ExecutionException {
		// Imposta nome e priorità del thread principale
		Thread.currentThread().setName("WaldOT_main");
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		
		// Crea configurazioni di default
		final DefaultHomunculusConfiguration configuration = (DefaultHomunculusConfiguration) DefaultHomunculusConfiguration.getDefault();
		final DefaultOpcUaConfiguration serverConfiguration = (DefaultOpcUaConfiguration) DefaultOpcUaConfiguration.getDefault();
		
		// Popola le configurazioni OPC UA con i parametri Picocli
		// I parametri Picocli hanno la precedenza sui valori di default
		if (applicationName != null) {
			serverConfiguration.setApplicationName(applicationName);
		}
		if (bindAddresses != null) {
			serverConfiguration.setBindAddresses(bindAddresses);
		}
		if (bindHostname != null) {
			serverConfiguration.setBindHostname(bindHostname);
		}
		if (dnsAddressCertificateGenerator != null) {
			serverConfiguration.setDnsAddressCertificateGenerator(dnsAddressCertificateGenerator);
		}
		serverConfiguration.setTcpBindPort(tcpBindPort);
		serverConfiguration.setHttpsBindPort(httpsBindPort);
		if (path != null) {
			serverConfiguration.setPath(path);
		}
		if (productName != null) {
			serverConfiguration.setProductName(productName);
		}
		if (productUri != null) {
			serverConfiguration.setProductUri(productUri);
		}
		if (manufacturerName != null) {
			serverConfiguration.setManufacturerName(manufacturerName);
		}
		if (securityTempDir != null) {
			serverConfiguration.setSecurityTempDir(securityTempDir);
		}
		if (buildNumber != null) {
			serverConfiguration.setBuildNumber(buildNumber);
		}
		
		// Popola le configurazioni WaldOT con i parametri Picocli
		configuration.setAnonymousAccessAllowed(anonymousAccessAllowed);
		if (factoryUsername != null) {
			configuration.setFactoryUsername(factoryUsername);
		}
		if (factoryPassword != null) {
			configuration.setFactoryPassword(factoryPassword);
		}
		if (managerNamespaceUri != null) {
			configuration.setManagerNamespaceUri(managerNamespaceUri);
		}
		if (helpDirectory != null) {
			configuration.setHelpDirectory(helpDirectory);
		}
		
		// Configurazione nodi root
		if (rootNodeId != null) {
			configuration.setRootNodeId(rootNodeId);
		}
		if (rootNodeBrowseName != null) {
			configuration.setRootNodeBrowseName(rootNodeBrowseName);
		}
		if (rootNodeDisplayName != null) {
			configuration.setRootNodeDisplayName(rootNodeDisplayName);
		}
		
		// Configurazione nodo asset
		if (assetRootNodeId != null) {
			configuration.setAssetRootNodeId(assetRootNodeId);
		}
		if (assetRootNodeBrowseName != null) {
			configuration.setAssetRootNodeBrowseName(assetRootNodeBrowseName);
		}
		if (assetRootNodeDisplayName != null) {
			configuration.setAssetRootNodeDisplayName(assetRootNodeDisplayName);
		}
		
		// Configurazione nodo interfaccia
		if (interfaceRootNodeId != null) {
			configuration.setInterfaceRootNodeId(interfaceRootNodeId);
		}
		if (interfaceRootNodeBrowseName != null) {
			configuration.setInterfaceRootNodeBrowseName(interfaceRootNodeBrowseName);
		}
		if (interfaceRootNodeDisplayName != null) {
			configuration.setInterfaceRootNodeDisplayName(interfaceRootNodeDisplayName);
		}
		
		// Configurazione comandi About
		if (aboutCommandLabel != null) {
			configuration.setAboutCommandLabel(aboutCommandLabel);
		}
		if (aboutCommandDescription != null) {
			configuration.setAboutCommandDescription(aboutCommandDescription);
		}
		if (aboutCommandExecutable != null) {
			configuration.setAboutCommandExecutable(aboutCommandExecutable);
		}
		if (aboutCommandUserExecutable != null) {
			configuration.setAboutCommandUserExecutable(aboutCommandUserExecutable);
		}
		
		// Configurazione comandi Exec
		if (execCommandLabel != null) {
			configuration.setExecCommandLabel(execCommandLabel);
		}
		if (execCommandDescription != null) {
			configuration.setExecCommandDescription(execCommandDescription);
		}
		if (execCommandExecutable != null) {
			configuration.setExecCommandExecutable(execCommandExecutable);
		}
		if (execCommandUserExecutable != null) {
			configuration.setExecCommandUserExecutable(execCommandUserExecutable);
		}
		
		// Configurazione comandi Help
		if (helpCommandLabel != null) {
			configuration.setHelpCommandLabel(helpCommandLabel);
		}
		if (helpCommandDescription != null) {
			configuration.setHelpCommandDescription(helpCommandDescription);
		}
		if (helpCommandExecutable != null) {
			configuration.setHelpCommandExecutable(helpCommandExecutable);
		}
		if (helpCommandUserExecutable != null) {
			configuration.setHelpCommandUserExecutable(helpCommandUserExecutable);
		}
		
		// Configurazione comandi Waldot (Gremlin query)
		if (waldotCommandLabel != null) {
			configuration.setWaldotCommandLabel(waldotCommandLabel);
		}
		if (waldotCommandDescription != null) {
			configuration.setWaldotCommandDescription(waldotCommandDescription);
		}
		if (waldotCommandExecutable != null) {
			configuration.setWaldotCommandExecutable(waldotCommandExecutable);
		}
		if (waldotCommandUserExecutable != null) {
			configuration.setWaldotCommandUserExecutable(waldotCommandUserExecutable);
		}
		
		// Configurazione parametri aggiuntivi
		configuration.setDefaultFactsValidDelayMs(defaultFactsValidDelayMs);
		configuration.setDefaultFactsValidUntilMs(defaultFactsValidUntilMs);
		
		// Log della configurazione
		logger.info("Starting WaldOT OPC UA Server");
		logger.info("  Application Name: {}", serverConfiguration.getApplicationName());
		logger.info("  TCP Port: {}", serverConfiguration.getTcpBindPort());
		logger.info("  HTTPS Port: {}", serverConfiguration.getHttpsBindPort());
		logger.info("  Bind Addresses: {}", serverConfiguration.getBindAddresses());
		logger.info("  Hostname: {}", serverConfiguration.getBindHostname());
		logger.info("  Endpoint Path: {}", serverConfiguration.getPath());
		logger.info("  Namespace URI: {}", configuration.getManagerNamespaceUri());
		logger.info("  Anonymous Access: {}", configuration.getAnonymousAccessAllowed());
		logger.info("  Boot URL: {}", bootUrl);
		
		// Inizializza il server OPC UA
		waldot = new WaldotOpcUaServer(configuration, serverConfiguration, new DefaultAnonymousValidator(configuration),
				new DefaultIdentityValidator(configuration), new DefaultX509IdentityValidator(configuration));
		
		// Crea il namespace Homunculus
		final HomunculusNamespace namespace = new HomunculusNamespace(waldot, new MiloSingleServerBaseStrategy(),
				new LoggerHistoryStrategy(), new BaseConsoleStrategy(), configuration,
				new SingleFileBootstrapStrategy(), new BaseClientManagementStrategy(), bootUrl);
		
		// Avvia il server e attendi il completamento
		waldot.startup(namespace).get();
		logger.info("WaldOT OPC UA Server started successfully");
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