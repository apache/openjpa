package org.apache.openjpa.persistence.query;

import javax.persistence.Expression;
import javax.persistence.Subquery;

/**
 * Denotes e1 IN (e2) Expression.
 * 
 * @author Pinaki Poddar
 *
 */
public class InExpression extends BinaryExpressionPredicate  {
	public InExpression(Expression op, ArrayExpression op2) {
		super(op, BinaryConditionalOperator.IN, 
			BinaryConditionalOperator.IN_NOT, op2);
	}
	
	public InExpression(Expression op, Expression subquery) {
		super(op, BinaryConditionalOperator.IN, 
			BinaryConditionalOperator.IN_NOT, subquery);
	}
}
