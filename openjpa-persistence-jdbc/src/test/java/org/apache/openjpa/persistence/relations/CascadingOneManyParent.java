package org.apache.openjpa.persistence.relations;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Version;

@Entity
public class CascadingOneManyParent {

    @Id
    @GeneratedValue
    private long id;

    private String name;

    @OneToMany(mappedBy="parent", cascade=CascadeType.ALL)
    @OrderBy("name ASC")
    private List<CascadingOneManyChild> children = 
        new ArrayList<CascadingOneManyChild>();

    @Version
    private Integer optLock;

    public long getId() { 
        return id; 
    }

    public List<CascadingOneManyChild> getChildren() { 
        return children; 
    }

    public void addChild(CascadingOneManyChild child) {
        child.setParent(this);
        children.add(child);
    }

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }
}
