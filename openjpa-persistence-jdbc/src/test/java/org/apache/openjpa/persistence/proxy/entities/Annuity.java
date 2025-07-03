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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;


@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="DTYPE", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue(value="ANNUITY")
@AttributeOverride(name="lastUpdateDate", column=@Column(name="LAST_UPDATE_TS"))
public class Annuity extends AnnuityPersistebleObject implements IAnnuity {
    private static final long serialVersionUID = 1L;
    private Double lastPaidAmt;
    private String AccountNumber;
    private Double amount;
    private String annuityHolderId;
    private List<IPayout> payouts = new ArrayList<>();
    private List<IRider> riders = new ArrayList<>();
    private List<IPayor> payors = new ArrayList<>();
    private List<String> comments;
    private Date approvedAt;

    private Annuity previousAnnuity;
    public Annuity(){
    }

    @Override
    @Column(name="LAST_PAID_AMT")
    public Double getLastPaidAmt() {
        return lastPaidAmt;
    }
    @Override
    public void setLastPaidAmt(Double lastPaidAmt) {
        this.lastPaidAmt = lastPaidAmt;
        if (this.lastPaidAmt != null) {
            DecimalFormat df = new DecimalFormat("#.##");
            this.lastPaidAmt= Double.valueOf(df.format(lastPaidAmt));
        }
    }

    @Override
    @Column(name="ACCOUNT_NUMBER")
    public String getAccountNumber() {
        return AccountNumber;
    }
    @Override
    public void setAccountNumber(String accountNumber) {
        AccountNumber = accountNumber;
    }

    @Override
    @Column(name="AMOUNT")
    public Double getAmount() {
        return amount;
    }
    @Override
    public void setAmount(Double amount) {
        this.amount = amount;
        if (this.amount != null) {
            DecimalFormat df = new DecimalFormat("#.##");
            this.amount = Double.valueOf(df.format(amount));
        }
    }

    @Override
    @Column(name="FK_ANNUITY_HOLDER_ID")
    public String getAnnuityHolderId() {
        return this.annuityHolderId;
    }
    @Override
    public void setAnnuityHolderId(String annuityHolderId) {
        this.annuityHolderId = annuityHolderId;

    }

    @Override
    @ManyToMany(targetEntity=Payor.class,
            fetch=FetchType.EAGER)
    @JoinTable(name="ANNUITY_PAYOR",
            joinColumns={@JoinColumn(name="FK_ANNUITY_ID")},
            inverseJoinColumns={@JoinColumn(name="FK_PAYOR_ID")})
    public List<IPayor> getPayors() {
        return this.payors;
    }
    @Override
    public void setPayors(List<IPayor> payors) {
        this.payors = payors;

    }

    @Override
    @OneToMany(targetEntity=Payout.class,
            mappedBy="annuity",
            fetch=FetchType.EAGER)
    public List<IPayout> getPayouts() {
        return this.payouts;
    }
    @Override
    public void setPayouts(List<IPayout> payouts) {
        this.payouts = payouts;
    }

    @Override
    @OneToMany(cascade={CascadeType.ALL},
            targetEntity=Rider.class,
            fetch=FetchType.EAGER)
    @JoinTable(name="ANNUITY_RIDER",
            joinColumns={@JoinColumn(name="FK_ANNUITY_ID")},
            inverseJoinColumns={@JoinColumn(name="FK_RIDER_ID")})
    public List<IRider> getRiders() {
        return this.riders;
    }
    @Override
    public void setRiders(List<IRider> riders) {
        this.riders = riders;
    }

    @Override
    @ElementCollection
    public List<String> getComments() {
        return comments;
    }
    @Override
    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    @Override
    @Temporal(TemporalType.DATE)
    public Date getApprovedAt() {
        return approvedAt;
    }
    @Override
    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    @Override
    @OneToOne
    public Annuity getPreviousAnnuity() {
        return previousAnnuity;
    }
    @Override
    public void setPreviousAnnuity(Annuity previousAnnuity) {
        this.previousAnnuity = previousAnnuity;
    }
}
