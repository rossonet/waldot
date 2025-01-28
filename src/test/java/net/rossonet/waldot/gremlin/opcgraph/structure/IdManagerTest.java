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

import java.util.UUID;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

import net.rossonet.waldot.WaldotOpcUaServer;
import net.rossonet.waldot.api.OpcMappingStrategy;
import net.rossonet.waldot.configuration.HomunculusConfiguration;
import net.rossonet.waldot.configuration.OpcUaConfiguration;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph.IdManager;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class IdManagerTest {
	@Rule
	public ExpectedException exceptionRule = ExpectedException.none();

	private WaldotOpcUaServer createWaldOT() {
		final HomunculusConfiguration configuration = HomunculusConfiguration.getDefault();
		final OpcUaConfiguration serverConfiguration = OpcUaConfiguration.getDefault();
		return new WaldotOpcUaServer(configuration, serverConfiguration);

	}

	private IdManager<NodeId> getManager() {
		final IdManager<NodeId> manager = OpcMappingStrategy.getNodeIdManager();
		return manager;
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfEmptyStringToString() {
		final WaldotOpcUaServer w = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected a non-empty string but received an empty string.");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(w.getGremlinGraph(), ""));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfJunkToInt() {
		final WaldotOpcUaServer w = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(w.getGremlinGraph(), UUID.randomUUID()));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfJunkToLong() {
		final WaldotOpcUaServer w = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(w.getGremlinGraph(), UUID.randomUUID()));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfJunkToString() {
		final WaldotOpcUaServer w = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(w.getGremlinGraph(), Double.NaN));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfJunkToUUID() {
		final WaldotOpcUaServer w = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(w.getGremlinGraph(), Double.NaN));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfStringToInt() {
		final WaldotOpcUaServer w = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(w.getGremlinGraph(), "string-id"));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfStringToLong() {
		final WaldotOpcUaServer w = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(w.getGremlinGraph(), "string-id"));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfStringToUUID() {
		final WaldotOpcUaServer w = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(w.getGremlinGraph(), "string-id"));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfUUIDToString() {
		final WaldotOpcUaServer w = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(w.getGremlinGraph(), UUID.randomUUID()));
	}
}
