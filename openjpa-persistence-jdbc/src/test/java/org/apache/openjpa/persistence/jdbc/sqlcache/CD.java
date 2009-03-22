package org.apache.openjpa.persistence.jdbc.sqlcache;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@Entity
@DiscriminatorValue("CD")
public class CD extends Merchandise {
    private String label;
    
    @ManyToOne
    private Singer singer;

    public CD() {
        this("?");
    }
    
    public CD(String label) {
        setLabel(label);
    }
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Singer getSinger() {
        return singer;
    }

    public void setSinger(Singer singer) {
        this.singer = singer;
        singer.addCd(this);
    }
}
