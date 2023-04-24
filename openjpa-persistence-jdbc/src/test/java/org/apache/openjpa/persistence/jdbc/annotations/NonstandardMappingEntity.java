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
package org.apache.openjpa.persistence.jdbc.annotations;


import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;

import org.apache.openjpa.jdbc.meta.strats.ClassNameDiscriminatorStrategy;
import org.apache.openjpa.persistence.Persistent;
import org.apache.openjpa.persistence.PersistentCollection;
import org.apache.openjpa.persistence.PersistentMap;
import org.apache.openjpa.persistence.jdbc.Columns;
import org.apache.openjpa.persistence.jdbc.ContainerTable;
import org.apache.openjpa.persistence.jdbc.DataStoreIdColumn;
import org.apache.openjpa.persistence.jdbc.DiscriminatorStrategy;
import org.apache.openjpa.persistence.jdbc.ElementColumn;
import org.apache.openjpa.persistence.jdbc.ElementEmbeddedMapping;
import org.apache.openjpa.persistence.jdbc.ElementForeignKey;
import org.apache.openjpa.persistence.jdbc.ElementIndex;
import org.apache.openjpa.persistence.jdbc.ElementJoinColumn;
import org.apache.openjpa.persistence.jdbc.ElementNonpolymorphic;
import org.apache.openjpa.persistence.jdbc.EmbeddedMapping;
import org.apache.openjpa.persistence.jdbc.ForeignKey;
import org.apache.openjpa.persistence.jdbc.Index;
import org.apache.openjpa.persistence.jdbc.KeyColumn;
import org.apache.openjpa.persistence.jdbc.KeyForeignKey;
import org.apache.openjpa.persistence.jdbc.KeyIndex;
import org.apache.openjpa.persistence.jdbc.KeyJoinColumn;
import org.apache.openjpa.persistence.jdbc.KeyNonpolymorphic;
import org.apache.openjpa.persistence.jdbc.MappingOverride;
import org.apache.openjpa.persistence.jdbc.NonpolymorphicType;
import org.apache.openjpa.persistence.jdbc.OrderColumn;
import org.apache.openjpa.persistence.jdbc.Strategy;
import org.apache.openjpa.persistence.jdbc.XJoinColumn;
import org.apache.openjpa.persistence.jdbc.XMappingOverride;


@Entity
@Table(name = "NONSTD_ENTITY")
@DataStoreIdColumn(name = "OID")
@DiscriminatorStrategy(ClassNameDiscriminatorStrategy.ALIAS)
@DiscriminatorColumn(name = "DISCRIM", length = 128)
@XMappingOverride(name = "superCollection",
    containerTable = @ContainerTable(name = "SUP_COLL",
        joinColumns = @XJoinColumn(name = "OWNER")),
    elementColumns = @ElementColumn(name = "SUP_ELEM"))
public class NonstandardMappingEntity
    extends NonstandardMappingMappedSuper {

    @Persistent(fetch = FetchType.LAZY)
    @Strategy("org.apache.openjpa.persistence.jdbc.annotations.PointHandler")
    @Columns({
    @Column(name = "X_COL"),
    @Column(name = "Y_COL")
        })
    @Index(name = "PNT_IDX")
    private Point custom;

    @PersistentCollection(elementType = String.class)
    @ContainerTable(name = "STRINGS_COLL",
        joinColumns = @XJoinColumn(name = "OWNER"),
        joinIndex = @Index(enabled = false))
    @ElementColumn(name = "STR_ELEM", length = 127)
    @OrderColumn(name = "ORDER_COL")
    @ElementIndex
    private List stringCollection = new ArrayList();

    @PersistentCollection
    @ContainerTable(name = "JOIN_COLL",
        joinColumns = @XJoinColumn(name = "OWNER"),
        joinForeignKey = @ForeignKey)
    @ElementJoinColumn(name = "JOIN_ELEM")
    @ElementForeignKey
    @ElementNonpolymorphic(NonpolymorphicType.JOINABLE)
    private List<NonstandardMappingEntity> joinCollection =
        new ArrayList<>();

    @PersistentMap(keyType = String.class, elementType = String.class)
    @ContainerTable(name = "STRINGS_MAP",
        joinColumns = @XJoinColumn(name = "OWNER"),
        joinIndex = @Index(enabled = false))
    @KeyColumn(name = "STR_KEY", length = 127)
    @ElementColumn(name = "STR_VAL", length = 127)
    @KeyIndex
    @ElementIndex
    private Map stringMap = new HashMap();

    @PersistentMap
    @ContainerTable(name = "JOIN_MAP",
        joinColumns = @XJoinColumn(name = "OWNER"),
        joinForeignKey = @ForeignKey)
    @KeyJoinColumn(name = "JOIN_KEY")
    @KeyForeignKey
    @KeyNonpolymorphic
    @ElementJoinColumn(name = "JOIN_VAL")
    @ElementForeignKey
    @ElementNonpolymorphic
    private Map<NonstandardMappingEntity, NonstandardMappingEntity> joinMap =
        new HashMap<>();

    @Embedded
    @EmbeddedMapping(nullIndicatorAttributeName = "uuid", overrides = {
    @MappingOverride(name = "rel",
        joinColumns = @XJoinColumn(name = "EM_REL_ID")),
    @MappingOverride(name = "eager",
        containerTable = @ContainerTable(name = "EM_EAGER"),
        elementJoinColumns = @ElementJoinColumn(name = "ELEM_EAGER_ID"))
        })
    private ExtensionsEntity embed;

    @PersistentCollection(elementEmbedded = true)
    @ContainerTable(name = "EMBED_COLL")
    @ElementEmbeddedMapping(overrides = {
    @XMappingOverride(name = "basic", columns = @Column(name = "EM_BASIC"))
        })
    private List<EmbedValue2> embedCollection = new ArrayList<>();

    public Point getCustom() {
        return this.custom;
    }

    public void setCustom(Point custom) {
        this.custom = custom;
    }

    public List getStringCollection() {
        return this.stringCollection;
    }

    public List<NonstandardMappingEntity> getJoinCollection() {
        return this.joinCollection;
    }

    public Map getStringMap() {
        return this.stringMap;
    }

    public Map<NonstandardMappingEntity,NonstandardMappingEntity> getJoinMap() {
        return this.joinMap;
    }

    public ExtensionsEntity getEmbed() {
        return this.embed;
    }

    public void setEmbed(ExtensionsEntity embed) {
        this.embed = embed;
    }

    public List<EmbedValue2> getEmbedCollection() {
        return this.embedCollection;
    }
}
