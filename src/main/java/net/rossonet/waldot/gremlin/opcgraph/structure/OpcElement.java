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

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class OpcElement extends UaObjectNode implements Element {

	protected static IllegalStateException elementAlreadyRemoved(final Class<? extends Element> clazz,
			final NodeId id) {
		return new IllegalStateException(String.format("%s with id %s was removed.", clazz.getSimpleName(), id));
	}

	protected boolean removed = false;

	protected long currentVersion;

	protected OpcElement(UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask) {
		super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask);
		currentVersion = -1;
	}

	protected OpcElement(UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			UByte eventNotifier, long version) {
		super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier);
		currentVersion = version;
	}

	@Override
	public abstract Object clone();

	@Override
	public boolean equals(final Object object) {
		return ElementHelper.areEqual(this, object);
	}

	@Override
	public int hashCode() {
		return ElementHelper.hashCode(this);
	}

	@Override
	public NodeId id() {
		return getNodeId();
	}

	@Override
	public String label() {
		return getBrowseName().getName();
	}

	public long version() {
		return this.currentVersion;
	}
}
