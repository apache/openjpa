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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

@Entity
public class OrderedOneToManyEntity implements IOrderedEntity, java.io.Serializable {

    @Id
    private int id;

    @OneToMany
    @OrderColumn
    private List<INameEntity> oo2mEntities;

    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<INameEntity> getEntities() {
        return oo2mEntities;
    }

    public void setEntities(List<INameEntity> names) {
        this.oo2mEntities = names;
    }

    public void addEntities(INameEntity name) {
        if( oo2mEntities == null) {
            oo2mEntities = new ArrayList<INameEntity>();
        }
        oo2mEntities.add(name);
    }
    
    public INameEntity removeEntities(int location) {
        INameEntity rtnVal = null;
        if( oo2mEntities != null) {
            rtnVal = oo2mEntities.remove(location);
        }
        return rtnVal;
    }
    
    public void insertEntities(int location, INameEntity name) {
        if( oo2mEntities == null) {
            oo2mEntities = new ArrayList<INameEntity>();
        }
        oo2mEntities.add(location, name);
    }

    public String toString() {
        return "OrderedOneToManyEntity[" + id + "]=" + oo2mEntities;
    }
}
