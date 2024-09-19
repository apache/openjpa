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
package org.apache.openjpa.persistence.delimited.identifiers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name="primary entityA", schema="delim id2")
@SecondaryTable(name="secondary EntityA", schema="delim id2",
    uniqueConstraints=
        @UniqueConstraint(name="sec unq",
            columnNames={"secondary name"}))
public class EntityA {
    @TableGenerator(name = "id_gen", table = "id gen", schema = "delim id2",
        pkColumnName = "gen pk", valueColumnName = "gen value")
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "id_gen")
    @Id
    private int id;
    @Column(name="primary name", columnDefinition="VARCHAR")
    private String name;

    @Column(name="secondary name", table="secondary EntityA")
    private String secName;

    @ElementCollection
    // CollectionTable with default name generation
    @CollectionTable
    private Set<String> collectionSet = new HashSet<>();

    @ElementCollection
    @OrderColumn(name="order col")
    @CollectionTable(name="delim set", schema="delim id2")
    private Set<String> collectionDelimSet = new HashSet<>();

    @ElementCollection
    // MapKeyColumn with default name generation
    @MapKeyColumn
    private Map<String, String> collectionMap = new HashMap<>();

    @ElementCollection
    @MapKeyColumn(name="map key", columnDefinition="varchar(20)", table="m ktbl")
    private Map<String, String> delimCollectionMap =
        new HashMap<>();

    public EntityA(){
    }

    public EntityA(int id, String name) {
        this.name=name;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the secName
     */
    public String getSecName() {
        return secName;
    }

    /**
     * @param secName the secName to set
     */
    public void setSecName(String secName) {
        this.secName = secName;
    }

    /**
     * @return the collectionSet
     */
    public Set<String> getCollectionSet() {
        return collectionSet;
    }

    /**
     * @param collectionSet the collectionSet to set
     */
    public void setCollectionSet(Set<String> collectionSet) {
        this.collectionSet = collectionSet;
    }

    /**
     * Add an item to the collectionSet
     * @param item
     */
    public void addCollectionSet(String item) {
        collectionSet.add(item);
    }

    public Set<String> getCollectionDelimSet() {
        return collectionDelimSet;
    }

    public void setCollectionDelimSet(Set<String> collectionDelimSet) {
        this.collectionDelimSet = collectionDelimSet;
    }

    public void addCollectionDelimSet(String item) {
        this.collectionDelimSet.add(item);
    }

    /**
     * @return the delimCollectionMap
     */
    public Map<String, String> getDelimCollectionMap() {
        return delimCollectionMap;
    }

    /**
     * @param delimCollectionMap the delimCollectionMap to set
     */
    public void setDelimCollectionMap(Map<String, String> delimCollectionMap) {
        this.delimCollectionMap = delimCollectionMap;
    }

    public void addDelimCollectionMap(String key, String value) {
        this.delimCollectionMap.put(key, value);
    }
}
