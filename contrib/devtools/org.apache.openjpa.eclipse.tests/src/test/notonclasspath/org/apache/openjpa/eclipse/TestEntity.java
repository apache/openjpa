package org.apache.openjpa.eclipse;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Test JPA Entity.
 */
@Entity
public class TestEntity {

	@Id
	private long id;
	
	private String name;

    // ----------------------------------------------------------------------
    
	public String getName() {
		return name;
	}
}
