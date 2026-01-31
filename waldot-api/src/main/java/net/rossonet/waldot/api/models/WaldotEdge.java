package net.rossonet.waldot.api.models;

import java.util.List;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseObjectType;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.shaded.com.google.common.collect.ImmutableList;

import net.rossonet.waldot.api.PropertyObserver;

/**
 * WaldotEdge is an interface that extends Edge, WaldotElement, and
 * BaseObjectType. It represents an edge in the Waldot graph model and provides
 * methods to access properties associated with the edge.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotEdge extends Edge, WaldotElement, BaseObjectType {

	void addPropertyObserver(PropertyObserver propertyObserver);

	ImmutableList<WaldotProperty<Object>> getProperties();

	public <T> Optional<T> getProperty(QualifiedProperty<T> property);

	List<PropertyObserver> getPropertyObservers();

	void notifyPropertyValueChanging(String label, DataValue value);

	void removePropertyObserver(PropertyObserver observer);

}
