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
package org.apache.openjpa.persistence.simple;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Mirrors TCK DataTypes entity — field access with one property-access override.
 * The @Transient intData2 field is exposed as persistent via property access,
 * which triggers mixed access mode. This caused IndexOutOfBoundsException in
 * PCEnhancer.addAttributeTranslation() during runtime enhancement.
 * Named "Unenhanced*" so the build-time enhancer skips it.
 */
@Entity
@Table(name = "UNENHANCED_MIXED")
@Access(AccessType.FIELD)
public class UnenhancedMixedAccessEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    protected int id;

    @Column(name = "STRDATA")
    protected String strData;

    @Column(name = "INTDATA")
    protected int intData;

    @Column(name = "DBLDATA")
    protected double doubleData;

    @Column(name = "BOOLDATA")
    protected boolean boolData;

    @Column(name = "LONGDATA")
    protected long longData;

    @Column(name = "FLOATDATA")
    protected float floatData;

    @Column(name = "SHORTDATA")
    protected short shortData;

    @Column(name = "BYTEDATA")
    protected byte byteData;

    @Column(name = "CHARDATA")
    protected char charData;

    // @Transient at field level, but exposed as persistent via property access
    @Transient
    private int intData2;

    public UnenhancedMixedAccessEntity() {
    }

    public UnenhancedMixedAccessEntity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStrData() {
        return strData;
    }

    public void setStrData(String strData) {
        this.strData = strData;
    }

    public int getIntData() {
        return intData;
    }

    public void setIntData(int intData) {
        this.intData = intData;
    }

    public double getDoubleData() {
        return doubleData;
    }

    public void setDoubleData(double doubleData) {
        this.doubleData = doubleData;
    }

    public boolean isBoolData() {
        return boolData;
    }

    public void setBoolData(boolean boolData) {
        this.boolData = boolData;
    }

    public long getLongData() {
        return longData;
    }

    public void setLongData(long longData) {
        this.longData = longData;
    }

    public float getFloatData() {
        return floatData;
    }

    public void setFloatData(float floatData) {
        this.floatData = floatData;
    }

    public short getShortData() {
        return shortData;
    }

    public void setShortData(short shortData) {
        this.shortData = shortData;
    }

    public byte getByteData() {
        return byteData;
    }

    public void setByteData(byte byteData) {
        this.byteData = byteData;
    }

    public char getCharData() {
        return charData;
    }

    public void setCharData(char charData) {
        this.charData = charData;
    }

    // Property access override — triggers mixed access mode
    @Access(AccessType.PROPERTY)
    @Column(name = "INTDATA2")
    public int getIntData2() {
        return intData2;
    }

    public void setIntData2(int intData2) {
        this.intData2 = intData2;
    }
}
