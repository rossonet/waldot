package net.rossonet.waldot.api.models;

import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseObjectType;
import org.eclipse.milo.shaded.com.google.common.collect.ImmutableList;

/**
 * WaldotEdge is an interface that extends Edge, WaldotElement, and
 * BaseObjectType. It represents an edge in the Waldot graph model and provides
 * methods to access properties associated with the edge.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotEdge extends Edge, WaldotElement, BaseObjectType {

	ImmutableList<WaldotProperty<Object>> getProperties();

	public <T> Optional<T> getProperty(QualifiedProperty<T> property);

}
