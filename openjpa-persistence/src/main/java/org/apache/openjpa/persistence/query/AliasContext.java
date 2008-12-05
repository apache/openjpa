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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Expression;

class AliasContext {
	private Map<ExpressionImpl, String> _aliases = 
		new HashMap<ExpressionImpl, String>();
	
	/**
	 * Sets alias for the given Expression or gets the alias if the given
	 * path has already been assigned an alias.
	 * The given expression must provide a hint on what should be the 
	 * alias name. If the alias name is assigned by this context, then a
	 * different alias is generated.
	 * @param path
	 * @return the alias name
	 */
	public String getAlias(ExpressionImpl path) {
		String alias = _aliases.get(path);
		if (alias != null)
			return alias;
		alias = path.getAliasHint().substring(0,1).toLowerCase();
		int i = 2;
		while (_aliases.containsValue(alias)) {
			alias = alias + i;
			i++;
		}
		_aliases.put(path, alias);
		return alias;
	}
	
	/**
	 * Affirms if the given Expression has been assigned an alias by this
	 * context.
	 */
	public boolean hasAlias(Expression path) {
		return _aliases.containsKey(path);
	}
}
