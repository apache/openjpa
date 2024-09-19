/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.jpql.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;

public class XMLOrderedNameEntity implements INameEntity, java.io.Serializable {

    
    private static final long serialVersionUID = 1L;

    private int id;

    private String name;

    @ManyToMany
    @OrderColumn
    private List <IColumnEntity> columns;


    public XMLOrderedNameEntity() {
    }

    public XMLOrderedNameEntity(String name) {
        this.id = name.charAt(0) - 'A' + 1;
        this.name = name;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "XMLOrderedNameEntity[" + id + "]=" + name;
    }

    public List<IColumnEntity> getColumns() {
        return columns;
    }

    public void setColumns(List<IColumnEntity> columns) {
        this.columns = columns;
    }

    public void addColumns(IColumnEntity column) {
        if( columns == null) {
            columns = new ArrayList<>();
        }
        columns.add(column);
    }

    public IColumnEntity removeColumns(IColumnEntity entity) {
        IColumnEntity rtnVal = null;
        if( columns != null) {
            if( columns.remove(entity) )
                rtnVal = entity;
        }
        return rtnVal;
    }
}
