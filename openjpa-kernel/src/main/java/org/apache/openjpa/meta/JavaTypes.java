/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.meta;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.lib.meta.CFMetaDataParser;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.MetaDataException;
import serp.util.Numbers;
import serp.util.Strings;

/**
 * <p>Type constants for managed fields.</p>
 *
 * @author Abe White
 */
public class JavaTypes {

    public static final int BOOLEAN = 0;
    public static final int BYTE = 1;
    public static final int CHAR = 2;
    public static final int DOUBLE = 3;
    public static final int FLOAT = 4;
    public static final int INT = 5;
    public static final int LONG = 6;
    public static final int SHORT = 7;
    // keep OBJECT as first non-primitive type code; other code relies on it
    public static final int OBJECT = 8;
    public static final int STRING = 9;
    public static final int NUMBER = 10;
    public static final int ARRAY = 11;
    public static final int COLLECTION = 12;
    public static final int MAP = 13;
    public static final int DATE = 14;
    public static final int PC = 15;
    public static final int BOOLEAN_OBJ = 16;
    public static final int BYTE_OBJ = 17;
    public static final int CHAR_OBJ = 18;
    public static final int DOUBLE_OBJ = 19;
    public static final int FLOAT_OBJ = 20;
    public static final int INT_OBJ = 21;
    public static final int LONG_OBJ = 22;
    public static final int SHORT_OBJ = 23;
    public static final int BIGDECIMAL = 24;
    public static final int BIGINTEGER = 25;
    public static final int LOCALE = 26;
    public static final int PC_UNTYPED = 27;
    public static final int CALENDAR = 28;
    public static final int OID = 29;

    private static final Localizer _loc = Localizer.forPackage(JavaTypes.class);

    private static final Map _typeCodes = new HashMap();

    static {
        _typeCodes.put(String.class, Numbers.valueOf(STRING));
        _typeCodes.put(Boolean.class, Numbers.valueOf(BOOLEAN_OBJ));
        _typeCodes.put(Byte.class, Numbers.valueOf(BYTE_OBJ));
        _typeCodes.put(Character.class, Numbers.valueOf(CHAR_OBJ));
        _typeCodes.put(Double.class, Numbers.valueOf(DOUBLE_OBJ));
        _typeCodes.put(Float.class, Numbers.valueOf(FLOAT_OBJ));
        _typeCodes.put(Integer.class, Numbers.valueOf(INT_OBJ));
        _typeCodes.put(Long.class, Numbers.valueOf(LONG_OBJ));
        _typeCodes.put(Short.class, Numbers.valueOf(SHORT_OBJ));
        _typeCodes.put(Date.class, Numbers.valueOf(DATE));
        _typeCodes.put(java.sql.Date.class, Numbers.valueOf(DATE));
        _typeCodes.put(java.sql.Timestamp.class, Numbers.valueOf(DATE));
        _typeCodes.put(java.sql.Time.class, Numbers.valueOf(DATE));
        _typeCodes.put(BigInteger.class, Numbers.valueOf(BIGINTEGER));
        _typeCodes.put(BigDecimal.class, Numbers.valueOf(BIGDECIMAL));
        _typeCodes.put(Number.class, Numbers.valueOf(NUMBER));
        _typeCodes.put(Locale.class, Numbers.valueOf(LOCALE));
        _typeCodes.put(Object.class, Numbers.valueOf(OBJECT));
        _typeCodes.put(PersistenceCapable.class, Numbers.valueOf(PC_UNTYPED));
        _typeCodes.put(Properties.class, Numbers.valueOf(MAP));
        _typeCodes.put(Calendar.class, Numbers.valueOf(CALENDAR));
    }

    /**
     * Return the field metadata type code for the given class.  First class
     * objects are not recognized in this method.
     */
    public static int getTypeCode(Class type) {
        if (type == null)
            return OBJECT;

        if (type.isPrimitive()) {
            switch (type.getName().charAt(0)) {
                case 'b':
                    return (type == boolean.class) ? BOOLEAN : BYTE;
                case 'c':
                    return CHAR;
                case 'd':
                    return DOUBLE;
                case 'f':
                    return FLOAT;
                case 'i':
                    return INT;
                case 'l':
                    return LONG;
                case 's':
                    return SHORT;
            }
        }

        Integer code = (Integer) _typeCodes.get(type);
        if (code != null)
            return code.intValue();

        // have to do this first to catch custom collection and map types;
        // on resolve we figure out if these custom types are
        // persistence-capable
        if (Collection.class.isAssignableFrom(type))
            return COLLECTION;
        if (Map.class.isAssignableFrom(type))
            return MAP;
        if (type.isArray())
            return ARRAY;
        if (Calendar.class.isAssignableFrom(type))
            return CALENDAR;

        if (type.isInterface()) {
            if (type == Serializable.class)
                return OBJECT;
            return PC_UNTYPED;
        }
        return OBJECT;
    }

    /**
     * Check the given name against the same set of standard packages used
     * when parsing metadata.
     */
    public static Class classForName(String name, ClassMetaData context) {
        return classForName(name, context, null);
    }

    /**
     * Check the given name against the same set of standard packages used
     * when parsing metadata.
     */
    public static Class classForName(String name, ClassMetaData context,
        ClassLoader loader) {
        return classForName(name, context, context.getDescribedType(), null,
            loader);
    }

    /**
     * Check the given name against the same set of standard packages used
     * when parsing metadata.
     */
    public static Class classForName(String name, ValueMetaData context) {
        return classForName(name, context, null);
    }

    /**
     * Check the given name against the same set of standard packages used
     * when parsing metadata.
     */
    public static Class classForName(String name, ValueMetaData context,
        ClassLoader loader) {
        return classForName(name,
            context.getFieldMetaData().getDefiningMetaData(),
            context.getFieldMetaData().getDeclaringType(), context, loader);
    }

    /**
     * Check the given name against the same set of standard packages used
     * when parsing metadata.
     */
    private static Class classForName(String name, ClassMetaData meta,
        Class dec, ValueMetaData vmd, ClassLoader loader) {
        // special case for PersistenceCapable and Object
        if ("PersistenceCapable".equals(name)
            || "javax.jdo.PersistenceCapable".equals(name)) // backwards compat
            return PersistenceCapable.class;
        if ("Object".equals(name))
            return Object.class;

        MetaDataRepository rep = meta.getRepository();
        boolean runtime = (rep.getValidate() & rep.VALIDATE_RUNTIME) != 0;
        if (loader == null)
            loader = rep.getConfiguration().getClassResolverInstance().
                getClassLoader(dec, meta.getEnvClassLoader());

        // try the owner's package
        String pkg = Strings.getPackageName(dec);
        Class cls = CFMetaDataParser.classForName(name, pkg, runtime, loader);
        if (cls == null && vmd != null) {
            // try against this value type's package too
            pkg = Strings.getPackageName(vmd.getDeclaredType());
            cls = CFMetaDataParser.classForName(name, pkg, runtime, loader);
        }
        if (cls == null)
            throw new MetaDataException(_loc.get("bad-class", name,
                (vmd == null) ? (Object) meta : (Object) vmd));
        return cls;
    }

    /**
     * Convert the given object to the given type if possible.  If the type is
     * a numeric primitive, this method only guarantees that the return value
     * is a {@link Number}.  If no known conversion or the value is null,
     * returns the original value.
     */
    public static Object convert(Object val, int typeCode) {
        if (val == null)
            return null;

        switch (typeCode) {
            case BIGDECIMAL:
                if (val instanceof BigDecimal)
                    return val;
                if (val instanceof Number)
                    return new BigDecimal(((Number) val).doubleValue());
                if (val instanceof String)
                    return new BigDecimal(val.toString());
                return val;
            case BIGINTEGER:
                if (val instanceof BigInteger)
                    return val;
                if (val instanceof Number || val instanceof String)
                    return new BigInteger(val.toString());
                return val;
            case BOOLEAN:
            case BOOLEAN_OBJ:
                if (val instanceof String)
                    return Boolean.valueOf(val.toString());
                return val;
            case BYTE_OBJ:
                if (val instanceof Byte)
                    return val;
                if (val instanceof Number)
                    return new Byte(((Number) val).byteValue());
                // no break
            case BYTE:
                if (val instanceof String)
                    return new Byte(val.toString());
                return val;
            case CHAR:
            case CHAR_OBJ:
                if (val instanceof Character)
                    return val;
                if (val instanceof String)
                    return new Character(val.toString().charAt(0));
                if (val instanceof Number)
                    return new Character((char) ((Number) val).intValue());
                return val;
            case DATE:
                if (val instanceof String)
                    return new Date(val.toString());
                return val;
            case DOUBLE_OBJ:
                if (val instanceof Double)
                    return val;
                if (val instanceof Number)
                    return new Double(((Number) val).doubleValue());
                // no break
            case DOUBLE:
                if (val instanceof String)
                    new Double(val.toString());
                return val;
            case FLOAT_OBJ:
                if (val instanceof Float)
                    return val;
                if (val instanceof Number)
                    return new Float(((Number) val).floatValue());
                // no break
            case FLOAT:
                if (val instanceof String)
                    new Float(val.toString());
                return val;
            case INT_OBJ:
                if (val instanceof Integer)
                    return val;
                if (val instanceof Number)
                    return Numbers.valueOf(((Number) val).intValue());
                // no break
            case INT:
                if (val instanceof String)
                    new Integer(val.toString());
                return val;
            case LONG_OBJ:
                if (val instanceof Long)
                    return val;
                if (val instanceof Number)
                    return Numbers.valueOf(((Number) val).longValue());
                // no break
            case LONG:
                if (val instanceof String)
                    new Long(val.toString());
                return val;
            case NUMBER:
                if (val instanceof Number)
                    return val;
                if (val instanceof String)
                    return new BigDecimal(val.toString());
                return val;
            case SHORT_OBJ:
                if (val instanceof Short)
                    return val;
                if (val instanceof Number)
                    return new Short(((Number) val).shortValue());
                // no break
            case SHORT:
                if (val instanceof String)
                    new Short(val.toString());
                return val;
            case STRING:
                return val.toString();
            default:
                return val;
        }
    }

    /**
     * Return true if the (possibly unresolved) field or its elements might be
     * persistence capable objects.
     */
    public static boolean maybePC(FieldMetaData field) {
        switch (field.getDeclaredTypeCode()) {
            case JavaTypes.ARRAY:
            case JavaTypes.COLLECTION:
                return maybePC(field.getElement());
            case JavaTypes.MAP:
                return maybePC(field.getKey()) || maybePC(field.getElement());
            default:
                return maybePC((ValueMetaData) field);
        }
    }

    /**
     * Return true if the (possibly unresolved) value might be a first class
     * object.
     */
    public static boolean maybePC(ValueMetaData val) {
        return maybePC(val.getDeclaredTypeCode(), val.getDeclaredType());
    }

    /**
     * Return true if the given unresolved typecode/type pair may represent a
     * persistent object.
     */
    static boolean maybePC(int typeCode, Class type) {
        if (type == null)
            return false;
        switch (typeCode) {
            case JavaTypes.OBJECT:
            case JavaTypes.PC:
            case JavaTypes.PC_UNTYPED:
                return true;
            case JavaTypes.COLLECTION:
            case JavaTypes.MAP:
                return !type.getName().startsWith("java.util.");
            default:
                return false;
        }
    }

    /**
     * Helper method to return the given array value as a collection.
     */
    public static List toList(Object val, Class elem, boolean mutable) {
        if (val == null)
            return null;

        List l;
        if (!elem.isPrimitive()) {
            // if an object array, use built-in list function
            l = Arrays.asList((Object[]) val);
            if (mutable)
                l = new ArrayList(l);
        } else {
            // convert to list of wrapper objects
            int length = Array.getLength(val);
            l = new ArrayList(length);
            for (int i = 0; i < length; i++)
                l.add(Array.get(val, i));
        }
        return l;
    }

    /**
     *	Helper method to return the given collection as an array.
     */
    public static Object toArray(Collection coll, Class elem) {
        if (coll == null)
            return null;

        Object array = Array.newInstance(elem, coll.size());
        int idx = 0;
        for (Iterator itr = coll.iterator(); itr.hasNext(); idx++)
            Array.set(array, idx, itr.next ());
		return array;
	}
}
