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
package org.apache.openjpa.jdbc.meta.strats;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.MetaDataException;

/**
 * ValueHandler for element collections that use a JPA
 * {@code AttributeConverter}. Unlike {@link ConverterValueHandler}, this
 * handler actively converts values because element collection elements
 * are not routed through the externalization path.
 *
 * @since 4.2.0
 */
public class ConverterElementHandler extends AbstractValueHandler {

    private static final long serialVersionUID = 1L;

    private final Class<?> _converterClass;
    private final Class<?> _dbType;

    // Lazily populated converter caches. The handler is held by the field
    // strategy and shared across brokers and threads, so these are volatile
    // to guarantee safe publication. The converter instance is created at
    // most once while holding this instance's monitor.
    private transient volatile Object _converterInstance;
    private transient volatile Method _toDbMethod;
    private transient volatile Method _toEntityMethod;

    public ConverterElementHandler(Class<?> converterClass, Class<?> dbType) {
        _converterClass = converterClass;
        _dbType = dbType;
    }

    @Override
    public Column[] map(ValueMapping vm, String name, ColumnIO io,
            boolean adapt) {
        DBDictionary dict = vm.getMappingRepository().getDBDictionary();
        DBIdentifier colName = DBIdentifier.newColumn(name,
            dict != null ? dict.delimitAll() : false);

        Column col = new Column();
        col.setIdentifier(colName);
        col.setJavaType(JavaTypes.getTypeCode(_dbType));
        return new Column[]{ col };
    }

    @Override
    public Object toDataStoreValue(ValueMapping vm, Object val,
            JDBCStore store) {
        if (val == null) {
            return null;
        }
        try {
            Object converter = getConverterInstance();
            return getToDbMethod().invoke(converter, val);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getTargetException();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new MetaDataException(cause.toString()).setCause(cause);
        } catch (Exception e) {
            throw new MetaDataException(e.toString()).setCause(e);
        }
    }

    @Override
    public Object toObjectValue(ValueMapping vm, Object val) {
        if (val == null) {
            return null;
        }
        try {
            Object converter = getConverterInstance();
            return getToEntityMethod().invoke(converter, val);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getTargetException();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new MetaDataException(cause.toString()).setCause(cause);
        } catch (Exception e) {
            throw new MetaDataException(e.toString()).setCause(e);
        }
    }

    /**
     * Get (or create) the cached converter instance. A single instance is
     * shared by all brokers and threads using this handler, so
     * AttributeConverter implementations must be stateless / thread-safe.
     */
    private Object getConverterInstance() throws Exception {
        Object instance = _converterInstance;
        if (instance != null) {
            return instance;
        }
        synchronized (this) {
            instance = _converterInstance;
            if (instance == null) {
                Constructor<?> ctor = _converterClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                instance = ctor.newInstance();
                _converterInstance = instance;
            }
            return instance;
        }
    }

    private Method getToDbMethod() {
        // racy single-check: concurrent lookups resolve the same method
        Method m = _toDbMethod;
        if (m == null) {
            m = findMethod("convertToDatabaseColumn");
            _toDbMethod = m;
        }
        return m;
    }

    private Method getToEntityMethod() {
        Method m = _toEntityMethod;
        if (m == null) {
            m = findMethod("convertToEntityAttribute");
            _toEntityMethod = m;
        }
        return m;
    }

    private Method findMethod(String methodName) {
        Method bridge = null;
        for (Method m : _converterClass.getMethods()) {
            if (!m.getName().equals(methodName))
                continue;
            if (m.getParameterCount() != 1)
                continue;
            if (m.isBridge()) {
                bridge = m;
                continue;
            }
            m.setAccessible(true);
            return m;
        }
        if (bridge != null) {
            bridge.setAccessible(true);
            return bridge;
        }
        throw new MetaDataException("No method " + methodName
            + " found on converter " + _converterClass.getName());
    }
}
