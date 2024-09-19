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
package org.apache.openjpa.persistence.jdbc.order;

import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

/*
 * Entity used for testing order column base values.  All metadata and
 * mapping information is defined in order-orm.xml
 */

@Entity
public class ColDefTestEntity {

    @Id
    @GeneratedValue
    private int id;

    @OneToMany(cascade=CascadeType.PERSIST)
    @OrderColumn(name="one2McoldefOrder", columnDefinition="INTEGER")
    private List<ColDefTestElement> one2Mcoldef;

    @ElementCollection
    @OrderColumn(name="colcoldefOrder", columnDefinition="INTEGER")
    private Set<ColDefTestElement> collcoldef;

    @ManyToMany(cascade=CascadeType.PERSIST)
    @OrderColumn(name="m2McoldefOrder", columnDefinition="INTEGER")
    private List<ColDefTestElement> m2mcoldef;

    public void setOne2Mcoldef(List<ColDefTestElement> one2Mcoldef) {
        this.one2Mcoldef = one2Mcoldef;
    }

    public List<ColDefTestElement> getOne2Mcoldef() {
        return one2Mcoldef;
    }

    public void setCollcoldef(Set<ColDefTestElement> collcoldef) {
        this.collcoldef = collcoldef;
    }

    public Set<ColDefTestElement> getCollcoldef() {
        return collcoldef;
    }

    public void setM2mcoldef(List<ColDefTestElement> m2mcoldef) {
        this.m2mcoldef = m2mcoldef;
    }

    public List<ColDefTestElement> getM2mcoldef() {
        return m2mcoldef;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
