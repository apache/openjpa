/*
 * TestPerClassSequenceFactory.java
 *
 * Created on October 6, 2006, 2:25 PM
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
package org.apache.openjpa.persistence.jdbc.schema;

import java.util.*;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;

import org.apache.openjpa.persistence.jdbc.common.apps.*;


import java.lang.annotation.Annotation;
import junit.framework.*;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManager;


public class TestPerClassSequenceFactory
        extends org.apache.openjpa.persistence.jdbc.kernel.BaseJDBCTest {
    
   
    /** Creates a new instance of TestPerClassSequenceFactory */
    public TestPerClassSequenceFactory() {
    }
    public TestPerClassSequenceFactory(String test) {
        super(test);
    }

    public void testPerClassSequenceFactory()
        throws Exception {
        Map props = new HashMap();
        props.put("openjpa.Sequence", "table");
        
        OpenJPAEntityManagerFactory factory =(OpenJPAEntityManagerFactory)getEmf(props);

        JDBCConfiguration conf = (JDBCConfiguration) factory.getConfiguration();
        MappingRepository repos = conf.getMappingRepositoryInstance();
        ClassMapping mapping1 = repos.getMapping(RuntimeTest1.class, true);
        ClassMapping mapping2 = repos.getMapping(PerClassTestObject.class, true);
        ClassMapping mapping3 = repos.getMapping(PerClassTestObject2.class, true);

        OpenJPAEntityManager pm =(OpenJPAEntityManager)currentEntityManager();
    }

    public static class DummySequenceFactory
        implements Seq {

        private long _val = 1;

        public void setType(int type) {
        }

        public Object next(StoreContext ctx, ClassMetaData meta) {
            _val++;
            return current(ctx, meta);
        }

        public Object current(StoreContext ctx, ClassMetaData meta) {
            return new Long(_val);
        }

        public void allocate(int num, StoreContext ctx, ClassMetaData meta) {
        }

        public void close() {
        }
    }
    
    
}
