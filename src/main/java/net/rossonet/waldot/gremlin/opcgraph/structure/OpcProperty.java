/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.rossonet.waldot.gremlin.opcgraph.structure;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.namespaces.HomunculusNamespace;
import net.rossonet.waldot.utils.LogHelper;

public final class OpcProperty<DATA_TYPE> extends UaVariableNode implements Property<DATA_TYPE> {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final AbstractOpcGraph graph;

	public OpcProperty(AbstractOpcGraph graph, final OpcEdge edge, final String key, final DATA_TYPE value,
			UaNodeContext context, NodeId nodeId, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			NodeId dataType, Integer valueRank, UInteger[] arrayDimensions, UByte accessLevel, UByte userAccessLevel,
			Double minimumSamplingInterval, boolean historizing) {
		super(context, nodeId, QualifiedName.parse(key), LocalizedText.english(key), description, writeMask,
				userWriteMask, null, dataType, valueRank, arrayDimensions, accessLevel, userAccessLevel,
				minimumSamplingInterval, historizing);
		this.graph = graph;
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
	public Object clone() {
		return new OpcProperty<DATA_TYPE>(graph, getPropertyReference(), key(), value(), getNodeContext(), getNodeId(),
				getDescription(), getWriteMask(), getUserWriteMask(), getDataType(), getValueRank(),
				getArrayDimensions(), getAccessLevel(), getUserAccessLevel(), getMinimumSamplingInterval(),
				getHistorizing());
	}

	public OpcProperty<DATA_TYPE> copy(final OpcEdge newOwner) {
		return new OpcProperty<DATA_TYPE>(graph, newOwner, key(), value(), getNodeContext(), getNodeId(),
				getDescription(), getWriteMask(), getUserWriteMask(), getDataType(), getValueRank(),
				getArrayDimensions(), getAccessLevel(), getUserAccessLevel(), getMinimumSamplingInterval(),
				getHistorizing());
	}

	@Override
	public Element element() {
		return getPropertyReference();
	}

	@Override
	public boolean equals(final Object object) {
		return ElementHelper.areEqual(this, object);
	}

	private HomunculusNamespace getNamespace() {
		return graph.getOpcNamespace();
	}

	private OpcEdge getPropertyReference() {
		return getNamespace().getPropertyReference(this);
	}

	@Override
	public int hashCode() {
		return ElementHelper.hashCode(this);
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
	public void remove() {
		getPropertyReference().removeComponent(this);
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
