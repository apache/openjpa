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
package org.apache.openjpa.persistence.criteria;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name="CR_CRDTC")
public class CreditCard {
    @Id
    @GeneratedValue
    private long id;

    @OneToOne
	private Customer customer;

	@OneToMany
	@OrderColumn
	private List<TransactionHistory> transactionHistory;

    public long getId() {
        return id;
    }

    public List<TransactionHistory> getTransactionHistory() {
        return transactionHistory;
    }

    public void setTransactionHistory(List<TransactionHistory>
        transactionHistory) {
        this.transactionHistory = transactionHistory;
    }

    public void addTransactionHistory(TransactionHistory transaction) {
        transactionHistory.add(transaction);
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomerr(Customer customer) {
        this.customer = customer;
    }
}
