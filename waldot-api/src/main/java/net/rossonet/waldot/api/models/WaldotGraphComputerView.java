package net.rossonet.waldot.api.models;

import java.util.List;

import org.apache.tinkerpop.gremlin.process.computer.GraphComputer.Persist;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer.ResultGraph;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

/**
 * WaldotGraphComputerView is an interface that provides methods to interact
 * with the graph computer view of a Waldot graph. It allows adding properties,
 * retrieving properties, checking edge and vertex legality, processing result
 * graphs, and removing properties.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotGraphComputerView {

	<V> VertexProperty<V> addProperty(Vertex vertex, String key, V value);

	void complete();

	List<Property> getProperties(Vertex vertex);

	List<VertexProperty<?>> getProperty(Vertex vertex, String key);

	boolean legalEdge(Vertex vertex, Edge edge);

	boolean legalVertex(Vertex vertex);

	Graph processResultGraphPersist(ResultGraph resultGraph, Persist persist);

	void removeProperty(Vertex vertex, String key, VertexProperty property);

}
