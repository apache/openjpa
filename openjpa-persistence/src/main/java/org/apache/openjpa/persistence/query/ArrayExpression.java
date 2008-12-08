package org.apache.openjpa.persistence.query;

import java.util.Arrays;

public class ArrayExpression extends ExpressionImpl {
	private final Object[] _values;
	
	public ArrayExpression(Object[] values) {
		_values = values;
	}

	@Override
	public String asExpression(AliasContext ctx) {
		StringBuffer tmp = new StringBuffer("(");
		for (int i = 0; i < _values.length; i++) {
			tmp.append(JPQLHelper.toJPQL(ctx, _values[i]))
			   .append(i == _values.length-1 ? "" : ", ");
		}
		tmp.append(")");
		return tmp.toString();
	}

	@Override
	public String asProjection(AliasContext ctx) {
		return asExpression(ctx);
	}

}
