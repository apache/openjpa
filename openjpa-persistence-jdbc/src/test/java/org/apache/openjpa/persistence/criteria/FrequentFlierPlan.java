package org.apache.openjpa.persistence.criteria;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class FrequentFlierPlan {
    @Id
    @GeneratedValue
    private String id;
	private String name;
	private int annualMiles;
}
