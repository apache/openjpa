package org.apache.openjpa.persistence.inheritance.abstractjoinedappid;

import java.util.*;
import javax.persistence.*;

import org.apache.openjpa.persistence.jdbc.ElementJoinColumn;

@Entity
@Table(name="TEST")
public class RelationOwner {
	
	private Integer id;
	private Collection<Superclass> supers = new ArrayList<Superclass>();
	
	@Id
	@Column(name="ID")
	public Integer getId() { return id;	}
	public void setId(Integer id) { this.id = id; }
	
	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
	@ElementJoinColumn(name="TEST", referencedColumnName="ID")
	public Collection<Superclass> getSupers() {	return supers; }
	public void setSupers(Collection<Superclass> supers) { this.supers = supers; }
}
