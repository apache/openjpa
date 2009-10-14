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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="WB_Entity02")
public class Entity02 implements Serializable {

    private static final long serialVersionUID = 2961572787273807912L;
    
    @Id
    private int id; 
    private String ent02_str01;
    private String ent02_str02;
    private String ent02_str03;
    private int ent02_int01;
    private int ent02_int02;
    private int ent02_int03;

    public Entity02() {
    }
    public Entity02(String ent02_str01, 
                    String ent02_str02, 
                    String ent02_str03,
                    int ent02_int01, 
                    int ent02_int02, 
                    int ent02_int03) {
        this.ent02_str01 = ent02_str01;
        this.ent02_str02 = ent02_str02;
        this.ent02_str02 = ent02_str03;
        this.ent02_int01 = ent02_int01;
        this.ent02_int02 = ent02_int02;
        this.ent02_int03 = ent02_int03;
    }

    public String toString() {
        return( "Entity02: id: " + getId() + 
                " ent02_str01: " + getEnt02_str01() +
                " ent02_str02: " + getEnt02_str02() + 
                " ent02_str03: " + getEnt02_str03() +
                " ent02_int01: " + getEnt02_int01() + 
                " ent02_int02: " + getEnt02_int02() + 
                " ent02_int03: " + getEnt02_int03() );
    }

    //----------------------------------------------------------------------------------------------
    // Entity02 fields
    //----------------------------------------------------------------------------------------------
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getEnt02_str01() {
        return ent02_str01;
    }
    public void setEnt02_str01(String str) {
        this.ent02_str01 = str;
    }

    public String getEnt02_str02() {
        return ent02_str02;
    }
    public void setEnt02_str02(String str) {
        this.ent02_str02 = str;
    }

    public String getEnt02_str03() {
        return ent02_str03;
    }
    public void setEnt02_str03(String str) {
        this.ent02_str03 = str;
    }

    public int getEnt02_int01() {
        return ent02_int01;
    }
    public void setEnt02_int01(int ii) {
        this.ent02_int01 = ii;
    }

    public int getEnt02_int02() {
        return ent02_int02;
    }
    public void setEnt02_int02(int ii) {
        this.ent02_int02 = ii;
    }

    public int getEnt02_int03() {
        return ent02_int03;
    } 
    public void setEnt02_int03(int ii) {
        this.ent02_int03 = ii;
    }
}
