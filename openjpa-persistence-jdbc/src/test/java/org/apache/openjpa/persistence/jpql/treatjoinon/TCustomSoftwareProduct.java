package org.apache.openjpa.persistence.jpql.treatjoinon;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "TPRODUCT")
@DiscriminatorValue("CS")
public class TCustomSoftwareProduct extends TSoftwareProduct {
	
	private int customizationHours;

	public int getCustomizationHours() {
		return customizationHours;
	}

	public void setCustomizationHours(int customizationHours) {
		this.customizationHours = customizationHours;
	}

}
