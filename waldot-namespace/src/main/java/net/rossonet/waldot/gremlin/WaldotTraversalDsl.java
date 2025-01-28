package net.rossonet.waldot.gremlin;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.GremlinDsl;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;

import net.rossonet.waldot.opc.AbstractOpcCommand;

@GremlinDsl
public interface WaldotTraversalDsl<S, E> extends GraphTraversal.Admin<S, E> {

	public default GraphTraversal<S, Object> exec(final String... methodInputs) {
		if (this instanceof AbstractOpcCommand) {
			final AbstractOpcCommand thisAsAbstractWaldotCommand = ((AbstractOpcCommand) this);
			final Object output = thisAsAbstractWaldotCommand.runCommand(methodInputs);
			// TODO capire come iplementare la risposta con i dati di output dello script
		}
		throw new UnsupportedOperationException("Vertex is not a UaMethodNode");
	}
}