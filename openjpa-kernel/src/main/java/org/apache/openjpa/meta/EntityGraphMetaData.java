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
package org.apache.openjpa.meta;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds raw annotation data for a {@code @NamedEntityGraph} declaration.
 * Instances are created during annotation parsing (before the Metamodel is
 * available) and later materialised into EntityGraph objects by the
 * persistence layer.
 *
 * @since 4.2.0
 */
public class EntityGraphMetaData {

    private String name;
    private Class<?> entityClass;
    private boolean includeAllAttributes;
    private final List<AttributeNodeData> attributeNodes = new ArrayList<>();
    private final List<SubgraphData> subgraphs = new ArrayList<>();
    private final List<SubgraphData> subclassSubgraphs = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    public boolean isIncludeAllAttributes() {
        return includeAllAttributes;
    }

    public void setIncludeAllAttributes(boolean includeAllAttributes) {
        this.includeAllAttributes = includeAllAttributes;
    }

    public List<AttributeNodeData> getAttributeNodes() {
        return attributeNodes;
    }

    public List<SubgraphData> getSubgraphs() {
        return subgraphs;
    }

    public List<SubgraphData> getSubclassSubgraphs() {
        return subclassSubgraphs;
    }

    /**
         * Raw data for a single {@code @NamedAttributeNode}.
         */
        public record AttributeNodeData(String attributeName, String subgraphName, String keySubgraphName) {
    }

    /**
     * Raw data for a single {@code @NamedSubgraph}.
     */
    public static class SubgraphData {
        private final String name;
        private final Class<?> type;
        private final List<AttributeNodeData> attributeNodes = new ArrayList<>();

        public SubgraphData(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Class<?> getType() {
            return type;
        }

        public List<AttributeNodeData> getAttributeNodes() {
            return attributeNodes;
        }
    }
}
