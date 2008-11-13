package org.apache.openjpa.persistence.inheritance.abstractjoinedappid;

import java.io.Serializable;

public abstract class SuperID implements Serializable {
	
	private Integer id;

	public Integer getId() { return id; }
	public void setId(Integer id) {	this.id = id; }
	
	public int hashCode() {
		return id;
	}
	
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof SuperID)) return false;
		SuperID pk = (SuperID) obj;
		if ( pk.getId().equals(id)) {
			return true;
		} else {
			return false;
		}
	}
}
