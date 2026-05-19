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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.Exceptions;
import org.apache.openjpa.util.MetaDataException;

/**
 * Value handler for JDK1.5 enum field types.
 *
 */
public class EnumValueHandler extends AbstractValueHandler {
    private static final long serialVersionUID = 1L;
    private Enum<?>[] _vals = null;
    private boolean _ordinal = false;
    private boolean _useEnumeratedValue = false;
    private transient Field _enumeratedValueField = null;
    private transient Map<Object, Enum<?>> _dbToEnum = null;
    private static final Localizer _loc = Localizer.forPackage(EnumValueHandler.class);
    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation> ENUMERATED_VALUE_CLASS = loadEnumeratedValueClass();

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadEnumeratedValueClass() {
        try {
            return (Class<? extends Annotation>)
                Class.forName("jakarta.persistence.EnumeratedValue");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Whether to store the enum value as its ordinal.
     */
    public boolean getStoreOrdinal() {
        return _ordinal;
    }

    /**
     * Whether to store the enum value as its ordinal.
     */
    public void setStoreOrdinal(boolean ordinal) {
        _ordinal = ordinal;
    }

    /**
     * Whether to use the @EnumeratedValue annotated field for DB mapping.
     */
    public boolean getUseEnumeratedValue() {
        return _useEnumeratedValue;
    }

    /**
     * Whether to use the @EnumeratedValue annotated field for DB mapping.
     */
    public void setUseEnumeratedValue(boolean useEnumeratedValue) {
        _useEnumeratedValue = useEnumeratedValue;
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public Column[] map(ValueMapping vm, String name, ColumnIO io,
        boolean adapt) {
        DBDictionary dict = vm.getMappingRepository().getDBDictionary();
        DBIdentifier colName = DBIdentifier.newColumn(name, dict != null ? dict.delimitAll() : false);
        return map(vm, colName, io, adapt);
    }

    public Column[] map(ValueMapping vm, DBIdentifier name, ColumnIO io,
        boolean adapt) {
        // all enum classes have a static method called 'values()'
        // that returns an array of all the enum values
        try {
            Method m = vm.getType().getMethod("values", (Class[]) null);
            _vals = (Enum[]) m.invoke(null, (Object[]) null);
        } catch (Exception e) {

            throw new MetaDataException(_loc.get("not-enum-field",
                    vm.getFieldMapping().getFullName(true), Exceptions.toClassName(vm.getType()))).setCause(e);
        }

        Column col = new Column();
        col.setIdentifier(name);

        if (_useEnumeratedValue) {
            initEnumeratedValueField(vm.getType());
            Class<?> fieldType = _enumeratedValueField.getType();
            if (fieldType == int.class || fieldType == Integer.class
                || fieldType == short.class || fieldType == Short.class) {
                col.setJavaType(JavaTypes.INT);
            } else {
                col.setJavaType(JavaTypes.STRING);
                int len = 20;
                for (Enum<?> val : _vals) {
                    Object dbVal = getEnumeratedFieldValue(val);
                    if (dbVal != null) {
                        len = Math.max(dbVal.toString().length(), len);
                    }
                }
                col.setSize(len);
            }
        } else if (_ordinal) {
            col.setJavaType(JavaTypes.SHORT);
        } else {
            // look for the longest enum value name; use 20 as min length to
            // leave room for future long names
            int len = 20;
            for (Enum<?> val : _vals) {
                len = Math.max(val.name().length(), len);
            }

            col.setJavaType(JavaTypes.STRING);
            col.setSize(len);
        }
        return new Column[]{ col };
    }

    private void initEnumeratedValueField(Class<?> enumType) {
        if (_enumeratedValueField != null) {
            return;
        }
        for (Field f : enumType.getDeclaredFields()) {
            if (f.isAnnotationPresent(ENUMERATED_VALUE_CLASS)) {
                f.setAccessible(true);
                _enumeratedValueField = f;
                break;
            }
        }
        if (_enumeratedValueField == null) {
            throw new MetaDataException(_loc.get("no-enumerated-value-field",
                enumType.getName()));
        }
        // build reverse lookup map
        _dbToEnum = new HashMap<>();
        for (Enum<?> val : _vals) {
            Object dbVal = getEnumeratedFieldValue(val);
            _dbToEnum.put(dbVal, val);
        }
    }

    /**
     * Check if the given enum type has a field annotated with @EnumeratedValue.
     */
    public static boolean hasEnumeratedValue(Class<?> enumType) {
        if (enumType == null || !enumType.isEnum() || ENUMERATED_VALUE_CLASS == null) {
            return false;
        }
        for (Field f : enumType.getDeclaredFields()) {
            if (f.isAnnotationPresent(ENUMERATED_VALUE_CLASS)) {
                return true;
            }
        }
        return false;
    }

    private Object getEnumeratedFieldValue(Enum<?> val) {
        try {
            return _enumeratedValueField.get(val);
        } catch (IllegalAccessException e) {
            throw new MetaDataException(_loc.get("enum-value-access-error",
                val.getClass().getName(), _enumeratedValueField.getName()))
                .setCause(e);
        }
    }

    @Override
    public boolean isVersionable(ValueMapping vm) {
        return true;
    }

    @Override
    public Object toDataStoreValue(ValueMapping vm, Object val, JDBCStore store) {
        if (val == null)
            return null;
        if (_useEnumeratedValue) {
            initEnumeratedValueField(vm.getType());
            return getEnumeratedFieldValue((Enum<?>) val);
        }
        if (_ordinal)
            return ((Enum) val).ordinal();
        return ((Enum) val).name();
    }

    @Override
    public Object toObjectValue(ValueMapping vm, Object val) {
        if (val == null)
            return null;
        if (_useEnumeratedValue) {
            initEnumeratedValueField(vm.getType());
            Enum<?> result = _dbToEnum.get(val);
            if (result == null && val instanceof Number) {
                result = _dbToEnum.get(((Number) val).intValue());
            }
            if (result == null) {
                result = _dbToEnum.get(val.toString());
            }
            return result;
        }
        if (_ordinal) {
            if (val instanceof Number) {
                return _vals[((Number) val).intValue()];
            }
            return _vals[Integer.parseInt(val.toString().trim())];
        }
        if (val instanceof String) {
            return Enum.valueOf(vm.getType(), (String) val);
        }
        return _vals[((Number) val).intValue()];
    }
}
