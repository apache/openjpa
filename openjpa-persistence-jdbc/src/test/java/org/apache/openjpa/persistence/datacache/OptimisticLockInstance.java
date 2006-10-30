package org.apache.openjpa.persistence.datacache;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;
import javax.persistence.GeneratedValue;

import javax.persistence.Table;

@Entity
@Table(name="OPTIMISTIC_LOCK_INSTANCE")
public class OptimisticLockInstance {
    @Id @GeneratedValue 
    private int pk;

    @Version 
    private int oplock;

    private String str;

    protected OptimisticLockInstance() { }

    public OptimisticLockInstance(String str) {
        this.str = str;
    }

    public int getPK() {
        return pk;
    }

    public int getOpLock() {
        return oplock;
    }

    public String getStr() {
        return str;
    }
}
  
