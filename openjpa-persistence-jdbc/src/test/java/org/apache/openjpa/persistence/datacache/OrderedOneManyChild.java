package org.apache.openjpa.persistence.datacache;

import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Entity;

@Entity
public class OrderedOneManyChild {
    @Id
    private long id;

    private String name;

    @ManyToOne
    private OrderedOneManyParent parent;
    
    public long getId() { 
        return id; 
    }

    public void setId(long id) { 
        this.id = id; 
    }

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }

    public OrderedOneManyParent getParent() { 
        return parent; 
    }

    public void setParent(OrderedOneManyParent parent) { 
        this.parent = parent; 
    }
    
}
