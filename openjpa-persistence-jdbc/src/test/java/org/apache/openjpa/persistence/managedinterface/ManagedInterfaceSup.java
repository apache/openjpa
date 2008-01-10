package org.apache.openjpa.persistence.managedinterface;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

import org.apache.openjpa.persistence.ManagedInterface;

@ManagedInterface
@Entity
public interface ManagedInterfaceSup {
    @Id @GeneratedValue
    public int getId();
    public void setId(int id);

    public int getIntFieldSup();
    public void setIntFieldSup(int i);
}
