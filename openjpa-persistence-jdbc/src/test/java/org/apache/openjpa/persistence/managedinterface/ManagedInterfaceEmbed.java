package org.apache.openjpa.persistence.managedinterface;

import javax.persistence.Embeddable;
import javax.persistence.Basic;

import org.apache.openjpa.persistence.ManagedInterface;

@ManagedInterface
@Embeddable
public interface ManagedInterfaceEmbed {
    public int getEmbedIntField();
    public void setEmbedIntField(int i);
}
