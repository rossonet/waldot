package net.rossonet.waldot.gremlin.opcgraph.structure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * Lifecycle between {@link OpcGraph} and its namespace, without starting an
 * OPC UA server: the namespace is a JDK proxy.
 */
class OpcGraphLifecycleTest {

	@Test
	void closingTheGraphClosesTheNamespaceOnce() {
		final OpcGraph graph = OpcGraph.open();
		final AtomicInteger namespaceCloses = new AtomicInteger();
		graph.setNamespace(namespace(() -> namespaceCloses.incrementAndGet()));

		graph.close();
		graph.close();

		assertTrue(graph.isClosed());
		assertEquals(1, namespaceCloses.get());
	}

	@Test
	void namespaceClosingTheGraphBackDoesNotRecurse() {
		final OpcGraph graph = OpcGraph.open();
		final AtomicInteger namespaceCloses = new AtomicInteger();
		// like HomunculusNamespace.close(), the namespace closes its graph
		graph.setNamespace(namespace(() -> {
			namespaceCloses.incrementAndGet();
			graph.close();
		}));

		assertDoesNotThrow(graph::close);

		assertTrue(graph.isClosed());
		assertEquals(1, namespaceCloses.get());
	}

	@Test
	void failingNamespaceCloseStillClosesTheGraph() {
		final OpcGraph graph = OpcGraph.open();
		graph.setNamespace(namespace(() -> {
			throw new IllegalStateException("namespace close failed");
		}));

		assertDoesNotThrow(graph::close);
		assertTrue(graph.isClosed());
	}

	@Test
	void graphWithoutNamespaceCloses() {
		final OpcGraph graph = OpcGraph.open();
		assertDoesNotThrow(graph::close);
		assertTrue(graph.isClosed());
	}

	private static WaldotNamespace namespace(final Runnable onClose) {
		return (WaldotNamespace) Proxy.newProxyInstance(WaldotNamespace.class.getClassLoader(),
				new Class<?>[] { WaldotNamespace.class }, (proxy, method, args) -> {
					return switch (method.getName()) {
					case "close" -> {
						onClose.run();
						yield null;
					}
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					case "toString" -> "namespace-proxy";
					default -> method.getReturnType() == boolean.class ? false
							: method.getReturnType().isPrimitive() ? 0 : null;
					};
				});
	}
}
