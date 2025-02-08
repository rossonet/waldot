package net.rossonet.waldot.opc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.PropertyObserver;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotProperty;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;
import net.rossonet.waldot.opc.gremlin.GremlinProperty;
import net.rossonet.waldot.utils.LogHelper;

public abstract class AbstractOpcProperty<DATA_TYPE> extends GremlinProperty<DATA_TYPE>
		implements WaldotProperty<DATA_TYPE> {
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final WaldotGraph graph;

	protected boolean allowNullPropertyValues = false;

	protected final List<PropertyObserver> propertyObservers = new ArrayList<>();
	protected final List<EventObserver> eventObservers = new ArrayList<>();
	private ByteString icon;
	private final WaldotEdge referenceEdge;

	public AbstractOpcProperty(WaldotGraph graph, final WaldotEdge edge, final String key, final DATA_TYPE value,
			UaNodeContext context, NodeId nodeId, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			NodeId dataType, Integer valueRank, UInteger[] arrayDimensions, UByte accessLevel, UByte userAccessLevel,
			Double minimumSamplingInterval, boolean historizing) {
		super(graph, key, value, context, nodeId, description, writeMask, userWriteMask, dataType, valueRank,
				arrayDimensions, accessLevel, userAccessLevel, minimumSamplingInterval, historizing);
		this.graph = graph;
		this.referenceEdge = edge;
		this.allowNullPropertyValues = graph.features().vertex().supportsNullPropertyValues();
		try {
			final Variant variant = new Variant(value);
			final DataValue dataValue = DataValue.newValue().setStatus(StatusCode.GOOD).setSourceTime(DateTime.now())
					.setValue(variant).build();
			setValue(dataValue);
			edge.addComponent(this);
		} catch (final Exception a) {
			final DataValue errorDataValue = DataValue.newValue().setStatus(StatusCode.BAD).build();
			setValue(errorDataValue);
			edge.addComponent(this);
			logger.error(LogHelper.stackTraceToString(a));
		}
	}

	@Override
	public Element element() {
		return getPropertyReference();
	}

	@Override
	public ByteString getIcon() {
		return icon;
	}

	@Override
	public WaldotNamespace getNamespace() {
		return graph.getWaldotNamespace();
	}

	@Override
	public WaldotEdge getPropertyReference() {
		return getNamespace().getPropertyReference(this);
	}

	public WaldotEdge getReferenceEdge() {
		return referenceEdge;
	}

	@Override
	public Graph graph() {
		return this.graph;
	}

	@Override
	public boolean isPresent() {
		return getValue().getStatusCode().isGood();
	}

	@Override
	public String key() {
		return getBrowseName().getName();
	}

	@Override
	public <V> Iterator<? extends Property<V>> properties(String... propertyKeys) {
		return Collections.emptyIterator();
	}

	@Override
	public <V> Property<V> property(String key, V value) {
		return Property.empty();
	}

	@Override
	public void remove() {

	}

	@Override
	public void setIcon(ByteString icon) {
		this.icon = icon;

	}

	@Override
	public String toString() {
		if (!isPresent()) {
			return AbstractOpcGraph.EMPTY_PROPERTY;
		}
		final String valueString = String.valueOf(value());
		return AbstractOpcGraph.P + AbstractOpcGraph.L_BRACKET + getBrowseName().getName() + AbstractOpcGraph.ARROW
				+ StringUtils.abbreviate(valueString, 20) + AbstractOpcGraph.R_BRACKET;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DATA_TYPE value() {
		return (DATA_TYPE) getValue().getValue().getValue();
	}
}
