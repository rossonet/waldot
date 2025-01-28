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
package net.rossonet.waldot.gremlin.opcgraph.process.traversal.step.sideEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.tinkerpop.gremlin.process.traversal.GremlinTypeErrorException;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.step.HasContainerHolder;
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.FilterStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer;
import org.apache.tinkerpop.gremlin.process.traversal.util.AndP;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.CloseableIterator;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphIterator;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Pieter Martin
 */
public final class OpcGraphStep<S, E extends Element> extends GraphStep<S, E>
		implements HasContainerHolder, AutoCloseable {

	private final List<HasContainer> hasContainers = new ArrayList<>();
	/**
	 * List of iterators opened by this step.
	 */
	private final List<Iterator> iterators = new ArrayList<>();

	public OpcGraphStep(final GraphStep<S, E> originalGraphStep) {
		super(originalGraphStep.getTraversal(), originalGraphStep.getReturnClass(), originalGraphStep.isStartStep(),
				originalGraphStep.getIds());
		originalGraphStep.getLabels().forEach(this::addLabel);

		// we used to only setIteratorSupplier() if there were no ids OR the first id
		// was instanceof Element,
		// but that allowed the filter in g.V(v).has('k','v') to be ignored. this
		// created problems for
		// PartitionStrategy which wants to prevent someone from passing "v" from one
		// TraversalSource to
		// another TraversalSource using a different partition
		this.setIteratorSupplier(
				() -> (Iterator<E>) (Vertex.class.isAssignableFrom(this.returnClass) ? this.vertices() : this.edges()));
	}

	@Override
	public void addHasContainer(final HasContainer hasContainer) {
		if (hasContainer.getPredicate() instanceof AndP) {
			for (final P<?> predicate : ((AndP<?>) hasContainer.getPredicate()).getPredicates()) {
				this.addHasContainer(new HasContainer(hasContainer.getKey(), predicate));
			}
		} else {
			this.hasContainers.add(hasContainer);
		}
	}

	@Override
	public void close() {
		iterators.forEach(CloseableIterator::closeIterator);
	}

	private Iterator<? extends Edge> edges() {
		final AbstractOpcGraph graph = (AbstractOpcGraph) this.getTraversal().getGraph().get();
		Iterator<Edge> iterator;
		// ids are present, filter on them first
		if (null == this.ids) {
			iterator = Collections.emptyIterator();
		} else if (this.ids.length > 0) {
			iterator = this.iteratorList(graph.edges(this.ids));
		} else {
			iterator = this.iteratorList(graph.edges());
		}

		iterators.add(iterator);

		return iterator;
	}

	@Override
	public List<HasContainer> getHasContainers() {
		return Collections.unmodifiableList(this.hasContainers);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ this.hasContainers.hashCode();
	}

	private <E extends Element> Iterator<E> iteratorList(final Iterator<E> iterator) {
		final List<E> list = new ArrayList<>();

		try {
			while (iterator.hasNext()) {
				final E e = iterator.next();
				try {
					if (HasContainer.testAll(e, this.hasContainers)) {
						list.add(e);
					}
				} catch (final GremlinTypeErrorException ex) {
					if (getTraversal().isRoot() || !(getTraversal().getParent() instanceof FilterStep)) {
						/*
						 * Either we are at the top level of the query, or our parent query is not a
						 * FilterStep and thus cannot handle a GremlinTypeErrorException. In any of
						 * these cases we do a binary reduction from ERROR -> FALSE and filter the
						 * solution quietly.
						 */
					} else {
						// not a ternary -> binary reducer, pass the ERROR on
						throw ex;
					}
				}
			}
		} finally {
			// close the old iterator to release resources since we are returning a new
			// iterator (over list)
			// out of this function.
			CloseableIterator.closeIterator(iterator);
		}

		return new OpcGraphIterator<>(list.iterator());
	}

	@Override
	public String toString() {
		if (this.hasContainers.isEmpty()) {
			return super.toString();
		} else {
			return (null == this.ids || 0 == this.ids.length)
					? StringFactory.stepString(this, this.returnClass.getSimpleName().toLowerCase(), this.hasContainers)
					: StringFactory.stepString(this, this.returnClass.getSimpleName().toLowerCase(),
							Arrays.toString(this.ids), this.hasContainers);
		}
	}

	private Iterator<? extends Vertex> vertices() {
		final AbstractOpcGraph graph = (AbstractOpcGraph) this.getTraversal().getGraph().get();
		Iterator<? extends Vertex> iterator;
		// ids are present, filter on them first
		if (null == this.ids) {
			iterator = Collections.emptyIterator();
		} else if (this.ids.length > 0) {
			iterator = this.iteratorList(graph.vertices(this.ids));
		} else {
			iterator = this.iteratorList(graph.vertices());
		}

		iterators.add(iterator);

		return iterator;
	}
}
