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
package org.apache.openjpa.jdbc.kernel;

import java.sql.Connection;

import org.apache.openjpa.jdbc.sql.DerbyDictionary;

/**
 * A Derby dictionary that takes every connection for a replication
 * connection, as MySQL's does for one of Connector/J's. Derby rejects the
 * writes of a read-only connection, as a replica does, so a write that is
 * sent to a connection the store keeps read-only fails.
 */
public class ReplicationDerbyDictionary extends DerbyDictionary {

    @Override
    public boolean isReplicationConnection(Connection conn) {
        return true;
    }
}
