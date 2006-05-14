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


import java.lang.ref.*;

import org.apache.commons.collections.map.*;


/**
 *	<p>Reference map that provides overridable methods to take action when
 *	a key or value expires, assuming the other member of the map entry uses
 *	a hard reference.</p>
 *
 *	@author		Abe White
 *	@since		4.0
 *	@nojavadoc
 */
public class ExpirationNotifyingReferenceMap
	extends ReferenceMap
{
	public ExpirationNotifyingReferenceMap ()
	{
	}


	public ExpirationNotifyingReferenceMap (int keyType, int valueType)
	{
		super (keyType, valueType);
	}


	public ExpirationNotifyingReferenceMap (int keyType, int valueType,
		int capacity, float loadFactor)
	{
		super (keyType, valueType, capacity, loadFactor);
	}


	/**
	 *	Notification that the value for the given key has expired.
	 */
	protected void valueExpired (Object key)
	{
	}

	
	/**
	 *	Notification that the key for the given value has expired.
	 */
	protected void keyExpired (Object value)
	{
	}


	/**
	 *	Remove expired references.
	 */
	public void removeExpired ()
	{
		purge ();
	}


	protected HashEntry createEntry (HashEntry next, int hashCode, Object key,
		Object value)
	{
		return new AccessibleEntry (this, next, hashCode, key, value);
	}


	protected void purge (Reference ref)
	{
		// the logic for this method is taken from the original purge method
		// we're overriding, with added logic to track the expired key/value
		int index = hashIndex (ref.hashCode (), data.length);
		AccessibleEntry entry = (AccessibleEntry) data[index];
		AccessibleEntry prev = null;
		Object key = null, value = null;
		while (entry != null)
		{
			if (purge (entry, ref))
			{
				if (keyType == HARD)
					key = entry.key ();
				else if (valueType == HARD)
					value = entry.value ();

				if (prev == null)
					data[index] = entry.nextEntry ();
				else
					prev.setNextEntry (entry.nextEntry ());
				size--;
				break;
			}
			prev = entry;
			entry = entry.nextEntry ();
		}
		
		if (key != null)
			valueExpired (key);
		else if (value != null)
			keyExpired (value);
	}


	/**
	 *	See the code for <code>ReferenceMap.ReferenceEntry.purge</code>.
	 */
	private boolean purge (AccessibleEntry entry, Reference ref)
	{
		boolean match = (keyType != HARD && entry.key () == ref)
			|| (valueType != HARD && entry.value () == ref);
		if (match)
		{
			if (keyType != HARD)
				((Reference) entry.key ()).clear ();
			if (valueType != HARD)
				((Reference) entry.value ()).clear ();
			else if (purgeValues)
				entry.nullValue ();
		}
		return match;
	}


	/**
	 *	Extension of the base entry type that allows our outer class to access
	 *	protected state.
	 */
	private static class AccessibleEntry
		extends ReferenceEntry
	{
		public AccessibleEntry (AbstractReferenceMap map, HashEntry next,
			int hashCode, Object key, Object value)
		{
			super (map, next, hashCode, key, value);
		}


		public Object key ()
		{
			return key;
		}


		public Object value ()
		{
			return value;
		}


		public void nullValue ()
		{
			value = null;
		}


		public AccessibleEntry nextEntry ()
		{
			return (AccessibleEntry) next;
		}


		public void setNextEntry (AccessibleEntry next)
		{
			this.next = next;
		}
	}
}
