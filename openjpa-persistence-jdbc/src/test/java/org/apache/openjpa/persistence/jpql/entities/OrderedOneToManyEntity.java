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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

@Entity
public class OrderedOneToManyEntity implements IOrderedEntity, java.io.Serializable {

    
    private static final long serialVersionUID = 1L;

    @Id
    private int id;

    @OneToMany
    @OrderColumn
    private List<INameEntity> entities;


    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public List<INameEntity> getEntities() {
        return entities;
    }

    @Override
    public void setEntities(List<INameEntity> names) {
        this.entities = names;
    }

    @Override
    public void addEntity(INameEntity name) {
        if( entities == null) {
            entities = new ArrayList<>();
        }
        entities.add(name);
    }

    @Override
    public INameEntity removeEntity(int location) {
        INameEntity rtnVal = null;
        if( entities != null) {
            rtnVal = entities.remove(location);
        }
        return rtnVal;
    }

    @Override
    public void insertEntity(int location, INameEntity name) {
        if( entities == null) {
            entities = new ArrayList<>();
        }
        entities.add(location, name);
    }

    @Override
    public String toString() {
        return "OrderedOneToManyEntity[" + id + "]=" + entities;
    }
}
