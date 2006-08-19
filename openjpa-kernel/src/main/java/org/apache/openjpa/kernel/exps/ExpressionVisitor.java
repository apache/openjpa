package org.apache.openjpa.kernel.exps;


/**
 * Visits nodes of a query expression tree.
 *
 * @author Abe White
 */
public interface ExpressionVisitor {

    /**
     * Enter an expression.  The expression will then invoke visits on its
     * components.
     */
    public void enter(Expression exp);

    /**
     * Leave an expression.
     */
    public void exit(Expression exp);

    /**
     * Enter a value.  The value will then invoke visits on its components.
     */
    public void enter(Value val);

    /**
     * Leave a value.
     */
    public void exit(Value val);
}
