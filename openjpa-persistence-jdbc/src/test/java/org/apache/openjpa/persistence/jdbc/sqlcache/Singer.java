package org.apache.openjpa.persistence.jdbc.sqlcache;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

@Entity
@DiscriminatorValue("SINGER")
public class Singer extends Person {
    
    @OneToMany
    private Set<CD> cds;

    public Singer() {
        super();
    }

    public Singer(String firstName, String lastName, short age, int yob) {
        super(firstName, lastName, age, yob);
    }
    
    public Set<CD> getCds() {
        return cds;
    }

    public void addCd(CD cd) {
        if (cds == null)
            cds = new HashSet<CD>();
        if (cds.add(cd))
            cd.setSinger(this);
    }
}
