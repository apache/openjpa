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
package org.apache.openjpa.persistence.jpql.expressions;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.proxy.TreeNode;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests index function
 *  
 * @author Catalina Wei
 */
public class TestIndex extends SingleEMFTestCase {
    public void setUp() {
        super.setUp(CLEAR_TABLES, TreeNode.class);
    }
    
    public void testQueryIndex() {
        persistTree();
        EntityManager em = emf.createEntityManager();
        String query = "SELECT index(c) from TreeNode t, in (t.childern) c" +
            " WHERE index(c) = 2"; 
        
        List<Object> rs = em.createQuery(query).getResultList();
        for (Object t: rs)
            assertEquals(2, Integer.parseInt(t.toString()));
        
        em.close();                
    }

    public void createTree() {
        TreeNode root = new TreeNode();
        root.setName("0");
        int[] fanOuts = {1,2,3};
        root.createTree(fanOuts);
        assertArrayEquals(fanOuts, root.getFanOuts());
    }

    public void persistTree() {
        int[] fanOuts = {2,3,4};
        create(fanOuts);
    }

    /**
     * Create a uniform tree with given fan out.
     * Persist.
     */
    TreeNode create(int[] original) {
        TreeNode root = new TreeNode();
        root.createTree(original);
        
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(root);
        em.getTransaction().commit();
        em.clear();
        
        return root;
    }

    /**
     *  Asserts the given arrays have exactly same elements at the same index.
     */
    void assertArrayEquals(int[] a, int[] b) {
        assertEquals(a.length, b.length);
        for (int i = 0; i<a.length; i++)
            assertEquals(a[i], b[i]);
    }
}
