package net.rossonet.waldot.tinkerpop;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource;
import org.apache.tinkerpop.gremlin.server.GraphManager;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;

/**
 * Graph manager of the embedded Gremlin Server that does not own the graphs.
 * <p>
 * The graph exposed by the server is the WaldOT graph, whose life cycle
 * belongs to the namespace: closing it closes the whole namespace and the OPC
 * UA server. {@code DefaultGraphManager} cannot be used because its methods
 * are final and {@link #removeGraph(String)} closes the graph; moreover
 * {@code GremlinServer.stop()} closes every graph still registered. Before
 * stopping the server {@link TinkerPopVertex} calls {@link #detachAll()}, so
 * that the stop of the server does not reach the WaldOT graph.
 * </p>
 * <p>
 * The graphs configured in {@link Settings#graphs} are ignored: the graph and
 * the traversal source come from the bindings of the script engine.
 * </p>
 */
public class WaldotGraphManager implements GraphManager {

	private final Map<String, Graph> graphs = new ConcurrentHashMap<>();

	private final Map<String, TraversalSource> traversalSources = new ConcurrentHashMap<>();

	public WaldotGraphManager(Settings settings) {
	}

	private void closeTx(final Set<String> graphSourceNamesToCloseTxOn,
			final Transaction.Status status) {
		final Set<Graph> graphsToCloseTxOn = new HashSet<>();
		graphSourceNamesToCloseTxOn.forEach(name -> {
			if (graphs.containsKey(name)) {
				graphsToCloseTxOn.add(graphs.get(name));
			} else if (traversalSources.containsKey(name)) {
				graphsToCloseTxOn.add(traversalSources.get(name).getGraph());
			}
		});
		graphsToCloseTxOn.forEach(graph -> {
			if (graph.features().graph().supportsTransactions()
					&& graph.tx().isOpen()) {
				if (status == Transaction.Status.COMMIT) {
					graph.tx().commit();
				} else {
					graph.tx().rollback();
				}
			}
		});
	}

	@Override
	public void commit(final Set<String> graphSourceNamesToCloseTxOn) {
		closeTx(graphSourceNamesToCloseTxOn, Transaction.Status.COMMIT);
	}

	@Override
	public void commitAll() {
		closeTx(graphs.keySet(), Transaction.Status.COMMIT);
	}

	/**
	 * Forgets every graph and traversal source without closing them.
	 */
	public void detachAll() {
		traversalSources.clear();
		graphs.clear();
	}

	@Override
	public Bindings getAsBindings() {
		final Bindings bindings = new SimpleBindings();
		graphs.forEach(bindings::put);
		traversalSources.forEach(bindings::put);
		return bindings;
	}

	@Override
	public Graph getGraph(final String graphName) {
		return graphs.get(graphName);
	}

	@Override
	public Set<String> getGraphNames() {
		return graphs.keySet();
	}

	@Override
	public TraversalSource getTraversalSource(
			final String traversalSourceName) {
		return traversalSources.get(traversalSourceName);
	}

	@Override
	public Set<String> getTraversalSourceNames() {
		return traversalSources.keySet();
	}

	@Override
	public Graph openGraph(final String graphName,
			final Function<String, Graph> supplier) {
		return graphs.computeIfAbsent(graphName, supplier);
	}

	@Override
	public void putGraph(final String graphName, final Graph graph) {
		graphs.put(graphName, graph);
	}

	@Override
	public void putTraversalSource(final String tsName,
			final TraversalSource ts) {
		traversalSources.put(tsName, ts);
	}

	/**
	 * Removes the graph without closing it: it belongs to the WaldOT namespace.
	 */
	@Override
	public Graph removeGraph(final String graphName) {
		return graphs.remove(graphName);
	}

	@Override
	public TraversalSource removeTraversalSource(final String tsName) {
		return traversalSources.remove(tsName);
	}

	@Override
	public void rollback(final Set<String> graphSourceNamesToCloseTxOn) {
		closeTx(graphSourceNamesToCloseTxOn, Transaction.Status.ROLLBACK);
	}

	@Override
	public void rollbackAll() {
		closeTx(graphs.keySet(), Transaction.Status.ROLLBACK);
	}

}
