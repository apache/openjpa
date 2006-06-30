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
package org.apache.openjpa.lib.conf;

import java.io.*;
import java.util.*;
import org.apache.commons.collections.*;
import org.apache.openjpa.lib.log.*;
import org.apache.openjpa.lib.util.*;

/**
 * Simple configuration provider that sets configuration based on a
 * provided map.
 * 
 * @author Abe White
 * @nojavadoc
 */
public class MapConfigurationProvider implements ConfigurationProvider {
    private static final Localizer _loc = Localizer.forPackage
        (MapConfigurationProvider.class);

    private Map _props = null;

    /**
     * Construct with null properties.
     */
    public MapConfigurationProvider() {
    }

    /**
     * Constructor; supply properties map.
     */
    public MapConfigurationProvider(Map props) {
        addProperties(props);
    }

    public boolean loadDefaults(ClassLoader loader) throws Exception {
        return false;
    }

    public boolean load(String resource, ClassLoader loader) throws Exception {
        return false;
    }

    public boolean load(File file) throws Exception {
        return false;
    }

    public Map getProperties() {
        return(_props == null) ? Collections.EMPTY_MAP : _props;
    }

    public void addProperties(Map props) {
        if (props == null || props.isEmpty())
            return;
        if (_props == null)
            _props = new HashMap();
        _props.putAll(props);
    }

    public Object addProperty(String key, Object value) {
        if (_props == null)
            _props = new HashMap();
        return _props.put(key, value);
    }

    public void setInto(Configuration conf) {
        setInto(conf, conf.getConfigurationLog());
    }

    /**
     * Set properties into configuration. If the log is non-null, will log
     * a TRACE message about the set.
     */
    protected void setInto(Configuration conf, Log log) {
        if (log != null && log.isTraceEnabled())
            log.trace(_loc.get("conf-load", getProperties()));
        if (_props != null)
            conf.fromProperties(_props);
    }
}
