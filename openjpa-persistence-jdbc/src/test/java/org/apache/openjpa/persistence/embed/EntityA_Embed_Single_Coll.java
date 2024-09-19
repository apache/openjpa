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
package org.apache.openjpa.persistence.embed;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
public class EntityA_Embed_Single_Coll implements Serializable {

    
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    protected Embed_Single_Coll embed;

    @Column(length = 30)
    String name;

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Embed_Single_Coll getEmbed() {
        return embed;
    }

    public void setEmbed(Embed_Single_Coll embed) {
        this.embed = embed;
    }
}
