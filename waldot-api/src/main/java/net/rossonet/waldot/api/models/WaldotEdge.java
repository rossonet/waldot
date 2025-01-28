package net.rossonet.waldot.api.models;

import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;

import com.google.common.collect.ImmutableList;

public interface WaldotEdge extends Edge, WaldotElement {

	ImmutableList<WaldotProperty<Object>> getProperties();

	public <T> Optional<T> getProperty(QualifiedProperty<T> property);

}
