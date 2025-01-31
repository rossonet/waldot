
package net.rossonet.waldot.gremlin.opcgraph.jsr223;

import org.apache.tinkerpop.gremlin.jsr223.AbstractGremlinPlugin;
import org.apache.tinkerpop.gremlin.jsr223.DefaultImportCustomizer;
import org.apache.tinkerpop.gremlin.jsr223.ImportCustomizer;

import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputer;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputerView;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcMapEmitter;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcMemory;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcMessenger;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcReduceEmitter;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcWorkerPool;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcEdge;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphVariables;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcHelper;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV1;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV2;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV3;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcProperty;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertex;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertexProperty;
import net.rossonet.waldot.opc.gremlin.GremlinElement;

public final class OpcGraphGremlinPlugin extends AbstractGremlinPlugin {
	private static final String NAME = "tinkerpop.opcgraph";

	private static final ImportCustomizer imports = DefaultImportCustomizer.build()
			.addClassImports(OpcEdge.class, GremlinElement.class, OpcFactory.class, OpcGraph.class, OpcGraphVariables.class,
					OpcHelper.class, OpcIoRegistryV1.class, OpcIoRegistryV2.class, OpcIoRegistryV3.class,
					OpcProperty.class, OpcVertex.class, OpcVertexProperty.class, OpcGraphComputer.class,
					OpcGraphComputerView.class, OpcMapEmitter.class, OpcMemory.class, OpcMessenger.class,
					OpcReduceEmitter.class, OpcWorkerPool.class)
			.create();

	private static final OpcGraphGremlinPlugin instance = new OpcGraphGremlinPlugin();

	public static OpcGraphGremlinPlugin instance() {
		return instance;
	}

	public OpcGraphGremlinPlugin() {
		super(NAME, imports);
	}
}