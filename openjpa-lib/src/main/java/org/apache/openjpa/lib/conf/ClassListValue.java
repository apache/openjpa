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
package org.apache.openjpa.lib.conf;

import static java.util.Optional.ofNullable;

import java.security.AccessController;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.StringUtil;

public class ClassListValue extends Value {
    private Class<?>[] _values = new Class<?>[0];

    public void set(final Class<?>[] values) {
        assertChangeable();
        if (values != null) {
            _values = values;
        }
        valueChanged();
    }

    @Override
    public Class<?>[] get() {
        return _values;
    }

    @Override
    public Class<Class[]> getValueType() {
        return Class[].class;
    }

    @Override
    protected String getInternalString() {
        return Stream.of(_values).map(Class::getName).collect(Collectors.joining(","));
    }

    @Override
    protected void setInternalString(String val) {
        String[] vals = StringUtil.split(val, ",", 0);
        if (vals != null) {
            for (int i = 0; i < vals.length; i++)
                vals[i] = vals[i].trim();
        }

        final ClassLoader loader = AccessController.doPrivileged(J2DoPrivHelper.getContextClassLoaderAction());
        set(ofNullable(StringUtil.split(val, ",", 0))
                .map(it -> Stream.of(it).map(v -> {
                    try {
                        return loader.loadClass(v.trim());
                    } catch (final ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                }).toArray(Class<?>[]::new))
                .orElse(null));
    }

    @Override
    protected void setInternalObject(Object obj) {
        set((Class<?>[]) obj);
    }
}
