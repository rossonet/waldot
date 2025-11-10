package net.rossonet.agent;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.WaldotOsPlugin;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;

public class WaldOTServerTest {

	// @Test
	public void run20000GeneratorServer() throws Exception {
		LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(500);
		for (int i = 0; i < 20000; i++) {
			g.addVertex(MiloStrategy.ID_PARAMETER, "test" + i, WaldotOsPlugin.ALGORITHM_FIELD.toLowerCase(), "random",
					MiloStrategy.LABEL_FIELD.toLowerCase(), "test" + i, MiloStrategy.TYPE_DEFINITION_PARAMETER,
					WaldotOsPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL, WaldotOsPlugin.DELAY_FIELD.toLowerCase(), "1000");
		}
		Thread.sleep(120_000);
		g.getWaldotNamespace().getOpcuaServer().close();
	}

	@Test
	public void runAllGeneratorServer() throws Exception {
		LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(500);
		for (int i = 0; i < 5000; i++) {
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "random", MiloStrategy.ID_PARAMETER, "random" + i,
					WaldotOsPlugin.ALGORITHM_FIELD.toLowerCase(), "random", MiloStrategy.LABEL_FIELD.toLowerCase(),
					"random" + i, MiloStrategy.TYPE_DEFINITION_PARAMETER,
					WaldotOsPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL, WaldotOsPlugin.DELAY_FIELD.toLowerCase(), "1000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "incrmental", MiloStrategy.ID_PARAMETER, "incrmental" + i,
					WaldotOsPlugin.ALGORITHM_FIELD.toLowerCase(), "incrmental", MiloStrategy.LABEL_FIELD.toLowerCase(),
					"incrmental" + i, MiloStrategy.TYPE_DEFINITION_PARAMETER,
					WaldotOsPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL, WaldotOsPlugin.DELAY_FIELD.toLowerCase(), "1000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "decremental", MiloStrategy.ID_PARAMETER, "decremental" + i,
					WaldotOsPlugin.ALGORITHM_FIELD.toLowerCase(), "decremental", MiloStrategy.LABEL_FIELD.toLowerCase(),
					"decremental" + i, MiloStrategy.TYPE_DEFINITION_PARAMETER,
					WaldotOsPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL, WaldotOsPlugin.DELAY_FIELD.toLowerCase(), "1000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "sinusoidal", MiloStrategy.ID_PARAMETER, "sinusoidal" + i,
					WaldotOsPlugin.ALGORITHM_FIELD.toLowerCase(), "sinusoidal", MiloStrategy.LABEL_FIELD.toLowerCase(),
					"sinusoidal" + i, MiloStrategy.TYPE_DEFINITION_PARAMETER,
					WaldotOsPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL, WaldotOsPlugin.DELAY_FIELD.toLowerCase(), "5000");
			g.addVertex(MiloStrategy.DIRECTORY_PARAMETER, "triangular", MiloStrategy.ID_PARAMETER, "triangular" + i,
					WaldotOsPlugin.ALGORITHM_FIELD.toLowerCase(), "triangular", MiloStrategy.LABEL_FIELD.toLowerCase(),
					"triangular" + i, MiloStrategy.TYPE_DEFINITION_PARAMETER,
					WaldotOsPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL, WaldotOsPlugin.DELAY_FIELD.toLowerCase(), "8000");
		}
		Thread.sleep(60_000 * 5);
		g.getWaldotNamespace().getOpcuaServer().close();
	}

	// @Test
	public void runSimpleGeneratorServer() throws Exception {
		LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(500);
		g.addVertex(MiloStrategy.ID_PARAMETER, "test1", MiloStrategy.LABEL_FIELD.toLowerCase(), "test1",
				MiloStrategy.TYPE_DEFINITION_PARAMETER, WaldotOsPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL);
		g.addVertex(MiloStrategy.ID_PARAMETER, "test2", MiloStrategy.LABEL_FIELD.toLowerCase(), "test2",
				MiloStrategy.TYPE_DEFINITION_PARAMETER, WaldotOsPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL,
				WaldotOsPlugin.DELAY_FIELD.toLowerCase(), "1000");
		g.addVertex(MiloStrategy.ID_PARAMETER, "test3", MiloStrategy.LABEL_FIELD.toLowerCase(), "test3",
				MiloStrategy.TYPE_DEFINITION_PARAMETER, WaldotOsPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL,
				WaldotOsPlugin.DELAY_FIELD.toLowerCase(), "400", WaldotOsPlugin.ALGORITHM_FIELD.toLowerCase(),
				"decremental", WaldotOsPlugin.MAX_VALUE_FIELD.toLowerCase(), "100",
				WaldotOsPlugin.MIN_VALUE_FIELD.toLowerCase(), "50");
		g.addVertex(MiloStrategy.ID_PARAMETER, "test4", MiloStrategy.LABEL_FIELD.toLowerCase(), "test4",
				MiloStrategy.TYPE_DEFINITION_PARAMETER, WaldotOsPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL,
				WaldotOsPlugin.DELAY_FIELD.toLowerCase(), "4000", WaldotOsPlugin.ALGORITHM_FIELD.toLowerCase(),
				"random", WaldotOsPlugin.MAX_VALUE_FIELD.toLowerCase(), "-1",
				WaldotOsPlugin.MIN_VALUE_FIELD.toLowerCase(), "-5000");
		g.addVertex(MiloStrategy.ID_PARAMETER, "test5", MiloStrategy.LABEL_FIELD.toLowerCase(), "test5",
				MiloStrategy.TYPE_DEFINITION_PARAMETER, WaldotOsPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL,
				WaldotOsPlugin.DELAY_FIELD.toLowerCase(), "4000", WaldotOsPlugin.ALGORITHM_FIELD.toLowerCase(),
				"error_field");
		Thread.sleep(120_000);
		g.getWaldotNamespace().getOpcuaServer().close();
	}
}
