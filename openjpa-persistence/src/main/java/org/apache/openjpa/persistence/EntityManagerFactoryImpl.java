/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.persistence.EntityManagerFactory;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.AutoDetach;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.kernel.DelegatingBrokerFactory;
import org.apache.openjpa.kernel.DelegatingFetchConfiguration;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.lib.conf.Value;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.OpenJPAException;
import serp.util.Strings;

/**
 * Implementation of {@link EntityManagerFactory} that acts as a
 * facade to a {@link BrokerFactory}.
 *
 * @author Marc Prud'hommeaux
 * @nojavadoc
 */
public class EntityManagerFactoryImpl
    implements OpenJPAEntityManagerFactory {

    private static final Localizer _loc = Localizer.forPackage
        (EntityManagerFactoryImpl.class);

    private final DelegatingBrokerFactory _factory;
    private transient Constructor<FetchPlan> _plan = null;
    private transient StoreCache _cache = null;
    private transient QueryResultCache _queryCache = null;

    /**
     * Constructor. Delegate must be provided on construction.
     */
    protected EntityManagerFactoryImpl(BrokerFactory factory) {
        _factory = new DelegatingBrokerFactory(factory,
            PersistenceExceptions.TRANSLATOR);
    }

    /**
     * Delegate.
     */
    public BrokerFactory getBrokerFactory() {
        return _factory.getDelegate();
    }

    public OpenJPAConfiguration getConfiguration() {
        return _factory.getConfiguration();
    }

    public Properties getProperties() {
        return _factory.getProperties();
    }

    public Object putUserObject(Object key, Object val) {
        return _factory.putUserObject(key, val);
    }

    public Object getUserObject(Object key) {
        return _factory.getUserObject(key);
    }

    public StoreCache getStoreCache() {
        _factory.lock();
        try {
            if (_cache == null) {
                OpenJPAConfiguration conf = _factory.getConfiguration();
                _cache = new StoreCache(this,
                    conf.getDataCacheManagerInstance().getSystemDataCache());
            }
            return _cache;
        } finally {
            _factory.unlock();
        }
    }

    public StoreCache getStoreCache(String cacheName) {
        return new StoreCache(this, _factory.getConfiguration().
            getDataCacheManagerInstance().getDataCache(cacheName));
    }

    public QueryResultCache getQueryResultCache() {
        _factory.lock();
        try {
            if (_queryCache == null)
                _queryCache = new QueryResultCache(_factory.getConfiguration().
                    getDataCacheManagerInstance().getSystemQueryCache());
            return _queryCache;
        } finally {
            _factory.unlock();
        }
    }

    public OpenJPAEntityManager createEntityManager() {
        return createEntityManager(null);
    }

    public OpenJPAEntityManager createEntityManager(Map props) {
        if (props == null)
            props = Collections.EMPTY_MAP;
        else if (!props.isEmpty())
            props = new HashMap(props);

        OpenJPAConfiguration conf = getConfiguration();
        String user =
            (String) props.remove("openjpa.ConnectionUserName");
        if (user == null)
            user = conf.getConnectionUserName();
        String pass =
            (String) props.remove("openjpa.ConnectionPassword");
        if (pass == null)
            pass = conf.getConnectionPassword();

        String str =
            (String) props.remove("openjpa.TransactionMode");
        boolean managed;
        if (str == null)
            managed = conf.isTransactionModeManaged();
        else {
            Value val = conf.getValue("openjpa.TransactionMode");
            managed = Boolean.parseBoolean(val.unalias(str));
        }

        Object obj = props.remove("openjpa.ConnectionRetainMode");
        int retainMode;
        if (obj instanceof Number)
            retainMode = ((Number) obj).intValue();
        else if (obj != null) {
            Value val =
                conf.getValue("openjpa.ConnectionRetainMode");
            try {
                retainMode = Integer.parseInt(val.unalias((String) obj));
            } catch (Exception e) {
                throw new ArgumentException(_loc.get("bad-em-prop",
                    "openjpa.ConnectionRetainMode", obj),
                    new Throwable[]{ e },
                    obj, true);
            }
        } else
            retainMode = conf.getConnectionRetainModeConstant();

        Broker broker = _factory.newBroker(user, pass, managed, retainMode,
            false);
        // we should allow the user to specify these settings in conf
        // regardless of PersistenceContextType
        broker.setAutoDetach(AutoDetach.DETACH_CLOSE);
        broker.setDetachedNew(false);
        OpenJPAEntityManager em = OpenJPAPersistence.toEntityManager(broker);

        // allow setting of other bean properties of EM
        List<RuntimeException> errs = null;
        Method setter = null;
        String prop;
        Object val;
        for (Map.Entry entry : (Set<Map.Entry>) props.entrySet()) {
            prop = (String) entry.getKey();
            if (!prop.startsWith("openjpa."))
                continue;
            prop = prop.substring(5);
            try {
                setter = ImplHelper.getSetter(em.getClass(), prop);
            } catch (OpenJPAException ke) {
                if (errs == null)
                    errs = new LinkedList<RuntimeException>();
                errs.add(PersistenceExceptions.toPersistenceException(ke));
                continue;
            }

            val = entry.getValue();
            try {
                if (val instanceof String) {
                    if ("null".equals(val))
                        val = null;
                    else
                        val = Strings.parse((String) val,
                            setter.getParameterTypes()[0]);
                }
                setter.invoke(em, new Object[]{ val });
            } catch (Exception e) {
                ArgumentException err = new ArgumentException(_loc.get
                    ("bad-em-prop", prop, entry.getValue()),
                    new Throwable[]{ e }, null, true);
                if (errs == null)
                    errs = new LinkedList<RuntimeException>();
                errs.add(err);
            }
        }

        if (errs != null) {
            em.close();
            if (errs.size() == 1)
                throw errs.get(0);
            throw new ArgumentException(_loc.get("bad-em-props"),
                (Throwable[]) errs.toArray(new Throwable[errs.size()]),
                null, true);
        }

        return em;
    }

    public void addLifecycleListener(Object listener, Class... classes) {
        _factory.addLifecycleListener(listener, classes);
    }

    public void removeLifecycleListener(Object listener) {
        _factory.removeLifecycleListener(listener);
    }

    public void close() {
        _factory.close();
    }

    public boolean isOpen() {
        return !_factory.isClosed();
    }

    public int hashCode() {
        return _factory.hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof EntityManagerFactoryImpl))
            return false;
        return _factory.equals(((EntityManagerFactoryImpl) other)._factory);
    }

    /**
     * Create a store-specific facade for the given fetch configuration.
	 * If no facade class exists, we use the default {@link FetchPlan}.
     */
    FetchPlan toFetchPlan(Broker broker, FetchConfiguration fetch) {
        if (fetch == null)
            return null;

        FetchConfiguration inner = fetch;
        if (inner instanceof DelegatingFetchConfiguration)
            inner = ((DelegatingFetchConfiguration) inner).
                getInnermostDelegate();

        try {
            if (_plan == null) {
                Class storeType = (broker == null) ? null : broker.
                    getStoreManager().getInnermostDelegate().getClass();
                Class cls = _factory.getConfiguration().
                    getStoreFacadeTypeRegistry().
                    getImplementation(FetchPlan.class, storeType);
                if (cls == null)
                    cls = FetchPlan.class;
                _plan = cls.getConstructor(FetchConfiguration.class);
            }
            return _plan.newInstance(fetch);
        } catch (InvocationTargetException ite) {
            throw PersistenceExceptions.toPersistenceException
                (ite.getTargetException());
        } catch (Exception e) {
            throw PersistenceExceptions.toPersistenceException(e);
        }
	}
}
