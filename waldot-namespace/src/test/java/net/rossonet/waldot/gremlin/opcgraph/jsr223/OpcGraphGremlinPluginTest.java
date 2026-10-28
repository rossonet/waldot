package net.rossonet.waldot.gremlin.opcgraph.jsr223;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tinkerpop.gremlin.jsr223.Customizer;
import org.apache.tinkerpop.gremlin.jsr223.ImportCustomizer;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.CallStep;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.gremlin.WaldotTraversalDsl;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcCommandListServiceFactory;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcCommandServiceFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;

/**
 * Gremlin script engine plugin and Waldot traversal DSL.
 */
class OpcGraphGremlinPluginTest {

	@Test
	void pluginImportsTheGraphClasses() {
		final OpcGraphGremlinPlugin plugin = OpcGraphGremlinPlugin.instance();
		assertSame(plugin, OpcGraphGremlinPlugin.instance());
		assertEquals("tinkerpop.opcgraph", plugin.getName());
		final Customizer[] customizers = plugin.getCustomizers().orElseThrow();
		final Set<Class> imported = java.util.Arrays.stream(customizers).filter(ImportCustomizer.class::isInstance)
				.map(ImportCustomizer.class::cast).flatMap(c -> c.getClassImports().stream())
				.collect(Collectors.toSet());
		assertTrue(imported.contains(OpcGraph.class));
		assertTrue(imported.contains(OpcFactory.class));
	}

	private static final class PlainTraversal extends DefaultGraphTraversal<Object, Object>
			implements WaldotTraversalDsl<Object, Object> {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void execAddsACallStepOfTheCommandService() {
		final PlainTraversal traversal = new PlainTraversal();
		traversal.exec("property", "color", "red");
		final CallStep<?, ?> step = (CallStep<?, ?>) traversal.getEndStep();
		assertEquals(OpcCommandServiceFactory.NAME, step.getServiceName());
		assertEquals(Map.of(OpcCommandServiceFactory.Params.COMMAND, "property",
				OpcCommandServiceFactory.Params.ARGS, List.of("color", "red")), step.getStaticParams());
	}

	@Test
	void execWithoutArgumentsPassesAnEmptyList() {
		final PlainTraversal traversal = new PlainTraversal();
		traversal.exec("delete");
		final CallStep<?, ?> step = (CallStep<?, ?>) traversal.getEndStep();
		assertEquals(List.of(), step.getStaticParams().get(OpcCommandServiceFactory.Params.ARGS));
	}

	@Test
	void commandsAddsACallStepOfTheListService() {
		final PlainTraversal traversal = new PlainTraversal();
		traversal.commands();
		final CallStep<?, ?> step = (CallStep<?, ?>) traversal.getEndStep();
		assertEquals(OpcCommandListServiceFactory.NAME, step.getServiceName());
		assertTrue(step.getStaticParams().isEmpty());
	}
}
