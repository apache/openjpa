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
package org.apache.openjpa.persistence.query;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * The persistent class for the Hardware table. Include references to
 * the People table for various owners.
 */
@Entity
public class Hardware implements Serializable {
   private static final long serialVersionUID = 1L;

   @Id
   private int id;

   @Column(length = 20)
   private String empNo;

   @OneToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
   @JoinColumn(referencedColumnName = "EmpNo")
   private Person techOwner;

   public Hardware() {
   }

   public Hardware(int id, String empNo) {
       this.id = id;
       this.empNo = empNo;
   }

   public int getId() {
       return this.id;
   }

   public void setId(int id) {
       this.id = id;
   }

   public String getEmpNo() {
       return this.empNo;
   }

   public void setEmpNo(String empNo) {
       this.empNo = empNo;
   }

   public Person getTechOwner() {
       return techOwner;
   }

   public void setTechOwner(Person techOwner) {
       this.techOwner = techOwner;
   }
}
