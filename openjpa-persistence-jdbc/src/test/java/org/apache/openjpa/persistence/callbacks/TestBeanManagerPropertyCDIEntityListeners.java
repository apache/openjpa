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
package org.apache.openjpa.persistence.callbacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.common.utils.BufferedLogFactory;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Checks that the CDI entity listener factory uses the BeanManager passed under
 * <code>jakarta.persistence.bean.manager</code> instead of looking one up.
 */
@Cdi(disableDiscovery = true, classes = CDIBean.class)
public class TestBeanManagerPropertyCDIEntityListeners extends SingleEMFTestCase {

    @Inject
    private CDIBean bean;

    private final AtomicInteger beanManagerCalls = new AtomicInteger();

    @BeforeEach
    public void setUp() {
        setUp(CLEAR_TABLES, "openjpa.EntityListenerFactory", "cdi",
            "jakarta.persistence.bean.manager", recordingBeanManager());
        CDIBean.reset();
    }

    @AfterEach
    public void after() {
        CDIBean.reset();
    }

    @Override
    protected String getPersistenceUnitName() {
        return "cdi-listener-pu";
    }

    @Test
    void passedBeanManagerIsUsedForListeners() {
        assertNotNull(bean);
        try (EntityManager em = emf.createEntityManager()) {
            // the passed object may be a lazy proxy, it must not be called before a listener is needed
            assertEquals(0, beanManagerCalls.get());

            em.getTransaction().begin();
            CDIListenedEntity e0 = new CDIListenedEntity();
            em.persist(e0);
            assertEquals(1, CDIBean.getCount());
            em.getTransaction().commit();
            assertNotNull(e0.getValue());
        }
        assertTrue(beanManagerCalls.get() > 0, "BeanManager passed in the properties was not used");
    }

    @Test
    void passedBeanManagerIsAvailableFromConfiguration() {
        Object beanManager = getEmf().getConfiguration().getBeanManagerInstance();
        assertNotNull(beanManager);
        assertTrue(Proxy.isProxyClass(beanManager.getClass()));
        assertSame(beanManager, getEmf().getConfiguration().getBeanManagerInstance());
    }

    @Test
    void passedBeanManagerIsNotUsedWithoutCDIFactory() {
        int callsBefore = beanManagerCalls.get();
        try (OpenJPAEntityManagerFactorySPI statelessEmf = createEMF(
                "jakarta.persistence.bean.manager", recordingBeanManager());
             EntityManager em = statelessEmf.createEntityManager()) {
            em.getTransaction().begin();
            CDIListenedEntity e0 = new CDIListenedEntity();
            em.persist(e0);
            em.getTransaction().commit();
            assertEquals(0, CDIBean.getCount());
            assertNull(e0.getValue());
        }
        assertEquals(callsBefore, beanManagerCalls.get());
    }

    @Test
    void invalidPassedBeanManagerIsReported() {
        BufferedLogFactory logs = new BufferedLogFactory();
        try (OpenJPAEntityManagerFactorySPI invalidEmf = createEMF("openjpa.EntityListenerFactory", "cdi",
                "jakarta.persistence.bean.manager", new Object(), "openjpa.Log", logs);
             EntityManager em = invalidEmf.createEntityManager()) {
            em.getTransaction().begin();
            CDIListenedEntity e0 = new CDIListenedEntity();
            em.persist(e0);
            em.getTransaction().commit();
            // falls back to the factory without injection, but says so
            assertEquals(0, CDIBean.getCount());
            assertNull(e0.getValue());
        }
        assertTrue(logs.getBuffer().stream().anyMatch(m -> m.contains("jakarta.persistence.bean.manager")
            && m.contains(Object.class.getName())), "No warning for the invalid BeanManager in " + logs.getBuffer());
    }

    /**
     * Creates a BeanManager that delegates to the one of the running container and counts
     * all calls but hashCode, including equals and toString, like a container proxy that
     * resolves the real BeanManager on first use.
     */
    private Object recordingBeanManager() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("hashCode".equals(method.getName()) && method.getParameterCount() == 0) {
                    // not counted, the test framework caches EMFs by their properties
                    return System.identityHashCode(proxy);
                }
                beanManagerCalls.incrementAndGet();
                if (method.getDeclaringClass() == Object.class) {
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    return "RecordingBeanManager";
                }
                try {
                    return method.invoke(CDI.current().getBeanManager(), args);
                } catch (InvocationTargetException ex) {
                    throw ex.getCause();
                }
            }
        };
        return Proxy.newProxyInstance(BeanManager.class.getClassLoader(), new Class<?>[] { BeanManager.class },
            handler);
    }
}
