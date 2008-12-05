package org.apache.openjpa.persistence.query;

import javax.persistence.Expression;


public class IsEmptyExpression extends UnaryExpressionPredicate {
	public IsEmptyExpression(Expression op) {
		super(op, UnaryConditionalOperator.ISEMPTY, 
			UnaryConditionalOperator.ISEMPTY_NOT);
	}
	
	@Override
	public String asExpression(AliasContext ctx) {
		return ((Visitable)_e).asExpression(ctx) + " " + _op;
	}
}
