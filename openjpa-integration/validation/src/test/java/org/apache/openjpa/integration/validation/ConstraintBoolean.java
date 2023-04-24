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
package org.apache.openjpa.integration.validation;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;


@Entity(name = "VBOOLEAN")
@Table(name = "BOOLEAN_ENTITY")
public class ConstraintBoolean implements Serializable {

    @Transient
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private long id;

    @Basic
    @AssertTrue
    private Boolean trueRequired;

    @Basic
    private Boolean falseRequired;  // @AssertFalse constraint is on the getter


    /*
     * Some helper methods to create the entities to test with
     */
    public static ConstraintBoolean createInvalidTrue() {
        ConstraintBoolean c = new ConstraintBoolean();
        c.setTrueRequired(Boolean.FALSE);
        c.setFalseRequired(Boolean.FALSE);
        return c;
    }

    public static ConstraintBoolean createInvalidFalse() {
        ConstraintBoolean c = new ConstraintBoolean();
        c.setTrueRequired(Boolean.TRUE);
        c.setFalseRequired(Boolean.TRUE);
        return c;
    }

    public static ConstraintBoolean createValid() {
        ConstraintBoolean c = new ConstraintBoolean();
        c.setTrueRequired(Boolean.TRUE);
        c.setFalseRequired(Boolean.FALSE);
        return c;
    }


    /*
     * Main entity code
     */
    public ConstraintBoolean() {
    }

    public long getId() {
        return id;
    }

    public Boolean getTrueRequired() {
        return trueRequired;
    }

    public void setTrueRequired(Boolean b) {
        trueRequired = b;
    }

    @AssertFalse
    public Boolean getFalseRequired() {
        return falseRequired;
    }

    public void setFalseRequired(Boolean b) {
        falseRequired = b;
    }

}
