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
 * Take the right part of a string.
 *
 * @author Abe White
 * @author Paulo Cristovão Filho
 */
class Right
    extends Val {

    
    private static final long serialVersionUID = 1L;
    private final Val _val;
    private final Val _len;

    /**
     * Constructor. Provide substring and length of the right part that must be kept.
     */
    public Right(Val val, Val len) {
        _val = val;
        _len = len;
    }

    @Override
    public Class getType() {
        return String.class;
    }

    @Override
    public void setImplicitType(Class type) {
    }

    @Override
    protected Object eval(Object candidate, Object orig, StoreContext ctx, Object[] params) {
        Object str = _val.eval(candidate, orig, ctx, params);
        Object arg = _len.eval(candidate, orig, ctx, params);
        String s = str.toString();
        int len = ((Number) arg).intValue();
        int slen = s.length();
        return len > slen ? s : s.substring(slen - len, slen);
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val.acceptVisit(visitor);
        _len.acceptVisit(visitor);
        visitor.exit(this);
    }
}
