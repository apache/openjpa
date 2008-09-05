package org.apache.openjpa.persistence.inheritance.abstractjoinedappid;

import javax.persistence.*;

@Entity
@IdClass(SubID.class)
@Table(name="SUB")
public class Subclass extends Superclass {
	
	private String attr2;
	
	@Column(name="ATTR2")
	public String getAttr2() { return attr2; }
	public void setAttr2(String attr2) { this.attr2 = attr2; }

}
