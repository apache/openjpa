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
package org.apache.openjpa.persistence.jdbc.common.apps;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>Common interface for persistent types used in LRS testing.</p>
 *
 * @author Abe White
 */
public interface LRSPCIntf
    extends Comparable {

    Set getStringSet();

    void setStringSet(Set stringSet);

    Set getRelSet();

    void setRelSet(Set relSet);

    Collection getStringCollection();

    void setStringCollection(Collection stringCollection);

    Collection getRelCollection();

    void setRelCollection(Collection relCollection);

    Map getStringMap();

    void setStringMap(Map stringMap);

    Map getRelMap();

    void setRelMap(Map relMap);

    String getStringField();

    LRSPCIntf newInstance(String stringField);
}
