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
package org.apache.openjpa.enhance;

import java.io.Serializable;

/**
 *
 */
public class UnenhancedBootstrapInstanceId implements Serializable {
    private long billNumber;
    private long billVersion;
    private long billRevision;

    public UnenhancedBootstrapInstanceId() {

    }

    public UnenhancedBootstrapInstanceId(final long number, final long version, final long revision) {
        this.billNumber = number;
        this.billVersion = version;
        this.billRevision = revision;
    }

    public long getBillNumber() {
        return this.billNumber;
    }

    public void setBillNumber(final long number) {
        this.billNumber = number;
    }

    public long getBillVersion() {
        return this.billVersion;
    }

    public void setBillVersion(final long version) {
        this.billVersion = version;
    }

    public long getBillRevision() {
        return this.billRevision;
    }

    public void setBillRevision(final long revision) {
        this.billRevision = revision;
    }

    public boolean equals(final Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof UnenhancedBootstrapInstanceId))
            return false;

        final UnenhancedBootstrapInstanceId pk = (UnenhancedBootstrapInstanceId) obj;

        if (billNumber != pk.billNumber)
            return false;

        if (billVersion != pk.billVersion)
            return false;

        if (billRevision != pk.billRevision)
            return false;

        return true;
    }

    public int hashCode() {
        return (billNumber + "." + billVersion + "." + billRevision).hashCode();
    }
}
