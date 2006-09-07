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

import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;

/**
 * An empty expression.
 *
 * @author Abe White
 */
class EmptyExpression
    implements Exp {

    public ExpState initialize(Select sel, ExpContext ctx, Map contains) {
        return ExpState.NULL;
    }

    public void appendTo(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer buf) {
    }

    public void selectColumns(Select sel, ExpContext ctx, ExpState state, 
        boolean pks) {
    }

    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        visitor.exit(this);
    }
}
