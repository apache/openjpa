package org.apache.openjpa.persistence.callbacks;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Version;


@Entity
public class ExceptionsFromCallbacksEntity {
    @Id private long id;
    @Version private int version;
    private boolean throwOnPrePersist;
    private boolean throwOnPreUpdate;
    private boolean throwOnPostLoad;
    private String stringField;
    
    public void setThrowOnPrePersist(boolean b) {
        throwOnPrePersist = b;
    }

    public void setThrowOnPostLoad(boolean b) {
        throwOnPostLoad = b;
    }

    public void setThrowOnPreUpdate(boolean b) {
        throwOnPreUpdate = b;
    }

    public void setStringField(String s) {
        stringField = s;
    }

    @PrePersist
    public void prePersist() {
        if (throwOnPrePersist)
            throw new CallbackTestException();
    }

    @PreUpdate
    public void preUpdate() {
        if (throwOnPreUpdate)
            throw new CallbackTestException();
    }

    @PostLoad
    public void postLoad() {
        if (throwOnPostLoad)
            throw new CallbackTestException();
    }
    
    public class CallbackTestException
        extends RuntimeException {
    }
}
