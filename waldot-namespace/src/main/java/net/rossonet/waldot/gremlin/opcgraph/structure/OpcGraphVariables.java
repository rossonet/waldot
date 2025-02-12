package net.rossonet.waldot.gremlin.opcgraph.structure;

import java.util.Optional;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Graph.Variables;
import org.apache.tinkerpop.gremlin.structure.util.GraphVariableHelper;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class OpcGraphVariables implements Graph.Variables {

	private final WaldotNamespace waldotNamespace;

	public OpcGraphVariables(WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
	}

	@Override
	public <R> Optional<R> get(final String key) {
		return Optional.ofNullable((R) waldotNamespace.namespaceParametersGet(key));
	}

	@Override
	public Set<String> keys() {
		return waldotNamespace.namespaceParametersKeySet();
	}

	@Override
	public void remove(final String key) {
		waldotNamespace.namespaceParametersRemove(key);
	}

	@Override
	public void set(final String key, final Object value) {
		GraphVariableHelper.validateVariable(key, value);
		waldotNamespace.namespaceParametersPut(key, value);
	}

	@Override
	public String toString() {
		return StringFactory.graphVariablesString(this);
	}

	public Variables variables() {
		return waldotNamespace.namespaceParametersToVariables();

	}
}
