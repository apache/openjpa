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
package org.apache.openjpa.jdbc.writebehind.entities;

import java.io.Serializable;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="WB_Entity01")
public class Entity01 implements Serializable {

    private static final long serialVersionUID = 2961572787273807912L;
    
    @Id
    private int id; 
    private String ent01_str01;
    private String ent01_str02;
    private String ent01_str03;
    private int ent01_int01;
    private int ent01_int02;
    private int ent01_int03;
    @Embedded 
    private Embeddable01 embeddable01;

    public Entity01() {
        embeddable01 = new Embeddable01();
    }
    public Entity01(String ent01_str01, 
                    String ent01_str02, 
                    String ent01_str03, 
                    int ent01_int01, 
                    int ent01_int02, 
                    int ent01_int03,
                    Embeddable01 embeddable01) {
        this.ent01_str01 = ent01_str01;
        this.ent01_str02 = ent01_str02;
        this.ent01_str02 = ent01_str03;
        this.ent01_int01 = ent01_int01;
        this.ent01_int02 = ent01_int02;
        this.ent01_int03 = ent01_int03;
        this.embeddable01 = embeddable01;
    }

    public String toString() {
        return( "Entity01: id: " + getId() + 
                " ent01_str01: " + getEnt01_str01() +
                " ent01_str02: " + getEnt01_str02() + 
                " ent01_str03: " + getEnt01_str03() + 
                " ent01_int01: " + getEnt01_int01() + 
                " ent01_int02: " + getEnt01_int02() + 
                " ent01_int03: " + getEnt01_int03() +
                " embeddable01: " + getEmbeddable01() );
    }

    //----------------------------------------------------------------------------------------------
    // Entity01 fields
    //----------------------------------------------------------------------------------------------
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getEnt01_str01() {
        return ent01_str01;
    }
    public void setEnt01_str01(String str) {
        this.ent01_str01 = str;
    }

    public String getEnt01_str02() {
        return ent01_str02;
    }
    public void setEnt01_str02(String str) {
        this.ent01_str02 = str;
    }

    public String getEnt01_str03() {
        return ent01_str03;
    }
    public void setEnt01_str03(String str) {
        this.ent01_str03 = str;
    }

    public int getEnt01_int01() {
        return ent01_int01;
    }
    public void setEnt01_int01(int ii) {
        this.ent01_int01 = ii;
    }

    public int getEnt01_int02() {
        return ent01_int02;
    }
    public void setEnt01_int02(int ii) {
        this.ent01_int02 = ii;
    }

    public int getEnt01_int03() {
        return ent01_int03;
    } 
    public void setEnt01_int03(int ii) {
        this.ent01_int03 = ii;
    }

    public Embeddable01 getEmbeddable01() {
        return embeddable01;
    }
    public void setEmbeddable01(Embeddable01 embeddable01) {
        this.embeddable01 = embeddable01;
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable01 fields
    //----------------------------------------------------------------------------------------------
    public int getEmb01_int01() {
        return embeddable01.getEmb01_int01();
    }
    public void setEmb01_int01(int ii) {
        embeddable01.setEmb01_int01(ii);
    }
    public int getEmb01_int02() {
        return embeddable01.getEmb01_int02();
    }
    public void setEmb01_int02(int ii) {
        embeddable01.setEmb01_int02(ii);
    }
    public int getEmb01_int03() {
        return embeddable01.getEmb01_int03();
    }
    public void setEmb01_int03(int ii) {
        embeddable01.setEmb01_int03(ii);
    }
}
