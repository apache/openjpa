package org.apache.openjpa.persistence.datacache.common.apps;
import javax.persistence.*;
import java.util.*;

@Entity
public class M2MEntityE  {
	@Id private int id;
	private String name;

	@ManyToMany
	@MapKey(name="id")
	private Map<Integer,M2MEntityF> entityf;
	
	public M2MEntityE() {
		entityf = new HashMap<Integer,M2MEntityF>();
		name="entitye";
	}
		
	public Map<Integer,M2MEntityF> getEntityF() {
		return entityf;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String toString(){
		return "EntityE:"+id;
	}
	public void print(){
		System.out.println("EntityD id="+id+" entityc="+ entityf);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
