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

import javax.persistence.Embeddable;

@Embeddable
public class Embeddable01 {

    private int emb01_int01;
    private int emb01_int02;
    private int emb01_int03;

    public Embeddable01() {        
    }
    public Embeddable01(int emb01_int01, 
                        int emb01_int02, 
                        int emb01_int03) {        
        this.emb01_int01 = emb01_int01;
        this.emb01_int02 = emb01_int02;
        this.emb01_int03 = emb01_int03;
    }

    public String toString() {
        return( "Embeddable01: " + 
                " emb01_int01: " + getEmb01_int01() + 
                " emb01_int02: " + getEmb01_int02() + 
                " emb01_int03: " + getEmb01_int03() );
    }

    //----------------------------------------------------------------------------------------------
    // Embeddable01 fields
    //----------------------------------------------------------------------------------------------
    public int getEmb01_int01() {
        return emb01_int01;
    }
    public void setEmb01_int01(int ii) {
        this.emb01_int01 = ii;
    }

    public int getEmb01_int02() {
        return emb01_int02;
    }
    public void setEmb01_int02(int ii) {
        this.emb01_int02 = ii;
    }

    public int getEmb01_int03() {
        return emb01_int03;
    } 
    public void setEmb01_int03(int ii) {
        this.emb01_int03 = ii;
    }
}
