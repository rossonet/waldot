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
package net.rossonet.waldot.gremlin.opcgraph.jsr223;

import org.apache.tinkerpop.gremlin.jsr223.AbstractGremlinPlugin;
import org.apache.tinkerpop.gremlin.jsr223.DefaultImportCustomizer;
import org.apache.tinkerpop.gremlin.jsr223.ImportCustomizer;

import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputer;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputerView;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcMapEmitter;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcMemory;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcMessenger;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcReduceEmitter;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcWorkerPool;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcEdge;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcElement;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphVariables;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcHelper;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV1;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV2;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV3;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcProperty;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertex;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertexProperty;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public final class OpcGraphGremlinPlugin extends AbstractGremlinPlugin {
	private static final String NAME = "tinkerpop.opcgraph";

	private static final ImportCustomizer imports = DefaultImportCustomizer.build()
			.addClassImports(OpcEdge.class, OpcElement.class, OpcFactory.class, OpcGraph.class, OpcGraphVariables.class,
					OpcHelper.class, OpcIoRegistryV1.class, OpcIoRegistryV2.class, OpcIoRegistryV3.class,
					OpcProperty.class, OpcVertex.class, OpcVertexProperty.class, OpcGraphComputer.class,
					OpcGraphComputerView.class, OpcMapEmitter.class, OpcMemory.class, OpcMessenger.class,
					OpcReduceEmitter.class, OpcWorkerPool.class)
			.create();

	private static final OpcGraphGremlinPlugin instance = new OpcGraphGremlinPlugin();

	public static OpcGraphGremlinPlugin instance() {
		return instance;
	}

	public OpcGraphGremlinPlugin() {
		super(NAME, imports);
	}
}