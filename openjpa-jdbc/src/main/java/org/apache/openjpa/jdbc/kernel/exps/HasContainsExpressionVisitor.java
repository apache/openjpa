package org.apache.openjpa.jdbc.kernel.exps;

import org.apache.openjpa.kernel.exps.AbstractExpressionVisitor;
import org.apache.openjpa.kernel.exps.Expression;
import org.apache.openjpa.kernel.exps.Value;

/**
 * Determines whether the visited expressions include a "contains" expression.
 * 
 * @author Abe White
 */
class HasContainsExpressionVisitor 
    extends AbstractExpressionVisitor {

    private boolean _found = false;

    /**
     * Whether a contains expression has been found.
     */
    public boolean foundContainsExpression() {
        return _found;
    }
    
    public void enter(Expression exp) {
        if (!_found)
            _found = exp instanceof ContainsExpression 
                || exp instanceof BindVariableAndExpression;
    }
} 
