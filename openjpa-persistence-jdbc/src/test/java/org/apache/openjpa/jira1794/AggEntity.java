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
package org.apache.openjpa.jira1794;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "j1794_ae")
public class AggEntity {

    @Id
    @GeneratedValue
    private int id;

    private short pshortVal;
    private Short shortVal;

    private int pintVal;
    private Integer intVal;

    private long plongVal;
    private Long longVal;

    private float pfloatVal;
    private Float floatVal;

    private double pdblVal;
    private Double dblVal;

    private String stringVal;

    private java.util.Date utilDate;

    private java.sql.Date sqlDate;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setPshortVal(short pshortVal) {
        this.pshortVal = pshortVal;
    }

    public short getPshortVal() {
        return pshortVal;
    }

    public void setShortVal(Short pShortVal) {
        this.shortVal = pShortVal;
    }

    public Short getShortVal() {
        return shortVal;
    }

    public void setPintVal(int pintVal) {
        this.pintVal = pintVal;
    }

    public int getPintVal() {
        return pintVal;
    }

    public void setIntVal(Integer intVal) {
        this.intVal = intVal;
    }

    public Integer getIntVal() {
        return intVal;
    }

    public void setPlongVal(long plongVal) {
        this.plongVal = plongVal;
    }

    public long getPlongVal() {
        return plongVal;
    }

    public void setLongVal(Long longVal) {
        this.longVal = longVal;
    }

    public Long getLongVal() {
        return longVal;
    }

    public void setPfloatVal(float pfloatVal) {
        this.pfloatVal = pfloatVal;
    }

    public float getPfloatVal() {
        return pfloatVal;
    }

    public void setFloatVal(Float floatVal) {
        this.floatVal = floatVal;
    }

    public Float getFloatVal() {
        return floatVal;
    }

    public void setPdblVal(double pdblVal) {
        this.pdblVal = pdblVal;
    }

    public double getPdblVal() {
        return pdblVal;
    }

    public void setDblVal(Double dblVal) {
        this.dblVal = dblVal;
    }

    public Double getDblVal() {
        return dblVal;
    }

    public void setStringVal(String stringVal) {
        this.stringVal = stringVal;
    }

    public String getStringVal() {
        return stringVal;
    }

    public Date getUtilDate() {
        return utilDate;
    }

    public void setUtilDate(Date utilDate) {
        this.utilDate = utilDate;
    }

    public java.sql.Date getSqlDate() {
        return sqlDate;
    }

    public void setSqlDate(java.sql.Date sqlDate) {
        this.sqlDate = sqlDate;
    }

    public void init() {
        setPshortVal((short) 1);
        setShortVal((short) 1);
        setIntVal(1);
        setPintVal(1);
        setLongVal(1L);
        setPlongVal(1L);
        setDblVal(1d);
        setPdblVal(1d);
        setFloatVal(1f);
        setPfloatVal(1f);
        setStringVal("1");
        setUtilDate(new java.util.Date());
        setSqlDate(new java.sql.Date(getUtilDate().getTime()));
    }
}
