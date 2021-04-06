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
package org.apache.openjpa.persistence.criteria.multiselect;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "T_FACT_WORK_ASGNMT")
public class FactWorkAssignment {
	@Id
	@Column(name = "CLNT_OBJ_ID")
	private String orgOID;

	@Column(name = "rec_eff_strt_dt")
	private Date effStartDt;

	@Column(name = "rec_eff_end_dt")
	private Date effEndDt;


	@Column(name = "rec_eff_strt_day_ky")
	private Long effectiveStartDate;

	@Column(name = "rec_eff_end_day_ky")
	private Long effectiveEndDate;

	@Column(name = "pers_ky")
	private Long personKey;

	@Column(name = "pers_obj_id")
	private String personObjId;

	@Column(name = "prmry_work_asgnmt_ind")
	private int primary;

	@Column(name = "empl_cnt")
	private int employeeCount;

	@Column(name = "work_asgnmt_stus_cd")
	private String statusCode;

	@Column(name="WORK_ASGNMT_STUS_DSC")
	private String statusDesc;

	@Column(name="WORK_ASGNMT_NBR")
	private String workAssgnmntNbr;

	@Column(name = "work_loc_ky")
	private Long workLocationKey;

	@Column(name = "hr_orgn_ky")
	private Long hrOrgKey;

	@Column(name = "PAYRL_ORGN_KY")
	private Long payrollOrgKey;

	@Column(name = "job_ky")
	private Long jobKey;

	@Column(name = "PERS_PRFL_ATTR_KY")
	private Long personProfileKey;

	@Column(name = "mngr_ky")
	private Long managerKey;

	@Column(name = "PAY_GRP_KY")
	private Long paygroupKey;

	@Column(name = "SAL_PLAN_KY")
	private Long salPlanKey;

	@Column(name = "COMPA_RT")
	private Double compaRt;

	@Column(name="CLK_NBR")
	private String clockNumber;

	@Column(name="DATA_CNTL_NBR")
	private String dataCntrlNumber;

	@Column(name="SEC_CLR_CD")
	private String secClrCd;

	@Column(name = "CUR_REC_IND")
	private boolean currentRecord;

	public Long getSalPlanKey() {
		return salPlanKey;
	}

	public void setSalPlanKey(Long salPlanKey) {
		this.salPlanKey = salPlanKey;
	}

	public Long getManagerKey() {
		return managerKey;
	}

	public void setManagerKey(Long managerKey) {
		this.managerKey = managerKey;
	}

	public Long getPersonProfileKey() {
		return personProfileKey;
	}

	public void setPersonProfileKey(Long personProfileKey) {
		this.personProfileKey = personProfileKey;
	}

	public int getPrimary() {
		return primary;
	}

	public void setPrimary(int primary) {
		this.primary = primary;
	}

	public int getEmployeeCount() {
		return employeeCount;
	}

	public void setEmployeeCount(int employeeCount) {
		this.employeeCount = employeeCount;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusDesc() {
		return statusDesc;
	}

	public void setStatusDesc(String statusDesc) {
		this.statusDesc = statusDesc;
	}

	public Long getWorkLocationKey() {
		return workLocationKey;
	}

	public void setWorkLocationKey(Long workLocationKey) {
		this.workLocationKey = workLocationKey;
	}

	public Long getHrOrgKey() {
		return hrOrgKey;
	}

	public void setHrOrgKey(Long hrOrgKey) {
		this.hrOrgKey = hrOrgKey;
	}

	public Long getJobKey() {
		return jobKey;
	}

	public void setJobKey(Long jobKey) {
		this.jobKey = jobKey;
	}

	public Long getPayrollOrgKey() {
		return payrollOrgKey;
	}

	public void setPayrollOrgKey(Long payrollOrgKey) {
		this.payrollOrgKey = payrollOrgKey;
	}

	public Double getCompaRt() {
		return compaRt;
	}

	public void setCompaRt(Double compaRt) {
		this.compaRt = compaRt;
	}

	public Long getPaygroupKey() {
		return paygroupKey;
	}

	public void setPaygroupKey(Long paygroupKey) {
		this.paygroupKey = paygroupKey;
	}

	public String getClockNumber() {
		return clockNumber;
	}

	public void setClockNumber(String clockNumber) {
		this.clockNumber = clockNumber;
	}

	public String getDataCntrlNumber() {
		return dataCntrlNumber;
	}

	public void setDataCntrlNumber(String dataCntrlNumber) {
		this.dataCntrlNumber = dataCntrlNumber;
	}

	public String getSecClrCd() {
		return secClrCd;
	}

	public void setSecClrCd(String secClrCd) {
		this.secClrCd = secClrCd;
	}

	public boolean isCurrentRecord() {
		return currentRecord;
	}

	public void setCurrentRecord(boolean currentRecord) {
		this.currentRecord = currentRecord;
	}

	public String getWorkAssgnmntNbr() {
		return workAssgnmntNbr;
	}

	public void setWorkAssgnmntNbr(String workAssgnmntNbr) {
		this.workAssgnmntNbr = workAssgnmntNbr;
	}

	public String getOrgOID() {
		return orgOID;
	}

	public void setOrgOID(String orgOID) {
		this.orgOID = orgOID;
	}

	public Long getEffectiveStartDate() {
		return effectiveStartDate;
	}

	public void setEffectiveStartDate(Long effectiveStartDate) {
		this.effectiveStartDate = effectiveStartDate;
	}

	public Long getEffectiveEndDate() {
		return effectiveEndDate;
	}

	public void setEffectiveEndDate(Long effectiveEndDate) {
		this.effectiveEndDate = effectiveEndDate;
	}

	public Long getPersonKey() {
		return personKey;
	}

	public void setPersonKey(Long personKey) {
		this.personKey = personKey;
	}

	public String getPersonObjId() {
		return personObjId;
	}

	public void setPersonObjId(String personObjId) {
		this.personObjId = personObjId;
	}

	public Date getEffStartDt() {
		return effStartDt;
	}

	public void setEffStartDt(Date effStartDt) {
		this.effStartDt = effStartDt;
	}

	public Date getEffEndDt() {
		return effEndDt;
	}

	public void setEffEndDt(Date effEndDt) {
		this.effEndDt = effEndDt;
	}

}
