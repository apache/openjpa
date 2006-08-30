/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.meta;

import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.StringDistance;
import org.apache.openjpa.util.MetaDataException;
import serp.util.Numbers;

/**
 * Strategies for persistent value generation.
 *
 * @author Abe White
 * @since 0.4.0
 */
public class ValueStrategies {

    /**
     * No value strategy.
     */
    public static final int NONE = 0;

    /**
     * "native" value strategy.
     */
    public static final int NATIVE = 1;

    /**
     * "sequence" value strategy.
     */
    public static final int SEQUENCE = 2;

    /**
     * "autoassigned" value strategy.
     */
    public static final int AUTOASSIGN = 3;

    /**
     * "increment" value strategy.
     */
    public static final int INCREMENT = 4;

    /**
     * "uuid-string" value strategy.
     */
    public static final int UUID_STRING = 5;

    /**
     * "uuid-hex" value strategy.
     */
    public static final int UUID_HEX = 6;

    private static final Localizer _loc = Localizer.forPackage
        (ValueStrategies.class);

    // table of names and strategies
    private static final TreeBidiMap _map = new TreeBidiMap();

    static {
        _map.put("none", Numbers.valueOf(NONE));
        _map.put("native", Numbers.valueOf(NATIVE));
        _map.put("sequence", Numbers.valueOf(SEQUENCE));
        _map.put("autoassign", Numbers.valueOf(AUTOASSIGN));
        _map.put("increment", Numbers.valueOf(INCREMENT));
        _map.put("uuid-string", Numbers.valueOf(UUID_STRING));
        _map.put("uuid-hex", Numbers.valueOf(UUID_HEX));
    }

    /**
     * Convert the given strategy to a name.
     */
    public static String getName(int strategy) {
        Object code = Numbers.valueOf(strategy);
        String name = (String) _map.getKey(code);
        if (name != null)
            return name;
        throw new IllegalArgumentException(code.toString());
    }

    /**
     * Convert the given strategy name to its constant.
     */
    public static int getCode(String val, Object context) {
        if (val == null)
            return NONE;
        Object code = _map.get(val);
        if (code != null)
            return ((Number) code).intValue();

        // not a recognized strategy; check for typo
        String closest = StringDistance.getClosestLevenshteinDistance(val,
            _map.keySet(), .5F);
        String msg;
        if (closest != null)
            msg = _loc.get("bad-value-strategy-hint", new Object[]{
                context, val, closest, _map.keySet() }).getMessage();
        else
            msg = _loc.get("bad-value-strategy", context, val, _map.keySet())
                .getMessage();
        throw new IllegalArgumentException(msg);
    }

    /**
     * Assert that the given strategy is supported by the current runtime.
     */
    public static void assertSupported(int strategy, MetaDataContext context,
        String attributeName) {
        OpenJPAConfiguration conf = context.getRepository().getConfiguration();
        boolean supported = true;
        switch (strategy) {
            case AUTOASSIGN:
                supported = conf.supportedOptions().contains
                    (OpenJPAConfiguration.OPTION_VALUE_AUTOASSIGN);
                break;
            case INCREMENT:
                supported = conf.supportedOptions().contains
                    (OpenJPAConfiguration.OPTION_VALUE_INCREMENT);
                break;
            case NATIVE:
                supported = context instanceof ClassMetaData;
                break;
        }
        if (!supported)
            throw new MetaDataException(_loc.get("unsupported-value-strategy",
                context, getName(strategy), attributeName));
	}
}
