package org.apache.openjpa.persistence.criteria;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class D {
    @Id
    private long id;
    
    private String name;
    
    @ManyToOne
    private C c;
}
