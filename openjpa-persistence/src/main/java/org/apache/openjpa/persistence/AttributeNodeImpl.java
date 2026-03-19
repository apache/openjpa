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
package org.apache.openjpa.persistence;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.Subgraph;

/**
 * Implementation of {@link AttributeNode}.
 */
public class AttributeNodeImpl<T> implements AttributeNode<T> {

    private final String attributeName;
    private final Map<Class, Subgraph> subgraphs = new HashMap<>();
    private final Map<Class, Subgraph> keySubgraphs = new HashMap<>();

    public AttributeNodeImpl(String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public String getAttributeName() {
        return attributeName;
    }

    @Override
    public Map<Class, Subgraph> getSubgraphs() {
        return Collections.unmodifiableMap(subgraphs);
    }

    @Override
    public Map<Class, Subgraph> getKeySubgraphs() {
        return Collections.unmodifiableMap(keySubgraphs);
    }

    void addSubgraph(Class type, Subgraph subgraph) {
        subgraphs.put(type, subgraph);
    }

    void addKeySubgraph(Class type, Subgraph subgraph) {
        keySubgraphs.put(type, subgraph);
    }

    AttributeNodeImpl<T> copy() {
        AttributeNodeImpl<T> clone = new AttributeNodeImpl<>(attributeName);
        clone.subgraphs.putAll(subgraphs);
        clone.keySubgraphs.putAll(keySubgraphs);
        return clone;
    }
}
