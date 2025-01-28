package net.rossonet.waldot.opc;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.GremlinDsl;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;

@GremlinDsl
public interface WaldotTraversalDsl<S, E> extends GraphTraversal.Admin<S, E> {

	public default GraphTraversal<S, Object> exec(final String... methodInputs) {
		if (this instanceof AbstractWaldotCommand) {
			final AbstractWaldotCommand thisAsAbstractWaldotCommand = ((AbstractWaldotCommand) this);
			final Object output = thisAsAbstractWaldotCommand.runCommand(methodInputs);
			// TODO capire come iplementare la risposta con i dati di output dello script
		}
		throw new UnsupportedOperationException("Vertex is not a UaMethodNode");
	}
}