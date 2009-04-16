package org.apache.openjpa.persistence.embed.attrOverrides;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity 
@Table(name="PSN")
public class Person {
    @Id 
    protected String ssn;
    protected String name;

    @ElementCollection
    @OrderBy("zipcode.zip, zipcode.plusFour")
    protected List<Address> residences = new ArrayList<Address>();
    
    public String getSsn() {
        return ssn;
    }
    
    public void setSsn(String ssn) {
        this.ssn = ssn;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public List<Address> getResidences() {
        return residences;
    }
    
    public void setResidences(List<Address> residences) {
        this.residences = residences;
    }
    
    public void addResidence(Address residence) {
        residences.add(residence);
    }
}


