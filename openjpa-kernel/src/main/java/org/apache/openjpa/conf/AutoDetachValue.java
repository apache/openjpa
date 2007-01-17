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
package org.apache.openjpa.conf;

import org.apache.openjpa.kernel.AutoDetach;
import org.apache.openjpa.lib.conf.StringListValue;

/**
 * Value type used to represent auto detach flags. Separate to
 * optimize translation of Strings to bit flags.
 *
 * @author Steve Kim
 * @nojavadoc
 */
class AutoDetachValue
    extends StringListValue {

    public static final String DETACH_CLOSE = "close";
    public static final String DETACH_COMMIT = "commit";
    public static final String DETACH_ROLLBACK= "rollback";
    public static final String DETACH_NONTXREAD = "nontx-read";

    private static String[] ALIASES = new String[]{
        DETACH_CLOSE, String.valueOf(AutoDetach.DETACH_CLOSE),
        DETACH_COMMIT, String.valueOf(AutoDetach.DETACH_COMMIT),
        DETACH_ROLLBACK, String.valueOf(AutoDetach.DETACH_ROLLBACK),
        DETACH_NONTXREAD, String.valueOf(AutoDetach.DETACH_NONTXREAD),
        // for compatibility with JDO DetachAllOnCommit
        "true", String.valueOf(AutoDetach.DETACH_COMMIT),
        "false", "0",
    };

    private int _flags;
    private boolean _flagsSet;

    public AutoDetachValue() {
        super("AutoDetach");
        setAliases(ALIASES);
        setAliasListComprehensive(true);
    }

    public Class getValueType() {
        return String[].class;
    }

    public void setConstant(int flags) {
        _flags = flags;
    }

    public int getConstant() {
        if (!_flagsSet) {
            String[] vals = get();
            for (int i = 0; i < vals.length; i++)
                _flags |= Integer.parseInt(unalias(vals[i]));
            _flagsSet = true;
        }
            
        return _flags;
    }
}
