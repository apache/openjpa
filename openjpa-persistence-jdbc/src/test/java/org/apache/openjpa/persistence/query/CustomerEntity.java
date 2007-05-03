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
package org.apache.openjpa.persistence.query;

import javax.persistence.*;

import java.util.Collection;
import java.util.ArrayList;

@Entity(name="Customer")
@Table(name="CUSTOMERTB")
public class CustomerEntity {
		
	public enum CreditRating { POOR, GOOD, EXCELLENT };
	
    @Id 
    @GeneratedValue
     long cid;
	@Column(length=30)
	 String name;
	@Enumerated
	 CreditRating creditRating;
	 @Version
	 long version;
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="customer")
	private Collection<OrderEntity> orders = new ArrayList<OrderEntity>();
		
	public CustomerEntity() {}
	
	public CustomerEntity(String name, CreditRating rating){
		this.name=name;
		this.creditRating=rating;
	}	

	public String getName() {
		return name;
	}
    
	public void setName(String name) {
		this.name = name;
	}
    
	public CreditRating getRating() {
		return creditRating;
	}
    
	public void setRating(CreditRating rating) {
		this.creditRating = rating;
	}

	public Collection<OrderEntity> getOrders() {
		return orders;
	}
    
	public void setOrders(Collection<OrderEntity> orders) {
		this.orders = orders;
	}
	
	public String toString() {
		return "Customer:"+cid+" name:"+name; 
	}

	public long getCid() {
		return cid;
	}

	public void setCid(long cid) {
		this.cid = cid;
	}
}
