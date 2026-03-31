package net.rossonet.waldot.gremlins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

/**
 * Tests for the new script mode in SingleFileBootstrapStrategy.
 * 
 * <p>This test class verifies that the bootstrap strategy correctly handles
 * both the legacy line-by-line format and the new multi-line Groovy script format.</p>
 * 
 * @author Andrea Ambrosini - Rossonet s.c.a r.l.
 */
public class BootstrapScriptModeTests {
	
	private WaldotGraph g;
	private final NamespaceListener listener = new TestNamespaceListener();
	private WaldotTestClientHandler waldotTestClientHandler;

	@AfterEach
	public void afterEach() {
		clean();
		System.out.println("Test completed");
	}

	@BeforeEach
	public void beforeEach(TestInfo testInfo) {
		System.out.println("Starting test " + testInfo.getTestMethod().get().getName());
		clean();
	}

	private void bootstrapUrlServerInit(String url)
			throws ConfigurationException, InterruptedException, ExecutionException {
		LogHelper.changeJulLogLevel("fine");
		g = OpcFactory.getOpcGraph(url, new LoggerHistoryStrategy());
		g.getWaldotNamespace().addListener(listener);
		Thread.sleep(500);
		waldotTestClientHandler = new WaldotTestClientHandler(g);
	}

	private void clean() {
		// Clean test files
		try {
			Files.deleteIfExists(Path.of("/tmp/boot-script.conf"));
			Files.deleteIfExists(Path.of("/tmp/boot-function.conf"));
			Files.deleteIfExists(Path.of("/tmp/boot-multiline.conf"));
			Files.deleteIfExists(Path.of("/tmp/boot-advanced.conf"));
			Files.deleteIfExists(Path.of("/tmp/boot-legacy-compat.conf"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		
		if (waldotTestClientHandler != null) {
			try {
				waldotTestClientHandler.disconnect();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		if (g != null && g.getWaldotNamespace() != null) {
			try {
				g.getWaldotNamespace().close();
				System.out.println("Graph namespace closed");
				g = null;
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		try {
			while (!NetworkHelper.checkLocalPortAvailable(12686)) {
				System.out.println("Waiting for server shutdown");
				Thread.sleep(5_000);
			}
			Thread.sleep(500);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(Path.of("/tmp/waldot-client.ks"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Test 1: Simple variable assignment (script mode).
	 * 
	 * <p>Verifica che il riconoscimento automatico dello script mode funzioni
	 * quando ci sono assegnazioni di variabili.</p>
	 */
	@Test
	public void testScriptModeWithVariableAssignment() throws Exception {
		final StringBuilder sb = new StringBuilder();
		sb.append("// Script mode test with variable\n");
		sb.append("sensor = graph.addVertex('id', 'test-var', 'label', 'test-script', 'value', 100)\n");
		sb.append("sensor.property('extra', 'data')\n");
		Files.writeString(Path.of("/tmp/boot-script.conf"), sb.toString());
		
		bootstrapUrlServerInit("file:///tmp/boot-script.conf");
		
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-var");
		assert waldotTestClientHandler.checkVertexExists("test-var");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-var", "value", 100);
		assert waldotTestClientHandler.checkVertexValueEquals("test-var", "extra", "data");
	}

	/**
	 * Test 2: Function definition (script mode).
	 * 
	 * <p>Verifica che le definizioni di funzioni attivino lo script mode
	 * e che le funzioni possano essere chiamate.</p>
	 */
	@Test
	public void testScriptModeWithFunctionDefinition() throws Exception {
		final StringBuilder sb = new StringBuilder();
		sb.append("// Function to create sensor\n");
		sb.append("def createSensor(id, val) {\n");
		sb.append("  graph.addVertex('id', id, 'label', 'sensor', 'value', val)\n");
		sb.append("}\n");
		sb.append("\n");
		sb.append("// Create two sensors\n");
		sb.append("createSensor('sensor1', 42)\n");
		sb.append("createSensor('sensor2', 84)\n");
		Files.writeString(Path.of("/tmp/boot-function.conf"), sb.toString());
		
		bootstrapUrlServerInit("file:///tmp/boot-function.conf");
		
		assert g.getWaldotNamespace().getVerticesCount() == 2;
		assert waldotTestClientHandler.checkOpcUaVertexExists("sensor1");
		assert waldotTestClientHandler.checkOpcUaVertexExists("sensor2");
		assert waldotTestClientHandler.checkVertexValueEquals("sensor1", "value", 42);
		assert waldotTestClientHandler.checkVertexValueEquals("sensor2", "value", 84);
	}

	/**
	 * Test 3: Multi-line method chains (script mode).
	 * 
	 * <p>Verifica che le catene di metodi multi-linea con indentazione
	 * attivino lo script mode.</p>
	 */
	@Test
	public void testScriptModeWithMultilineChains() throws Exception {
		final StringBuilder sb = new StringBuilder();
		sb.append("// Multi-line chain test\n");
		sb.append("sensor = g.addV('generator')\n");
		sb.append("  .property('type', 'generator')\n");
		sb.append("  .property('label', 'temp-sensor')\n");
		sb.append("  .property('Algorithm', 'sinusoidal')\n");
		sb.append("  .property('Min', '18')\n");
		sb.append("  .property('Max', '26')\n");
		sb.append("  .next()\n");
		Files.writeString(Path.of("/tmp/boot-multiline.conf"), sb.toString());
		
		bootstrapUrlServerInit("file:///tmp/boot-multiline.conf");
		
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		// Il generator crea un vertice con proprietà specifiche
		assert waldotTestClientHandler.checkVertexExists("temp-sensor");
	}

	/**
	 * Test 4: Advanced script with loops and conditionals.
	 * 
	 * <p>Verifica che script complessi con cicli e condizioni funzionino correttamente.</p>
	 */
	@Test
	public void testScriptModeWithLoopsAndConditionals() throws Exception {
		final StringBuilder sb = new StringBuilder();
		sb.append("// Advanced script with loops\n");
		sb.append("def createZone(name, tempTarget) {\n");
		sb.append("  temp = graph.addVertex('id', \"temp-${name}\", 'label', \"sensor-${name}\", 'target', tempTarget)\n");
		sb.append("  if (tempTarget > 20) {\n");
		sb.append("    temp.property('mode', 'hot')\n");
		sb.append("  } else {\n");
		sb.append("    temp.property('mode', 'cold')\n");
		sb.append("  }\n");
		sb.append("  return temp\n");
		sb.append("}\n");
		sb.append("\n");
		sb.append("// Create 3 zones\n");
		sb.append("3.times { i ->\n");
		sb.append("  createZone(\"zone${i}\", 18 + i * 2)\n");
		sb.append("}\n");
		Files.writeString(Path.of("/tmp/boot-advanced.conf"), sb.toString());
		
		bootstrapUrlServerInit("file:///tmp/boot-advanced.conf");
		
		assert g.getWaldotNamespace().getVerticesCount() == 3;
		assert waldotTestClientHandler.checkVertexExists("temp-zone0");
		assert waldotTestClientHandler.checkVertexExists("temp-zone1");
		assert waldotTestClientHandler.checkVertexExists("temp-zone2");
		
		// Verifica mode based on temperature
		assert waldotTestClientHandler.checkVertexValueEquals("temp-zone0", "mode", "cold");  // 18
		assert waldotTestClientHandler.checkVertexValueEquals("temp-zone1", "mode", "cold");  // 20
		assert waldotTestClientHandler.checkVertexValueEquals("temp-zone2", "mode", "hot");   // 22
	}

	/**
	 * Test 5: Legacy compatibility - line-by-line mode.
	 * 
	 * <p>Verifica che il vecchio formato con commenti '#' continui a funzionare
	 * correttamente (retrocompatibilità).</p>
	 */
	@Test
	public void testLegacyLineByLineMode() throws Exception {
		final StringBuilder sb = new StringBuilder();
		sb.append("# Legacy format with hash comments\n");
		sb.append("graph.addVertex('id', 'legacy1', 'label', 'test-legacy', 'value', 10)\n");
		sb.append("# Another comment\n");
		sb.append("graph.addVertex('id', 'legacy2', 'label', 'test-legacy', 'value', 20)\n");
		sb.append("\n");
		sb.append("graph.addVertex('id', 'legacy3', 'label', 'test-legacy', 'value', 30)\n");
		Files.writeString(Path.of("/tmp/boot-legacy-compat.conf"), sb.toString());
		
		bootstrapUrlServerInit("file:///tmp/boot-legacy-compat.conf");
		
		assert g.getWaldotNamespace().getVerticesCount() == 3;
		assert waldotTestClientHandler.checkVertexExists("legacy1");
		assert waldotTestClientHandler.checkVertexExists("legacy2");
		assert waldotTestClientHandler.checkVertexExists("legacy3");
		assert waldotTestClientHandler.checkVertexValueEquals("legacy1", "value", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("legacy2", "value", 20);
		assert waldotTestClientHandler.checkVertexValueEquals("legacy3", "value", 30);
	}

	/**
	 * Test 6: Mixed format detection.
	 * 
	 * <p>Verifica che quando ci sono sia commenti '#' che '//', venga
	 * correttamente rilevato lo script mode.</p>
	 */
	@Test
	public void testMixedCommentFormatDetection() throws Exception {
		final StringBuilder sb = new StringBuilder();
		sb.append("# Old style comment\n");
		sb.append("// New style comment - triggers script mode\n");
		sb.append("v1 = graph.addVertex('id', 'mixed1', 'label', 'mixed-test', 'value', 111)\n");
		sb.append("v2 = graph.addVertex('id', 'mixed2', 'label', 'mixed-test', 'value', 222)\n");
		Files.writeString(Path.of("/tmp/boot-script.conf"), sb.toString());
		
		bootstrapUrlServerInit("file:///tmp/boot-script.conf");
		
		assert g.getWaldotNamespace().getVerticesCount() == 2;
		assert waldotTestClientHandler.checkVertexExists("mixed1");
		assert waldotTestClientHandler.checkVertexExists("mixed2");
	}

	/**
	 * Test 7: Complex real-world scenario.
	 * 
	 * <p>Simula una configurazione reale con sensori, regole e compute.</p>
	 */
	@Test
	public void testRealWorldScenario() throws Exception {
		final StringBuilder sb = new StringBuilder();
		sb.append("// Real-world HVAC monitoring system\n");
		sb.append("\n");
		sb.append("// Helper function\n");
		sb.append("def createRoom(name, tempMin, tempMax) {\n");
		sb.append("  sensor = graph.addVertex(\n");
		sb.append("    'id', \"sensor-${name}\",\n");
		sb.append("    'label', name,\n");
		sb.append("    'type', 'sensor',\n");
		sb.append("    'tempMin', tempMin,\n");
		sb.append("    'tempMax', tempMax\n");
		sb.append("  )\n");
		sb.append("  return sensor\n");
		sb.append("}\n");
		sb.append("\n");
		sb.append("// Create rooms\n");
		sb.append("office = createRoom('office', 18, 26)\n");
		sb.append("server = createRoom('server-room', 16, 22)\n");
		sb.append("warehouse = createRoom('warehouse', 10, 30)\n");
		sb.append("\n");
		sb.append("// Log summary\n");
		sb.append("log.info(\"HVAC system configured with 3 rooms\")\n");
		Files.writeString(Path.of("/tmp/boot-advanced.conf"), sb.toString());
		
		bootstrapUrlServerInit("file:///tmp/boot-advanced.conf");
		
		assert g.getWaldotNamespace().getVerticesCount() == 3;
		assert waldotTestClientHandler.checkVertexExists("sensor-office");
		assert waldotTestClientHandler.checkVertexExists("sensor-server-room");
		assert waldotTestClientHandler.checkVertexExists("sensor-warehouse");
		
		// Verify properties
		assert waldotTestClientHandler.checkVertexValueEquals("sensor-office", "tempMin", 18);
		assert waldotTestClientHandler.checkVertexValueEquals("sensor-office", "tempMax", 26);
		assert waldotTestClientHandler.checkVertexValueEquals("sensor-server-room", "tempMin", 16);
		assert waldotTestClientHandler.checkVertexValueEquals("sensor-warehouse", "tempMax", 30);
	}

	/**
	 * Test 8: Empty file handling.
	 * 
	 * <p>Verifica che file vuoti o con solo commenti vengano gestiti correttamente.</p>
	 */
	@Test
	public void testEmptyFileHandling() throws Exception {
		final StringBuilder sb = new StringBuilder();
		sb.append("# Only comments\n");
		sb.append("# No actual commands\n");
		sb.append("\n");
		sb.append("# More comments\n");
		Files.writeString(Path.of("/tmp/boot-script.conf"), sb.toString());
		
		bootstrapUrlServerInit("file:///tmp/boot-script.conf");
		
		// Should initialize successfully with no vertices
		assert g.getWaldotNamespace().getVerticesCount() == 0;
	}
}
