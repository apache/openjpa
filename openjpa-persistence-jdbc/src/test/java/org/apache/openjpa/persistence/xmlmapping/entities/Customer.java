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
package org.apache.openjpa.persistence.xmlmapping.entities;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name="TCUSTOMER")
public class Customer {

	@Embeddable
	public static class CustomerKey {
		public String countryCode;
		public int id;

		public CustomerKey(){}

		public  CustomerKey(String cc, int id){
			countryCode=cc;
			this.id=id;
		}

		@Override
        public String toString() {
			return countryCode+"/"+id;
		}
		@Override
		public boolean equals(Object obj){
			if (obj == this)
                return true;
			if (! (obj instanceof CustomerKey))
                return false;
			CustomerKey key = (CustomerKey)obj;
			if (key.countryCode.equals(this.countryCode)
                && key.id==this.id)
                return true;
			return false;
		}

		@Override
		public int hashCode() {
			return this.countryCode.hashCode()
				^ this.id;
		}
	}

	public enum CreditRating { POOR, GOOD, EXCELLENT }

    @EmbeddedId
	CustomerKey cid;
	@Column(length=30)
	String name;
	@Enumerated
	CreditRating creditRating;
	@Embedded
	EAddress address;
	@Version
	long version;

	@OneToMany(fetch=FetchType.LAZY, mappedBy="customer")
	private Collection<Order> orders = new ArrayList<>();

	public Customer() {
    }

	public Customer(CustomerKey cid, String name, CreditRating rating) {
		this.cid=cid;
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

	public Collection<Order> getOrders() {
		return orders;
	}
	public void setOrders(Collection<Order> orders) {
		this.orders = orders;
	}

	@Override
    public String toString() {
		return "Customer:" + cid + " name:" + name;
	}

	public CustomerKey getCid() {
		return cid;
	}

	public void setCid(CustomerKey cid) {
		this.cid = cid;
	}

	public EAddress getAddress() {
		return address;
	}

	public void setAddress(EAddress address) {
		this.address = address;
	}
}
