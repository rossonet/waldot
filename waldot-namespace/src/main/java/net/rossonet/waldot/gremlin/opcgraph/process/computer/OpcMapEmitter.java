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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class OpcMapEmitter<K, V> implements MapReduce.MapEmitter<K, V> {

    public Map<K, Queue<V>> reduceMap;
    public Queue<KeyValue<K, V>> mapQueue;
    private final boolean doReduce;

    public OpcMapEmitter(final boolean doReduce) {
        this.doReduce = doReduce;
        if (this.doReduce)
            this.reduceMap = new ConcurrentHashMap<>();
        else
            this.mapQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void emit(K key, V value) {
        if (this.doReduce)
            this.reduceMap.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(value);
        else
            this.mapQueue.add(new KeyValue<>(key, value));
    }

    protected void complete(final MapReduce<K, V, ?, ?, ?> mapReduce) {
        if (!this.doReduce && mapReduce.getMapKeySort().isPresent()) {
            final Comparator<K> comparator = mapReduce.getMapKeySort().get();
            final List<KeyValue<K, V>> list = new ArrayList<>(this.mapQueue);
            Collections.sort(list, Comparator.comparing(KeyValue::getKey, comparator));
            this.mapQueue.clear();
            this.mapQueue.addAll(list);
        } else if (mapReduce.getMapKeySort().isPresent()) {
            final Comparator<K> comparator = mapReduce.getMapKeySort().get();
            final List<Map.Entry<K, Queue<V>>> list = new ArrayList<>();
            list.addAll(this.reduceMap.entrySet());
            Collections.sort(list, Comparator.comparing(Map.Entry::getKey, comparator));
            this.reduceMap = new LinkedHashMap<>();
            list.forEach(entry -> this.reduceMap.put(entry.getKey(), entry.getValue()));
        }
    }
}
