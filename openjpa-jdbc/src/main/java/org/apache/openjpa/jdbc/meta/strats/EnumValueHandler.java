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
import org.apache.openjpa.util.StoreException;

/**
 * Value handler for JDK1.5 enum field types.
 *
 */
public class EnumValueHandler extends AbstractValueHandler {
    private static final long serialVersionUID = 1L;
    private Enum<?>[] _vals = null;
    private boolean _ordinal = false;
    private boolean _useEnumeratedValue = false;
    private transient volatile Field _enumeratedValueField = null;
    private transient volatile Map<Object, Enum<?>> _dbToEnum = null;
    private transient int _enumeratedValueColumnType = JavaTypes.STRING;
    private static final Localizer _loc = Localizer.forPackage(EnumValueHandler.class);
    private static final Class<? extends Annotation> ENUMERATED_VALUE_CLASS = loadEnumeratedValueClass();

    /**
     * The @EnumeratedValue annotation is loaded reflectively on purpose:
     * openjpa-jdbc has no compile time dependency on jakarta.persistence-api,
     * and the annotation only exists as of JPA 3.2.  The result is null when
     * running against an older API jar, in which case the feature is disabled.
     */
    private static Class<? extends Annotation> loadEnumeratedValueClass() {
        try {
            return Class.forName("jakarta.persistence.EnumeratedValue")
                .asSubclass(Annotation.class);
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
            col.setJavaType(_enumeratedValueColumnType);
            if (_enumeratedValueColumnType == JavaTypes.STRING) {
                int len = 20;
                for (Object dbVal : _dbToEnum.keySet()) {
                    len = Math.max(dbVal.toString().length(), len);
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

    /**
     * Locate the @EnumeratedValue annotated field of the given enum type and
     * build the reverse lookup map.  Both fields are transient, so this may
     * also run after deserialization of the mapping metadata, i.e. without
     * {@link #map} having been called in this JVM.  The fully initialized
     * lookup map is published last, and both fields are volatile, so that a
     * concurrent reader either sees no initialization at all or a complete one.
     */
    private void initEnumeratedValueField(Class<?> enumType) {
        if (_dbToEnum != null) {
            return;
        }
        if (ENUMERATED_VALUE_CLASS == null) {
            throw new MetaDataException(_loc.get("enumerated-value-unsupported",
                enumType.getName()));
        }
        Field enumeratedValueField = null;
        for (Field f : enumType.getDeclaredFields()) {
            if (f.isAnnotationPresent(ENUMERATED_VALUE_CLASS)) {
                f.setAccessible(true);
                enumeratedValueField = f;
                break;
            }
        }
        if (enumeratedValueField == null) {
            throw new MetaDataException(_loc.get("no-enumerated-value-field",
                enumType.getName()));
        }
        _enumeratedValueField = enumeratedValueField;
        // the column domain is decided once from the declared field type; per
        // the JPA 3.2 specification the field may be a String, a char/Character
        // or an integral type.  The small integral types share an int column,
        // long uses a long column, everything else is stored as a string.  Any
        // other type is a mapping error and is rejected here, i.e. while the
        // mapping is resolved, rather than when a row is read.
        Class<?> fieldType = enumeratedValueField.getType();
        if (fieldType == int.class || fieldType == Integer.class
            || fieldType == short.class || fieldType == Short.class
            || fieldType == byte.class || fieldType == Byte.class) {
            _enumeratedValueColumnType = JavaTypes.INT;
        } else if (fieldType == long.class || fieldType == Long.class) {
            _enumeratedValueColumnType = JavaTypes.LONG;
        } else if (fieldType == String.class || fieldType == char.class
            || fieldType == Character.class) {
            _enumeratedValueColumnType = JavaTypes.STRING;
        } else {
            throw new MetaDataException(_loc.get("enumerated-value-bad-type",
                enumType.getName(), enumeratedValueField.getName(),
                Exceptions.toClassName(fieldType)));
        }

        // build reverse lookup map, keyed by the canonical column value
        Map<Object, Enum<?>> dbToEnum = new HashMap<>();
        for (Enum<?> val : _vals) {
            Object dbVal = getEnumeratedFieldValue(val);
            if (dbVal == null) {
                throw new MetaDataException(_loc.get("null-enumerated-value",
                    enumType.getName(), val.name(), enumeratedValueField.getName()));
            }
            Object key = normalizeEnumeratedValue(dbVal);
            Enum<?> prev = dbToEnum.put(key, val);
            if (prev != null) {
                throw new MetaDataException(_loc.get("dup-enumerated-value",
                    new Object[]{ enumType.getName(), String.valueOf(key), prev.name(),
                        val.name() }));
            }
        }
        _dbToEnum = dbToEnum;
    }

    /**
     * Canonicalize the given value to the domain of the mapped column, so that
     * the values put into the lookup map, the values written to the database
     * and the values read back from the database are always comparable.
     */
    private Object normalizeEnumeratedValue(Object val) {
        if (val == null) {
            return null;
        }
        if (_enumeratedValueColumnType == JavaTypes.INT) {
            if (val instanceof Integer) {
                return val;
            }
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            if (val instanceof Character) {
                return (int) (Character) val;
            }
            return Integer.valueOf(val.toString().trim());
        }
        if (_enumeratedValueColumnType == JavaTypes.LONG) {
            if (val instanceof Long) {
                return val;
            }
            if (val instanceof Number) {
                return ((Number) val).longValue();
            }
            if (val instanceof Character) {
                return (long) (Character) val;
            }
            return Long.valueOf(val.toString().trim());
        }
        if (val instanceof String) {
            return val;
        }
        return val.toString();
    }

    /**
     * Return the value handler strategy to use for the given enum type.  An
     * enum type with an @EnumeratedValue field is always mapped by that value,
     * as the JPA 3.2 specification gives it precedence over the ordinal/name
     * choice; every place that configures this handler from metadata has to go
     * through here, so that one enum type is encoded the same way no matter
     * which kind of metadata declared it.
     */
    public static String strategy(Class<?> enumType, boolean ordinal) {
        if (hasEnumeratedValue(enumType)) {
            return EnumValueHandler.class.getName() + "(UseEnumeratedValue=true)";
        }
        return EnumValueHandler.class.getName() + "(StoreOrdinal=" + ordinal + ")";
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
            return normalizeEnumeratedValue(getEnumeratedFieldValue((Enum<?>) val));
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
            Enum<?> result = null;
            try {
                result = _dbToEnum.get(normalizeEnumeratedValue(val));
            } catch (NumberFormatException nfe) {
                // not a valid code for this enum type; reported below
            }
            if (result == null) {
                // a column value that maps to no constant is bad data, not a
                // bad mapping, so it has to surface as a store exception
                throw new StoreException(_loc.get("unknown-enumerated-value",
                    new Object[]{ String.valueOf(val), Exceptions.toClassName(vm.getType()),
                        String.valueOf(_dbToEnum.keySet()) })).setFailedObject(val);
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
