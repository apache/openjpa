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
package org.apache.openjpa.persistence.criteria.init;

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "ADDRESSES")
public class AddressEntity implements Serializable {
	private static final long serialVersionUID = -6392378887188492506L;

	@EmbeddedId
    private AddressPk id;

    @ManyToOne
    @JoinColumn(name = "USERID",
        nullable = false,
        insertable = false,
        updatable = false)
    private MyUserEntity user;

    public AddressEntity() {

    }
    public AddressEntity(AddressPk p) {
        id = p;
    }
    public MyUserEntity getUser() {
        return user;
    }

	public AddressPk getId() {
		return id;
	}

	public void setId(AddressPk id) {
		this.id = id;
	}
}
