package org.apache.openjpa.persistence.detachment.model;

import javax.persistence.*;

@Entity
public class SimpleC {

    @Id
    @GeneratedValue
    protected int c_id;

    @Basic
    protected String name;

    @ManyToOne(cascade=CascadeType.PERSIST)
    @JoinColumn(name="B_ID", referencedColumnName="B_ID", nullable = false, updatable = false)
    protected SimpleB parent;

    public int getId() {
        return c_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParent(SimpleB b) {
       this.parent = b;
    }

    public SimpleB getParent() {
       return parent;
    }

}
