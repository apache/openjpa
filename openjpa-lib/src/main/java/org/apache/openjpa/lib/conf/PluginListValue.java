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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * A list of plugins. Defaults and aliases on plugin lists apply only
 * to individual class names.
 *
 * @author Abe White
 * @see PluginValue
 */
public class PluginListValue extends ObjectValue {

    private static final String[] EMPTY = new String[0];

    private String[] _names = EMPTY;
    private String[] _props = EMPTY;

    public PluginListValue(String prop) {
        super(prop);
    }

    /**
     * The plugin class names.
     */
    public String[] getClassNames() {
        return _names;
    }

    /**
     * The plugin class names.
     */
    public void setClassNames(String[] names) {
        if (names == null)
            names = EMPTY;
        _names = names;
        set(null, true);
        valueChanged();
    }

    /**
     * The plugin properties.
     */
    public String[] getProperties() {
        return _props;
    }

    /**
     * The plugin properties.
     */
    public void setProperties(String[] props) {
        if (props == null)
            props = EMPTY;
        _props = props;
        set(null, true);
        valueChanged();
    }

    /**
     * Instantiate the plugins as instances of the given class.
     */
    public Object instantiate(Class elemType, Configuration conf,
        boolean fatal) {
        Object[] ret;
        if (_names.length == 0)
            ret = (Object[]) Array.newInstance(elemType, 0);
        else {
            ret = (Object[]) Array.newInstance(elemType, _names.length);
            for (int i = 0; i < ret.length; i++) {
                ret[i] = newInstance(_names[i], elemType, conf, fatal);
                Configurations.configureInstance(ret[i], conf, _props[i],
                    getProperty());
            }
        }
        set(ret, true);
        return ret;
    }

    /**
     * Override to recognize aliases of the class name without the attached
     * properties string.
     */
    public String getString() {
        if (_names.length == 0)
            return null;

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < _names.length; i++) {
            if (i > 0)
                buf.append(", ");
            buf.append(Configurations.getPlugin(alias(_names[i]), _props[i]));
        }
        if (buf.length() == 0)
            return null;
        return buf.toString();
    }

    /**
     * Override to recognize aliases of the plugin name without the attached
     * properties string.
     */
    public void setString(String str) {
        if (StringUtils.isEmpty(str))
            str = getDefault();
        if (StringUtils.isEmpty(str)) {
            _names = EMPTY;
            _props = EMPTY;
            set(null, true);
            valueChanged();
            return;
        }

        // split up the string; each element might be a class name, or a
        // class name with properties settings
        List plugins = new ArrayList();
        StringBuffer plugin = new StringBuffer();
        boolean inParen = false;
        char c;
        for (int i = 0; i < str.length(); i++) {
            c = str.charAt(i);
            switch (c) {
                case '(':
                    inParen = true;
                    plugin.append(c);
                    break;
                case ')':
                    inParen = false;
                    plugin.append(c);
                    break;
                case ',':
                    if (inParen)
                        plugin.append(c);
                    else {
                        plugins.add(plugin.toString());
                        plugin = new StringBuffer();
                    }
                    break;
                default:
                    plugin.append(c);
            }
        }
        if (plugin.length() > 0)
            plugins.add(plugin.toString());

        // parse each plugin element into its name and properties
        List names = new ArrayList();
        List props = new ArrayList();
        String clsName;
        for (int i = 0; i < plugins.size(); i++) {
            str = (String) plugins.get(i);
            clsName = unalias(Configurations.getClassName(str));
            if (clsName != null) {
                names.add(clsName);
                props.add(Configurations.getProperties(str));
            }
        }
        _names = (String[]) names.toArray(new String[names.size()]);
        _props = (String[]) props.toArray(new String[props.size()]);
        set(null, true);
        valueChanged();
    }

    public Class getValueType() {
        return Object[].class;
    }

    protected void objectChanged() {
        Object[] vals = (Object[]) get();
        if (vals == null || vals.length == 0)
            _names = EMPTY;
        else {
            _names = new String[vals.length];
            for (int i = 0; i < vals.length; i++)
                _names[i] = (vals[i] == null) ? null
                    : vals[i].getClass().getName();
        }
        _props = EMPTY;
    }

    protected String getInternalString() {
        // should never get called
        throw new IllegalStateException();
    }

    protected void setInternalString(String str) {
        // should never get called
        throw new IllegalStateException();
    }
}
