package org.apache.openjpa.persistence.jdbc.maps.spec_10_1_27_ex0;

import javax.persistence.*;

@Entity
@Table(name="S27x0VP")
public class VicePresident {
    @Id
    int id;
    
    String name;
  
    @ManyToOne
    Compny1 co;
    
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
    
    public Compny1 getCompany() {
        return co;
    }
    
    public void setCompany(Compny1 co) {
        this.co = co;
    }

    public boolean equals(Object v) {
        if (this == v)
            return true;
        if (!(v instanceof VicePresident))
            return false;
        VicePresident o = (VicePresident) v;
        if (this.id == o.getId() &&
            this.name.equals(o.getName()))
            return true;
        return false;
    }
}
