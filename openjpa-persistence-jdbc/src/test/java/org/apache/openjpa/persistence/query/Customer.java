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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name="TCUS")
public class Customer {

    @Embeddable
	public static class CustomerKey implements Serializable {
		
        private static final long serialVersionUID = 1L;
        public String cyCode;
		public int id;

		public CustomerKey(){}

		public  CustomerKey(String cc, int id){
			cyCode=cc;
			this.id=id;
		}

		@Override
        public String toString() {
			return cyCode+"/"+id;
		}
		@Override
		public boolean equals(Object obj){
			if (obj==this) return true;
			if ( ! (obj instanceof CustomerKey) ) return false;
			CustomerKey key = (CustomerKey)obj;
			if (key.cyCode.equals(this.cyCode) &&
					key.id==this.id) return true;
			return false;
		}

		@Override
		public int hashCode() {
			return this.cyCode.hashCode()
				^ this.id;
		}
	}

	public enum CreditRating { POOR, GOOD, EXCELLENT }

    @EmbeddedId
	 CustomerKey cid;
	@Column(length=30)
    @Basic
	 String name;
	@Enumerated(EnumType.STRING)
    @Basic
	 CreditRating creditRating;
	@Version
	 long version;

    @OneToMany(fetch=FetchType.EAGER, mappedBy="customer")
    private Collection<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy="cust")
    private List<Account> accounts = new ArrayList<>();

	public Customer() {}

	public Customer(CustomerKey cid, String name, CreditRating rating){
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

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

	@Override
    public String toString() {
		return "Customer:"+cid+" name:"+name;
	}

	public CustomerKey getCid() {
		return cid;
	}

	public void setCid(CustomerKey cid) {
		this.cid = cid;
	}
}

