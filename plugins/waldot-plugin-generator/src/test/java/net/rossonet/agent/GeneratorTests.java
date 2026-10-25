package net.rossonet.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import net.rossonet.waldot.dataGenerator.GeneratorConstants;
import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;
import net.rossonet.waldot.utils.LogHelper;

/**
 * Integration tests for WaldOT Data Generator Plugin.
 * <p>
 * Test suite che verifica il funzionamento dei generatori di dati:
 * creazione di vertici generatori, verifica degli algoritmi, lettura valori
 * sia dal grafo TinkerPop che via client OPC-UA.
 * </p>
 * 
 * @author Andrea Ambrosini - Zona Autonoma srl
 */
public class GeneratorTests {
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
		Thread.sleep(1);
		waldotTestClientHandler = new WaldotTestClientHandler(g);
	}

	private void clean() {
		try {
			Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(Path.of("/tmp/index.txt"));
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
			WaldotTestEnvironment.awaitOpcPortReleased();
			Thread.sleep(1);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(WaldotTestEnvironment.clientKeystore());
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Test di carico: crea 1000 nodi generatori con algoritmo random.
	 * Verifica che tutti i nodi siano creati correttamente e generino valori
	 * nel range atteso (90-100).
	 */
	@Test
	public void create1000GeneratorNodes() throws Exception {
		simpleServerInit();
		// Crea 1000 generatori random
		for (int i = 0; i < 1000; i++) {
			g.addVertex(MiloStrategy.ID_PARAMETER, "test" + i, GeneratorConstants.ALGORITHM_FIELD.toLowerCase(),
					"random", MiloStrategy.LABEL_FIELD.toLowerCase(), "test" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					GeneratorConstants.DELAY_FIELD.toLowerCase(), "1000",
					GeneratorConstants.MAX_VALUE_FIELD.toLowerCase(), "100",
					GeneratorConstants.MIN_VALUE_FIELD.toLowerCase(), "90");
		}
		Thread.sleep(1);
		for (int i = 0; i < 1000; i++) {
			assert waldotTestClientHandler.checkOpcUaVertexExists("test" + i);
			assert waldotTestClientHandler.checkVertexExists("test" + i);
			assert waldotTestClientHandler.checkOpcUaVertexValueBetween("test" + i, DataGeneratorVertex.VALUE_KEY, 90,
					100);
			assert waldotTestClientHandler.checkVertexValueBetween("test" + i, DataGeneratorVertex.VALUE_KEY, 90, 100);
		}
	}

	/**
	 * Test di tutti gli algoritmi: crea 100 generatori per ogni algoritmo disponibile
	 * (random, incremental, decremental, sinusoidal, triangular).
	 * Verifica che tutti i nodi siano accessibili via OPC-UA e grafo.
	 */
	@Test
	public void createAllGenerator() throws Exception {
		simpleServerInit();
		// Crea 100 generatori per ogni algoritmo
		for (int i = 0; i < 100; i++) {
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "random", MiloStrategy.ID_PARAMETER, "random" + i,
					GeneratorConstants.ALGORITHM_FIELD.toLowerCase(), "random",
					MiloStrategy.LABEL_FIELD.toLowerCase(), "random" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					GeneratorConstants.DELAY_FIELD.toLowerCase(), "1000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "incremental", MiloStrategy.ID_PARAMETER, "incremental" + i,
					GeneratorConstants.ALGORITHM_FIELD.toLowerCase(), "incremental",
					MiloStrategy.LABEL_FIELD.toLowerCase(), "incremental" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					GeneratorConstants.DELAY_FIELD.toLowerCase(), "1000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "decremental", MiloStrategy.ID_PARAMETER, "decremental" + i,
					GeneratorConstants.ALGORITHM_FIELD.toLowerCase(), "decremental",
					MiloStrategy.LABEL_FIELD.toLowerCase(), "decremental" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					GeneratorConstants.DELAY_FIELD.toLowerCase(), "1000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "sinusoidal", MiloStrategy.ID_PARAMETER, "sinusoidal" + i,
					GeneratorConstants.ALGORITHM_FIELD.toLowerCase(), "sinusoidal",
					MiloStrategy.LABEL_FIELD.toLowerCase(), "sinusoidal" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					GeneratorConstants.DELAY_FIELD.toLowerCase(), "5000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "triangular", MiloStrategy.ID_PARAMETER, "triangular" + i,
					GeneratorConstants.ALGORITHM_FIELD.toLowerCase(), "triangular",
					MiloStrategy.LABEL_FIELD.toLowerCase(), "triangular" + i, MiloStrategy.TYPE_FIELD.toLowerCase(),
					GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL,
					GeneratorConstants.DELAY_FIELD.toLowerCase(), "8000");
		}
		Thread.sleep(1);
		for (int i = 0; i < 100; i++) {
			assert waldotTestClientHandler.checkOpcUaVertexExists("random" + i);
			assert waldotTestClientHandler.checkVertexExists("incremental" + i);
			assert waldotTestClientHandler.checkOpcUaVertexExists("decremental" + i);
			assert waldotTestClientHandler.checkVertexExists("sinusoidal" + i);
			assert waldotTestClientHandler.checkOpcUaVertexExists("triangular" + i);
		}
	}

	/**
	 * Test algoritmo decrementale: verifica che il valore decresca correttamente
	 * da 100 a 1 con wrap-around. Controlla la sincronizzazione tra grafo e OPC-UA.
	 */
	@Test
	public void createSingleDecremental() throws Exception {
		simpleServerInit();
		// Crea generatore decrementale
		final Vertex v = g.addVertex(MiloStrategy.ID_PARAMETER, "dectest", MiloStrategy.LABEL_FIELD.toLowerCase(),
				"dectest", MiloStrategy.TYPE_FIELD.toLowerCase(),
				GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL, GeneratorConstants.DELAY_FIELD.toLowerCase(),
				"1000", GeneratorConstants.ALGORITHM_FIELD.toLowerCase(), "decremental",
				GeneratorConstants.MAX_VALUE_FIELD.toLowerCase(), "100",
				GeneratorConstants.MIN_VALUE_FIELD.toLowerCase(), "1");
		System.out.println("created " + v);
		for (final String k : v.keys()) {
			System.out.println(k + ": " + v.property(k));
		}
		Thread.sleep(1);
		long value = waitForValueChange("dectest", -1);
		for (int i = 1; i < 6; i++) {
			assert waldotTestClientHandler.checkOpcUaVertexValueEquals("dectest", DataGeneratorVertex.VALUE_KEY, value);
			assert waldotTestClientHandler.checkVertexValueEquals("dectest", DataGeneratorVertex.VALUE_KEY, value);
			System.out.println("ok: " + value);
			long expected = value - 1;
			if (expected < 1) {
				expected = 100;
			}
			value = waitForValueChange("dectest", value);
			assert value == expected : "atteso " + expected + " ma letto " + value;
		}
	}

	/**
	 * Legge il valore del generatore finche' non diventa diverso da quello
	 * indicato (il generatore aggiorna ogni DELAY ms). Fallisce dopo 5 secondi.
	 */
	private long waitForValueChange(String vertexId, long previous) throws Exception {
		final long deadline = System.currentTimeMillis() + 5000;
		while (System.currentTimeMillis() < deadline) {
			final long current = waldotTestClientHandler.readIntOpcUaVertexValue(vertexId, DataGeneratorVertex.VALUE_KEY);
			if (current != previous && current != 0) {
				return current;
			}
			Thread.sleep(50);
		}
		throw new AssertionError("il valore di " + vertexId + " non e' cambiato entro 5 secondi");
	}

	/**
	 * Test algoritmo incrementale: verifica che il valore cresca correttamente
	 * da 1 a 100 con wrap-around. Controlla la sincronizzazione tra grafo e OPC-UA.
	 */
	@Test
	public void createSingleIncremetal() throws Exception {
		simpleServerInit();
		// Crea generatore incrementale
		final Vertex v = g.addVertex(MiloStrategy.ID_PARAMETER, "inctest", MiloStrategy.LABEL_FIELD.toLowerCase(),
				"inctest", MiloStrategy.TYPE_FIELD.toLowerCase(),
				GeneratorConstants.DATA_GENERATOR_OBJECT_TYPE_LABEL, GeneratorConstants.DELAY_FIELD.toLowerCase(),
				"1000", GeneratorConstants.ALGORITHM_FIELD.toLowerCase(), "incremental",
				GeneratorConstants.MAX_VALUE_FIELD.toLowerCase(), "100",
				GeneratorConstants.MIN_VALUE_FIELD.toLowerCase(), "1");
		System.out.println("created " + v);
		for (final String k : v.keys()) {
			System.out.println(k + ": " + v.property(k));
		}
		Thread.sleep(1);
		long value = waitForValueChange("inctest", -1);
		for (int i = 1; i < 6; i++) {
			assert waldotTestClientHandler.checkOpcUaVertexValueEquals("inctest", DataGeneratorVertex.VALUE_KEY, value);
			assert waldotTestClientHandler.checkVertexValueEquals("inctest", DataGeneratorVertex.VALUE_KEY, value);
			System.out.println("ok: " + value);
			long expected = value + 1;
			if (expected > 100) {
				expected = 1;
			}
			value = waitForValueChange("inctest", value);
			assert value == expected : "atteso " + expected + " ma letto " + value;
		}
	}

	private void simpleServerInit() throws ConfigurationException, InterruptedException, ExecutionException {
		bootstrapUrlServerInit(WaldotTestEnvironment.bootUrl());
	}
}
