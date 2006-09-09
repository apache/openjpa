package org.apache.openjpa.persistence.inheritance;

import javax.persistence.MappedSuperclass;

@MappedSuperclass 
public class MappedSuperclassL2 
    extends MappedSuperclassBase {

    private int l2data;

    public int getL2Data() {
        return l2data;
    }

    public void setL2Data(int data) {
        l2data = data;
    }
}

