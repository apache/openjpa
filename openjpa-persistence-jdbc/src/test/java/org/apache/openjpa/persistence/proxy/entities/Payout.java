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
package org.apache.openjpa.persistence.proxy.entities;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Calendar;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@AttributeOverride(name="lastUpdateDate", column=@Column(name="LAST_UPDATE_TS"))
public class Payout extends AnnuityPersistebleObject implements IPayout {
	private static final long serialVersionUID = 2837981324963617180L;
	private BigDecimal taxableAmount;
	private Calendar startDate;
	private Calendar endDate;
	private IAnnuity annuity;

	@Override
    @Column(name="TAXABLE_AMOUNT")
	public BigDecimal getTaxableAmount() {
		return this.taxableAmount;
	}
	@Override
    public void setTaxableAmount(BigDecimal payoutTaxableAmt) {
		this.taxableAmount = payoutTaxableAmt;
		if (payoutTaxableAmt != null) {
			DecimalFormat df = new DecimalFormat("#.##");
			this.taxableAmount = new BigDecimal(df.format(payoutTaxableAmt));
		}

	}
	@Override
    @Column(name="START_DATE")
	public Calendar getStartDate() {
		return startDate;
	}
	@Override
    public void setStartDate(Calendar startDate) {
		this.startDate = startDate;
	}

	@Override
    @Column(name="END_DATE")
	public Calendar getEndDate() {
		return endDate;
	}

	@Override
    public void setEndDate(Calendar payoutEndDate) {
		this.endDate = payoutEndDate;
	}

	@Override
    @ManyToOne(targetEntity=Annuity.class,
			fetch=FetchType.EAGER)
	@JoinColumn(name="FK_ANNUITY_ID")
	public IAnnuity getAnnuity() {
		return this.annuity;
	}
	@Override
    public void setAnnuity(IAnnuity annuity) {
		this.annuity = annuity;

	}

}
