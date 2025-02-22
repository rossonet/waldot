package net.rossonet.agent;

import org.apache.tinkerpop.gremlin.GraphProviderClass;
import org.apache.tinkerpop.gremlin.process.ProcessComputerSuite;
import org.apache.tinkerpop.gremlin.process.ProcessStandardSuite;
import org.apache.tinkerpop.gremlin.structure.StructureStandardSuite;
import org.junit.runner.RunWith;

import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;

public class GremlinOfficialTest {

	// Process API tests
	@RunWith(ProcessComputerSuite.class)
	@GraphProviderClass(provider = OpcGraphProvider.class, graph = OpcGraph.class)
	public class OpcProcessComputerTest {
	}

	@RunWith(ProcessStandardSuite.class)
	@GraphProviderClass(provider = OpcGraphProvider.class, graph = OpcGraph.class)
	public class OpcProcessStandardTest {
	}

	// Structure API tests
	@RunWith(StructureStandardSuite.class)
	@GraphProviderClass(provider = OpcGraphProvider.class, graph = OpcGraph.class)
	public class OpcStructureStandardTest {
	}

}