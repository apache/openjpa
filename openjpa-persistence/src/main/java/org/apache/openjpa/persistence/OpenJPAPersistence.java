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
package org.apache.openjpa.persistence;

import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.rmi.PortableRemoteObject;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.Bootstrap;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.ImplHelper;

/**
 * Static helper methods for JPA users.
 *
 * @author Abe White
 * @published
 * @since 0.4.0
 */
public class OpenJPAPersistence {

    private static final Localizer _loc =
        Localizer.forPackage(OpenJPAPersistence.class);

    /**
     * Return the OpenJPA facade to the given entity manager factory.
     */
    public static OpenJPAEntityManagerFactory cast(EntityManagerFactory emf) {
        return (OpenJPAEntityManagerFactory) emf;
    }

    /**
     * Return the OpenJPA facade to the given entity manager.
     */
    public static OpenJPAEntityManager cast(EntityManager em) {
        if (em instanceof OpenJPAEntityManager)
            return (OpenJPAEntityManager) em;
        return (OpenJPAEntityManager) em.getDelegate();
    }

    /**
     * Return the OpenJPA facade to the given query.
     */
    public static OpenJPAQuery cast(Query q) {
        return (OpenJPAQuery) q;
    }

    /**
     * Returns the {@link OpenJPAEntityManagerFactory} specified by
     * your OpenJPA defaults. This method will return the same logical factory
     * for each invocation.
     */
    public static OpenJPAEntityManagerFactory getEntityManagerFactory() {
        return getEntityManagerFactory(null);
    }

    /**
     * Returns the {@link OpenJPAEntityManagerFactory} specified by
     * your OpenJPA defaults, using <code>map</code> as overrides. This method
     * will return the same logical factory for invocations with the same
     * overrides.
     */
    public static OpenJPAEntityManagerFactory getEntityManagerFactory(Map map) {
        ConfigurationProvider cp = new PersistenceProductDerivation.
            ConfigurationProviderImpl(map);
        try {
            return JPAFacadeHelper.toEntityManagerFactory(
                Bootstrap.getBrokerFactory(cp, null));
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    /**
     * Returns a new {@link OpenJPAEntityManagerFactory} specified by
     * <code>name</code> in an XML configuration file at the resource location
     * <code>resource</code>. If <code>name</code> is <code>null</code>, uses
     * the first resource found in the specified location, regardless of the
     * name specified in the XML resource or the name of the jar that the
     * resource is contained in. If <code>resource</code> is <code>null</code>,
     * uses the spec-defined <code>META-INF/persistence.xml</code> resource.
     *  This method only resolves {@link OpenJPAEntityManagerFactory} instances.
     */
    public static OpenJPAEntityManagerFactory createEntityManagerFactory
        (String name, String resource) {
        return createEntityManagerFactory(name, resource, null);
    }

    /**
     * Returns a new {@link OpenJPAEntityManagerFactory} specified by
     * <code>name</code> in an XML configuration file at the resource location
     * <code>resource</code>, applying the properties specified in
     * <code>map</code> as overrides. If <code>name</code> is
     * <code>null</code>, uses the first resource found in the specified
     * location, regardless of the name specified in the XML resource or the
     * name of the jar that the resource is contained in.
     * If <code>resource</code> is <code>null</code>, uses the spec-defined
     * <code>META-INF/persistence.xml</code> resource.
     *  This method only resolves {@link OpenJPAEntityManagerFactory} instances.
     */
    public static OpenJPAEntityManagerFactory createEntityManagerFactory
        (String name, String resource, Map map) {
        return (OpenJPAEntityManagerFactory) new PersistenceProviderImpl().
            createEntityManagerFactory(name, resource, map);
    }

    /**
     * Returns the {@link EntityManagerFactory} at the JNDI location specified
     * by <code>jndiLocation</code> in the context <code>context</code>. If
     * <code>context</code> is <code>null</code>,
     * <code>new InitialContext()</code> will be used.
     */
    public static OpenJPAEntityManagerFactory createEntityManagerFactory
        (String jndiLocation, Context context) {
        if (jndiLocation == null)
            throw new NullPointerException("jndiLocation == null");

        try {
            if (context == null)
                context = new InitialContext();

            Object o = context.lookup(jndiLocation);
            return (OpenJPAEntityManagerFactory) PortableRemoteObject.narrow(o,
                OpenJPAEntityManagerFactory.class);
        } catch (NamingException ne) {
            throw new ArgumentException(_loc.get("naming-exception",
                jndiLocation), new Throwable[]{ ne }, null, true);
        }
    }

    /**
     * Return the entity manager for the given object, if one can be determined
     * from just the object alone. This method will succeed for instances that
     * are enhanced, that were loaded from the database (rather than
     * being constructed with <code>new</code>), or that were created through
     * {@link OpenJPAEntityManager#createInstance}.
     */
    public static OpenJPAEntityManager getEntityManager(Object o) {
        try {
            if (ImplHelper.isManageable(o)) {
                PersistenceCapable pc = ImplHelper.toPersistenceCapable(o, null);
                if (pc != null)
                    return JPAFacadeHelper.toEntityManager(
                        (Broker) pc.pcGetGenericContext());
            }
            return null;
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    /**
     * Close the given resource. The resource can be an extent iterator,
     * query result, large result set relation, or any closeable OpenJPA
     * component.
     */
    public static void close(Object o) {
        try {
            ImplHelper.close(o);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    /**
     * Returns true if the specified class is an entity or embeddable type.
     */
    public static boolean isManagedType(Class cls) {
        try {
            return ImplHelper.isManagedType(cls);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }
}
