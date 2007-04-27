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

import org.apache.openjpa.meta.JavaTypes;
import serp.util.Numbers;

/**
 * Uses a version number for optimistic versioning.
 *
 * @author Abe White
 */
public class NumberVersionStrategy
    extends ColumnVersionStrategy {

    public static final String ALIAS = "version-number";

    private Number _initial = Numbers.valueOf(1);

    /**
     * Set the initial value for version column. Defaults to 1.
     */
    public void setInitialValue(int initial) {
        _initial = Numbers.valueOf(initial);
    }

    /**
     * Return the initial value for version column. Defaults to 1.
     */
    public int getInitialValue() {
        return _initial.intValue();
    }

    public String getAlias() {
        return ALIAS;
    }

    protected int getJavaType() {
        return JavaTypes.INT;
    }

    protected Object nextVersion(Object version) {
        if (version == null)
            return _initial;
        return Numbers.valueOf(((Number) version).intValue() + 1);
    }
}
