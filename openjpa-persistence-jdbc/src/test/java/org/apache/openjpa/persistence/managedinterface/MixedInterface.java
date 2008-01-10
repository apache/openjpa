package org.apache.openjpa.persistence.managedinterface;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.openjpa.persistence.ManagedInterface;

@ManagedInterface
@Entity
public interface MixedInterface {

    @Id
    @GeneratedValue
    public int getId();
    public void setId(int id);

    public int getIntField();
    public void setIntField(int i);
}
