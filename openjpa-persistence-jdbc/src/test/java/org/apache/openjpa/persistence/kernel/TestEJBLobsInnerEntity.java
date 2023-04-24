/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.kernel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inntable")
public class TestEJBLobsInnerEntity {

    @Id
    private String string = null;
    private String clobField = null;
    private Object eblob = null;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private TestEJBLobsInner2Entity blobField = null;

    public String getString() {
        return string;
    }

    public void setString(String val) {
        string = val;
    }

    public String getClob() {
        return clobField;
    }

    public void setClob(String val) {
        clobField = val;
    }

    public String getEBlob() {
        return ((String) eblob);
    }

    public void setEBlob(String val) {
        eblob = val;
    }

    public TestEJBLobsInner2Entity getBlob() {
        return blobField;
    }

    public void setBlob(TestEJBLobsInner2Entity val) {
        blobField = val;
    }
}
