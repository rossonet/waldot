package net.rossonet.waldot.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.jexl.ClonableMapContext;
import net.rossonet.waldot.rules.vertices.RuleVertex;

/**
 * Estensione di {@link RuleVertex}: motore JEXL e contesto si possono
 * sostituire in una sottoclasse (regole lette da un file di configurazione,
 * con un motore ristretto e un contesto senza grafo e comandi).
 */
public class RuleVertexExtensionTest {

	/** Sottoclasse di prova: compila solo se i due metodi sono sovrascrivibili. */
	static final class RestrictedRule extends RuleVertex {

		private static final JexlEngine RESTRICTED = new JexlBuilder()
				.permissions(JexlPermissions.RESTRICTED).create();

		RestrictedRule(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
				QualifiedName browseName, LocalizedText displayName,
				LocalizedText description, UInteger writeMask,
				UInteger userWriteMask, UByte eventNotifier, long version,
				Object[] propertyKeyValues) {
			super(graph, context, nodeId, browseName, displayName, description,
					writeMask, userWriteMask, eventNotifier, version,
					propertyKeyValues);
		}

		@Override
		protected JexlEngine engine() {
			return RESTRICTED;
		}

		@Override
		protected ClonableMapContext newContext() {
			final ClonableMapContext context = new ClonableMapContext();
			context.set("answer", 42);
			return context;
		}
	}

	@Test
	public void engineAndContextAreOverridable() throws Exception {
		for (final String name : new String[] { "engine", "newContext" }) {
			final Method method = RuleVertex.class.getDeclaredMethod(name);
			assertTrue(Modifier.isProtected(method.getModifiers()),
					name + " must be protected");
			final Method overridden = RestrictedRule.class
					.getDeclaredMethod(name);
			assertTrue(
					method.getReturnType()
							.isAssignableFrom(overridden.getReturnType()),
					name + " overridden with a compatible type");
		}
	}
}
