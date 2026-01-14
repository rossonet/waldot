package net.rossonet.waldot.rules;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.models.WaldotNamespace;

public class AliasResolver {

	private final Map<NodeId, Map<String, NodeId>> resolver = new HashMap<>();

	public AliasResolver(WaldotNamespace namespace) {
		// FIXME: gestire alias
		// this.resolver = namespace.getAliasTable();
	}

	public Map<NodeId, Map<String, NodeId>> getResolver() {
		return resolver;
	}

}
