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
package org.apache.openjpa.persistence.flush;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Subclass of {@link UnenhancedFlushOrder}, used to assert that a dirty instance of a
 * subclass of a type in the query's access path forces a flush.
 */
@Entity
public class UnenhancedFlushExpressOrder extends UnenhancedFlushOrder {

    private String carrier;

    public UnenhancedFlushExpressOrder() {
    }

    public UnenhancedFlushExpressOrder(String id, UnenhancedFlushCustomer customer) {
        super(id, customer);
    }

    @Column(name = "CARRIER")
    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }
}
