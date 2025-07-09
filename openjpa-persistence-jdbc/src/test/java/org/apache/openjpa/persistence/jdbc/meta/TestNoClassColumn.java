/*
 * TestNoClassColumn.java
 *
 * Created on October 4, 2006, 2:44 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

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
package org.apache.openjpa.persistence.jdbc.meta;

import java.util.HashMap;
import java.util.Map;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.jdbc.common.apps.NoClassColumn;


public class TestNoClassColumn
        extends org.apache.openjpa.persistence.jdbc.kernel.BaseJDBCTest {

    /** Creates a new instance of TestNoClassColumn */
    public TestNoClassColumn() {
    }

    public TestNoClassColumn(String test) {
        super(test);
    }

    public void testQuery() {
        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
        OpenJPAQuery q = (OpenJPAQuery) pm.createNativeQuery("",NoClassColumn.class);
        //FIXME jthomas
        //q.declareParameters("java.lang.String input");
        //q.setFilter("test==input");
        Map params = new HashMap();
        params.put("input", "blah");
        //FIXME jthomas
        //Collection c = (Collection) q.executeWithMap(params);
        pm.close();
    }

}
