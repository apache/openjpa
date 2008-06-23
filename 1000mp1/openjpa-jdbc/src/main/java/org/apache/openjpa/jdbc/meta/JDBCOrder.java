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
package org.apache.openjpa.jdbc.meta;

import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.meta.Order;

/**
 * Order in the datastore using JDBC.
 *
 * @author Abe White
 */
interface JDBCOrder
    extends Order {

    /**
     * Whether the value is a member of the field's related type, rather than
     * columns of the field itself.
     */
    public boolean isInRelation();

    /**
     * Order by this value.
     *
     * @param elem if this value has independent mappings, the mapping
     * we're selecting
     */
    public void order(Select sel, ClassMapping elem, Joins joins);
}
