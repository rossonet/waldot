package net.rossonet.waldot.client.v2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.api.exchange.GraphRenderOptions;
import net.rossonet.waldot.client.api.WaldotClientException;
import net.rossonet.waldot.client.api.command.CommandResult;
import net.rossonet.waldot.client.api.graph.ElementDescription;
import net.rossonet.waldot.client.api.graph.GraphRendering;
import net.rossonet.waldot.client.api.graph.GraphSnapshot;
import net.rossonet.waldot.client.api.graph.QueryResult;
import net.rossonet.waldot.client.api.graph.WaldotGraphClient;
import net.rossonet.waldot.client.support.BaseOperationCalls;
import net.rossonet.waldot.client.support.ElementDescriptionParser;
import net.rossonet.waldot.client.support.GraphSonSupport;

/**
 * Graph access of the client v2 through the commands {@code query},
 * {@code query json}, {@code describe element} and {@code render graph}.
 */
final class GraphClientV2 implements WaldotGraphClient {

	private final WaldotClientImplV2 owner;

	GraphClientV2(final WaldotClientImplV2 owner) {
		this.owner = owner;
	}

	@Override
	public ElementDescription describe(final NodeId element) {
		return describe(element.toParseableString());
	}

	@Override
	public ElementDescription describe(final String element) {
		final CommandResult result = owner.commandClient()
				.invokeGlobal(DataExchangeProtocol.DESCRIBE_ELEMENT_COMMAND, element).orThrow();
		try {
			return ElementDescriptionParser.parse(result.getString(DataExchangeProtocol.ARG_JSON));
		} catch (final IOException e) {
			throw WaldotClientException.of("invalid description of " + element, e);
		}
	}

	@Override
	public List<Edge> edges(final String expression) {
		return queryObjects(expression, Edge.class);
	}

	@Override
	public NodeId propertyNodeId(final NodeId element, final String key) {
		return new NodeId(element.getNamespaceIndex(), element.getIdentifier() + "/" + key);
	}

	@Override
	public QueryResult query(final String expression) {
		final CommandResult result = owner.commandClient().invokeGlobal(BaseOperationCalls.QUERY_COMMAND, expression);
		if (result.status() != null && result.status().isBad()) {
			result.orThrow();
		}
		final Object output = result.get(0);
		final Object error = result.get(1);
		return new QueryResult(output == null ? "" : output.toString(), error == null ? "" : error.toString());
	}

	@Override
	public String queryJson(final String expression) {
		return owner.commandClient().invokeGlobal(DataExchangeProtocol.QUERY_JSON_COMMAND, expression).orThrow()
				.getString(DataExchangeProtocol.ARG_JSON);
	}

	@Override
	public List<Object> queryObjects(final String expression) {
		final String json = queryJson(expression);
		try {
			return GraphSonSupport.toList(GraphSonSupport.read(json));
		} catch (final IOException e) {
			throw WaldotClientException.of("invalid GraphSON result of '" + expression + "'", e);
		}
	}

	@Override
	public <T> List<T> queryObjects(final String expression, final Class<T> type) {
		final List<T> result = new ArrayList<>();
		for (final Object value : queryObjects(expression)) {
			if (type.isInstance(value)) {
				result.add(type.cast(value));
			}
		}
		return result;
	}

	@Override
	public Object queryValue(final String expression) {
		final String json = queryJson(expression);
		try {
			final Object value = GraphSonSupport.read(json);
			if (value instanceof List<?> list && list.size() <= 1) {
				return list.isEmpty() ? null : list.get(0);
			}
			return value;
		} catch (final IOException e) {
			throw WaldotClientException.of("invalid GraphSON result of '" + expression + "'", e);
		}
	}

	@Override
	public GraphRendering render(final GraphRenderOptions options, final int blockSize) {
		GraphSnapshotClientV2.checkBlockSize(blockSize);
		final CommandResult result = owner.commandClient().invokeGlobal(DataExchangeProtocol.RENDER_GRAPH_COMMAND,
				(options == null ? new GraphRenderOptions() : options).toJson(), "true", String.valueOf(blockSize))
				.orThrow();
		final byte[] payload = BlockDownloads.payload(owner, result, blockSize);
		final byte[] data;
		try {
			data = GraphSnapshot.of(payload).uncompressed();
		} catch (final UncheckedIOException e) {
			throw WaldotClientException.of("invalid drawing of the graph", e);
		}
		final String warnings = result.getString(DataExchangeProtocol.ARG_WARNINGS).trim();
		return new GraphRendering(data, result.getString(DataExchangeProtocol.ARG_FORMAT).trim(),
				BlockDownloads.intOutput(result, DataExchangeProtocol.ARG_VERTICES),
				BlockDownloads.intOutput(result, DataExchangeProtocol.ARG_EDGES),
				warnings.isEmpty() ? List.of() : List.of(warnings.split("\n")),
				Math.max(1, BlockDownloads.intOutput(result, DataExchangeProtocol.ARG_BLOCKS)));
	}

	@Override
	public List<Vertex> vertices(final String expression) {
		return queryObjects(expression, Vertex.class);
	}
}
