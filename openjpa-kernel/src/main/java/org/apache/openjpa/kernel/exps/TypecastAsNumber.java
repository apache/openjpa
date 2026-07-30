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

import org.apache.openjpa.kernel.StoreContext;

/**
 * Casts a value to a number
 *
 */
class TypecastAsNumber extends Val {
    private static final long serialVersionUID = 1L;
    private final Class<? extends Number> _targetType;
    private final Val _val;

    /**
     * Constructor. Provide target field and the value.
     */
    public TypecastAsNumber(Val val, Class<? extends Number> target) {
        _val = val;
        _targetType = target;
    }

    @Override
    public Class getType() {
        if (_targetType == Integer.class) {
            return int.class;
        } else if (_targetType == Long.class) {
            return long.class;
        } else if (_targetType == Float.class) {
            return float.class;
        } else if (_targetType == Double.class) {
            return double.class;
        } else {
            return _targetType;
        }
    }

    @Override
    public void setImplicitType(Class type) {
    }

    @Override
    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {

        Object r = _val.eval(candidate, orig, ctx, params);
        if (r == null) {
        	return null;
        }
        Class<?> clazz = r.getClass();
        if (_targetType == Integer.class) {
            if (r instanceof String s) {
                return Integer.valueOf(s);
            } else if (r instanceof Number n) {
                return n.intValue();
            }
        } else if (_targetType == Long.class) {
            if (r instanceof String s) {
                return Long.valueOf(s);
            } else if (r instanceof Number n) {
                return n.longValue();
            }
        } else if (_targetType == Float.class) {
            if (r instanceof String s) {
                return Float.valueOf(s);
            } else if (r instanceof Number n) {
                return n.floatValue();
            }
        } else if (_targetType == Double.class) {
            if (r instanceof String s) {
                return Double.valueOf(s);
            } else if (r instanceof Number n) {
                return n.doubleValue();
            }
        }
        throw new IllegalArgumentException("Value is not valid for typecast: " + r);
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val.acceptVisit(visitor);
        visitor.exit(this);
    }
}
