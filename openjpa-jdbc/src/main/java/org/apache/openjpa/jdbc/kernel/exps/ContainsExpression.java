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
package org.apache.openjpa.jdbc.kernel.exps;

import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.Select;
import serp.util.Numbers;

/**
 * Tests whether one value contains another.
 *
 * @author Abe White
 */
class ContainsExpression
    extends EqualExpression {

    /**
     * Constructor. Supply values to test.
     */
    public ContainsExpression(Val val1, Val val2) {
        super(val1, val2);
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        Val val1 = getValue1();
        if (contains != null && val1 instanceof PCPath) {
            PCPath sql = (PCPath) val1;
            String path = sql.getPath();

            // update the count for this path
            Integer count = (Integer) contains.get(path);
            if (count == null)
                count = Numbers.valueOf(0);
            else
                count = Numbers.valueOf(count.intValue() + 1);
            contains.put(path, count);

            sql.setContainsId(count.toString());
        }
        super.initialize(sel, store, params, contains);
    }

    protected boolean isDirectComparison() {
        return false;
    }

    public boolean hasContainsExpression() {
        return true;
    }
}
