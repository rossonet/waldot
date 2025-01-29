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

import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotProperty;
import net.rossonet.waldot.opc.AbstractOpcProperty;

public final class OpcProperty<DATA_TYPE> extends AbstractOpcProperty<DATA_TYPE> implements WaldotProperty<DATA_TYPE> {
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public OpcProperty(WaldotGraph graph, final WaldotEdge edge, final String key, final DATA_TYPE value,
			UaNodeContext context, NodeId nodeId, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			NodeId dataType, Integer valueRank, UInteger[] arrayDimensions, UByte accessLevel, UByte userAccessLevel,
			Double minimumSamplingInterval, boolean historizing) {
		super(graph, edge, key, value, context, nodeId, description, writeMask, userWriteMask, dataType, valueRank,
				arrayDimensions, accessLevel, userAccessLevel, minimumSamplingInterval, historizing);

	}

	@Override
	public Object clone() {
		return new OpcProperty<DATA_TYPE>(graph, getPropertyReference(), key(), value(), getNodeContext(), getNodeId(),
				getDescription(), getWriteMask(), getUserWriteMask(), getDataType(), getValueRank(),
				getArrayDimensions(), getAccessLevel(), getUserAccessLevel(), getMinimumSamplingInterval(),
				getHistorizing());
	}

	public WaldotProperty<DATA_TYPE> copy(final WaldotEdge newOwner) {
		return new OpcProperty<DATA_TYPE>(graph, newOwner, key(), value(), getNodeContext(), getNodeId(),
				getDescription(), getWriteMask(), getUserWriteMask(), getDataType(), getValueRank(),
				getArrayDimensions(), getAccessLevel(), getUserAccessLevel(), getMinimumSamplingInterval(),
				getHistorizing());
	}

	@Override
	public boolean equals(final Object object) {
		return ElementHelper.areEqual((Property<DATA_TYPE>) this, object);
	}

	@Override
	public int hashCode() {
		return ElementHelper.hashCode((Property<DATA_TYPE>) this);
	}

}
