/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package serp.bytecode;

import java.util.*;

/**
 * Caching and conversion of names in both internal and external form.
 * 
 * @author Abe White
 */
public class NameCache {
    static final Object[][] _codes = new Object[][] {
        { byte.class, "B" }, { char.class, "C" }, { double.class, "D" },
        { float.class, "F" }, { int.class, "I" }, { long.class, "J" },
        { short.class, "S" }, { boolean.class, "Z" }, { void.class, "V" }, };

    // caches of internal and external forms of strings
    private final Map _internal = new HashMap();
    private final Map _internalDescriptor = new HashMap();
    private final Map _external = new HashMap();
    private final Map _externalHuman = new HashMap();

    /**
     * Converts the given class name to its internal form.
     * 
     * @param className the name to convert
     * @param descriptor true if the name is to be used for a descriptor
     * section -- the difference seems to be that for
     * descriptors, non-primitives are prefixed with 'L' and ended with ';'
     */
    public String getInternalForm(String className, boolean descriptor) {
        if (className == null || className.length() == 0)
            return className;

        Map cache = (descriptor) ? _internalDescriptor : _internal;
        String cached = (String) cache.get(className);
        if (cached != null)
            return cached;

        String ret = getInternalFormInternal(className, descriptor);
        cache.put(className, ret);
        return ret;
    }

    /**
     * @see #getInternalForm
     */
    private String getInternalFormInternal(String cls, boolean descriptor) {
        // handle array types, whether already in internal form or not
        StringBuffer prefix = new StringBuffer();
        while (true) {
            if (cls.endsWith("[]")) {
                prefix.append("[");
                cls = cls.substring(0, cls.length() - 2);
            } else if (cls.startsWith("[")) {
                prefix.append("[");
                cls = cls.substring(1);
            } else
                break;
        }

        // handle primitive array types
        for (int i = 0; i < _codes.length; i++)
            if (cls.equals(_codes[i][1].toString())
                || cls.equals(_codes[i][0].toString()))
                return prefix.append(_codes[i][1]).toString();

        // if in descriptor form, strip leading 'L' and trailing ';'
        if (cls.startsWith("L") && cls.endsWith(";"))
            cls = cls.substring(1, cls.length() - 1);

        // non-primitive; make sure we don't prefix method descriptors with 'L'
        cls = cls.replace('.', '/');
        if ((descriptor || prefix.length() > 0) && cls.charAt(0) != '(')
            return prefix.append("L").append(cls).append(";").toString();
        return prefix.append(cls).toString();
    }

    /**
     * Given the internal name of the class, return the 'normal' java name.
     * 
     * @param internalName the internal name being used
     * @param humanReadable if the returned name should be in human-readable
     * form, rather than a form suitable for a
     * {@link Class#forName} call -- the difference
     * lies in the handling of arrays
     */
    public String getExternalForm(String internalName, boolean humanReadable) {
        if (internalName == null || internalName.length() == 0)
            return internalName;

        Map cache = (humanReadable) ? _externalHuman : _external;
        String cached = (String) cache.get(internalName);
        if (cached != null)
            return cached;

        String ret = getExternalFormInternal(internalName, humanReadable);
        cache.put(internalName, ret);
        return ret;
    }

    /**
     * @see #getExternalForm
     */
    private String getExternalFormInternal(String intern, boolean humanReadable) {
        if (!humanReadable) {
            // check against primitives
            for (int i = 0; i < _codes.length; i++) {
                if (intern.equals(_codes[i][1].toString()))
                    return _codes[i][0].toString();
                if (intern.equals(_codes[i][0].toString()))
                    return intern;
            }

            intern = getInternalForm(intern, false);
            return intern.replace('/', '.');
        }

        // handle arrays
        StringBuffer postfix = new StringBuffer(2);
        while (intern.startsWith("[")) {
            intern = intern.substring(1);
            postfix.append("[]");
        }

        // strip off leading 'L' and trailing ';'
        if (intern.endsWith(";"))
            intern = intern.substring(1, intern.length() - 1);

        // check primitives
        for (int i = 0; i < _codes.length; i++)
            if (intern.equals(_codes[i][1].toString()))
                return _codes[i][0].toString() + postfix;

        return intern.replace('/', '.') + postfix;
    }

    /**
     * Construct a method descriptor from the given return and parameter
     * types, which will be converted to internal form.
     */
    public String getDescriptor(String returnType, String[] paramTypes) {
        StringBuffer buf = new StringBuffer();
        buf.append("(");
        if (paramTypes != null) {
            for (int i = 0; i < paramTypes.length; i++) {
                if (paramTypes[i] == null)
                    throw new NullPointerException("paramTypes[" + i
                        + "] = null");

                buf.append(getInternalForm(paramTypes[i], true));
            }
        }
        buf.append(")");

        if (returnType == null)
            throw new NullPointerException("returnType = null");
        buf.append(getInternalForm(returnType, true));

        return buf.toString();
    }

    /**
     * Return the return type, in internal form, for the given method
     * descriptor string.
     */
    public String getDescriptorReturnName(String descriptor) {
        int index = descriptor.indexOf(')');
        if (index == -1)
            return "";
        return descriptor.substring(descriptor.indexOf(')') + 1);
    }

    /**
     * Return the parameter types, in internal form, for the given method
     * descriptor string.
     */
    public String[] getDescriptorParamNames(String descriptor) {
        if (descriptor == null || descriptor.length() == 0)
            return new String[0];

        int index = descriptor.indexOf(')');
        if (index == -1)
            return new String[0];

        // get rid of the parens and the return type
        descriptor = descriptor.substring(1, index);

        // break the param string into individual params
        List tokens = new LinkedList();
        while (descriptor.length() > 0) {
            index = 0;

            // skip the '[' up to the first letter code
            while (!Character.isLetter(descriptor.charAt(index)))
                index++;

            // non-primitives always start with 'L' and end with ';'
            if (descriptor.charAt(index) == 'L')
                index = descriptor.indexOf(';');

            tokens.add(descriptor.substring(0, index + 1));
            descriptor = descriptor.substring(index + 1);
        }

        return(String[]) tokens.toArray(new String[tokens.size()]);
    }

    /**
     * Return the component type name for the given array type, or null
     * if the given string does not represent an array type name. The name
     * given should be in proper {@link Class#forName} form.
     */
    public String getComponentName(String name) {
        if (name == null || !name.startsWith("["))
            return null;

        name = name.substring(1);
        if (!name.startsWith("[") && name.endsWith(";"))
            name = name.substring(1, name.length() - 1);

        // will convert primitive type codes to names
        return getExternalForm(name, false);
    }

    /**
     * Clear the cache.
     */
    public void clear() {
        _internal.clear();
        _internalDescriptor.clear();
        _external.clear();
        _externalHuman.clear();
    }
}
