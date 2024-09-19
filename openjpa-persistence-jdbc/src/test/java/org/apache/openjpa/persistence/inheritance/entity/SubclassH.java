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
package org.apache.openjpa.persistence.inheritance.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
public class SubclassH extends MidClass2 {

    @Basic
    private String classHName;

    @Basic
    private int intFieldSup;

    @OneToOne
    private BaseClass5 baseclass5;

    public void setClassHName(String classHName) {
        this.classHName = classHName;
    }

    public String getClassHName() {
        return classHName;
    }

    @Override
    public String toString() {
        return super.toString() + ";classHName=" + classHName +
            ";intFieldSup=" + intFieldSup;
    }

    public int getIntFieldSup() {
        return intFieldSup;
    }

    public void setIntFieldSup(int i) {
        this.intFieldSup = i;
    }

    public void setBaseclass5(BaseClass5 baseclass5) {
        this.baseclass5 = baseclass5;
    }

    public BaseClass5 getBaseclass5() {
        return baseclass5;
    }

}
