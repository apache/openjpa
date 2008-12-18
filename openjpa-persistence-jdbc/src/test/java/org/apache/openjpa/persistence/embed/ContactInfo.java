package org.apache.openjpa.persistence.embed;

import javax.persistence.*;
import java.util.*;

@Embeddable
public class ContactInfo {
    @ManyToMany 
    @JoinTable(name="EMP_PHONE",
        joinColumns = @JoinColumn(name="Emp_ID", referencedColumnName="EmpId"),
        inverseJoinColumns=
            @JoinColumn(name="PHONE_ID", referencedColumnName="Number")
        
    )
    List<PhoneNumber> phoneNumbers = new ArrayList<PhoneNumber>(); // Bidirectional
    
    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }
    
    public void addPhoneNumber(PhoneNumber phoneNumber) {
        phoneNumbers.add(phoneNumber);
    }
  
}
