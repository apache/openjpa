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
package org.apache.openjpa.kernel.exps;

import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UnsupportedException;

/**
 * An in-memory representation of a {@link Subquery}. Currently
 * subqueries aren't supported for in-memory use.
 *
 * @author Abe White
 */
class SubQ extends Val implements Subquery {

    private static final Localizer _loc = Localizer.forPackage(Subquery.class);
    private final String _alias;
    private Class _type = null;

    public SubQ(String alias) {
        _alias = alias;
    }

    public String getCandidateAlias() {
        return _alias;
    }

    public void setQueryExpressions(QueryExpressions q, long startIdx,
        long endIdx) {
    }

    public boolean isVariable() {
        return false;
    }

    public Class getType() {
        return _type;
    }

    public void setImplicitType(Class type) {
        _type = type;
    }

    public boolean hasVariables() {
        return false;
    }

    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {
        throw new UnsupportedException(_loc.get("in-mem-subquery"));
    }
}
