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

package org.apache.openjpa.persistence.jest;


import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.metamodel.Metamodel;
import javax.xml.validation.Schema;

import junit.framework.Assert;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Base class for testing JEST outside a container.
 *  
 * @author Pinaki Poddar
 *
 */
public class LocalJESTTest {
    private static EntityManagerFactory _emf;
    private static Metamodel _model;
    private EntityManager _em;

    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        _emf = Persistence.createEntityManagerFactory("jest");
        _model = _emf.getMetamodel();
        
        new DataLoader().populate(_emf.createEntityManager());
        
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        _emf.close();
    }

    @Before
    public void setUp() throws Exception {
        _em = _emf.createEntityManager();
        _em.getTransaction().begin();
    }

    @After
    public void tearDown() throws Exception {
        _em.getTransaction().rollback();
        _em.close();
    }
    
    @Test
    public void testUnitIsAvaliable() {
        Assert.assertNotNull(_emf);
    }
    @Test
    public void testModelIsAvaliable() {
        Assert.assertNotNull(_model);
        Assert.assertFalse(_model.getManagedTypes().isEmpty());
    }

    @Test
    public void testXMLEncoderForColllection() {
        encodeAndValidateQueryResult("select m from Movie m", false);
    }
    
    @Test
    public void testXMLEncoderForInstance() {
        encodeAndValidateQueryResult("select m from Movie m where m.title = '" + DataLoader.MOVIE_DATA[1][0] + "'", 
            true);
    }
    
    @Test
    public void testDomainModelEncoder() {
        DomainCommand formatter = new DomainCommand();
        Document doc = formatter.encode(_model);
        try {
            new XMLFormatter().write(doc, new PrintWriter(System.err));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }
    
    void encodeAndValidateQueryResult(String jpql, boolean single) {
        XMLFormatter formatter = new XMLFormatter();
        Query query = _em.createQuery(jpql);
        Document doc = null;
        if (single)
            doc = formatter.encodeManagedInstance(toStateManager(query.getSingleResult()), _model);
        else
            doc = formatter.encodeManagedInstances(toStateManager(query.getResultList()), _model);
        try {
            formatter.write(doc, new PrintWriter(System.err));
            formatter.validate(doc);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
    OpenJPAStateManager toStateManager(Object obj) {
        StoreContext broker = ((StoreContext)JPAFacadeHelper.toBroker(_em));
        return broker.getStateManager(obj);
    }
    
    List<OpenJPAStateManager> toStateManager(Collection<?> objects) {
        StoreContext broker = ((StoreContext)JPAFacadeHelper.toBroker(_em));
        List<OpenJPAStateManager> sms = new ArrayList<OpenJPAStateManager>();
        for (Object o : objects) {
            sms.add(broker.getStateManager(o));
        }
        return sms;
    }
    
}
