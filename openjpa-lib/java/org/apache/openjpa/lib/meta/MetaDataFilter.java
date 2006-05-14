/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.meta;


import java.io.*;


/**
 *	<p>Filters metadata resources.  Typically used to constrain the results
 *	of a {@link MetaDataIterator}.</p>
 *
 *	@author		Abe White
 *	@nojavadoc
 */
public interface MetaDataFilter
{
	/**
	 *	Return whether the given resource passes the filter.
	 */
	public boolean matches (Resource rsrc)
		throws IOException;


	/**
	 *	Information about a metadata resource.
	 */
	public static interface Resource
	{
		/**
		 *	The name of the resource.
	 	 */
		public String getName ();


		/**
	 	 *	Resource content.
		 */
		public byte[] getContent ()
			throws IOException;
	}
}
