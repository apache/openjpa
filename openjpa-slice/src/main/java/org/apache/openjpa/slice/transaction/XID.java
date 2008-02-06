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
package org.apache.openjpa.slice.transaction;

import javax.transaction.xa.Xid;

/**
 * Internally used Global Transaction Identifier for two-phase distributed
 * commit protocol.
 * 
 * @author Pinaki Poddar
 * 
 */
class XID implements Xid {
    private final int format;
    private final byte[] global;
    private final byte[] branch;

    public XID(int format, byte[] global, byte[] branch) {
        super();
        this.format = format;
        this.global = global;
        this.branch = branch;
    }

    public byte[] getBranchQualifier() {
        return branch;
    }

    public int getFormatId() {
        return format;
    }

    public byte[] getGlobalTransactionId() {
        return global;
    }

    XID branch(Number number) {
        return branch((number == null) ? "null" : number.toString());
    }
    
    XID branch(String branch) {
        return new XID(format, global, branch.getBytes());
    }

    public String toString() {
        return new String(global) + ":" + new String(branch);
    }

    public boolean equals(Object other) {
        if (other instanceof XID) {
            XID that = (XID) other;
            return format == that.format && equals(global, that.global)
                    && equals(branch, that.branch);
        }
        return false;
    }

    boolean equals(byte[] a, byte[] b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return new String(a).equals(new String(b));
    }

}
