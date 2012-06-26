package org.apache.openjpa.audit;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


/**
 * A simple persistent entity used to test audit facility.
 * An entity is annotated with {@link Auditable} annotation to qualify for audit.
 *  
 * @author Pinaki Poddar
 *
 */
@Entity
@Auditable
public class X {
	@Id
	@GeneratedValue
	private long id;
	
	private String name;
	private int price;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	public long getId() {
		return id;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		X other = (X) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	public String toString() {
		return "X[" + id + "]";
	}
}
