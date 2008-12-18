package org.apache.openjpa.persistence.embed;

import javax.persistence.*;

@Entity
@Table(name="ParkingEmbedTest")

public class ParkingSpot {
    @Id int id;
    String garage;
    
    @OneToOne(mappedBy="location.parkingSpot") 
    Employee assignedTo;
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getGarage() {
        return garage;
    }
    
    public void setGarage(String garage) {
        this.garage = garage;
    }
    
    public Employee getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(Employee assignedTo) {
        this.assignedTo = assignedTo;
    }
    
}
