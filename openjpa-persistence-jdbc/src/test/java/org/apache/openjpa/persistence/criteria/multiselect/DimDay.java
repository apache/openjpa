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
@Table(name="T_DIM_DAY")
public class DimDay {
	
	@Id
	@Column(name="DAY_KY")
	private Long key;
	
	@Column(name="DAY_DT")
	private Date date;
	
	@Column(name = "DAY_FULL_NM")
	private String dayFullName;
	
	@Column(name = "QTR_FULL_NM")
	private String qtrFullName;
	
	@Column(name = "MNTH_FULL_NM")
	private String monthFullName;

	@Column(name = "MNTH_SHRT_NM")
	private String monthName;
	
	@Column(name = "YR_NBR") 
	private String year;
	
	@Column(name = "QTR_IN_YR_NBR") 
	private int quarterInYearNumber;
	
	@Column(name = "YR_CD") 
	private String yearCode;
	
	@Column(name = "QTR_CD") 
	private String quarterCode;
	
	@Column(name = "MNTH_CD") 
	private String monthCode;
	
	@Column(name = "MNTH_IN_YR_NBR")
	private Long monthInYearNumber;
	
	@Column(name="CUR_MNTH_IND")
	private Long currentMonthInd;
	
	@Column(name="CUR_QTR_IND")
	private Long currentQtrInd;
	
	@Column(name="CUR_YR_IND")
	private Long currentYearInd;

	@Column(name="PREV_MNTH_IND")
	private Long prevMonthInd;
	
	@Column(name="PREV_QTR_IND")
	private Long prevQtrInd;
	
	@Column(name="PREV_YR_IND")
	private Long prevYearInd;
	
	@Column(name="CUR_MNTH_IN_PREV_YR_IND")
	private Long currentMonthVsPrevYearInd;
	
	@Column(name="CUR_QTR_IN_PREV_YR_IND")
	private Long currentQtrVsPrevYearInd;
	
	@Column(name="WK_IN_YR_NBR")
	private Long weekInYear;
	
	@Column(name="WK_IN_YR_FULL_NM")
	private String weekInYearFullNm;
	
	@Column(name = "DAY_IN_WK_NBR")
	private Long dayInWeek;

	@Column(name = "DAY_IN_MNTH_NBR")
	private Long dayInMonth;
	
	@Column(name = "DAY_IN_QTR_NBR")
	private Long dayInQuarter;
	
	@Column(name = "DAY_IN_YR_NBR")
	private Long dayInYear;
	
	@Column(name="EOM_IND")
	private Long eomInd; 
	
	@Column(name="EOQ_IND")
	private Long eoqInd;
	
	@Column(name="EOY_IND")
	private Long eoyInd; 
	
	@Column(name = "ROLL_13_MNTH_IND")
	private Long roll13MonthInd;
	
	@Column(name = "ROLL_4_YRS_IND")
	private Long roll4YearsInd;
	
	@Column(name = "ROLL_5_QTRS_IND")
	private Long roll5QuartersInd;
	
	@Column(name="MNTH_STRT_DAY_KY")
	private Long monthStrtDate;
	
	@Column(name="MNTH_END_DAY_KY")
	private Long monthEndDate;
	
	@Column(name="QTR_STRT_DAY_KY")
	private Long quarterStrtDate;
	
	@Column(name="QTR_END_DAY_KY")
	private Long quarterEndDate;
	
	@Column(name = "YR_STRT_DAY_KY")
	private Long yearStrtDate;
	
	@Column(name = "YR_END_DAY_KY")
	private Long yearEndDate;
	
	public Long getKey() {
		return key;
	}

	public void setKey(Long key) {
		this.key = key;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getDayFullName() {
		return dayFullName;
	}

	public void setDayFullName(String dayFullName) {
		this.dayFullName = dayFullName;
	}

	public String getQtrFullName() {
		return qtrFullName;
	}

	public void setQtrFullName(String qtrFullName) {
		this.qtrFullName = qtrFullName;
	}

	public String getMonthName() {
		return monthName;
	}

	public void setMonthFullName(String monthName) {
		this.monthName = monthName;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public Long getCurrentMonthInd() {
		return currentMonthInd;
	}

	public void setCurrentMonthInd(Long currentMonthInd) {
		this.currentMonthInd = currentMonthInd;
	}

	public Long getCurrentQtrInd() {
		return currentQtrInd;
	}

	public void setCurrentQtrInd(Long currentQtrInd) {
		this.currentQtrInd = currentQtrInd;
	}

	public Long getCurrentYearInd() {
		return currentYearInd;
	}

	public void setCurrentYearInd(Long currentYearInd) {
		this.currentYearInd = currentYearInd;
	}

	public Long getPrevQtrInd() {
		return prevQtrInd;
	}

	public void setPrevQtrInd(Long prevQtrInd) {
		this.prevQtrInd = prevQtrInd;
	}

	public Long getPrevYearInd() {
		return prevYearInd;
	}

	public void setPrevYearInd(Long prevYearInd) {
		this.prevYearInd = prevYearInd;
	}

	public Long getCurrentMonthVsPrevYearInd() {
		return currentMonthVsPrevYearInd;
	}

	public void setCurrentMonthVsPrevYearInd(Long currentMonthVsPrevYearInd) {
		this.currentMonthVsPrevYearInd = currentMonthVsPrevYearInd;
	}

	public Long getCurrentQtrVsPrevYearInd() {
		return currentQtrVsPrevYearInd;
	}

	public void setCurrentQtrVsPrevYearInd(Long currentQtrVsPrevYearInd) {
		this.currentQtrVsPrevYearInd = currentQtrVsPrevYearInd;
	}

	public Long getPrevMonthInd() {
		return prevMonthInd;
	}

	public void setPrevMonthInd(Long prevMonthInd) {
		this.prevMonthInd = prevMonthInd;
	}

	public Long getWeekInYear() {
		return weekInYear;
	}

	public void setWeekInYear(Long weekInYear) {
		this.weekInYear = weekInYear;
	}

	public void setMonthName(String monthName) {
		this.monthName = monthName;
	}

	public Long getEomInd() {
		return eomInd;
	}

	public void setEomInd(Long eomInd) {
		this.eomInd = eomInd;
	}

	public String getYearCode() {
		return yearCode;
	}

	public void setYearCode(String yearCode) {
		this.yearCode = yearCode;
	}

	public String getQuarterCode() {
		return quarterCode;
	}

	public void setQuarterCode(String quarterCode) {
		this.quarterCode = quarterCode;
	}

	public String getMonthCode() {
		return monthCode;
	}

	public void setMonthCode(String monthCode) {
		this.monthCode = monthCode;
	}

	public String getMonthFullName() {
		return monthFullName;
	}

	public String getWeekInYearFullNm() {
		return weekInYearFullNm;
	}

	public void setWeekInYearFullNm(String weekInYearFullNm) {
		this.weekInYearFullNm = weekInYearFullNm;
	}

	public Long getMonthInYearNumber() {
		return monthInYearNumber;
	}

	public void setMonthInYearNumber(Long monthInYearNumber) {
		this.monthInYearNumber = monthInYearNumber;
	}

	public int getQuarterInYearNumber() {
		return quarterInYearNumber;
	}

	public void setQuarterInYearNumber(int quarterInYearNumber) {
		this.quarterInYearNumber = quarterInYearNumber;
	}

	public Long getRoll13MonthInd() {
		return roll13MonthInd;
	}

	public void setRoll13MonthInd(Long roll13MonthInd) {
		this.roll13MonthInd = roll13MonthInd;
	}

	public Long getRoll4YearsInd() {
		return roll4YearsInd;
	}

	public void setRoll4YearsInd(Long roll4YearsInd) {
		this.roll4YearsInd = roll4YearsInd;
	}

	public Long getRoll5QuartersInd() {
		return roll5QuartersInd;
	}

	public void setRoll5QuartersInd(Long roll5QuartersInd) {
		this.roll5QuartersInd = roll5QuartersInd;
	}

	public Long getDayInMonth() {
		return dayInMonth;
	}

	public void setDayInMonth(Long dayInMonth) {
		this.dayInMonth = dayInMonth;
	}

	public Long getDayInQuarter() {
		return dayInQuarter;
	}

	public void setDayInQuarter(Long dayInQuarter) {
		this.dayInQuarter = dayInQuarter;
	}

	public Long getDayInYear() {
		return dayInYear;
	}

	public void setDayInYear(Long dayInYear) {
		this.dayInYear = dayInYear;
	}

	public Long getEoqInd() {
		return eoqInd;
	}

	public void setEoqInd(Long eoqInd) {
		this.eoqInd = eoqInd;
	}

	public Long getEoyInd() {
		return eoyInd;
	}

	public void setEoyInd(Long eoyInd) {
		this.eoyInd = eoyInd;
	}

	public Long getMonthStrtDate() {
		return monthStrtDate;
	}

	public void setMonthStrtDate(Long monthStrtDate) {
		this.monthStrtDate = monthStrtDate;
	}

	public Long getMonthEndDate() {
		return monthEndDate;
	}

	public void setMonthEndDate(Long monthEndDate) {
		this.monthEndDate = monthEndDate;
	}

	public Long getQuarterStrtDate() {
		return quarterStrtDate;
	}

	public void setQuarterStrtDate(Long quarterStrtDate) {
		this.quarterStrtDate = quarterStrtDate;
	}

	public Long getQuarterEndDate() {
		return quarterEndDate;
	}

	public void setQuarterEndDate(Long quarterEndDate) {
		this.quarterEndDate = quarterEndDate;
	}

	public Long getYearStrtDate() {
		return yearStrtDate;
	}

	public void setYearStrtDate(Long yearStrtDate) {
		this.yearStrtDate = yearStrtDate;
	}

	public Long getYearEndDate() {
		return yearEndDate;
	}

	public void setYearEndDate(Long yearEndDate) {
		this.yearEndDate = yearEndDate;
	}

	public Long getDayInWeek() {
		return dayInWeek;
	}

	public void setDayInWeek(Long dayInWeek) {
		this.dayInWeek = dayInWeek;
	}


}
