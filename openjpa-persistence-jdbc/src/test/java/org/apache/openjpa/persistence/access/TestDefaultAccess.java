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
package org.apache.openjpa.persistence.access;

import javax.persistence.Query;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.access.xml.XMLDefFieldMixedPropAccess;
import org.apache.openjpa.persistence.access.xml.XMLDefPropMixedFieldAccess;
import org.apache.openjpa.persistence.access.xml.XMLFieldAccess;
import org.apache.openjpa.persistence.access.xml.XMLPropAccess;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestDefaultAccess extends SingleEMFTestCase {

    /**
     * Validates use of access specifier of FIELD in entity-mappings.
     */
    public void testEMDefaultFieldAccess() {
        OpenJPAEntityManagerFactorySPI emf = 
            (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.
            createEntityManagerFactory("Access-EMFldDef",
            "org/apache/openjpa/persistence/access/" +
            "access-def-persistence.xml");
        
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        verifyDefaultFieldAccess(em);
                        
        em.close();
        emf.close();
    }

    /**
     * Validates use of access specifier of PROPERTY in entity-mappings.
     */
    public void testEMDefaultPropertyAccess() {
        OpenJPAEntityManagerFactorySPI emf = 
            (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.
            createEntityManagerFactory("Access-EMPropDef",
            "org/apache/openjpa/persistence/access/" +
            "access-def-persistence.xml");
        
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        verifyDefaultPropertyAccess(em);

        em.close();
        emf.close();
    }

    /**
     * Validates use of access specifier of FIELD in persistence unit defaults.
     */
    public void testPUDefaultFieldAccess() {
        OpenJPAEntityManagerFactorySPI emf = 
            (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.
            createEntityManagerFactory("Access-PUFldDef",
            "org/apache/openjpa/persistence/access/" +
            "access-pudef-persistence.xml");
        
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        verifyDefaultFieldAccess(em);
                        
        em.close();
        emf.close();
    }

    /**
     * Validates use of access specifier of PROPERTY in persistence unit 
     * defaults.
     */
    public void testPUDefaultPropertyAccess() {
        OpenJPAEntityManagerFactorySPI emf = 
            (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.
            createEntityManagerFactory("Access-PUPropDef",
            "org/apache/openjpa/persistence/access/" +
            "access-pudef-persistence.xml");
        
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        verifyDefaultPropertyAccess(em);

        em.close();
        emf.close();
    }
    
    private void verifyDefaultFieldAccess(OpenJPAEntityManagerSPI em) {
        XMLFieldAccess fa = new XMLFieldAccess();
        // Set the persistent field through a misnamed setter         
        fa.setStringField("XMLFieldAccess2");
        
        em.getTransaction().begin();
        em.persist(fa);
        em.getTransaction().commit();
        em.clear();
        
        // This value of a persistent field was set using the setter
        // above, but this query will use the field name to verify that
        // field access is in use.
        Query qry = em.createNamedQuery("XMLFieldAccess2.query");
        qry.setParameter("id", fa.getId());
        qry.setParameter("strVal", "XMLFieldAccess2");
        XMLFieldAccess fa2 = (XMLFieldAccess)qry.getSingleResult();
        assertEquals(fa.getId(), fa2.getId());

        XMLDefFieldMixedPropAccess dfmpa = new XMLDefFieldMixedPropAccess();
        // Call non-PC setter
        dfmpa.setStrField("NonPCSetter");
        // Call setter with property access
        dfmpa.setStringField("XMLDFMPA2");
        
        em.getTransaction().begin();
        em.persist(dfmpa);
        em.getTransaction().commit();
        em.clear();
        
        // This value of a persistent property was set using the setter
        // above, but this query will use the property name to verify that
        // property access is in use.
        qry = em.createNamedQuery("XMLDFMPA2.query");
        qry.setParameter("id", dfmpa.getId());
        qry.setParameter("strVal", "XMLDFMPA2");
        XMLDefFieldMixedPropAccess dfmpa2 = 
            (XMLDefFieldMixedPropAccess)qry.getSingleResult();
        assertEquals(dfmpa, dfmpa2);
        assertEquals(dfmpa2.getStringField(), "XMLDFMPA2");

        try {
            qry = em.createNamedQuery("XMLDFMPA2.badQuery");
            qry.setParameter("id", dfmpa.getId());
            qry.setParameter("strVal", "XMLDFMPA2");
            qry.getSingleResult();
            fail("Execution of this query should have thrown an exception");
        }
        catch (Exception e) {
            // Expected exception
        }
    }

    private void verifyDefaultPropertyAccess(OpenJPAEntityManagerSPI em) {
        XMLPropAccess pa = new XMLPropAccess();
        // Set the persistent field through a mis-named setter         
        pa.setStrProp("PropertyAccess");
        
        em.getTransaction().begin();
        em.persist(pa);
        em.getTransaction().commit();
        em.clear();
        
        // This value of a persistent field was set using the setter
        // above, but this query will use the field name to verify that
        // field access is in use.
        Query qry = em.createNamedQuery("XMLPropAccess2.query");
        qry.setParameter("id", pa.getId());
        qry.setParameter("strVal", "PropertyAccess");
        XMLPropAccess pa2 = (XMLPropAccess)qry.getSingleResult();
        assertEquals(pa, pa2);

        XMLDefPropMixedFieldAccess dpmfa = new XMLDefPropMixedFieldAccess();
        // Call setter with underlying field access
        dpmfa.setStrProp("XMLDPMFA2");
        
        em.getTransaction().begin();
        em.persist(dpmfa);
        em.getTransaction().commit();
        em.clear();
        
        // This value of a persistent field was set using the setter
        // above, but this query will use the property name to verify that
        // property access is in use.
        qry = em.createNamedQuery("XMLDPMFA2.query");
        qry.setParameter("id", dpmfa.getId());
        qry.setParameter("strVal", "XMLDPMFA2");
        XMLDefPropMixedFieldAccess dpmfa2 = 
            (XMLDefPropMixedFieldAccess)qry.getSingleResult();
        assertEquals(dpmfa, dpmfa2);
        assertEquals(dpmfa2.getStrProp(), "XMLDPMFA2");

        try {
            qry = em.createNamedQuery("XMLDPMFA2.badQuery");
            qry.setParameter("id", dpmfa.getId());
            qry.setParameter("strVal", "XMLDPMFA2");
            qry.getSingleResult();
            fail("Usage of this query should have thrown an exception");
        }
        catch (Exception e) {
            // Expected exception
        }
    }
}
