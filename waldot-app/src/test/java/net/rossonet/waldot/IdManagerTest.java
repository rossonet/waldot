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
package net.rossonet.waldot;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

import net.rossonet.waldot.api.models.IdManager;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.WaldotMappingStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class IdManagerTest {
	@Rule
	public ExpectedException exceptionRule = ExpectedException.none();

	private WaldotGraph createWaldOT() {
		try {
			return OpcFactory.getOpcGraph();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return null;
		}

	}

	private IdManager<NodeId> getManager() {
		final IdManager<NodeId> manager = WaldotMappingStrategy.getNodeIdManager();
		return manager;
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfEmptyStringToString() {
		final WaldotGraph g = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected a non-empty string but received an empty string.");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(g, ""));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfJunkToInt() {
		final WaldotGraph g = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(g, UUID.randomUUID()));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfJunkToLong() {
		final WaldotGraph g = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(g, UUID.randomUUID()));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfJunkToString() {
		final WaldotGraph g = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(g, Double.NaN));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfJunkToUUID() {
		final WaldotGraph g = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(g, Double.NaN));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfStringToInt() {
		final WaldotGraph g = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(g, "string-id"));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfStringToLong() {
		final WaldotGraph g = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(g, "string-id"));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfStringToUUID() {
		final WaldotGraph g = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(g, "string-id"));
	}

	@Test
	public void shouldGenerateNiceErrorOnConversionOfUUIDToString() {
		final WaldotGraph g = createWaldOT();
		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Expected an id that is convertible to");
		final IdManager<NodeId> manager = getManager();
		System.out.println(manager.convert(g, UUID.randomUUID()));
	}
}
