package net.rossonet.waldot.gremlin.opcgraph.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.B_O_Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.TraverserRequirement;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.util.TraverserSet;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.service.Service;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.gremlin.opcgraph.services.OpcServiceRegistry.LambdaBarrierService;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcServiceRegistry.LambdaServiceFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.testutil.FakeNamespaces;

/**
 * Service registry of the Gremlin graph, without the OPC UA server.
 */
class OpcServiceRegistryTest {

	private OpcGraph graph;

	@BeforeEach
	void setUp() {
		graph = OpcGraph.open();
		graph.setNamespace(FakeNamespaces.empty());
	}

	@AfterEach
	void tearDown() {
		graph.close();
	}

	private static <T> List<T> toList(final Iterator<T> iterator) {
		final List<T> list = new ArrayList<>();
		iterator.forEachRemaining(list::add);
		return list;
	}

	@Test
	void lambdaStartService() {
		final LambdaServiceFactory<Object, String> factory = graph.getServiceRegistry().registerLambdaService("start");
		factory.addStartLambda((ctx, params) -> IteratorUtils.of("a", "b"));
		factory.addDescribeParams(Map.of("p", "description"));
		assertEquals("start", factory.getName());
		assertEquals(Set.of(Service.Type.Start), factory.getSupportedTypes());
		assertEquals("description", factory.describeParams().get("p"));
		final Service<Object, String> service = factory.createService(true, Map.of());
		assertEquals(Service.Type.Start, service.getType());
		assertEquals(List.of("a", "b"), toList(service.execute(null, Map.of())));
		// no streaming/barrier lambda: it cannot be used mid traversal
		assertThrows(UnsupportedOperationException.class, () -> factory.createService(false, Map.of()));
	}

	@Test
	void lambdaStreamingService() {
		final LambdaServiceFactory<String, String> factory = graph.getServiceRegistry().registerLambdaService("stream");
		factory.addStreamingLambda((ctx, traverser, params) -> IteratorUtils.of(traverser.get().toUpperCase()));
		assertThrows(UnsupportedOperationException.class, () -> factory.createService(true, Map.of()));
		final Service<String, String> service = factory.createService(false, Map.of());
		assertEquals(Service.Type.Streaming, service.getType());
		final Traverser.Admin<String> in = new B_O_Traverser<>("abc", 1);
		assertEquals(List.of("ABC"), toList(service.execute(null, in, Map.of())));
	}

	@Test
	void lambdaBarrierServiceWithDynamicChunkSize() {
		final LambdaServiceFactory<String, Integer> factory = graph.getServiceRegistry().registerLambdaService("barrier");
		factory.addBarrierLambda((ctx, traversers, params) -> IteratorUtils.of(traversers.size()), 10);
		final Service<String, Integer> service = factory.createService(false, Map.of());
		assertEquals(Service.Type.Barrier, service.getType());
		assertEquals(10, service.getMaxBarrierSize());
		final Service<String, Integer> chunked = factory
				.createService(false, Map.of(LambdaServiceFactory.Options.CHUNK_SIZE, 2));
		assertEquals(2, chunked.getMaxBarrierSize());
		final TraverserSet<String> in = new TraverserSet<>();
		in.add(new B_O_Traverser<>("a", 1));
		in.add(new B_O_Traverser<>("b", 1));
		assertEquals(List.of(2), toList(chunked.execute(null, in, Map.of())));
	}

	@Test
	void streamingAndBarrierAreChosenByParameter() {
		final LambdaServiceFactory<String, String> factory = graph.getServiceRegistry().registerLambdaService("both");
		factory.addStreamingLambda((ctx, traverser, params) -> IteratorUtils.of("streaming"));
		final LambdaBarrierService<String, String> barrier = factory
				.addBarrierLambda((ctx, traversers, params) -> IteratorUtils.of("barrier"));
		assertEquals(Integer.MAX_VALUE, barrier.getMaxBarrierSize());
		assertEquals(Service.Type.Streaming, factory.createService(false, Map.of()).getType());
		assertSame(barrier, factory.createService(false,
				Map.of(LambdaServiceFactory.Options.TYPE, Service.Type.Barrier)));
		final TraverserSet<String> in = new TraverserSet<>();
		assertEquals(List.of("barrier"), toList(barrier.execute(null, in, Map.of())));
	}

	@Test
	void factoryWithoutLambdasCannotBeUsed() {
		final LambdaServiceFactory<Object, Object> factory = graph.getServiceRegistry().registerLambdaService("none");
		assertTrue(factory.getSupportedTypes().isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> factory.createService(false, Map.of()));
		assertThrows(UnsupportedOperationException.class, () -> factory.createService(true, Map.of()));
	}

	@Test
	void requirementsAreKeptPerType() {
		final LambdaServiceFactory<String, String> factory = graph.getServiceRegistry().registerLambdaService("req");
		final OpcServiceRegistry.TinkerService<String, String> service = factory
				.addStreamingLambda((ctx, traverser, params) -> IteratorUtils.of("x"));
		assertTrue(service.getRequirements().isEmpty());
		service.addRequirements(TraverserRequirement.OBJECT);
		assertEquals(Set.of(TraverserRequirement.OBJECT), service.getRequirements());
		assertTrue(factory.getRequirements(Service.Type.Barrier).isEmpty());
	}

	@Test
	void degreeCentralityCountsTheEdgesOfTheDirection() {
		final OpcDegreeCentralityFactory factory = new OpcDegreeCentralityFactory(graph);
		assertEquals(OpcDegreeCentralityFactory.NAME, factory.getName());
		assertEquals(Service.Type.Streaming, factory.getType());
		assertEquals(Set.of(Service.Type.Streaming), factory.getSupportedTypes());
		assertTrue(factory.describeParams().containsKey(OpcDegreeCentralityFactory.Params.DIRECTION));
		assertThrows(UnsupportedOperationException.class, () -> factory.createService(true, Map.of()));
		assertSame(factory, factory.createService(false, Map.of()));

		final Vertex vertex = vertexWithEdges(Map.of(Direction.IN, 2, Direction.OUT, 3));
		// bulk 2: one result for each traverser in the bulk
		final Traverser.Admin<Vertex> in = new B_O_Traverser<>(vertex, 2);
		assertEquals(List.of(2L, 2L), toList(factory.execute(null, in, Map.of())));
		assertEquals(List.of(3L, 3L),
				toList(factory.execute(null, in, Map.of(OpcDegreeCentralityFactory.Params.DIRECTION, Direction.OUT))));
		factory.close();
	}

	@Test
	void textSearchValidatesItsParameters() {
		final OpcTextSearchFactory<Object, Object> factory = new OpcTextSearchFactory<>(graph);
		assertEquals(OpcTextSearchFactory.NAME, factory.getName());
		assertEquals(Service.Type.Start, factory.getType());
		assertEquals(Set.of(Service.Type.Start), factory.getSupportedTypes());
		assertTrue(factory.describeParams().containsKey(OpcTextSearchFactory.Params.SEARCH));
		assertThrows(UnsupportedOperationException.class, () -> factory.createService(false, Map.of()));
		assertSame(factory, factory.createService(true, Map.of()));
		assertThrows(IllegalStateException.class, () -> factory.execute(null, Map.of()));
		assertThrows(IllegalArgumentException.class, () -> factory.execute(null,
				Map.of(OpcTextSearchFactory.Params.SEARCH, "x", OpcTextSearchFactory.Params.TYPE, "Graph")));
		factory.close();
	}

	@Test
	void textSearchOnAnEmptyGraphFindsNothing() {
		final OpcTextSearchFactory<Object, Object> factory = new OpcTextSearchFactory<>(graph);
		for (final String type : new String[] { null, "Vertex", "Edge", "VertexProperty" }) {
			final Map<String, Object> params = new java.util.HashMap<>();
			params.put(OpcTextSearchFactory.Params.REGEX, ".*");
			if (type != null) {
				params.put(OpcTextSearchFactory.Params.TYPE, type);
			}
			assertFalse(factory.execute(null, params).hasNext(), "type " + type);
		}
		assertFalse(factory.execute(null, Map.of(OpcTextSearchFactory.Params.SEARCH, "marko")).hasNext());
	}

	@Test
	void registryReturnsTheRegisteredFactory() {
		final OpcDegreeCentralityFactory factory = new OpcDegreeCentralityFactory(graph);
		assertSame(factory, graph.getServiceRegistry().registerService(factory));
		graph.getServiceRegistry().checkRegisteredService(OpcDegreeCentralityFactory.NAME);
		assertInstanceOf(OpcDegreeCentralityFactory.class,
				graph.getServiceRegistry().get(OpcDegreeCentralityFactory.NAME, false, Map.of()));
		assertThrows(IllegalArgumentException.class,
				() -> graph.getServiceRegistry().checkRegisteredService("not.registered"));
	}

	@Test
	void commandServicesAreBuiltIn() {
		graph.getServiceRegistry().checkRegisteredService(OpcCommandServiceFactory.NAME);
		graph.getServiceRegistry().checkRegisteredService(OpcCommandListServiceFactory.NAME);
		// the list service runs nothing: available with the flag off
		assertInstanceOf(OpcCommandListServiceFactory.class,
				graph.getServiceRegistry().get(OpcCommandListServiceFactory.NAME, false, Map.of()));
	}

	@Test
	void commandServiceFailsWhenDisabled() {
		final String saved = System.getProperty(GremlinCommandSettings.ENABLED_KEY);
		System.clearProperty(GremlinCommandSettings.ENABLED_KEY);
		try {
			final IllegalStateException e = assertThrows(IllegalStateException.class,
					() -> graph.getServiceRegistry().get(OpcCommandServiceFactory.NAME, false,
							Map.of(OpcCommandServiceFactory.Params.COMMAND, "delete")));
			assertTrue(e.getMessage().contains(GremlinCommandSettings.ENABLED_KEY), e.getMessage());
		} finally {
			restore(saved);
		}
	}

	@Test
	void commandServiceTypesAndParameters() {
		final String saved = System.getProperty(GremlinCommandSettings.ENABLED_KEY);
		System.setProperty(GremlinCommandSettings.ENABLED_KEY, "true");
		try {
			final OpcCommandServiceFactory factory = new OpcCommandServiceFactory(graph);
			assertEquals(OpcCommandServiceFactory.NAME, factory.getName());
			assertEquals(Set.of(Service.Type.Barrier, Service.Type.Start), factory.getSupportedTypes());
			assertTrue(factory.describeParams().containsKey(OpcCommandServiceFactory.Params.COMMAND));
			assertTrue(factory.describeParams().containsKey(OpcCommandServiceFactory.Params.ARGS));
			assertThrows(IllegalArgumentException.class, () -> factory.createService(false, Map.of()));
			assertThrows(IllegalArgumentException.class,
					() -> factory.createService(true, Map.of(OpcCommandServiceFactory.Params.COMMAND, " ")));
			final Map<String, Object> params = Map.of(OpcCommandServiceFactory.Params.COMMAND, "delete");
			final Service<Object, Map<String, Object>> barrier = factory.createService(false, params);
			assertEquals(Service.Type.Barrier, barrier.getType());
			assertEquals(Integer.MAX_VALUE, barrier.getMaxBarrierSize());
			assertEquals(Service.Type.Start, factory.createService(true, params).getType());
			assertEquals(3, factory.createService(false, Map.of(OpcCommandServiceFactory.Params.COMMAND, "delete",
					OpcCommandServiceFactory.Params.CHUNK_SIZE, "3")).getMaxBarrierSize());
			assertThrows(IllegalArgumentException.class,
					() -> factory.createService(false, Map.of(OpcCommandServiceFactory.Params.COMMAND, "delete",
							OpcCommandServiceFactory.Params.CHUNK_SIZE, 0)));
			assertThrows(IllegalArgumentException.class,
					() -> factory.createService(false, Map.of(OpcCommandServiceFactory.Params.COMMAND, "delete",
							OpcCommandServiceFactory.Params.CHUNK_SIZE, "x")));
			factory.close();
		} finally {
			restore(saved);
		}
	}

	@Test
	void argumentsAreConvertedToStrings() {
		assertEquals(List.of(), List.of(OpcCommandServiceFactory.args(Map.of())));
		assertEquals(List.of("a", "1"),
				List.of(OpcCommandServiceFactory.args(Map.of(OpcCommandServiceFactory.Params.ARGS, List.of("a", 1)))));
		assertEquals(List.of("x", "true"), List.of(OpcCommandServiceFactory
				.args(Map.of(OpcCommandServiceFactory.Params.ARGS, new Object[] { "x", true }))));
		assertEquals(List.of("1", "2"), List.of(
				OpcCommandServiceFactory.args(Map.of(OpcCommandServiceFactory.Params.ARGS, new int[] { 1, 2 }))));
		assertEquals(List.of("single"),
				List.of(OpcCommandServiceFactory.args(Map.of(OpcCommandServiceFactory.Params.ARGS, "single"))));
	}

	@Test
	void barrierRunsOncePerElementAndReportsNonOpcElements() {
		final String saved = System.getProperty(GremlinCommandSettings.ENABLED_KEY);
		System.setProperty(GremlinCommandSettings.ENABLED_KEY, "true");
		try {
			final OpcCommandServiceFactory factory = new OpcCommandServiceFactory(graph);
			final Map<String, Object> params = Map.of(OpcCommandServiceFactory.Params.COMMAND, "delete");
			final Service<Object, Map<String, Object>> service = factory.createService(false, params);
			final TraverserSet<Object> in = new TraverserSet<>();
			in.add(new B_O_Traverser<>("first", 3));
			in.add(new B_O_Traverser<>("second", 1));
			final List<Map<String, Object>> results = toList(service.execute(null, in, params));
			// bulk 3: one invocation only
			assertEquals(2, results.size());
			assertEquals("delete", results.get(0).get(OpcCommandServiceFactory.RESULT_COMMAND));
			assertTrue(results.get(0).get(OpcCommandServiceFactory.RESULT_ERROR).toString().contains("first"));
			assertTrue(results.get(1).get(OpcCommandServiceFactory.RESULT_ERROR).toString().contains("second"));
			// start mode on a namespace without console strategy: command not found
			final List<Map<String, Object>> global = toList(
					factory.createService(true, params).execute(null, params));
			assertEquals(1, global.size());
			assertEquals(null, global.get(0).get(OpcCommandServiceFactory.RESULT_ELEMENT));
			assertTrue(global.get(0).get(OpcCommandServiceFactory.RESULT_ERROR).toString().contains("not found"));
		} finally {
			restore(saved);
		}
	}

	@Test
	void listServiceOnNonOpcElementsIsEmpty() {
		final OpcCommandListServiceFactory factory = new OpcCommandListServiceFactory(graph);
		assertEquals(Set.of(Service.Type.Streaming, Service.Type.Start), factory.getSupportedTypes());
		assertTrue(factory.describeParams().isEmpty());
		assertSame(factory, factory.createService(false, Map.of()));
		assertEquals(Service.Type.Streaming, factory.getType());
		assertFalse(factory.execute(null, new B_O_Traverser<>((Object) "x", 1), Map.of()).hasNext());
		final Service<Object, Map<String, Object>> start = factory.createService(true, Map.of());
		assertEquals(Service.Type.Start, start.getType());
		assertFalse(start.execute(null, Map.of()).hasNext());
		factory.close();
	}

	private static void restore(final String saved) {
		if (saved == null) {
			System.clearProperty(GremlinCommandSettings.ENABLED_KEY);
		} else {
			System.setProperty(GremlinCommandSettings.ENABLED_KEY, saved);
		}
	}

	private static Vertex vertexWithEdges(final Map<Direction, Integer> counts) {
		return (Vertex) Proxy.newProxyInstance(Vertex.class.getClassLoader(), new Class<?>[] { Vertex.class },
				(proxy, method, args) -> {
					if ("edges".equals(method.getName())) {
						final int count = counts.getOrDefault(args[0], 0);
						final List<Edge> edges = new ArrayList<>();
						for (int i = 0; i < count; i++) {
							edges.add(null);
						}
						return edges.iterator();
					}
					return switch (method.getName()) {
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					case "toString" -> "vertex-proxy";
					default -> null;
					};
				});
	}
}
