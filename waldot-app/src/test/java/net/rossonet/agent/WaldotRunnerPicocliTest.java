package net.rossonet.agent;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

/**
 * Test class for WaldotRunner Picocli parameter parsing.
 * 
 * <p>Questa classe testa il corretto funzionamento delle annotazioni Picocli
 * in WaldotRunner, verificando che i parametri da linea di comando vengano
 * correttamente mappati sui campi della classe.</p>
 * 
 * @author Andrea Ambrosini - Rossonet s.c.a r.l.
 */
public class WaldotRunnerPicocliTest {

	/**
	 * Test OPC UA configuration parameters parsing.
	 * 
	 * <p>Verifica che i parametri di configurazione OPC UA vengano correttamente
	 * parsati da Picocli.</p>
	 */
	@Test
	public void testOpcUaConfigurationParameters() {
		WaldotRunner runner = new WaldotRunner();
		CommandLine cmd = new CommandLine(runner);
		
		// Test TCP port - use parseArgs instead of execute to avoid running the server
		String[] args = {"--tcp-port", "4840"};
		cmd.parseArgs(args);
		assertEquals(4840, runner.getTcpBindPort());
		
		// Test HTTPS port
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--https-port", "8080"};
		cmd.parseArgs(args);
		assertEquals(8080, runner.getHttpsBindPort());
		
		// Test application name
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--application-name", "Test Server"};
		cmd.parseArgs(args);
		assertEquals("Test Server", runner.getApplicationName());
		
		// Test bind addresses
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--bind-addresses", "192.168.1.100"};
		cmd.parseArgs(args);
		assertEquals("192.168.1.100", runner.getBindAddresses());
		
		// Test bind hostname
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--bind-hostname", "testhost.local"};
		cmd.parseArgs(args);
		assertEquals("testhost.local", runner.getBindHostname());
		
		// Test endpoint path
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--endpoint-path", "/test"};
		cmd.parseArgs(args);
		assertEquals("/test", runner.getPath());
		
		// Test product name
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--product-name", "TestProduct"};
		cmd.parseArgs(args);
		assertEquals("TestProduct", runner.getProductName());
		
		// Test manufacturer name
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--manufacturer-name", "Test Company"};
		cmd.parseArgs(args);
		assertEquals("Test Company", runner.getManufacturerName());
		
		// Test security directory
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--security-dir", "/custom/security"};
		cmd.parseArgs(args);
		assertEquals("/custom/security", runner.getSecurityTempDir());
	}

	/**
	 * Test WaldOT application configuration parameters parsing.
	 * 
	 * <p>Verifica che i parametri di configurazione WaldOT vengano correttamente
	 * parsati da Picocli.</p>
	 */
	@Test
	public void testWaldotConfigurationParameters() {
		WaldotRunner runner = new WaldotRunner();
		CommandLine cmd = new CommandLine(runner);
		
		// Test anonymous access
		String[] args = {"--anonymous-access", "false"};
		cmd.parseArgs(args);
		assertFalse(runner.getAnonymousAccessAllowed());
		
		// Test factory username
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--factory-username", "testuser"};
		cmd.parseArgs(args);
		assertEquals("testuser", runner.getFactoryUsername());
		
		// Test factory password
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--factory-password", "testpass"};
		cmd.parseArgs(args);
		assertEquals("testpass", runner.getFactoryPassword());
		
		// Test namespace URI
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--namespace-uri", "urn:test:namespace"};
		cmd.parseArgs(args);
		assertEquals("urn:test:namespace", runner.getManagerNamespaceUri());
		
		// Test help directory
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--help-directory", "/custom/help"};
		cmd.parseArgs(args);
		assertEquals("/custom/help", runner.getHelpDirectory());
		
		// Test boot URL
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{"--boot-url", "file:///custom/boot.conf"};
		cmd.parseArgs(args);
		assertEquals("file:///custom/boot.conf", runner.getBootUrl());
	}

	/**
	 * Test node configuration parameters parsing.
	 * 
	 * <p>Verifica che i parametri di configurazione dei nodi OPC UA vengano
	 * correttamente parsati da Picocli.</p>
	 */
	@Test
	public void testNodeConfigurationParameters() {
		WaldotRunner runner = new WaldotRunner();
		CommandLine cmd = new CommandLine(runner);
		
		// Test root node configuration
		String[] args = {
			"--root-node-id", "custom_root",
			"--root-node-browse-name", "Custom Root",
			"--root-node-display-name", "Custom Root Display"
		};
		cmd.parseArgs(args);
		assertEquals("custom_root", runner.getRootNodeId());
		assertEquals("Custom Root", runner.getRootNodeBrowseName());
		assertEquals("Custom Root Display", runner.getRootNodeDisplayName());
		
		// Test asset node configuration
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{
			"--asset-node-id", "custom_asset",
			"--asset-node-browse-name", "Custom Asset",
			"--asset-node-display-name", "Custom Asset Display"
		};
		cmd.parseArgs(args);
		assertEquals("custom_asset", runner.getAssetRootNodeId());
		assertEquals("Custom Asset", runner.getAssetRootNodeBrowseName());
		assertEquals("Custom Asset Display", runner.getAssetRootNodeDisplayName());
		
		// Test interface node configuration
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{
			"--interface-node-id", "custom_interface",
			"--interface-node-browse-name", "Custom Interface",
			"--interface-node-display-name", "Custom Interface Display"
		};
		cmd.parseArgs(args);
		assertEquals("custom_interface", runner.getInterfaceRootNodeId());
		assertEquals("Custom Interface", runner.getInterfaceRootNodeBrowseName());
		assertEquals("Custom Interface Display", runner.getInterfaceRootNodeDisplayName());
	}

	/**
	 * Test command configuration parameters parsing.
	 * 
	 * <p>Verifica che i parametri di configurazione dei comandi OPC UA vengano
	 * correttamente parsati da Picocli.</p>
	 */
	@Test
	public void testCommandConfigurationParameters() {
		WaldotRunner runner = new WaldotRunner();
		CommandLine cmd = new CommandLine(runner);
		
		// Test about command configuration
		String[] args = {
			"--about-command-label", "info",
			"--about-command-description", "Show info",
			"--about-command-executable", "false"
		};
		cmd.parseArgs(args);
		assertEquals("info", runner.getAboutCommandLabel());
		assertEquals("Show info", runner.getAboutCommandDescription());
		assertFalse(runner.getAboutCommandExecutable());
		
		// Test exec command configuration
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{
			"--exec-command-label", "execute",
			"--exec-command-description", "Execute command",
			"--exec-command-user-executable", "false"
		};
		cmd.parseArgs(args);
		assertEquals("execute", runner.getExecCommandLabel());
		assertEquals("Execute command", runner.getExecCommandDescription());
		assertFalse(runner.getExecCommandUserExecutable());
		
		// Test help command configuration
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{
			"--help-command-label", "assist",
			"--help-command-description", "Get assistance"
		};
		cmd.parseArgs(args);
		assertEquals("assist", runner.getHelpCommandLabel());
		assertEquals("Get assistance", runner.getHelpCommandDescription());
		
		// Test waldot command configuration
		runner = new WaldotRunner();
		cmd = new CommandLine(runner);
		args = new String[]{
			"--waldot-command-label", "gremlin",
			"--waldot-command-description", "Execute Gremlin query"
		};
		cmd.parseArgs(args);
		assertEquals("gremlin", runner.getWaldotCommandLabel());
		assertEquals("Execute Gremlin query", runner.getWaldotCommandDescription());
	}

	/**
	 * Test default values when no parameters are provided.
	 * 
	 * <p>Verifica che i valori di default vengano utilizzati quando non vengono
	 * forniti parametri da linea di comando.</p>
	 */
	@Test
	public void testDefaultValues() {
		WaldotRunner runner = new WaldotRunner();
		CommandLine cmd = new CommandLine(runner);
		
		// Parse empty args
		cmd.parseArgs(new String[]{});
		
		// Verify OPC UA defaults
		assertEquals(12686, runner.getTcpBindPort());
		assertEquals(8443, runner.getHttpsBindPort());
		assertEquals("WaldOT OPCUA server", runner.getApplicationName());
		assertEquals("0.0.0.0", runner.getBindAddresses());
		assertEquals("127.0.0.1", runner.getBindHostname());
		assertEquals("/waldot", runner.getPath());
		assertEquals("WaldOT", runner.getProductName());
		assertEquals("Rossonet s.c.a r.l.", runner.getManufacturerName());
		assertEquals(".security", runner.getSecurityTempDir());
		
		// Verify WaldOT defaults
		assertTrue(runner.getAnonymousAccessAllowed());
		assertEquals("admin", runner.getFactoryUsername());
		assertEquals("password123", runner.getFactoryPassword());
		assertEquals("urn:rossonet:waldot:engine", runner.getManagerNamespaceUri());
		assertEquals("/app/help", runner.getHelpDirectory());
		
		// Verify node defaults
		assertEquals("waldot", runner.getRootNodeId());
		assertEquals("Gremlin Engine", runner.getRootNodeBrowseName());
		assertEquals("aas", runner.getAssetRootNodeId());
		assertEquals("cmd", runner.getInterfaceRootNodeId());
		
		// Verify command defaults
		assertEquals("about", runner.getAboutCommandLabel());
		assertEquals("exec", runner.getExecCommandLabel());
		assertEquals("help", runner.getHelpCommandLabel());
		assertEquals("query", runner.getWaldotCommandLabel());
	}

	/**
	 * Test help option.
	 * 
	 * <p>Verifica che l'opzione --help di Picocli funzioni correttamente.</p>
	 */
	@Test
	public void testHelpOption() {
		WaldotRunner runner = new WaldotRunner();
		CommandLine cmd = new CommandLine(runner);
		
		// Check that help option is available
		assertTrue(cmd.getCommandSpec().mixinStandardHelpOptions());
	}

	/**
	 * Test version option.
	 * 
	 * <p>Verifica che l'opzione --version di Picocli funzioni correttamente.</p>
	 */
	@Test
	public void testVersionOption() {
		WaldotRunner runner = new WaldotRunner();
		CommandLine cmd = new CommandLine(runner);
		
		// Check that version option is available
		assertTrue(cmd.getCommandSpec().mixinStandardHelpOptions());
	}

	/**
	 * Test multiple parameters combination.
	 * 
	 * <p>Verifica che sia possibile specificare multiple parametri contemporaneamente.</p>
	 */
	@Test
	public void testMultipleParametersCombination() {
		WaldotRunner runner = new WaldotRunner();
		CommandLine cmd = new CommandLine(runner);
		
		String[] args = {
			"--tcp-port", "5000",
			"--application-name", "Multi Test",
			"--anonymous-access", "false",
			"--factory-username", "multiuser",
			"--root-node-id", "multi_root",
			"--about-command-label", "multiabout"
		};
		
		cmd.parseArgs(args);
		
		// Verify all parameters are set correctly
		assertEquals(5000, runner.getTcpBindPort());
		assertEquals("Multi Test", runner.getApplicationName());
		assertFalse(runner.getAnonymousAccessAllowed());
		assertEquals("multiuser", runner.getFactoryUsername());
		assertEquals("multi_root", runner.getRootNodeId());
		assertEquals("multiabout", runner.getAboutCommandLabel());
	}

	/**
	 * Test short option names.
	 * 
	 * <p>Verifica che le opzioni corte (es. -tp invece di --tcp-port) funzionino correttamente.</p>
	 */
	@Test
	public void testShortOptionNames() {
		WaldotRunner runner = new WaldotRunner();
		CommandLine cmd = new CommandLine(runner);
		
		String[] args = {
			"-tp", "6000",
			"-hp", "9000",
			"-an", "Short Test",
			"-ba", "10.0.0.1"
		};
		
		cmd.parseArgs(args);
		
		assertEquals(6000, runner.getTcpBindPort());
		assertEquals(9000, runner.getHttpsBindPort());
		assertEquals("Short Test", runner.getApplicationName());
		assertEquals("10.0.0.1", runner.getBindAddresses());
	}
}
