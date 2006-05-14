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
package org.apache.openjpa.lib.util;


import java.util.*;

import org.apache.commons.collections.map.*;


/**
 *	<p>Reference map that is very efficient when it knows that it contains
 *	no entries, such as after being cleared.</p>
 *
 *	@author		Abe White
 *	@nojavadoc
 */
public class EfficientEmptyReferenceMap
	extends ReferenceMap
{
	private boolean _empty = true;


	/**
	 *	Default constructor.
	 */
	public EfficientEmptyReferenceMap ()
	{
		super ();
	}


	/**
	 *	Allow specification of reference type to use for keys and values.
	 */
	public EfficientEmptyReferenceMap (int keyRef, int valRef)
	{
		super (keyRef, valRef);
	}


	public Object put (Object key, Object val)
	{
		_empty = false;
		return super.put (key, val);
	}


	public void putAll (Map map)
	{
		_empty = false;
		super.putAll (map);
	}


	public Object get (Object key)
	{
		if (_empty)
			return null;
		return super.get (key);
	}


	public Object remove (Object key)
	{
		if (_empty)
			return null;
		return super.remove (key);
	}


	public Set keySet ()
	{
		if (_empty)
			return Collections.EMPTY_SET;
		return super.keySet ();
	}


	public Collection values ()
	{
		if (_empty)
			return Collections.EMPTY_SET;
		return super.values ();
	}


	public Set entrySet ()
	{
		if (_empty)
			return Collections.EMPTY_SET;
		return super.entrySet ();
	}


	public void clear ()
	{
		super.clear ();
		_empty = true;
	}


	public int size ()
	{
		if (_empty)
			return 0;
		return super.size ();
	}


	public boolean isEmpty ()
	{
		if (_empty)
			return true;
		return super.isEmpty ();
	}
}
