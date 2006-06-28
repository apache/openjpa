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
package org.apache.openjpa.lib.conf;

import serp.util.*;


/**
 *  <p>A comma-separated list of string values.</p>
 *
 *  @author Abe White
 */
public class StringListValue extends Value {
    public static final String[] EMPTY = new String[0];
    private String[] _values = EMPTY;

    public StringListValue(String prop) {
        super(prop);
    }

    /**
     *  The internal value.
     */
    public void set(String[] values) {
        _values = (values == null) ? EMPTY : values;
        valueChanged();
    }

    /**
     *  The internal value.
     */
    public String[] get() {
        return _values;
    }

    public Class getValueType() {
        return String[].class;
    }

    protected String getInternalString() {
        return Strings.join(_values, ", ");
    }

    protected void setInternalString(String val) {
        String[] vals = Strings.split(val, ",", 0);

        if (vals != null) {
            for (int i = 0; i < vals.length; i++)
                vals[i] = vals[i].trim();
        }

        set(vals);
    }

    protected void setInternalObject(Object obj) {
        set((String[]) obj);
    }
}
