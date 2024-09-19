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
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name="\"primary entityF\"", schema="\"delim id\"",
    uniqueConstraints=
        @UniqueConstraint(columnNames={"\"f name\"", "f_nonDelimName"}))
@SecondaryTable(name="\"secondary entityF\"", schema="\"delim id\"",
    uniqueConstraints=
        @UniqueConstraint(name="\"sec unq\"",
            columnNames={"\"secondary name\""}))
public class EntityF {
    @TableGenerator(name = "f_id_gen", table = "\"f_id_gen\"",
        schema = "\"delim id\"",
        pkColumnName = "\"gen_pk\"", valueColumnName = "\"gen_value\"")
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "f_id_gen")
    @Id
    private int id;
    // Note: Delimited columnDefinition is not supported on some DBs
    // TODO: copy into a separate entity and conditionally run a different test
    @Column(name="\"f name\"", columnDefinition="char(15)")
    private String name;
    @Column(name="f_nonDelimName")
    private String nonDelimName;
    @Column(name="\"secondary name\"", table="\"secondary entityF\"")
    private String secName;

    @ElementCollection
    // CollectionTable with default name generation
    @CollectionTable
    private Set<String> cSet = new HashSet<>();

    @ElementCollection
    @CollectionTable(name="\"collectionDelimSet\"", schema="\"delim id\"")
    private Set<String> collectionDelimSet = new HashSet<>();

    @ElementCollection
    // MapKeyColumn with default name generation
    @MapKeyColumn
    private Map<String, String> cMap = new HashMap<>();

    @ElementCollection
    // Note: Delimited column definition is not supported on some DBs, so
    // it is not delimited here
    // TODO: create a separate entity and conditionally run the test on a supported DB
    @MapKeyColumn(name="\"mapKey\"", columnDefinition="varchar(20)", table="\"d colmap\"")
    private Map<String, String> dcMap =
        new HashMap<>();

    public EntityF(String name) {
        this.name = name;
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
     * @return the nonDelimName
     */
    public String getNonDelimName() {
        return nonDelimName;
    }

    /**
     * @param nonDelimName the nonDelimName to set
     */
    public void setNonDelimName(String nonDelimName) {
        this.nonDelimName = nonDelimName;
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
        return cSet;
    }

    /**
     * @param collectionSet the collectionSet to set
     */
    public void setCollectionSet(Set<String> collectionSet) {
        this.cSet = collectionSet;
    }

    public void addCollectionSet(String item) {
        cSet.add(item);
    }

    /**
     * @return the collectionNamedSet
     */
    public Set<String> getCollectionDelimSet() {
        return collectionDelimSet;
    }

    /**
     * @param collectionNamedSet the collectionNamedSet to set
     */
    public void setCollectionDelimSet(Set<String> collectionDelimSet) {
        this.collectionDelimSet = collectionDelimSet;
    }

    public void addCollectionDelimSet(String item) {
        this.collectionDelimSet.add(item);
    }

    /**
     * @return the collectionMap
     */
    public Map<String, String> getCollectionMap() {
        return cMap;
    }

    /**
     * @param collectionMap the collectionMap to set
     */
    public void setCollectionMap(Map<String, String> collectionMap) {
        this.cMap = collectionMap;
    }

    public void addCollectionMap(String key, String value) {
        cMap.put(key, value);
    }

    /**
     * @return the delimCollectionMap
     */
    public Map<String, String> getDelimCollectionMap() {
        return dcMap;
    }

    /**
     * @param delimCollectionMap the delimCollectionMap to set
     */
    public void setDelimCollectionMap(Map<String, String> delimCollectionMap) {
        this.dcMap = delimCollectionMap;
    }

    public void addDelimCollectionMap(String key, String value) {
        dcMap.put(key, value);
    }
}
