package org.apache.openjpa.persistence.managedinterface;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

@Entity
public class MixedInterfaceImpl implements MixedInterface {
    @Id
    @GeneratedValue
    private int id;

    private int intField;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIntField() {
        return intField;
    }

    public void setIntField(int i) {
        intField = i;
    }
}
