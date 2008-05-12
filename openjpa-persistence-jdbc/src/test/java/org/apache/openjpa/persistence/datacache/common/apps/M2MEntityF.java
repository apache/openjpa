package org.apache.openjpa.persistence.datacache.common.apps;

import javax.persistence.*;
import java.util.*;

@Entity
public class M2MEntityF  {
	@Id private int id;

	@ManyToMany(mappedBy="entityf")
	@MapKey(name="name")
	private Map<String, M2MEntityE> entitye;
	
	public M2MEntityF() {
		entitye = new HashMap<String,M2MEntityE>();
	}
	public Map<String, M2MEntityE> getEntityE() {
		return entitye;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String toString(){
		return "EntityF:"+id;
	}
	public void print(){
		System.out.println("EntityF id="+id+" entitye="+ entitye);
	}

}
