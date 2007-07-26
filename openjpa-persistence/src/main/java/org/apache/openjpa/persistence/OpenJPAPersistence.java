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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.rmi.PortableRemoteObject;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.Bootstrap;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.ByteId;
import org.apache.openjpa.util.CharId;
import org.apache.openjpa.util.DoubleId;
import org.apache.openjpa.util.FloatId;
import org.apache.openjpa.util.Id;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.IntId;
import org.apache.openjpa.util.LongId;
import org.apache.openjpa.util.ObjectId;
import org.apache.openjpa.util.OpenJPAId;
import org.apache.openjpa.util.ShortId;
import org.apache.openjpa.util.StringId;

/**
 * Static helper method for JPA users, including switching
 * between OpenJPA native and Java Persistence APIs.
 *
 * @author Abe White
 * @published
 * @since 0.4.0
 */
public class OpenJPAPersistence
    extends Persistence {

    public static final String EM_KEY =
        "org.apache.openjpa.persistence.EntityManager";
    public static final String EMF_KEY =
        "org.apache.openjpa.persistence.EntityManagerFactory";

    private static Localizer _loc =
        Localizer.forPackage(OpenJPAPersistence.class);

    
    public static OpenJPAEntityManagerFactory toEntityManagerFactory
       (BrokerFactory factory) {
        if (factory == null)
            return null;

        factory.lock();
        try {
            OpenJPAEntityManagerFactory emf = (OpenJPAEntityManagerFactory)
                factory.getUserObject(EMF_KEY);
            if (emf == null) {
                emf = EntityManagerFactoryValue.newFactory(factory);
                factory.putUserObject(EMF_KEY, emf);
            }
            return emf;
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        } finally {
            factory.unlock();
        }
    }
    
    /**
     * Return the underlying broker factory for the given persistence manager
     * factory facade.
     */
    public static BrokerFactory toBrokerFactory(EntityManagerFactory emf) {
        if (emf == null)
            return null;
        if (!(emf instanceof EntityManagerFactoryImpl)) {
            Class c = emf.getClass();
            try {
                // either cast here may fail
                emf = (EntityManagerFactoryImpl) ((OpenJPAEntityManagerFactory)
                    emf).getUserObject(EMF_KEY);
            } catch (ClassCastException cce) {
                throw new ArgumentException(_loc.get
                    ("cant-convert-brokerfactory", c), null, null, false);
            }
        }
        return ((EntityManagerFactoryImpl) emf).getBrokerFactory();
    }

    /**
     * Return a persistence manager facade to the given broker retaining
     * previously associated persistence context type.
     */
    public static OpenJPAEntityManager toEntityManager(Broker broker) {
        if (broker == null)
            return null;

        broker.lock();
        try {
            OpenJPAEntityManager em = (OpenJPAEntityManager)
                broker.getUserObject(EM_KEY);
            if (em == null) {
                EntityManagerFactoryImpl emf = (EntityManagerFactoryImpl)
                    toEntityManagerFactory(broker.getBrokerFactory());
                em = emf.newEntityManagerImpl(broker);
                broker.putUserObject(EM_KEY, em);
            }
            return em;
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        } finally {
            broker.unlock();
        }
    }

    /**
     * Return the underlying broker for the given entity manager facade.
     */
    public static Broker toBroker(EntityManager em) {
        if (em == null)
            return null;
        if (!(em instanceof EntityManagerImpl)) {
            Class c = em.getClass();
            try {
                // either cast here may fail
                em = (EntityManagerImpl) ((OpenJPAEntityManager) em).
                    getUserObject(EM_KEY);
            } catch (ClassCastException cce) {
                throw new ArgumentException(_loc.get("cant-convert-broker", c),
                    null, null, false);
            }
        }
        return ((EntityManagerImpl) em).getBroker();
    }

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
            return toEntityManagerFactory(Bootstrap.getBrokerFactory(cp, null));
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
                    return toEntityManager((Broker) pc.pcGetGenericContext());
            }
            return null;
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    /**
     * Returns the {@link ClassMetaData} associated with the
     * persistent object <code>o</code>.
     */
    public static ClassMetaData getMetaData(Object o) {
        if (o == null)
            return null;
        EntityManager em = getEntityManager(o);
        return (em == null) ? null : getMetaData(em,
            ImplHelper.getManagedInstance(o).getClass());
    }

    /**
     * Returns the {@link ClassMetaData} associated with the
     * persistent type <code>cls</code>.
     */
    public static ClassMetaData getMetaData(EntityManager em, Class cls) {
        if (em == null)
            throw new NullPointerException("em == null");

        OpenJPAEntityManager kem = cast(em);
        try {
            return kem.getConfiguration().getMetaDataRepositoryInstance().
                getMetaData(cls, kem.getClassLoader(), false);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    /**
     * Returns the {@link ClassMetaData} associated with the
     * persistent type <code>cls</code>.
     */
    public static ClassMetaData getMetaData(EntityManagerFactory emf,
        Class cls) {
        if (emf == null)
            throw new NullPointerException("emf == null");

        OpenJPAEntityManagerFactory kemf = cast(emf);
        try {
            return kemf.getConfiguration().getMetaDataRepositoryInstance().
                getMetaData(cls, null, false);
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
    public static boolean isManagedType(EntityManager em, Class cls) {
        try {
            return ImplHelper.isManagedType(cls);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    /**
     * Returns true if the specified class is an entity or embeddable type.
     */
    public static boolean isManagedType(EntityManagerFactory emf, Class cls) {
        try {
            return ImplHelper.isManagedType(cls);
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
    }

    /**
     * Translate from a OpenJPA identity object to a Persistence one.
     */
    public static Object fromOpenJPAObjectId(Object oid) {
        if (oid instanceof OpenJPAId)
            return ((OpenJPAId) oid).getIdObject();
        return oid;
    }

    /**
     * Translate from a Persistence identity object to a OpenJPA one.
     */
    public static Object toOpenJPAObjectId(ClassMetaData meta, Object oid) {
        if (oid == null || meta == null)
            return null;

        Class cls = meta.getDescribedType();
        if (meta.getIdentityType() == ClassMetaData.ID_DATASTORE)
            return new Id(cls, ((Number) oid).longValue());

        if (oid instanceof Byte)
            return new ByteId(cls, (Byte) oid);
        if (oid instanceof Character)
            return new CharId(cls, (Character) oid);
        if (oid instanceof Double)
            return new DoubleId(cls, (Double) oid);
        if (oid instanceof Float)
            return new FloatId(cls, (Float) oid);
        if (oid instanceof Integer)
            return new IntId(cls, (Integer) oid);
        if (oid instanceof Long)
            return new LongId(cls, (Long) oid);
        if (oid instanceof Short)
            return new ShortId(cls, (Short) oid);
        if (oid instanceof String)
            return new StringId(cls, (String) oid);
        return new ObjectId(cls, oid);
    }

    /**
     * Return an array of OpenJPA oids for the given native oid array.
     */
    public static Object[] toOpenJPAObjectIds(ClassMetaData meta,
        Object... oids) {
        if (oids == null || oids.length == 0)
            return oids;

        // since the class if fixed for all oids, we can tell if we have to
        // translate the array based on whether the first oid needs translating
        Object oid = toOpenJPAObjectId(meta, oids[0]);
        if (oid == oids[0])
            return oids;

        Object[] copy = new Object[oids.length];
        copy[0] = oid;
        for (int i = 1; i < oids.length; i++)
            copy[i] = toOpenJPAObjectId(meta, oids[i]);
        return copy;
    }

    /**
     * Return a collection of OpenJPA oids for the given native oid collection.
     */
    public static Collection toOpenJPAObjectIds(ClassMetaData meta,
        Collection oids) {
        if (oids == null || oids.isEmpty())
            return oids;

        // since the class if fixed for all oids, we can tell if we have to
        // translate the array based on whether the first oid needs translating
        Iterator itr = oids.iterator();
        Object orig = itr.next();
        Object oid = toOpenJPAObjectId(meta, orig);
        if (oid == orig)
            return oids;

        Collection copy = new ArrayList(oids.size());
        copy.add(oid);
        while (itr.hasNext())
            copy.add(toOpenJPAObjectId(meta, itr.next()));
        return copy;
    }

    /**
     * Translate from a OpenJPA identity class to a native one.
     */
    public static Class fromOpenJPAObjectIdClass(Class oidClass) {
        if (oidClass == null)
            return null;
        if (oidClass == Id.class)
            return Long.class;
        if (oidClass == ByteId.class)
            return Byte.class;
        if (oidClass == CharId.class)
            return Character.class;
        if (oidClass == DoubleId.class)
            return Double.class;
        if (oidClass == FloatId.class)
            return Float.class;
        if (oidClass == IntId.class)
            return Integer.class;
        if (oidClass == LongId.class)
            return Long.class;
        if (oidClass == ShortId.class)
            return Short.class;
        if (oidClass == StringId.class)
			return String.class;
		return oidClass;
	}
}
