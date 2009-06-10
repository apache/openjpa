package org.apache.openjpa.persistence.criteria;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class A {
    @Id
    private long id;
    
    private String name;
    
    @OneToOne(fetch=FetchType.LAZY)
    private B b;
}
