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
package org.apache.openjpa.persistence.merge;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table( name = "ORDER_TABLE" )
public class Order {
      @Id 
      @Column( name = "ID", nullable = false )
      private Long id; 

      @Column( name = "ENTRY_DATE", nullable = false )
      @Temporal(TemporalType.TIMESTAMP)
      private Date orderEntry;
      
      // When using a List, things fails...using a Set all works fine.
      @OneToMany( fetch = FetchType.EAGER, cascade = CascadeType.ALL )
      @JoinColumn( name = "ORDER_ID", referencedColumnName = "ID" )
      private List<LineItem> items;
      
      public Order() {
          orderEntry = new Date( System.currentTimeMillis() );
          items = new ArrayList<LineItem>();
      }
      
      public Order( long id ) {
          this();
          this.id = id;
      }
      
      public Long getId() {
          return id;
      }
        
      public void setId(Long id) {
        this.id = id;
      }
        
      public Date getOrderEntry() {
          return orderEntry;
      }
        
    public void setOrderEntry(Date orderEntry) {
        this.orderEntry = orderEntry;
    }
    
    public void addItem( LineItem item ) {
        items.add(item);
        item.setOrderId(id);
        item.setItemId((long)items.size() );
    }
    
    public List<LineItem> getItems() {
        return items;
    }

    public void setItems(List<LineItem> items) {
        this.items = items;
    }
}
