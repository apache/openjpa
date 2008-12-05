package org.apache.openjpa.persistence.query;

import javax.persistence.Expression;

/**
 * Denotes e1 IN (e2) Expression.
 * 
 * @author Pinaki Poddar
 *
 */
public class InExpression extends BinaryExpressionPredicate  {
	public InExpression(Expression op, Expression op2) {
		super(op, BinaryConditionalOperator.IN, BinaryConditionalOperator.IN_NOT, op2);
	}
}
