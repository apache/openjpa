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
package org.apache.openjpa.kernel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.openjpa.enhance.Reflection;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.UserException;
import serp.util.Numbers;
import serp.util.Strings;

/**
 * Helper methods for dealing with query filters.
 *
 * @author Abe White
 * @nojavadoc
 */
public class Filters {

    private static final BigDecimal ZERO_BIGDECIMAL = new BigDecimal(0D);
    private static final BigInteger ZERO_BIGINTEGER = new BigInteger("0");

    private static final int OP_ADD = 0;
    private static final int OP_SUBTRACT = 1;
    private static final int OP_MULTIPLY = 2;
    private static final int OP_DIVIDE = 3;
    private static final int OP_MOD = 4;

    private static final Localizer _loc = Localizer.forPackage(Filters.class);

    /**
     * Return the correct wrapper type for the given class.
     */
    public static Class wrap(Class c) {
        if (!c.isPrimitive())
            return c;
        if (c == int.class)
            return Integer.class;
        if (c == float.class)
            return Float.class;
        if (c == double.class)
            return Double.class;
        if (c == long.class)
            return Long.class;
        if (c == boolean.class)
            return Boolean.class;
        if (c == short.class)
            return Short.class;
        if (c == byte.class)
            return Byte.class;
        if (c == char.class)
            return Character.class;
        return c;
    }

    /**
     * Return the correct primitive type for the given class, if it is a
     * wrapper.
     */
    public static Class unwrap(Class c) {
        if (c.isPrimitive() || c == String.class)
            return c;
        if (c == Integer.class)
            return int.class;
        if (c == Float.class)
            return float.class;
        if (c == Double.class)
            return double.class;
        if (c == Long.class)
            return long.class;
        if (c == Boolean.class)
            return boolean.class;
        if (c == Short.class)
            return short.class;
        if (c == Byte.class)
            return byte.class;
        if (c == Character.class)
            return char.class;
        return c;
    }

    /**
     * Given two types, return type they should both be converted
     * to before performing any operations between them.
     */
    public static Class promote(Class c1, Class c2) {
        if (c1 == c2)
            return unwrap(c1);
        Class w1 = wrap(c1);
        Class w2 = wrap(c2);
        if (w1 == w2)
            return unwrap(c1);

        // not numbers?
        boolean w1Number = Number.class.isAssignableFrom(w1);
        boolean w2Number = Number.class.isAssignableFrom(w2);
        if (!w1Number || !w2Number) {
            // the only non-numeric promotion we do is string to char,
            // or from char/string to number
            if (!w1Number) {
                if (w2Number && (w1 == Character.class || w1 == String.class))
                    return (w2 == Byte.class || w2 == Short.class)
                        ? Integer.class : unwrap(c2);
                if (!w2Number && w1 == Character.class && w2 == String.class)
                    return String.class;
                if (w2Number)
                    return unwrap(c2);
            }
            if (!w2Number) {
                if (w1Number && (w2 == Character.class || w2 == String.class))
                    return (w1 == Byte.class || w1 == Short.class)
                        ? Integer.class : unwrap(c1);
                if (!w1Number && w2 == Character.class && w1 == String.class)
                    return String.class;
                if (w1Number)
                    return unwrap(c1);
            }

            // if neither are numbers, use least-derived of the two.  if neither
            // is assignable from the other but one is a standard type, assume
            // the other can be converted to that standard type
            if (!w1Number && !w2Number) {
                if (w1 == Object.class)
                    return unwrap(c2);
                if (w2 == Object.class)
                    return unwrap(c1);
                if (w1.isAssignableFrom(w2))
                    return unwrap(c1);
                if (w2.isAssignableFrom(w1))
                    return unwrap(c2);
                if (isNonstandardType(w1))
                    return (isNonstandardType(w2)) ? Object.class : unwrap(c2);
                if (isNonstandardType(w2))
                    return (isNonstandardType(w1)) ? Object.class : unwrap(c1);
            }
            return Object.class;
        }

        if (w1 == BigDecimal.class || w2 == BigDecimal.class)
            return BigDecimal.class;
        if (w1 == BigInteger.class) {
            if (w2 == Float.class || w2 == Double.class)
                return BigDecimal.class;
            return BigInteger.class;
        }
        if (w2 == BigInteger.class) {
            if (w1 == Float.class || w1 == Double.class)
                return BigDecimal.class;
            return BigInteger.class;
        }
        if (w1 == Double.class || w2 == Double.class)
            return double.class;
        if (w1 == Float.class || w2 == Float.class)
            return float.class;
        if (w1 == Long.class || w2 == Long.class)
            return long.class;
        return int.class;
    }

    /**
     * Return whether the given type is not a standard persistent type.
     */
    private static boolean isNonstandardType(Class c) {
        switch (JavaTypes.getTypeCode(c))
        {
        case JavaTypes.ARRAY:
        case JavaTypes.COLLECTION:
        case JavaTypes.MAP:
        case JavaTypes.PC:
        case JavaTypes.PC_UNTYPED:
        case JavaTypes.OID:
        case JavaTypes.OBJECT:
            return true;
        default:
            return false;
        }
    }

    /**
     * Return whether an instance of the first class can be converted to
     * an instance of the second.
     */
    public static boolean canConvert(Class c1, Class c2, boolean strict) {
        c1 = wrap(c1);
        c2 = wrap(c2);
        if (c2.isAssignableFrom(c1))
            return true;

        boolean c1Number = Number.class.isAssignableFrom(c1);
        boolean c2Number = Number.class.isAssignableFrom(c2);
        if (c1Number && c2Number)
            return true;
        if ((c1Number && (c2 == Character.class
            || (!strict && c2 == String.class)))
            || (c2Number && (c1 == Character.class
            || (!strict && c1 == String.class))))
            return true;
        if (c1 == String.class && c2 == Character.class)
            return true;
        if (c2 == String.class)
            return !strict;
        return false;
    }

    /**
     * Convert the given value to the given type.
     */
    public static Object convert(Object o, Class type) {
        if (o == null)
            return null;
        if (o.getClass() == type)
            return o;

        type = wrap(type);
        if (type.isAssignableFrom(o.getClass()))
            return o;

        // the only non-numeric conversions we do are to string, or from
        // string/char to number, or calendar/date
        boolean num = o instanceof Number;
        if (!num) {
            if (type == String.class)
                return o.toString();
            else if (type == Character.class) {
                String str = o.toString();
                if (str != null && str.length() == 1)
                    return new Character(str.charAt(0));
            } else if (Calendar.class.isAssignableFrom(type) &&
                o instanceof Date) {
                Calendar cal = Calendar.getInstance();
                cal.setTime((Date) o);
                return cal;
            } else if (Date.class.isAssignableFrom(type) &&
                o instanceof Calendar) {
                return ((Calendar) o).getTime();
            } else if (Number.class.isAssignableFrom(type)) {
                Integer i = null;
                if (o instanceof Character)
                    i = Numbers.valueOf(((Character) o).charValue());
                else if (o instanceof String && ((String) o).length() == 1)
                    i = Numbers.valueOf(((String) o).charAt(0));

                if (i != null) {
                    if (type == Integer.class)
                        return i;
                    num = true;
                }
            }
        }
        if (!num)
            throw new ClassCastException(_loc.get("cant-convert", o,
                o.getClass(), type).getMessage());

        if (type == Integer.class) {
            return Numbers.valueOf(((Number) o).intValue());
        } else if (type == Float.class) {
            return new Float(((Number) o).floatValue());
        } else if (type == Double.class) {
            return new Double(((Number) o).doubleValue());
        } else if (type == Long.class) {
            return Numbers.valueOf(((Number) o).longValue());
        } else if (type == BigDecimal.class) {
            // the BigDecimal constructor doesn't handle the
            // "NaN" string version of Double.NaN and Float.NaN, nor
            // does it handle infinity; we need to instead use the Double
            // and Float versions, despite wanting to cast it to BigDecimal
            double dval = ((Number) o).doubleValue();
            if (Double.isNaN(dval) || Double.isInfinite(dval))
                return new Double(dval);

            float fval = ((Number) o).floatValue();
            if (Float.isNaN(fval) || Float.isInfinite(fval))
                return new Float(fval);

            return new BigDecimal(o.toString());
        } else if (type == BigInteger.class) {
            return new BigInteger(o.toString());
        } else if (type == Short.class) {
            return new Short(((Number) o).shortValue());
        } else if (type == Byte.class) {
            return new Byte(((Number) o).byteValue());
        } else {
            return Numbers.valueOf(((Number) o).intValue());
        }
    }

    /**
     * Add the given values.
     */
    public static Object add(Object o1, Class c1, Object o2, Class c2) {
        return op(o1, c1, o2, c2, OP_ADD);
    }

    /**
     * Subtract the given values.
     */
    public static Object subtract(Object o1, Class c1, Object o2, Class c2) {
        return op(o1, c1, o2, c2, OP_SUBTRACT);
    }

    /**
     * Multiply the given values.
     */
    public static Object multiply(Object o1, Class c1, Object o2, Class c2) {
        return op(o1, c1, o2, c2, OP_MULTIPLY);
    }

    /**
     * Divide the given values.
     */
    public static Object divide(Object o1, Class c1, Object o2, Class c2) {
        return op(o1, c1, o2, c2, OP_DIVIDE);
    }

    /**
     * Mod the given values.
     */
    public static Object mod(Object o1, Class c1, Object o2, Class c2) {
        return op(o1, c1, o2, c2, OP_MOD);
    }

    /**
     * Perform the given operation on two numbers.
     */
    private static Object op(Object o1, Class c1, Object o2, Class c2, int op) {
        Class promote = promote(c1, c2);
        if (promote == int.class) {
            int n1 = (o1 == null) ? 0 : ((Number) o1).intValue();
            int n2 = (o2 == null) ? 0 : ((Number) o2).intValue();
            return op(n1, n2, op);
        }
        if (promote == float.class) {
            float n1 = (o1 == null) ? 0F : ((Number) o1).floatValue();
            float n2 = (o2 == null) ? 0F : ((Number) o2).floatValue();
            return op(n1, n2, op);
        }
        if (promote == double.class) {
            double n1 = (o1 == null) ? 0D : ((Number) o1).doubleValue();
            double n2 = (o2 == null) ? 0D : ((Number) o2).doubleValue();
            return op(n1, n2, op);
        }
        if (promote == long.class) {
            long n1 = (o1 == null) ? 0L : ((Number) o1).longValue();
            long n2 = (o2 == null) ? 0L : ((Number) o2).longValue();
            return op(n1, n2, op);
        }
        if (promote == BigDecimal.class) {
            BigDecimal n1 = (o1 == null) ? ZERO_BIGDECIMAL
                : (BigDecimal) convert(o1, promote);
            BigDecimal n2 = (o2 == null) ? ZERO_BIGDECIMAL
                : (BigDecimal) convert(o2, promote);
            return op(n1, n2, op);
        }
        if (promote == BigInteger.class) {
            BigInteger n1 = (o1 == null) ? ZERO_BIGINTEGER
                : (BigInteger) convert(o1, promote);
            BigInteger n2 = (o2 == null) ? ZERO_BIGINTEGER
                : (BigInteger) convert(o2, promote);
            return op(n1, n2, op);
        }
        // default to int
        int n1 = (o1 == null) ? 0 : ((Number) o1).intValue();
        int n2 = (o2 == null) ? 0 : ((Number) o2).intValue();
        return op(n1, n2, op);
    }

    /**
     * Return the result of a mathematical operation.
     */
    private static Object op(int n1, int n2, int op) {
        int tot;
        switch (op) {
            case OP_ADD:
                tot = n1 + n2;
                break;
            case OP_SUBTRACT:
                tot = n1 - n2;
                break;
            case OP_MULTIPLY:
                tot = n1 * n2;
                break;
            case OP_DIVIDE:
                tot = n1 / n2;
                break;
            case OP_MOD:
                tot = n1 % n2;
                break;
            default:
                throw new InternalException();
        }
        return Numbers.valueOf(tot);
    }

    /**
     * Return the result of a mathematical operation.
     */
    private static Object op(float n1, float n2, int op) {
        float tot;
        switch (op) {
            case OP_ADD:
                tot = n1 + n2;
                break;
            case OP_SUBTRACT:
                tot = n1 - n2;
                break;
            case OP_MULTIPLY:
                tot = n1 * n2;
                break;
            case OP_DIVIDE:
                tot = n1 / n2;
                break;
            case OP_MOD:
                tot = n1 % n2;
                break;
            default:
                throw new InternalException();
        }
        return new Float(tot);
    }

    /**
     * Return the result of a mathematical operation.
     */
    private static Object op(double n1, double n2, int op) {
        double tot;
        switch (op) {
            case OP_ADD:
                tot = n1 + n2;
                break;
            case OP_SUBTRACT:
                tot = n1 - n2;
                break;
            case OP_MULTIPLY:
                tot = n1 * n2;
                break;
            case OP_DIVIDE:
                tot = n1 / n2;
                break;
            case OP_MOD:
                tot = n1 % n2;
                break;
            default:
                throw new InternalException();
        }
        return new Double(tot);
    }

    /**
     * Return the result of a mathematical operation.
     */
    private static Object op(long n1, long n2, int op) {
        long tot;
        switch (op) {
            case OP_ADD:
                tot = n1 + n2;
                break;
            case OP_SUBTRACT:
                tot = n1 - n2;
                break;
            case OP_MULTIPLY:
                tot = n1 * n2;
                break;
            case OP_DIVIDE:
                tot = n1 / n2;
                break;
            case OP_MOD:
                tot = n1 % n2;
                break;
            default:
                throw new InternalException();
        }
        return Numbers.valueOf(tot);
    }

    /**
     * Return the result of a mathematical operation.
     */
    private static Object op(BigDecimal n1, BigDecimal n2, int op) {
        switch (op) {
            case OP_ADD:
                return n1.add(n2);
            case OP_SUBTRACT:
                return n1.subtract(n2);
            case OP_MULTIPLY:
                return n1.multiply(n2);
            case OP_DIVIDE:
                int scale = Math.max(n1.scale(), n2.scale());
                return n1.divide(n2, scale, BigDecimal.ROUND_HALF_UP);
            case OP_MOD:
                throw new UserException(_loc.get("mod-bigdecimal"));
            default:
                throw new InternalException();
        }
    }

    /**
     * Return the result of a mathematical operation.
     */
    private static Object op(BigInteger n1, BigInteger n2, int op) {
        switch (op) {
            case OP_ADD:
                return n1.add(n2);
            case OP_SUBTRACT:
                return n1.subtract(n2);
            case OP_MULTIPLY:
                return n1.multiply(n2);
            case OP_DIVIDE:
                return n1.divide(n2);
            default:
                throw new InternalException();
        }
    }

    /**
     * Parses the given declarations into a list of type, name, type, name...
     * Returns null if no declarations. Assumes declaration is not an empty
     * string and is already trimmed (valid assumptions given the checks made
     * in our setters).
     *
     * @param decType the type of declaration being parsed, for use in
     * error messages
     */
    public static List parseDeclaration(String dec, char split,
        String decType) {
        if (dec == null)
            return null;

        // watch for common mixups between commas and semis
        char bad = (char) 0;
        if (split == ',')
            bad = ';';
        else if (split == ';')
            bad = ',';

        char sentinal = ' ';
        char cur;
        int start = 0;
        boolean skipSpace = false;
        List results = new ArrayList(6);
        for (int i = 0; i < dec.length(); i++) {
            cur = dec.charAt(i);
            if (cur == bad)
                throw new UserException(_loc.get("bad-dec", dec, decType));
            if (cur == ' ' && skipSpace) {
                start++;
                continue;
            }

            skipSpace = false;
            if (cur != sentinal)
                continue;

            // if looking for spaces, look for split char, or vice versa
            sentinal = (sentinal == ' ') ? split : ' ';
            results.add(dec.substring(start, i).trim());
            start = i + 1;
            skipSpace = true;
        }

        // add last token, if any
        if (start < dec.length())
            results.add(dec.substring(start));

        // if not an even number of elements, something is wrong
        if (results.isEmpty() || results.size() % 2 != 0)
            throw new UserException(_loc.get("bad-dec", dec, decType));

        return results;
    }

    /**
     * Split the given expression list into distinct expressions. Assumes the
     * given string is not null or of zero length and is already trimmed
     * (valid assumptions given the checks in our setters and before
     * this method call).
     */
    public static List splitExpressions(String str, char split, int expected) {
        if (str == null)
            return null;

        List exps = null;
        int parenDepth = 0;
        int begin = 0, pos = 0;
        boolean escape = false;
        boolean string = false;
        boolean nonspace = false;
        char quote = 0;
        for (char c; pos < str.length(); pos++) {
            c = str.charAt(pos);
            if (c == '\\') {
                escape = !escape;
                continue;
            }
            if (escape) {
                escape = false;
                continue;
            }

            switch (c) {
                case '\'':
                case '"':
                    if (string && quote == c)
                        string = false;
                    else if (!string) {
                        quote = c;
                        string = true;
                    }
                    nonspace = true;
                    break;
                case '(':
                    if (!string)
                        parenDepth++;
                    nonspace = true;
                    break;
                case ')':
                    if (!string)
                        parenDepth--;
                    nonspace = true;
                    break;
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                    if (c == split && !string && parenDepth == 0 && nonspace) {
                        if (exps == null)
                            exps = new ArrayList(expected);
                        exps.add(str.substring(begin, pos).trim());
                        begin = pos + 1;
                        nonspace = false;
                    }
                    break;
                default:
                    if (c == split && !string && parenDepth == 0) {
                        if (exps == null)
                            exps = new ArrayList(expected);
                        exps.add(str.substring(begin, pos).trim());
                        begin = pos + 1;
                    }
                    nonspace = true;
            }
            escape = false;
        }

        if (exps == null) {
            // Collections.singletonList wasn't added until 1.3
            exps = new ArrayList(1);
            exps.add(str);
            return exps;
        }

        // add last exp and return array
        String last = str.substring(begin).trim();
        if (last.length() > 0)
            exps.add(last);
        return exps;
    }

    /**
     * Add the given access path metadatas to the full path list, making sure
     * to maintain only base metadatas in the list. The given list may be null.
     */
    public static List addAccessPathMetaDatas(List metas,
        ClassMetaData[] path) {
        if (path == null || path.length == 0)
            return metas;

        // create set of base class metadatas in access path
        if (metas == null)
            metas = new ArrayList();
        int last = metas.size();

        // for every element in the path of this executor, compare it
        // to already-gathered elements to see if it should replace
        // a subclass in the list or should be added as a new base;
        // at least it's n^2 of a small n...
        ClassMetaData meta;
        boolean add;
        for (int i = 0; i < path.length; i++) {
            add = true;
            for (int j = 0; add && j < last; j++) {
                meta = (ClassMetaData) metas.get(j);

                if (meta.getDescribedType().isAssignableFrom
                    (path[i].getDescribedType())) {
                    // list already contains base class
                    add = false;
                } else if (path[i].getDescribedType().isAssignableFrom
                    (meta.getDescribedType())) {
                    // this element replaces its subclass
                    add = false;
                    metas.set(j, path[i]);
                }
            }

            // if no base class of current path element already in
            // list and path element didn't replace a subclass in the
            // list, then add it now as a new base
            if (add)
                metas.add(path[i]);
        }
        return metas;
    }

    /**
     * Convert the user-given hint value to an aggregate listener.
     * The hint can be an aggregate listener instance or class name.
     */
    public static AggregateListener hintToAggregateListener(Object hint,
        ClassLoader loader) {
        if (hint == null)
            return null;
        if (hint instanceof AggregateListener)
            return (AggregateListener) hint;

        Exception cause = null;
        if (hint instanceof String) {
            try {
                return (AggregateListener) Class.forName((String) hint, true,
                    loader).newInstance();
            } catch (Exception e) {
                cause = e;
            }
        }
        throw new UserException(_loc.get("bad-agg-listener-hint", hint,
            hint.getClass())).setCause(cause);
    }

    /**
     * Convert the user-given hint value to an array of aggregate listeners.
     * The hint can be an aggregate listener, aggregate listener array,
     * collection, or comma-separated class names.
     */
    public static AggregateListener[] hintToAggregateListeners(Object hint,
        ClassLoader loader) {
        if (hint == null)
            return null;
        if (hint instanceof AggregateListener[])
            return (AggregateListener[]) hint;
        if (hint instanceof AggregateListener)
            return new AggregateListener[]{ (AggregateListener) hint };
        if (hint instanceof Collection) {
            Collection c = (Collection) hint;
            return (AggregateListener[]) c.toArray
                (new AggregateListener[c.size()]);
        }

        Exception cause = null;
        if (hint instanceof String) {
            String[] clss = Strings.split((String) hint, ",", 0);
            AggregateListener[] aggs = new AggregateListener[clss.length];
            try {
                for (int i = 0; i < clss.length; i++)
                    aggs[i] = (AggregateListener) Class.forName(clss[i], true,
                        loader).newInstance();
                return aggs;
            } catch (Exception e) {
                cause = e;
            }
        }
        throw new UserException(_loc.get("bad-agg-listener-hint", hint,
            hint.getClass())).setCause(cause);
    }

    /**
     * Convert the user-given hint value to a filter listener.
     * The hint can be a filter listener instance or class name.
     */
    public static FilterListener hintToFilterListener(Object hint,
        ClassLoader loader) {
        if (hint == null)
            return null;
        if (hint instanceof FilterListener)
            return (FilterListener) hint;

        Exception cause = null;
        if (hint instanceof String) {
            try {
                return (FilterListener) Class.forName((String) hint, true,
                    loader).newInstance();
            } catch (Exception e) {
                cause = e;
            }
        }
        throw new UserException(_loc.get("bad-filter-listener-hint", hint,
            hint.getClass())).setCause(cause);
    }

    /**
     * Convert the user-given hint value to an array of filter listeners.
     * The hint can be a filter listener, filter listener array,
     * collection, or comma-separated class names.
     */
    public static FilterListener[] hintToFilterListeners(Object hint,
        ClassLoader loader) {
        if (hint == null)
            return null;
        if (hint instanceof FilterListener[])
            return (FilterListener[]) hint;
        if (hint instanceof FilterListener)
            return new FilterListener[]{ (FilterListener) hint };
        if (hint instanceof Collection) {
            Collection c = (Collection) hint;
            return (FilterListener[]) c.toArray(new FilterListener[c.size()]);
        }

        Exception cause = null;
        if (hint instanceof String) {
            String[] clss = Strings.split((String) hint, ",", 0);
            FilterListener[] filts = new FilterListener[clss.length];
            try {
                for (int i = 0; i < clss.length; i++)
                    filts[i] = (FilterListener) Class.forName(clss[i], true,
                        loader).newInstance();
                return filts;
            } catch (Exception e) {
                cause = e;
            }
        }
        throw new UserException(_loc.get("bad-filter-listener-hint", hint,
            hint.getClass())).setCause(cause);
    }

    /**
     * Return the value of the property named by the hint key.
     */
    public static Object hintToGetter(Object target, String hintKey) {
        if (target == null || hintKey == null)
            return null;

        Method getter = Reflection.findGetter(target.getClass(), hintKey, true);
        return Reflection.get(target, getter);
    }

    /**
     * Set the value of the property named by the hint key.
     */
    public static void hintToSetter(Object target, String hintKey,
        Object value) {
        if (target == null || hintKey == null)
            return;

        Method setter = Reflection.findSetter(target.getClass(), hintKey, true);
        if (value instanceof String) {
            if ("null".equals(value))
                value = null;
            else {
                try {
                    value = Strings.parse((String) value,
                        setter.getParameterTypes()[0]);
                } catch (Exception e) {
                    throw new UserException(_loc.get("bad-setter-hint-arg",
                        hintKey, value, setter.getParameterTypes()[0])).
                        setCause(e);
                }
            }
        }
        Reflection.set(target, setter, value);
	}
}
