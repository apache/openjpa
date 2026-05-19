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
package org.apache.openjpa.persistence.simple;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Mimics TCK HardwareProduct entity: subclass with PROPERTY access,
 * inherits @Id from parent. Prefixed "Unenhanced" to skip build-time enhancement.
 */
@Entity
@DiscriminatorValue("HW")
public class UnenhancedTCKHardwareProduct extends UnenhancedTCKProduct
    implements java.io.Serializable {

    private int modelNumber;

    public UnenhancedTCKHardwareProduct() {
        super();
    }

    @Column(name = "MODEL", nullable = true)
    public int getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(int modelNumber) {
        this.modelNumber = modelNumber;
    }
}
