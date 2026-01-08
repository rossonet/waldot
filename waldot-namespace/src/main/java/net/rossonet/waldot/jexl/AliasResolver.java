package net.rossonet.waldot.jexl;

import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.namespaces.HomunculusNamespace;

public class AliasResolver {

	private final Map<NodeId, Map<String, NodeId>> resolver;

	public AliasResolver(HomunculusNamespace homunculusNamespace) {
		this.resolver = homunculusNamespace.getAliasTable();
	}

}
