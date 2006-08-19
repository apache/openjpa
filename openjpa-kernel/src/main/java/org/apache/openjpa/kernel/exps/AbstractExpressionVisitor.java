package org.apache.openjpa.kernel.exps;


/**
 * No-op abstract visitor meant for easy extension.
 *
 * @author Abe White
 * @nojavadoc
 */
public abstract class AbstractExpressionVisitor 
    implements ExpressionVisitor {

    public void enter(Expression exp) {
    }

    public void exit(Expression exp) {
    }

    public void enter(Value val) {
    }

    public void exit(Value val) {
    }
}
