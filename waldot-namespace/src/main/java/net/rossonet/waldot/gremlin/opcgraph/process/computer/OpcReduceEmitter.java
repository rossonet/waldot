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
package net.rossonet.waldot.gremlin.opcgraph.process.computer;

import org.apache.tinkerpop.gremlin.process.computer.KeyValue;
import org.apache.tinkerpop.gremlin.process.computer.MapReduce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class OpcReduceEmitter<OK, OV> implements MapReduce.ReduceEmitter<OK, OV> {

    protected Queue<KeyValue<OK, OV>> reduceQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void emit(final OK key, final OV value) {
        this.reduceQueue.add(new KeyValue<>(key, value));
    }

    protected void complete(final MapReduce<?, ?, OK, OV, ?> mapReduce) {
        if (mapReduce.getReduceKeySort().isPresent()) {
            final Comparator<OK> comparator = mapReduce.getReduceKeySort().get();
            final List<KeyValue<OK, OV>> list = new ArrayList<>(this.reduceQueue);
            Collections.sort(list, Comparator.comparing(KeyValue::getKey, comparator));
            this.reduceQueue.clear();
            this.reduceQueue.addAll(list);
        }
    }
}
