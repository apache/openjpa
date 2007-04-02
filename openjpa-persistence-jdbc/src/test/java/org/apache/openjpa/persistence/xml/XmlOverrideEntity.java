package org.apache.openjpa.persistence.xml;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class XmlOverrideEntity {

    @Id
    @GeneratedValue
    int id;
    
    @Basic(optional=false)
    String name;
    
    @Basic(optional=true)
    String description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    } 
}

