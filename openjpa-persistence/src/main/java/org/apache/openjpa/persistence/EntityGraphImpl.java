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

import jakarta.persistence.EntityGraph;
import jakarta.persistence.Subgraph;
import jakarta.persistence.AttributeNode;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;

/**
 * Implementation of {@link EntityGraph}.
 *
 * @since 4.2.0
 */
public class EntityGraphImpl<T> implements EntityGraph<T> {

    private final String name;
    private final Class<T> entityType;
    private final Map<String, AttributeNodeImpl<?>> nodes = new LinkedHashMap<>();
    private final MetamodelImpl metamodel;

    public EntityGraphImpl(Class<T> entityType, MetamodelImpl metamodel) {
        this(null, entityType, metamodel);
    }

    public EntityGraphImpl(String name, Class<T> entityType, MetamodelImpl metamodel) {
        this.name = name;
        this.entityType = entityType;
        this.metamodel = metamodel;
    }

    @Override
    public String getName() {
        return name;
    }

    public Class<T> getEntityType() {
        return entityType;
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
        String attrName = attribute.getName();
        return (AttributeNode<Y>) nodes.computeIfAbsent(attrName, AttributeNodeImpl::new);
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
            throw new NoSuchElementException(
                "No attribute node for: " + attributeName);
        }
        return (AttributeNode<Y>) node;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Y> AttributeNode<Y> getAttributeNode(Attribute<? super T, Y> attribute) {
        AttributeNodeImpl<?> node = nodes.get(attribute.getName());
        if (node == null) {
            throw new NoSuchElementException(
                "No attribute node for: " + attribute.getName());
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
            ManagedType<T> managedType = metamodel.managedType(entityType);
            nodes.entrySet().removeIf(entry -> {
                try {
                    Attribute<? super T, ?> attr = managedType.getAttribute(entry.getKey());
                    return attr.getPersistentAttributeType() == nodeTypes;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            });
        } catch (IllegalArgumentException e) {
            // entity type not in metamodel
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
        String attrName = attribute.getName();
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attrName, AttributeNodeImpl::new);
        SubgraphImpl<X> subgraph = new SubgraphImpl<>(attribute.getJavaType(), metamodel);
        node.addSubgraph(attribute.getJavaType(), subgraph);
        return subgraph;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Y> Subgraph<Y> addTreatedSubgraph(Attribute<? super T, ? super Y> attribute, Class<Y> type) {
        String attrName = attribute.getName();
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attrName, AttributeNodeImpl::new);
        SubgraphImpl<Y> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public <X> Subgraph<? extends X> addSubgraph(Attribute<? super T, X> attribute, Class<? extends X> type) {
        String attrName = attribute.getName();
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attrName, AttributeNodeImpl::new);
        SubgraphImpl<? extends X> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Subgraph<X> addSubgraph(String attributeName) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        ManagedType<T> managedType = metamodel.managedType(entityType);
        Attribute<? super T, ?> attr = managedType.getAttribute(attributeName);
        Class<X> type = (Class<X>) attr.getJavaType();
        SubgraphImpl<X> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Subgraph<X> addSubgraph(String attributeName, Class<X> type) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        SubgraphImpl<X> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Subgraph<E> addElementSubgraph(PluralAttribute<? super T, ?, E> attribute) {
        String attrName = attribute.getName();
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attrName, AttributeNodeImpl::new);
        Class<E> elementType = attribute.getBindableJavaType();
        SubgraphImpl<E> subgraph = new SubgraphImpl<>(elementType, metamodel);
        node.addSubgraph(elementType, subgraph);
        return subgraph;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Subgraph<E> addTreatedElementSubgraph(PluralAttribute<? super T, ?, ? super E> attribute, Class<E> type) {
        String attrName = attribute.getName();
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attrName, AttributeNodeImpl::new);
        SubgraphImpl<E> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Subgraph<X> addElementSubgraph(String attributeName) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        ManagedType<T> managedType = metamodel.managedType(entityType);
        Attribute<? super T, ?> attr = managedType.getAttribute(attributeName);
        Class<X> type = (Class<X>) attr.getJavaType();
        SubgraphImpl<X> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    public <X> Subgraph<X> addElementSubgraph(String attributeName, Class<X> type) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        SubgraphImpl<X> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addSubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K> Subgraph<K> addMapKeySubgraph(MapAttribute<? super T, K, ?> attribute) {
        String attrName = attribute.getName();
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attrName, AttributeNodeImpl::new);
        Class<K> keyType = attribute.getKeyJavaType();
        SubgraphImpl<K> subgraph = new SubgraphImpl<>(keyType, metamodel);
        node.addKeySubgraph(keyType, subgraph);
        return subgraph;
    }

    @Override
    public <K> Subgraph<K> addTreatedMapKeySubgraph(MapAttribute<? super T, ? super K, ?> attribute, Class<K> type) {
        String attrName = attribute.getName();
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attrName, AttributeNodeImpl::new);
        SubgraphImpl<K> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addKeySubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public <X> Subgraph<X> addKeySubgraph(Attribute<? super T, X> attribute) {
        String attrName = attribute.getName();
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attrName, AttributeNodeImpl::new);
        Class<X> type = attribute.getJavaType();
        SubgraphImpl<X> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addKeySubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public <X> Subgraph<? extends X> addKeySubgraph(Attribute<? super T, X> attribute, Class<? extends X> type) {
        String attrName = attribute.getName();
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attrName, AttributeNodeImpl::new);
        SubgraphImpl<? extends X> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addKeySubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Subgraph<X> addKeySubgraph(String attributeName) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        ManagedType<T> managedType = metamodel.managedType(entityType);
        Attribute<? super T, ?> attr = managedType.getAttribute(attributeName);
        Class<X> type = (Class<X>) attr.getJavaType();
        SubgraphImpl<X> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addKeySubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    public <X> Subgraph<X> addKeySubgraph(String attributeName, Class<X> type) {
        validateAttribute(attributeName);
        AttributeNodeImpl<?> node = nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
        SubgraphImpl<X> subgraph = new SubgraphImpl<>(type, metamodel);
        node.addKeySubgraph(type, subgraph);
        return subgraph;
    }

    @Override
    public List<AttributeNode<?>> getAttributeNodes() {
        return new ArrayList<>(nodes.values());
    }

    // ---- EntityGraph-specific methods ----

    @Override
    public <S extends T> Subgraph<S> addTreatedSubgraph(Class<S> type) {
        return new SubgraphImpl<>(type, metamodel);
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public <X> Subgraph<? extends X> addSubclassSubgraph(Class<? extends X> type) {
        return new SubgraphImpl<>(type, metamodel);
    }

    // ---- Internal helpers ----

    private void validateAttribute(String attributeName) {
        if (metamodel == null) return;
        try {
            ManagedType<T> managedType = metamodel.managedType(entityType);
            managedType.getAttribute(attributeName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "The attribute [" + attributeName
                + "] is not present in the managed type ["
                + entityType.getName() + "]", e);
        }
    }

    /**
     * Add an attribute node without metamodel validation.
     * Used during materialisation from annotation metadata.
     */
    void addAttributeNodeDirect(String attributeName) {
        nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
    }

    /**
     * Get or create an attribute node without validation, for internal wiring.
     */
    AttributeNodeImpl<?> getOrCreateNode(String attributeName) {
        return nodes.computeIfAbsent(attributeName, AttributeNodeImpl::new);
    }

    /**
     * Create a mutable copy of this graph.
     */
    public EntityGraphImpl<T> copy() {
        EntityGraphImpl<T> clone = new EntityGraphImpl<>(null, entityType, metamodel);
        copyNodesTo(clone);
        return clone;
    }

    /**
     * Create a mutable copy with the given name.
     */
    public EntityGraphImpl<T> copyWithName(String newName) {
        EntityGraphImpl<T> clone = new EntityGraphImpl<>(newName, entityType, metamodel);
        copyNodesTo(clone);
        return clone;
    }

    private void copyNodesTo(EntityGraphImpl<T> target) {
        for (Map.Entry<String, AttributeNodeImpl<?>> entry : nodes.entrySet()) {
            target.nodes.put(entry.getKey(), entry.getValue().copy());
        }
    }
}
