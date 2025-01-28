package net.rossonet.waldot.api.models;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

public interface WaldotVertexProperty<DATA_TYPE> extends VertexProperty<DATA_TYPE>, WaldotProperty<DATA_TYPE>, Element {

}
