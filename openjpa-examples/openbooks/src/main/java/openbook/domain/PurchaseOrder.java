/*
 * Copyright 2010 Pinaki Poddar
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package openbook.domain;


import java.io.Serializable;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A persistent entity.
 * Auto-generated identity.
 * Enum and Date type persistent attribute.
 * One-to-One uni-directional mapping to Customer.
 * One-to-Many bi-directional mapping to LineItem.
 * 
 * @author Pinaki Poddar
 *
 */
@SuppressWarnings("serial")
@Entity
public class PurchaseOrder implements Serializable {
    public enum Status {PENDING, DELEVERED};
    
    @Id
    @GeneratedValue
    private long id;
    
    @OneToOne(optional=false)
    private Customer customer;
    
    private Status status;
    
    @OneToMany(mappedBy="order", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<LineItem> items;
    
    private int total;
    
    @Temporal(TemporalType.TIME)
    private Date placedOn;
    
    @Temporal(TemporalType.TIME)
    private Date deliveredOn;
    
    protected PurchaseOrder() {}
    
    /**
     * Constructed by transferring the content of the given {@linkplain ShoppingCart}.
     * @param cart
     */
    public PurchaseOrder(ShoppingCart cart) {
        customer = cart.getCustomer();
        status = Status.PENDING;
        placedOn = new Time(System.currentTimeMillis());
        Map<Book, Integer> items = cart.getItems();
        for (Map.Entry<Book, Integer> entry : items.entrySet()) {
            addItem(entry.getKey(), entry.getValue());
        }
    }
    
    public long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setDelivered() {
        if (this.status == Status.DELEVERED)
            throw new IllegalStateException(this + " has been delivered");
        this.status = Status.DELEVERED;
        this.deliveredOn = new Time(System.currentTimeMillis());
    }
    
    public List<LineItem> getItems() {
        return items;
    }
    
    void addItem(Book book, int quantity) {
        if (items == null)
            items = new ArrayList<LineItem>();
        items.add(new LineItem(this, items.size()+1, book, quantity));
        total += (book.getPrice() * quantity);
    }
    
    public double getTotal() {
        return total;
    }
    
    public Date getPlacedOn() {
        return placedOn;
    }

    public Date getDeliveredOn() {
        return deliveredOn;
    }
}
