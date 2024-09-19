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
package org.apache.openjpa.persistence.access;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "CAPITALIZATION_OLD_BEHAVIOR_TABLE")
@Access(AccessType.PROPERTY)
public class PropertyAccessCapitalizationOldBehavior {
    private long id;
    private int word;
    private int aWord;
    private int aaWord;
    private int aaaWord;
    private int CAPITAL;
    private int aCAPITAL;
    private int Another;
    private int a1;
    private int B1;
    private int a;
    private int B;
    private boolean aBoolean;
    private boolean BBoolean;
    private boolean BOOLEAN;
    private boolean Bool;

    public int getAWord() {
        return aWord;
    }

    public void setAWord(int aWord) {
        this.aWord = aWord;
    }

    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getWord() {
        return word;
    }

    public void setWord(int word) {
        this.word = word;
    }

    public int getAaWord() {
        return aaWord;
    }

    public void setAaWord(int aaWord) {
        this.aaWord = aaWord;
    }

    public int getAaaWord() {
        return aaaWord;
    }

    public void setAaaWord(int aaaWord) {
        this.aaaWord = aaaWord;
    }

    public int getCAPITAL() {
        return CAPITAL;
    }

    public void setCAPITAL(int cAPITAL) {
        CAPITAL = cAPITAL;
    }

    public int getACAPITAL() {
        return aCAPITAL;
    }

    public void setACAPITAL(int aCAPITAL) {
        this.aCAPITAL = aCAPITAL;
    }

    public int getA1() {
        return a1;
    }

    public void setA1(int a1) {
        this.a1 = a1;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public int getB1() {
        return B1;
    }

    public void setB1(int b1) {
        B1 = b1;
    }

    public int getB() {
        return B;
    }

    public void setB(int b) {
        B = b;
    }

    public int getAnother() {
        return Another;
    }

    public void setAnother(int another) {
        Another = another;
    }

    public boolean isABoolean() {
        return aBoolean;
    }

    public void setABoolean(boolean aBoolean) {
        this.aBoolean = aBoolean;
    }

    public boolean isBBoolean() {
        return BBoolean;
    }

    public void setBBoolean(boolean bBoolean) {
        BBoolean = bBoolean;
    }

    public boolean isBOOLEAN() {
        return BOOLEAN;
    }

    public void setBOOLEAN(boolean bOOLEAN) {
        BOOLEAN = bOOLEAN;
    }

    public boolean isBool() {
        return Bool;
    }

    public void setBool(boolean bool) {
        Bool = bool;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PropertyAccessCapitalizationOldBehavior other = (PropertyAccessCapitalizationOldBehavior) obj;
        if (getAnother() != other.getAnother())
            return false;
        if (getB() != other.getB())
            return false;
        if (getB1() != other.getB1())
            return false;
        if (isBBoolean() != other.isBBoolean())
            return false;
        if (isBOOLEAN() != other.isBOOLEAN())
            return false;
        if (isBool() != other.isBool())
            return false;
        if (getCAPITAL() != other.getCAPITAL())
            return false;
        if (getA() != other.getA())
            return false;
        if (getA1() != other.getA1())
            return false;
        if (isABoolean() != other.isABoolean())
            return false;
        if (getACAPITAL() != other.getACAPITAL())
            return false;
        if (getAWord() != other.getAWord())
            return false;
        if (getAaWord() != other.getAaWord())
            return false;
        if (getAaaWord() != other.getAaaWord())
            return false;
        if (getId() != other.getId())
            return false;
        if (getWord() != other.getWord())
            return false;
        return true;
    }
}
