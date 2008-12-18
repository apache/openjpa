package org.apache.openjpa.persistence.embed;

import javax.persistence.*;

import java.util.*;

@Entity
@Table(name="PMEmbedTest")

public class ProgramManager {
    @Id 
    int id;
    
    @OneToMany(mappedBy="jobInfo.pm")
    Collection<Employee> manages = new ArrayList<Employee>();
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public Collection<Employee> getManages() {
        return manages;
    }
    
    public void addManage(Employee e) {
        manages.add(e);
    }
}

