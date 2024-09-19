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
package org.apache.openjpa.persistence.jdbc.order;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class ColDefTestElement {

  @Id
  @GeneratedValue
  private int id;

  @Basic
  private String name;


  public ColDefTestElement() {
  }

  public ColDefTestElement(String name) {
      this.name = name;
  }

  public void setName(String name) {
      this.name = name;
  }

  public String getName() {
      return name;
  }

  public void setId(int id) {
      this.id = id;
  }

  public int getId() {
      return id;
  }

  @Override
public boolean equals(Object obj) {
      if (obj instanceof ColDefTestElement) {
          ColDefTestElement bte = (ColDefTestElement)obj;
          return getId() == bte.getId() &&
             bte.getName().equalsIgnoreCase(bte.getName());
      }
      return false;
  }
}
