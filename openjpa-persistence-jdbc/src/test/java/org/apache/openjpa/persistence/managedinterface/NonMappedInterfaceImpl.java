package org.apache.openjpa.persistence.managedinterface;

import javax.persistence.Entity;

@Entity
public class NonMappedInterfaceImpl
    implements NonMappedInterface {
    private int mismatch;

    public int getIntField() {
        return mismatch;
    }

    public void setIntField(int i) {
        mismatch = i;
    }
}
