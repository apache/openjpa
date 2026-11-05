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
package org.apache.openjpa.persistence.query;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite identity of {@link BulkCompositePart}.
 */
public class BulkCompositeId implements Serializable {

    private static final long serialVersionUID = 1L;

    private long region;

    private long number;

    public BulkCompositeId() {
    }

    public BulkCompositeId(long region, long number) {
        this.region = region;
        this.number = number;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BulkCompositeId)) {
            return false;
        }
        BulkCompositeId id = (BulkCompositeId) other;
        return region == id.region && number == id.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, number);
    }
}
