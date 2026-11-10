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
package com.mysql.cj.jdbc.ha;

import java.sql.Connection;

/**
 * Stand-in for the interface of the same name in MySQL Connector/J 8 and
 * later, which a connection opened with a <code>jdbc:mysql:replication:</code>
 * URL implements. The MySQL driver is not on the test class path, so this
 * lets the tests look the interface up by name as the dictionary does.
 */
public interface ReplicationConnection extends Connection {
}
