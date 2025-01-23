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
package org.apache.openjpa.kernel.exps;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;

import org.apache.openjpa.kernel.StoreContext;

/**
 * Represents the current temporal.
 *
 */
class CurrentTemporal
    extends Val {
    
    private static final long serialVersionUID = 1L;
    private final Class<? extends Temporal> _type;

    public CurrentTemporal(Class<? extends Temporal> type) {
        _type = type;
    }

    @Override
    public Class getType() {
        return _type;
    }

    @Override
    public void setImplicitType(Class type) {
    }

    @Override
    protected Object eval(Object candidate, Object orig, StoreContext ctx, Object[] params) {
        if (LocalDateTime.class.isAssignableFrom(_type)) {
            return LocalDateTime.now();
        } else if (LocalDate.class.isAssignableFrom(_type)) {
            return LocalDate.now();
        } else if (LocalTime.class.isAssignableFrom(_type)) {
            return LocalTime.now();
        }
        return null;
    }
}
