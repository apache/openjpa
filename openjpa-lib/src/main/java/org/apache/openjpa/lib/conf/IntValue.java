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

import org.apache.commons.lang.StringUtils;

/**
 * An int {@link Value}.
 *
 * @author Marc Prud'hommeaux
 */
public class IntValue extends Value<Number> {

    private int value;

    public IntValue(String prop) {
        super(Number.class, prop);
    }

    /**
     * The internal value.
     */
    public void set(int value) {
        assertChangeable();
        int oldValue = this.value;
        this.value = value;
        if (value != oldValue)
            valueChanged();
    }

    /**
     * The internal value.
     */
    public Integer get() {
        return this.value;
    }

    protected String getInternalString() {
        return String.valueOf(this.value);
    }

    protected void setInternalString(String val) {
        set (StringUtils.isEmpty(val) ? 0 : Integer.parseInt(val));
    }

    protected void setInternalObject(Object num) {
        set (num == null ? 0 : ((Number)num).intValue());
    }
}
