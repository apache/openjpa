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
package org.apache.openjpa.persistence.annotations.common.apps.annotApp.ddtype;

import java.util.logging.Logger;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.PrePersist;

@Entity
@ExcludeSuperclassListeners
@EntityListeners(LongNameValidator.class)
public class ContractEmployee extends Employee {
	private static final Logger logger = Logger.getLogger(ContractEmployee.class.getCanonicalName());
	private int dailyRate;
	private int term;

	public ContractEmployee(){}

	public ContractEmployee(int id, String name, int dRate, int term) {
		super(id, name);
		this.dailyRate = dRate;
		this.term = term;
	}

	@PrePersist
	public void verifyTerm() {
        logger.finest(String.format("VerifyTerm of ContractEmployee running on %s", this));

		CallbackStorage store = CallbackStorage.getInstance();
		store.getClist().add("contractemployee");
	}

	public int getDailyRate() {
		return dailyRate;
	}
	
	public void setDailyRate(int dailyRate) {
		this.dailyRate = dailyRate;
	}
	
	public int getTerm() {
		return term;
	}
	
	public void setTerm(int term) {
		this.term = term;
	}
}
