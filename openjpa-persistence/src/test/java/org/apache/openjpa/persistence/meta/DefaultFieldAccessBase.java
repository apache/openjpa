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

package org.apache.openjpa.persistence.meta;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * Domain classes used by meta-model testing.
 * 
 * Uses default, field based access type.
 * 
 * @author Pinaki Poddar
 *
 */
@Entity
public class DefaultFieldAccessBase extends DefaultFieldAccessMappedSuperclass {
    private String   string;
    private int      primitiveInt;
    private Integer  boxedInt;
    
    @OneToOne
    private ExplicitFieldAccess one2oneRelation;
    @OneToMany
    private Collection<ExplicitFieldAccess> collectionRelation;
    @OneToMany
    private List<ExplicitFieldAccess> listRelation;
    @OneToMany
    private Set<ExplicitFieldAccess> setRelation;
    @ManyToMany
    private Map<ExplicitPropertyAccess, ExplicitFieldAccess> 
    mapRelationKeyPC;
    @ManyToMany
    private Map<Integer, ExplicitFieldAccess> mapRelationKeyBasic;
    
	public String getString() {
		return string;
	}
	public void setString(String string) {
		this.string = string;
	}
	public int getPrimitiveInt() {
		return primitiveInt;
	}
	public void setPrimitiveInt(int primitiveInt) {
		this.primitiveInt = primitiveInt;
	}
	public Integer getBoxedInt() {
		return boxedInt;
	}
	public void setBoxedInt(Integer boxedInt) {
		this.boxedInt = boxedInt;
	}
	public ExplicitFieldAccess getOne2oneRelation() {
		return one2oneRelation;
	}
	public void setOne2oneRelation(ExplicitFieldAccess 
			one2oneRelation) {
		this.one2oneRelation = one2oneRelation;
	}
	public Collection<ExplicitFieldAccess> getCollectionRelation() {
		return collectionRelation;
	}
	public void setCollectionRelation(Collection<ExplicitFieldAccess> 
	collection) {
		this.collectionRelation = collection;
	}
	public List<ExplicitFieldAccess> getListRelation() {
		return listRelation;
	}
	public void setListRelation(List<ExplicitFieldAccess> 
	listRelation) {
		this.listRelation = listRelation;
	}
	public Set<ExplicitFieldAccess> getSetRelation() {
		return setRelation;
	}
	public void setSetRelation(Set<ExplicitFieldAccess> setRelation) {
		this.setRelation = setRelation;
	}
	public Map<ExplicitPropertyAccess, ExplicitFieldAccess> 
	getMapRelationKeyPC() {
		return mapRelationKeyPC;
	}
	public void setMapRelationKeyPC(Map<ExplicitPropertyAccess, 
			ExplicitFieldAccess> map) {
		this.mapRelationKeyPC = map;
	}
	public Map<Integer, ExplicitFieldAccess> getMapRelationKeyBasic() {
		return mapRelationKeyBasic;
	}
	public void setMapRelationKeyBasic(Map<Integer, 
			ExplicitFieldAccess> map) {
		this.mapRelationKeyBasic = map;
	}
}
