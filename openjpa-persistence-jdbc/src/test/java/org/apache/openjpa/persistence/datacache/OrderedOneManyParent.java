package org.apache.openjpa.persistence.datacache;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

@Entity
public class OrderedOneManyParent {
	@Id
    @GeneratedValue
    private long id;

    private String name;

    @OneToMany(mappedBy="parent")
    @OrderBy("name ASC")
    private List<OrderedOneManyChild> children = 
        new ArrayList<OrderedOneManyChild>();

    public long getId() { 
        return id; 
    }

    public List<OrderedOneManyChild> getChildren() { 
        return children; 
    }

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }
}

