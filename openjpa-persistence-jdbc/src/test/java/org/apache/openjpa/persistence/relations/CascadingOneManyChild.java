package org.apache.openjpa.persistence.relations;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.openjpa.persistence.jdbc.ForeignKey;

@Entity
public class CascadingOneManyChild {

    @Id
    @GeneratedValue
    private long id;

    private String name;

    @ManyToOne(optional=false)
    @JoinColumn(name="PARENT_ID")
    @ForeignKey
    private CascadingOneManyParent parent;

    @Version
    private Integer optLock;

    public long getId() { 
        return id; 
    }

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }

    public CascadingOneManyParent getParent() { 
        return parent; 
    }

    public void setParent(CascadingOneManyParent parent) { 
        this.parent = parent; 
    }
}
