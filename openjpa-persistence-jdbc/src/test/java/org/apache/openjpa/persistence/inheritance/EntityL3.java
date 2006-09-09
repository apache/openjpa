package org.apache.openjpa.persistence.inheritance;

import javax.persistence.Entity;

@Entity 
public class EntityL3 
    extends MappedSuperclassL2 {

    private int l3data;

    public int getL3Data() {
        return l3data;
    }

    public void setL3Data(int data) {
        l3data = data;
    }
}

