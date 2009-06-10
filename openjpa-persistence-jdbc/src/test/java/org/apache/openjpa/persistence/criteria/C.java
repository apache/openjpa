package org.apache.openjpa.persistence.criteria;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

@Entity
public class C {
    @Id
    private long id;
    
    private String name;
    
    @OneToMany(mappedBy="c")
    private Set<D> set;
    
    @OneToMany(mappedBy="c")
    private List<D> list;
    
    @OneToMany(mappedBy="c")
    private Collection<D> coll;
    
    @ManyToMany
    private Map<Integer, D> map;
}
