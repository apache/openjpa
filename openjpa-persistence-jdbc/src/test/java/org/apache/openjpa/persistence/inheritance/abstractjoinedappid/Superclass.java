package org.apache.openjpa.persistence.inheritance.abstractjoinedappid;

import javax.persistence.*;

@Entity
@IdClass(SuperID.class)
@Table(name="SUPER")
@Inheritance(strategy=InheritanceType.JOINED)
public abstract class Superclass {
	
	private Integer id;
	private String attr1;
	
	@Id
	@Column(name="ID")
	public Integer getId() { return id; }
	public void setId(Integer id) {	this.id = id; }
	
	@Column(name="ATTR1")
	public String getAttr1() { return attr1; }
	public void setAttr1(String attr1) { this.attr1 = attr1; }
}
