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

import java.util.Optional;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Graph.Variables;
import org.apache.tinkerpop.gremlin.structure.util.GraphVariableHelper;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class OpcGraphVariables implements Graph.Variables {

	private final WaldotNamespace waldotNamespace;

	public OpcGraphVariables(WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
	}

	@Override
	public <R> Optional<R> get(final String key) {
		return Optional.ofNullable((R) waldotNamespace.namespaceParametersGet(key));
	}

	@Override
	public Set<String> keys() {
		return waldotNamespace.namespaceParametersKeySet();
	}

	@Override
	public void remove(final String key) {
		waldotNamespace.namespaceParametersRemove(key);
	}

	@Override
	public void set(final String key, final Object value) {
		GraphVariableHelper.validateVariable(key, value);
		waldotNamespace.namespaceParametersPut(key, value);
	}

	@Override
	public String toString() {
		return StringFactory.graphVariablesString(this);
	}

	public Variables variables() {
		return waldotNamespace.namespaceParametersToVariables();

	}
}
