package org.apache.openjpa.persistence.criteria;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class B {
    @Id
    private long id;
    
    private int age;
}
