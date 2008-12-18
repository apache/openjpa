package org.apache.openjpa.persistence.embed;

import javax.persistence.*;

@Embeddable
public class LocationDetails {
    int officeNumber;
    
    @OneToOne 
    ParkingSpot parkingSpot;
    
    public int getOfficeNumber() {
        return officeNumber;
    }
    
    public void setOfficeNumber(int officeNumber) {
        this.officeNumber = officeNumber;
    }
    
    public ParkingSpot getParkingSpot() {
        return parkingSpot;
    }
    
    public void setParkingSpot(ParkingSpot parkingSpot) {
        this.parkingSpot = parkingSpot;
    }
    
}
