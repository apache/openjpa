/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.openjpa.eclipse.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.eclipse.util.pathmatch.AntPathMatcher;

/**
 * Util to check matching of Ant-style path patterns. 
 * 
 * @author Michael Vorburger (MVO)
 */
public class PathMatcherUtil {

	private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
	private final List<String> includePatterns = new LinkedList<String>();
	
	@SuppressWarnings("unchecked")
	public PathMatcherUtil(Map args) {
		if (args != null) {
			Set<String> keys = args.keySet();
			for (String key : keys) {
				if (key.toLowerCase().startsWith("include")) {
					includePatterns.add((String) args.get(key));
				}
			}
		}
	}

	public boolean match(String fileName) {
		if (includePatterns.isEmpty())
			return true;
		
		for (String includePattern : includePatterns) {
			if (antPathMatcher.match(includePattern, fileName))
				return true;
		}
		return false;
	}
	
}
