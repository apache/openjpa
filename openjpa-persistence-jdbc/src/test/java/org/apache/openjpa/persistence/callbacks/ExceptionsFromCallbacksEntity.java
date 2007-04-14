/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
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
