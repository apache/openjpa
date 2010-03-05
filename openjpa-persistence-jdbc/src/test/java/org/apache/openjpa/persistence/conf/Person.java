package org.apache.openjpa.persistence.conf;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

// override defaults to attempt to prevent collisions.
@Entity(name="confPerson")
@Table(name="CONF_PERSON")
public class Person {
    
    @Id
    private int id;

    @Version
    private int version;
    
    @Column(length=16)
    private String name;
    
    public Person() { 
        super();
    }
   
    public Person(int id) { 
        super();
        setId(id);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
