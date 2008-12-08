/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.persistence.query;

import javax.persistence.Expression;
import javax.persistence.TrimSpec;

/**
 * Denotes TRIM(e1,x) Expression.
 * 
 * @author Pinaki Poddar
 *
 */
public class TrimExpression extends UnaryOperatorExpression {
	private final Expression _trimChar;
	private final TrimSpec _spec;
	private static final String BLANK = "' '";
	
	public TrimExpression(Expression op, char ch, TrimSpec spec) {
		super(op, UnaryFunctionalOperator.TRIM);
		_trimChar = new ConstantExpression(ch);
		_spec     = spec;
	}
	
	public TrimExpression(Expression op, Expression ch, TrimSpec spec) {
		super(op, UnaryFunctionalOperator.TRIM);
		_trimChar = ch;
		_spec = spec;
	}
	
	public String asExpression(AliasContext ctx) {
		String trim = _trimChar == null ? BLANK 
			: ((Visitable)_trimChar).asExpression(ctx);
		String spec = _spec == null ? "" : _spec.toString();
		return _op + "(" + spec + " " + trim + " FROM " 
			+ ((Visitable)_e).asExpression(ctx) + ")";
	}

}
