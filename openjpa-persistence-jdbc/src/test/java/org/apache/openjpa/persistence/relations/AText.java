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
package org.apache.openjpa.persistence.relations;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@Entity
public class AText {

	private int id;
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

    private String name;
    public String getName() {
    	return this.name;
    }

    public void setName(String name) {
    	this.name = name;
    }

	private ACase aCase;
	@OneToOne(fetch=FetchType.LAZY, cascade=CascadeType.MERGE)
	@JoinColumn(name="ACASE_ID", nullable=false)
	public ACase getACase() {
		return aCase;
	}

	public void setACase(ACase aCase) {
		this.aCase = aCase;
	}


	private Set<AEvident> aEvidents = new HashSet<>();
	@OneToMany(targetEntity=AEvident.class, mappedBy="aText", cascade=CascadeType.MERGE)
	public Set<AEvident> getAEvidents() {
		return aEvidents;
	}

	public void setAEvidents(Set<AEvident> aEvidents) {
		this.aEvidents = aEvidents;
	}

	private int aCaseId;
	@Column(name="ACASE_ID", insertable=false, updatable=false, unique=true)
	public int getACaseId() {
	    return aCaseId;
	}

	public void setACaseId(int aCaseId) {
	    this.aCaseId = aCaseId;
	}

}
