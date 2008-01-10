package org.apache.openjpa.persistence.managedinterface;

import javax.persistence.OneToOne;
import javax.persistence.Id;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.CascadeType;

@Entity
public class ManagedInterfaceOwner {

    @Id
    private int id;

    private int intField;

    @OneToOne(cascade=CascadeType.PERSIST)
    private ManagedInterfaceSup iface;

    @Embedded
    private ManagedInterfaceEmbed embed;

    public int getIntField() {
        return intField;
    }

    public void setIntField(int i) {
        intField = i;
    }

    public ManagedInterfaceSup getIFace() {
        return iface;
    }

    public void setIFace(ManagedInterfaceSup iface) {
        this.iface = iface;
    }

    public ManagedInterfaceEmbed getEmbed() {
        return embed;
    }

    public void setEmbed(ManagedInterfaceEmbed embed) {
        this.embed = embed;
    }
}
