package org.apache.openjpa.persistence.jdbc.update;

import java.sql.Timestamp;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

@MappedSuperclass
public abstract class BaseTimestampedEntity {
    @Id
    @GeneratedValue
    private long id;
    
    private String name;
    
    @Version
    private Timestamp version;
    

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Timestamp getVersion() {
        return version;
    }
}
