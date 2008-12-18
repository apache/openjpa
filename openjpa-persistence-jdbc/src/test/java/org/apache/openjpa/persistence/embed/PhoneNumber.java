package org.apache.openjpa.persistence.embed;

import javax.persistence.*;

import java.util.*;

@Entity
@Table(name="PhoneEmbedTest")

public class PhoneNumber {
    @Id 
    int number;
    
    @ManyToMany(mappedBy="contactInfo.phoneNumbers")
    Collection<Employee> employees = new ArrayList<Employee>();
    
    public int getNumber() {
        return number;
    }
    
    public void setNumber(int number) {
        this.number = number;
    }
    
    public Collection<Employee> getEmployees() {
        return employees;
    }
    
    public void addEmployees(Employee employee) {
        employees.add(employee);
    }
    
    
    
}
