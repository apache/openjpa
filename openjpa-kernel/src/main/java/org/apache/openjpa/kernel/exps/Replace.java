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
 * Take the replaces part of the given string
 *
 * @author Abe White
 * @author Paulo Cristovão Filho
 */
class Replace
    extends Val {

    
    private static final long serialVersionUID = 1L;
    private final Val _orig;
    private final Val _patt;
    private final Val _repl;

    /**
     * Constructor. Provides values of replacement.
     */
    public Replace(Val orig, Val pattern, Val replacement) {
        _orig = orig;
        _patt = pattern;
        _repl = replacement;
    }

    @Override
    public Class getType() {
        return String.class;
    }

    @Override
    public void setImplicitType(Class type) {
    }

    @Override
    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {
        String str = _orig.eval(candidate, orig, ctx, params).toString();
        String patt = _patt.eval(candidate, orig, ctx, params).toString();
        String repl = _repl.eval(candidate, orig, ctx, params).toString();
        while (str.indexOf(patt) != -1) {
        	str = str.replace(patt, repl);
        }
        return str;
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _orig.acceptVisit(visitor);
        _patt.acceptVisit(visitor);
        _repl.acceptVisit(visitor);
        visitor.exit(this);
    }
}
