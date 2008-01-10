package org.apache.openjpa.persistence.managedinterface;


import java.util.*;

import javax.persistence.Entity;
import javax.persistence.Embedded;
import javax.persistence.OneToOne;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;

import org.apache.openjpa.persistence.PersistentCollection;
import org.apache.openjpa.persistence.ManagedInterface;
import org.apache.openjpa.persistence.query.SimpleEntity;

@ManagedInterface
@Entity
public interface ManagedIface extends ManagedInterfaceSup {
    public int getIntField();
    public void setIntField(int i);

    @Embedded
    public ManagedInterfaceEmbed getEmbed();
    public void setEmbed(ManagedInterfaceEmbed embed);

    @OneToOne(cascade=CascadeType.PERSIST)
    public ManagedIface getSelf();
    public void setSelf(ManagedIface iface);

    @PersistentCollection
    public Set<Integer> getSetInteger();
    public void setSetInteger(Set<Integer> collection);

    @OneToMany(cascade=CascadeType.PERSIST)
    public Set<SimpleEntity> getSetPC();
    public void setSetPC(Set<SimpleEntity> collection);

    @OneToMany(cascade=CascadeType.PERSIST)
    public Set<ManagedIface> getSetI();
    public void setSetI(Set<ManagedIface> collection);

    @OneToOne(cascade=CascadeType.PERSIST)
    public SimpleEntity getPC();
    public void setPC(SimpleEntity pc);

    public void unimplemented();
}
