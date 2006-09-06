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

import java.util.Iterator;
import java.util.List;

import org.apache.openjpa.kernel.DelegatingExtent;

/**
 * An extent is a logical view of all instances of a class.
 *
 * @author Abe White
 * @author Pinaki Poddar
 * @since 0.4.1
 * @nojavadoc
 */
public class ExtentImpl<T>
    implements Extent<T> {

    private final EntityManagerImpl _em;
    private final DelegatingExtent _extent;
    private FetchPlan _fetch = null;


    /**
     * Constructor; supply delegate.
     */
    public ExtentImpl(EntityManagerImpl em,
        org.apache.openjpa.kernel.Extent extent) {
        _em = em;
        _extent = new DelegatingExtent(extent,
            PersistenceExceptions.getRollbackTranslator(em));
    }

    public org.apache.openjpa.kernel.Extent getDelegate() {
        return _extent.getDelegate();
    }

    public Class<T> getElementClass() {
        return _extent.getElementType();
    }

    public boolean hasSubclasses() {
        return _extent.hasSubclasses();
    }

    public OpenJPAEntityManager getEntityManager() {
        return _em;
    }

    public FetchPlan getFetchPlan() {
        _extent.lock();
        try {
            if (_fetch == null)
                _fetch = ((EntityManagerFactoryImpl) _em.
                    getEntityManagerFactory()).toFetchPlan(_extent.getBroker(),
                    _extent.getFetchConfiguration());
            return _fetch;
        } finally {
            _extent.unlock();
        }
    }

    public boolean getIgnoreChanges() {
        return _extent.getIgnoreChanges();
    }

    public void setIgnoreChanges(boolean ignoreChanges) {
        _extent.setIgnoreChanges(ignoreChanges);
    }

    public List<T> list() {
        return _extent.list();
    }

    public Iterator<T> iterator() {
        return _extent.iterator();
    }

    public void closeAll() {
        _extent.closeAll();
    }

    public int hashCode() {
        return _extent.hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof ExtentImpl))
            return false;
        return _extent.equals(((ExtentImpl) other)._extent);
	}
}
