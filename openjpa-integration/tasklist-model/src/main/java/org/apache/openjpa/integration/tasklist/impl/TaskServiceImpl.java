/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openjpa.integration.tasklist.impl;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;

import org.apache.openjpa.integration.tasklist.model.Task;
import org.apache.openjpa.integration.tasklist.model.TaskService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TaskServiceImpl implements TaskService {
    Logger LOG = LoggerFactory.getLogger(TaskServiceImpl.class);

    // Wait for DataSource to avoid race condition at startup
    @Reference
    DataSource ds;

    @Reference
    PersistenceProvider provider;

    private EntityManagerFactory emf;

    @Activate
    public void activate() throws SQLException {
        this. emf = createEMF();
    }

    private EntityManagerFactory createEMF() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(Task.class.getClassLoader());
            Map<String, Object> map = new HashMap<String, Object>();
            return provider.createEntityManagerFactory("tasklist", map);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);             
        }
        
    }
    
    @Deactivate
    public void deActivate() throws SQLException {
        emf.close();
    }

    @Override
    public Task getTask(Integer id) {
        return null;
    }

    @Override
    public void addTask(Task task) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(task);
        em.flush();
        em.getTransaction().commit();
        em.close();
    }

    @Override
    public void updateTask(Task task) {
    }

    @Override
    public void deleteTask(Integer id) {
    }

    @Override
    public Collection<Task> getTasks() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            CriteriaQuery<Task> query = em.getCriteriaBuilder().createQuery(Task.class);
            return em.createQuery(query.select(query.from(Task.class))).getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

}
