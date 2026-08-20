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
package org.apache.openjpa.persistence.util;

import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

@Entity
public class EagerEntity implements Eager {

    @Id
    private int id;

    @Basic
    private String name;

    @Embedded
    private EagerEmbed eagerEmbed;

    @Embedded
    private EagerEmbedRel eagerEmbedRel;

    @ElementCollection(fetch=FetchType.EAGER)
    private List<EagerEmbed> eagerEmbedColl;

    @OneToMany(fetch=FetchType.EAGER)
    private List<EagerEntity> eagerSelf;
    
    @Version
    @Column(name = "version_")
    private Integer version;

    @Transient
    private String transField;

    @Override
	public void setId(int id) {
        this.id = id;
    }

    @Override
	public int getId() {
        return id;
    }

    @Override
	public void setName(String name) {
        this.name = name;
    }

    @Override
	public String getName() {
        return name;
    }

    @Override
	public void setEagerEmbed(EagerEmbed eagerEmbed) {
        this.eagerEmbed = eagerEmbed;
    }

    @Override
	public EagerEmbed getEagerEmbed() {
        return eagerEmbed;
    }

    @Override
	public void setTransField(String transField) {
        this.transField = transField;
    }

    @Override
	public String getTransField() {
        return transField;
    }

    @Override
	public void setEagerEmbedColl(List<EagerEmbed> eagerEmbedColl) {
        this.eagerEmbedColl = eagerEmbedColl;
    }

    @Override
	public List<EagerEmbed> getEagerEmbedColl() {
        return eagerEmbedColl;
    }

    @Override
	public void setEagerEmbedRel(EagerEmbedRel eagerEmbedRel) {
        this.eagerEmbedRel = eagerEmbedRel;
    }

    @Override
	public EagerEmbedRel getEagerEmbedRel() {
        return eagerEmbedRel;
    }

	@Override
	public Integer getVersion() {
		return version;
	}

	@Override
	public void setVersion(Integer version) {
		this.version = version;
	}
}
