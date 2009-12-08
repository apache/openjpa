package org.apache.openjpa.persistence.identity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * JPA Entity
 *
 * @author Michael Vorburger
 */
@Entity
@Table(name = "test_type")
public class TypeEntity {

	@Id
	private Long id;
	
	private String code;
	
	// ...
}
