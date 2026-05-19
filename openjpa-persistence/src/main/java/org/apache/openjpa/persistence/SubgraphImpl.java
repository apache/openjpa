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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.openjpa.persistence.meta.MetamodelImpl;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.Subgraph;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;

/**
 * Implementation of {@link Subgraph}.
 *
 * @since 4.2.0
 */
public class SubgraphImpl<T> implements Subgraph<T> {

    private final Class<T> classType;
    private final Map<String, AttributeNodeImpl<?>> nodes = new LinkedHashMap<>();
    private final MetamodelImpl metamodel;

    public SubgraphImpl(Class<T> classType, MetamodelImpl metamodel) {
        this.classType = classType;
        this.metamodel = metamodel;
    }

    @Override
    public Class<T> getClassType() {
        return classType;
    }

    // ---- Graph<T> methods ----

    @Override
    @SuppressWarnings("unchecked")
    public <Y> AttributeNode<Y> addAttributeNode(String attributeName) {
        validateAttribute(attributeName);
        return (AttributeNode<Y>) nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Y> AttributeNode<Y> addAttributeNode(Attribute<? super T, Y> attribute) {
        return (AttributeNode<Y>) nodes.computeIfAbsent(attribute.getName(), AttributeNodeImpl::new);
    }

    @Override
    public boolean hasAttributeNode(String attributeName) {
        validateAttribute(attributeName);
        return nodes.containsKey(attributeName);
    }

    @Override
    public boolean hasAttributeNode(Attribute<? super T, ?> attribute) {
        return nodes.containsKey(attribute.getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Y> AttributeNode<Y> getAttributeNode(String attributeName) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.get(attributeName);
        if (node == null) {
            throw new NoSuchElementException("No attribute node for: " + attributeName);
        }
        return (AttributeNode<Y>) node;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Y> AttributeNode<Y> getAttributeNode(Attribute<? super T, Y> attribute) {
        AttributeNodeImpl<?> node = nodes.get(attribute.getName());
        if (node == null) {
            throw new NoSuchElementException("No attribute node for: " + attribute.getName());
        }
        return (AttributeNode<Y>) node;
    }

    @Override
    public void removeAttributeNode(String attributeName) {
        nodes.remove(attributeName);
    }

    @Override
    public void removeAttributeNode(Attribute<? super T, ?> attribute) {
        nodes.remove(attribute.getName());
    }

    @Override
    public void removeAttributeNodes(Attribute.PersistentAttributeType nodeTypes) {
        if (metamodel == null) return;
        try {
            ManagedType<T> managedType = metamodel.managedType(classType);
            nodes.entrySet().removeIf(entry -> {
                try {
                    Attribute<? super T, ?> attr = managedType.getAttribute(entry.getKey());
                    return attr.getPersistentAttributeType() == nodeTypes;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            });
        } catch (IllegalArgumentException e) {
            // type not in metamodel
        }
    }

    @Override
    public void addAttributeNodes(String... attributeNames) {
        for (String name : attributeNames) {
            validateAttribute(name);
            nodes.computeIfAbsent(name, AttributeNodeImpl::new);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addAttributeNodes(Attribute<? super T, ?>... attributes) {
        for (Attribute<? super T, ?> attr : attributes) {
            nodes.computeIfAbsent(attr.getName(), AttributeNodeImpl::new);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Subgraph<X> addSubgraph(Attribute<? super T, X> attribute) {
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attribute.getName(), AttributeNodeImpl::new);
        SubgraphImpl<X> sg = new SubgraphImpl<>(attribute.getJavaType(), metamodel);
        node.addSubgraph(attribute.getJavaType(), sg);
        return sg;
    }

    @Override
    public <Y> Subgraph<Y> addTreatedSubgraph(Attribute<? super T, ? super Y> attribute, Class<Y> type) {
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attribute.getName(), AttributeNodeImpl::new);
        SubgraphImpl<Y> sg = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, sg);
        return sg;
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public <X> Subgraph<? extends X> addSubgraph(Attribute<? super T, X> attribute, Class<? extends X> type) {
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attribute.getName(), AttributeNodeImpl::new);
        SubgraphImpl<? extends X> sg = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, sg);
        return sg;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Subgraph<X> addSubgraph(String attributeName) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        ManagedType<T> managedType = metamodel.managedType(classType);
        Attribute<? super T, ?> attr = managedType.getAttribute(attributeName);
        Class<X> type = (Class<X>) attr.getJavaType();
        SubgraphImpl<X> sg = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, sg);
        return sg;
    }

    @Override
    public <X> Subgraph<X> addSubgraph(String attributeName, Class<X> type) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        SubgraphImpl<X> sg = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, sg);
        return sg;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Subgraph<E> addElementSubgraph(PluralAttribute<? super T, ?, E> attribute) {
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attribute.getName(), AttributeNodeImpl::new);
        SubgraphImpl<E> sg = new SubgraphImpl<>(attribute.getBindableJavaType(), metamodel);
        node.addSubgraph(attribute.getBindableJavaType(), sg);
        return sg;
    }

    @Override
    public <E> Subgraph<E> addTreatedElementSubgraph(PluralAttribute<? super T, ?, ? super E> attribute, Class<E> type) {
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attribute.getName(), AttributeNodeImpl::new);
        SubgraphImpl<E> sg = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, sg);
        return sg;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Subgraph<X> addElementSubgraph(String attributeName) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        ManagedType<T> managedType = metamodel.managedType(classType);
        Attribute<? super T, ?> attr = managedType.getAttribute(attributeName);
        Class<X> type = (Class<X>) attr.getJavaType();
        SubgraphImpl<X> sg = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, sg);
        return sg;
    }

    @Override
    public <X> Subgraph<X> addElementSubgraph(String attributeName, Class<X> type) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        SubgraphImpl<X> sg = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, sg);
        return sg;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K> Subgraph<K> addMapKeySubgraph(MapAttribute<? super T, K, ?> attribute) {
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attribute.getName(), AttributeNodeImpl::new);
        SubgraphImpl<K> sg = new SubgraphImpl<>(attribute.getKeyJavaType(), metamodel);
        node.addKeySubgraph(attribute.getKeyJavaType(), sg);
        return sg;
    }

    @Override
    public <K> Subgraph<K> addTreatedMapKeySubgraph(MapAttribute<? super T, ? super K, ?> attribute, Class<K> type) {
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attribute.getName(), AttributeNodeImpl::new);
        SubgraphImpl<K> sg = new SubgraphImpl<>(type, metamodel);
        node.addKeySubgraph(type, sg);
        return sg;
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public <X> Subgraph<X> addKeySubgraph(Attribute<? super T, X> attribute) {
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attribute.getName(), AttributeNodeImpl::new);
        Class<X> type = attribute.getJavaType();
        SubgraphImpl<X> sg = new SubgraphImpl<>(type, metamodel);
        node.addKeySubgraph(type, sg);
        return sg;
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public <X> Subgraph<? extends X> addKeySubgraph(Attribute<? super T, X> attribute, Class<? extends X> type) {
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attribute.getName(), AttributeNodeImpl::new);
        SubgraphImpl<? extends X> sg = new SubgraphImpl<>(type, metamodel);
        node.addKeySubgraph(type, sg);
        return sg;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Subgraph<X> addKeySubgraph(String attributeName) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        ManagedType<T> managedType = metamodel.managedType(classType);
        Attribute<? super T, ?> attr = managedType.getAttribute(attributeName);
        Class<X> type = (Class<X>) attr.getJavaType();
        SubgraphImpl<X> sg = new SubgraphImpl<>(type, metamodel);
        node.addKeySubgraph(type, sg);
        return sg;
    }

    @Override
    public <X> Subgraph<X> addKeySubgraph(String attributeName, Class<X> type) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        SubgraphImpl<X> sg = new SubgraphImpl<>(type, metamodel);
        node.addKeySubgraph(type, sg);
        return sg;
    }

    @Override
    public List<AttributeNode<?>> getAttributeNodes() {
        return new ArrayList<>(nodes.values());
    }

    // ---- Internal helpers ----

    private void validateAttribute(String attributeName) {
        if (metamodel == null) return;
        try {
            ManagedType<T> managedType = metamodel.managedType(classType);
            managedType.getAttribute(attributeName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "The attribute [" + attributeName
                + "] is not present in the managed type ["
                + classType.getName() + "]", e);
        }
    }

    void addAttributeNodeDirect(String attributeName) {
        nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
    }
}
