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
package org.apache.openjpa.lib.conf;


/**
 *	An int {@link Value}.
 *
 *	@author	Marc Prud'hommeaux
 */
public class IntValue
	extends Value
{
 	private int value;


	public IntValue (String prop)
	{
		super (prop);
	}


	public Class getValueType ()
	{
		return int.class;
	}


	/**
	 *	The internal value.
	 */
	public void set (int value)
	{
		int oldValue = this.value;
		this.value = value;
		if (value != oldValue)
			valueChanged ();
	}


	/**
	 *	The internal value.
	 */
	public int get ()
	{
		return this.value;
	}


	protected String getInternalString ()
	{
		return String.valueOf (this.value);
	}


	protected void setInternalString (String val)
	{
		if (val == null || val.length () == 0)
			set (0);
		else
			set (Integer.parseInt (val));
	}


	protected void setInternalObject (Object obj)
	{
		if (obj == null)
			set (0);
		else
			set (((Number) obj).intValue ());
	}
}
