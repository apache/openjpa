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

import java.util.Properties;

import junit.framework.TestCase;

/**
 * Test for PathMatcherUtil. 
 * 
 * @author Michael Vorburger (MVO)
 */
public class PathMatcherUtilTest extends TestCase {

	/**
	 * Test method for {@link org.apache.openjpa.eclipse.util.PathMatcherUtil#match(java.lang.String)}.
	 */
	public void testPathMatcherUtil() {
		Properties p = new Properties();
		p.setProperty("include1", "**/*Entity.class");
		p.setProperty("include2", "**/*EntityRef.class");
		PathMatcherUtil o = new PathMatcherUtil(p);
		
		assertTrue(o.match("asdjflaskdf/asdkfmasdlfk/asdklfmasdlf/SomeEntity.class"));
		assertTrue(o.match("asdjflaskdf/asdkfmasdlfk/asdklfmasdlf/SomeEntityRef.class"));
		assertFalse(o.match("asdjflaskdf/asdkfmasdlfk/asdklfmasdlf/ThisEntityIsNot.class"));
		assertFalse(o.match("asdjflaskdf/asdkfmasdlfk/asdklfmasdlf/SomethingElse.class"));
	}

	public void testPathMatcherUtilWithNull() {
		PathMatcherUtil o = new PathMatcherUtil(null);

		assertTrue(o.match("asdjflaskdf/asdkfmasdlfk/asdklfmasdlf/SomeEntity.class"));
		assertTrue(o.match("asdjflaskdf/asdkfmasdlfk/asdklfmasdlf/SomeEntity.Refclass"));
		assertTrue(o.match("asdjflaskdf/asdkfmasdlfk/asdklfmasdlf/ThisEntityIsNot.class"));
		assertTrue(o.match("asdjflaskdf/asdkfmasdlfk/asdklfmasdlf/SomethingElse.class"));
}
}
