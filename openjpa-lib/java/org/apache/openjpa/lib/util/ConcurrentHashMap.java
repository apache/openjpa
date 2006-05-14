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


/*
 * @author Copyright (c) 1997 by WebLogic, Inc. All Rights Reserved.
 */
import java.io.*;
import java.util.*;


/** This class implements a HashMap which has limited synchronization.
  * In particular mutators are generally synchronized while accessors
  * are generally not.  Additionally the Iterators returned by this
  * class are not "fail-fast", but instead try to continue to iterate
  * over the data structure after changes have been made.
  *
  * The synchronization semantics are built right in to the
  * implementation rather than using a delegating wrapper like the
  * other collection classes do because it wasn't clear to me that the
  * how the two should be seperated or that it would be useful to do
  * so.  This can probably be a topic for further debate in the
  * future.
  *
  * This class is based heavily on the HashMap class in the Java
  * collections package. */
public class ConcurrentHashMap extends AbstractMap
	implements Map, Cloneable, Serializable 
{
	private static Localizer _loc = Localizer.forPackage
		(ConcurrentHashMap.class);

	/**
	 * The default initial capacity - MUST be a power of two.
	 */
	private static final int DEFAULT_INITIAL_CAPACITY = 16;

	/**
	 * The maximum capacity, used if a higher value is implicitly specified
	 * by either of the constructors with arguments.
	 * MUST be a power of two <= 1<<30.
	 */
	private static final int MAXIMUM_CAPACITY = 1 << 30;

	/**
	 * The load fast used when none specified in constructor.
	 **/
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	/**
	 * Value representing null keys inside tables.
	 */
	private static final Object NULL_KEY = new Object ();

	/**
	 * The table, resized as necessary. Length MUST Always be a power of two.
	 */
	private transient Entry[] table;

	/**
	 * The number of key-value mappings contained in this identity hash map.
	 */
	private transient int size;

	/**
	 * The next size value at which to resize (capacity * load factor).
	 * @serial
	 */
	private int threshold;

	/**
	 * The load factor for the hash table.
	 *
	 * @serial
	 */
	private final float loadFactor;

	/**
	 * Constructs an empty <tt>ConcurrentHashMap</tt> with the specified initial
	 * capacity and load factor.
	 *
	 * @param	initialCapacity The initial capacity.
	 * @param	loadFactor			The load factor.
	 * @throws IllegalArgumentException if the initial capacity is negative
	 *				 or the load factor is nonpositive.
	 */
	public ConcurrentHashMap (int initialCapacity, float loadFactor) 
	{
		if (initialCapacity < 0) 
		{
			throw new IllegalArgumentException (_loc.get ("concurrent-initial",
				initialCapacity + ""));
		}
		if (initialCapacity > MAXIMUM_CAPACITY)
			initialCapacity = MAXIMUM_CAPACITY;
		if (loadFactor <= 0 || loadFactor > 1) 
		{
			throw new IllegalArgumentException (_loc.get ("concurrent-load",
				loadFactor + ""));
		}

		// Find a power of 2 >= initialCapacity
		int capacity = 1;
		while (capacity < initialCapacity) capacity <<= 1;

		this.loadFactor = loadFactor;
		threshold = (int) (capacity * loadFactor);
		table = new Entry[capacity];
	}


	/**
	 * Constructs an empty <tt>ConcurrentHashMap</tt> with the specified initial
	 * capacity and the default load factor (0.75).
	 *
	 * @param	initialCapacity the initial capacity.
	 * @throws IllegalArgumentException if the initial capacity is negative.
	 */
	public ConcurrentHashMap (int initialCapacity) 
	{
		this (initialCapacity, DEFAULT_LOAD_FACTOR);
	}


	/**
	 * Constructs an empty <tt>ConcurrentHashMap</tt> with the default initial
	 * capacity (16) and the default load factor (0.75).
	 */
	public ConcurrentHashMap () 
	{
		this (DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
	}


	/**
	 * Constructs a new <tt>ConcurrentHashMap</tt> with the same mappings as the
	 * specified <tt>Map</tt>.	The <tt>ConcurrentHashMap</tt> is created with
	 * default load factor (0.75) and an initial capacity sufficient to
	 * hold the mappings in the specified <tt>Map</tt>.
	 *
	 * @param	 m the map whose mappings are to be placed in this map.
	 * @throws	NullPointerException if the specified map is null.
	 */
	public ConcurrentHashMap (Map m) 
	{
		this (Math.max ( (int) (m.size () / DEFAULT_LOAD_FACTOR) + 1,
			DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
		putAll (m);
	}


	// internal utilities


	/**
	 * Returns internal representation for key. Use NULL_KEY if key is null.
	 */
	private static Object maskNull (Object key) 
	{
		return (key == null ? NULL_KEY : key);
	}


	/**
	 * Returns key represented by specified internal representation.
	 */
	private static Object unmaskNull (Object key) 
	{
		return (key == NULL_KEY ? null : key);
	}


	/**
	 * Returns a hash code for non-null Object x.
	 */
	private static int hash (Object x) 
	{
		int h = x.hashCode ();
		return h - (h << 7);	// i.e., -127 * h
	}


	/**
	 * Check for equality of non-null reference x and possibly-null y.
	 */
	private static boolean eq (Object x, Object y) 
	{
		return x == y || x.equals (y);
	}


	/**
	 * Returns the current capacity of backing table in this map.
	 *
	 * @return the current capacity of backing table in this map.
	 */
	public final int capacity () 
	{
		return table.length;
	}
	
	/**
	 * Returns the load factor for this map.
	 *
	 * @return the load factor for this map.
	 */
	public final float loadFactor () 
	{
		return loadFactor;
	}
	
	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map.
	 */
	public final int size () 
	{
		return size;
	}


	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 *
	 * @return <tt>true</tt> if this map contains no key-value mappings.
	 */
	public final boolean isEmpty () 
	{
		return size == 0;
	}


	/**
	 * Returns the value to which the specified key is mapped in this identity
	 * hash map, or <tt>null</tt> if the map contains no mapping for this key.
	 * A return value of <tt>null</tt> does not <i>necessarily</i> indicate
	 * that the map contains no mapping for the key; it is also possible that
	 * the map explicitly maps the key to <tt>null</tt>. The
	 * <tt>containsKey</tt> method may be used to distinguish these two cases.
	 *
	 * @param	 key the key whose associated value is to be returned.
	 * @return	the value to which this map maps the specified key, or
	 *					<tt>null</tt> if the map contains no mapping for this 
						key.
	 * @see #put (Object, Object)
	 */
	public Object get (Object key) 
	{
		Entry e = getEntry (key);
		return e == null? null: e.value;
	}


	/**
	 * Returns <tt>true</tt> if this map contains a mapping for the
	 * specified key.
	 *
	 * @param	 key	 The key whose presence in this map is to be tested
	 * @return <tt>true</tt> if this map contains a mapping for the specified
	 * key.
	 */
	public final boolean containsKey (Object key) 
	{
		return getEntry (key) != null;
	}


	/**
	 * Returns the entry associated with the specified key in the
	 * ConcurrentHashMap.	Returns null if the ConcurrentHashMap contains no
	 * mapping for this key.
	 */
	protected Entry getEntry (Object key) 
	{
		Object k = maskNull (key);
		int hash = hash (k);
		Entry[] tab = table;
		for (Entry e = tab[hash & (tab.length-1)]; e != null; e = e.next) 
		{
			if (e.hash == hash && eq (k, e.key)) return e;
		}
		return null;
	}


	/**
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for this key, the old
	 * value is replaced.
	 *
	 * @param key key with which the specified value is to be associated.
	 * @param value value to be associated with the specified key.
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *				 if there was no mapping for key.	A <tt>null</tt> return
	 *				 can also indicate that the ConcurrentHashMap previously
	 *				 associated
	 *				 <tt>null</tt> with the specified key.
	 */
	public Object put (Object key, Object value) 
	{
		Object k = maskNull (key);
		int hash = hash (k);
		synchronized (this) 
	{
			int i = hash & (table.length - 1);

			for (Entry e = table[i]; e != null; e = e.next) 
			{
				if (e.hash == hash && eq (k, e.key)) 
				{
					Object oldValue = e.value;
					e.value = value;
					return oldValue;
				}
			}

			table[i] = createEntry (hash, k, value, table[i]);
			if (size++ >= threshold) resize (2 * table.length);
		}
		return null;
	}


	public Object putIfAbsent (Object key, Object value) 
	{
		Object k = maskNull (key);
		int hash = hash (k);
		synchronized (this) 
		{
			int i = hash & (table.length - 1);

			for (Entry e = table[i]; e != null; e = e.next) 
			{
				if (e.hash == hash && eq (k, e.key)) 
				{
					return e.value;
				}
			}

			table[i] = createEntry (hash, k, value, table[i]);
			if (size++ >= threshold) resize (2 * table.length);
		}
		return null;
	}


	/**
	 * Rehashes the contents of this map into a new <tt>ConcurrentHashMap</tt>
	 * instance with a larger capacity. This method is called automatically when
	 * the number of keys in this map exceeds its capacity and load factor.
	 *
	 * @param newCapacity the new capacity, MUST be a power of two.
	 */
	private void resize (int newCapacity) 
	{
		// assert (newCapacity & -newCapacity) == newCapacity; // power of 2
		Entry[] oldTable = table;
		int oldCapacity = oldTable.length;

		// check if needed
		if (size < threshold || oldCapacity > newCapacity) return;

		Entry[] newTable = new Entry[newCapacity];
		int mask = newCapacity-1;
		for (int i = oldCapacity; i-- > 0; ) 
		{
			for (Entry e = oldTable[i]; e != null; e = e.next) 
			{
				Entry clone = (Entry) e.clone ();
				int j = clone.hash & mask;
				clone.next = newTable[j];
				newTable[j] = clone;
			}
		}
		table = newTable;
		threshold = (int) (newCapacity * loadFactor);
	}


	/**
	 * Copies all of the mappings from the specified map to this map
	 * These mappings will replace any mappings that
	 * this map had for any of the keys currently in the specified map.
	 *
	 * @param t mappings to be stored in this map.
	 * @throws NullPointerException if the specified map is null.
	 */
	public final synchronized void putAll (Map t) 
	{
		// Expand enough to hold t's elements without resizing.
		int n = t.size ();
		if (n == 0) return;
		if (n >= threshold) 
		{
			n = (int) (n / loadFactor + 1);
			if (n > MAXIMUM_CAPACITY) n = MAXIMUM_CAPACITY;
			int capacity = table.length;
			while (capacity < n) capacity <<= 1;
			resize (capacity);
		}

		for (Iterator i = t.entrySet ().iterator (); i.hasNext (); ) 
		{
			Map.Entry e = (Map.Entry) i.next ();
			put (e.getKey (), e.getValue ());
		}
	}


	/**
	 * Removes the mapping for this key from this map if present.
	 *
	 * @param	key key whose mapping is to be removed from the map.
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *				 if there was no mapping for key.	A <tt>null</tt> return
	 *				 can also indicate that the map previously associated
	 *				 <tt>null</tt> with the specified key.
	 */
	public Object remove (Object key) 
	{
		Entry e = removeEntryForKey (key, null);
		return (e == null ? e : e.value);
	}


	/**
	 * Removes the mapping for this key from this map if present and value
	 * equals the parameter value. If parameter value is null, behaves
	 * exactly like <code>remove (Object key)</code>.
	 *
	 * @param	key key whose mapping is to be removed from the map.
	 * @param	value value that is mapped to this key.
	 * @return <tt>true</tt> if the entry was removed, or <tt>false</tt>
	 *				 if there was no mapping for key or the key is not mapped to
	 *					 the parameter value.
	 */
	public boolean remove (Object key, Object value) 
	{
		Entry e = removeEntryForKey (key, value);
		return (e == null ? false : true);
	}


	/**
	 * Removes and returns the entry associated with the specified key and value
	 * in the ConcurrentHashMap. If value is null, only matches the key.
	 * Returns null if the ConcurrentHashMap contains no mapping for this key or
	 * key is not mapped to the input value.
	 */
	private Entry removeEntryForKey (Object key, Object v) 
	{
		Object k = maskNull (key);
		int hash = hash (k);
		synchronized (this) 
		{
			int i = hash & (table.length - 1);
			Entry e = table[i];

			if (e == null) return null;
			if (e.hash == hash && eq (k, e.key) && 
				(v == null || eq (v, e.value))) 
			{
				size--;
				table[i] = e.next;
				return e;
			}

			Entry prev = e;
			for (e = e.next; e != null; prev = e, e = e.next) 
			{
				if (e.hash == hash && eq (k, e.key) && 
					(v == null || eq (v, e.value))) 
				{
					size--;
					prev.next = e.next;
					return e;
				}
			}
		}
		return null;
	}

	/**
	 * Special version of remove for EntrySet.
	 */
	private Entry removeMapping (Object o) 
	{
		if (! (o instanceof Map.Entry)) return null;

		Map.Entry entry = (Map.Entry) o;
		Object k = maskNull (entry.getKey ());
		int hash = hash (k);
		synchronized (this) 
		{
			int i = hash & (table.length - 1);
			Entry e = table[i];

			if (e == null) return null;
			if (e.hash == hash && e.equals (entry)) 
			{
				size--;
				table[i] = e.next;
				return e;
			}

			Entry prev = e;
			for (e = e.next; e != null; prev = e, e = e.next) 
			{
				if (e.hash == hash && e.equals (entry)) 
				{
					size--;
					prev.next = e.next;
					return e;
				}
			}
		}
		return null;
	}


	/**
	 * Removes all mappings from this map.
	 */
	public synchronized void clear () 
	{
		table = new Entry[table.length];
		size = 0;
	}


	/**
	 * Returns <tt>true</tt> if this map maps one or more keys to the
	 * specified value.
	 *
	 * @param value value whose presence in this map is to be tested.
	 * @return <tt>true</tt> if this map maps one or more keys to the
	 *				 specified value.
	 */
	public final boolean containsValue (Object value) 
	{
		if (value == null) return containsNullValue ();

		Entry tab[] = table;
		for (int i = 0; i < tab.length ; i++) 
		{
			for (Entry e = tab[i] ; e != null ; e = e.next) 
			{
				if (value.equals (e.value)) return true;
			}
		}
		return false;
	}


	/**
	 * Special-case code for containsValue with null argument
	 **/
	private boolean containsNullValue () 
	{
		Entry tab[] = table;
		for (int i = 0; i < tab.length ; i++) 
		{
			for (Entry e = tab[i] ; e != null ; e = e.next) 
			{
				if (e.value == null) return true;
			}
		}
		return false;
	}


	/**
	 * Returns a shallow copy of this <tt>ConcurrentHashMap</tt> instance: the
	 * keys and values themselves are not cloned.
	 *
	 * @return a shallow copy of this map.
	 */
	public final Object clone () 
	{
		return new ConcurrentHashMap (this);
	}


	protected Entry createEntry (int h, Object k, Object v, Entry n) 
	{
		return new Entry (h, k, v, n);
	}


	protected static class Entry implements Map.Entry 
	{

		final Object key;
		Object value;
		final int hash;
		Entry next;

		/**
		 * Create new entry.
		 */
		protected Entry (int h, Object k, Object v, Entry n) 
		{
			value = v;
			next = n;
			key = k;
			hash = h;
		}


		public Object getKey () 
		{
			return unmaskNull (key);
		}


		public Object getValue () 
		{
			return value;
		}


		public Object setValue (Object newValue) 
		{
			Object oldValue = value;
			value = newValue;
			return oldValue;
		}


		public boolean equals (Object o) 
		{
			if (! (o instanceof Map.Entry)) return false;
			Map.Entry e = (Map.Entry) o;
			Object k1 = getKey ();
			Object k2 = e.getKey ();
			if (k1 == k2 || (k1 != null && k1.equals (k2))) 
			{
				Object v1 = getValue ();
				Object v2 = e.getValue ();
				if (v1 == v2 || (v1 != null && v1.equals (v2)))
					return true;
			}
			return false;
		}


		public int hashCode () 
		{
			return (key==NULL_KEY ? 0 : key.hashCode ()) ^
				 (value==null	 ? 0 : value.hashCode ());
		}


		protected Object clone () 
		{
			// It is the callers responsibility to set the next field
			// correctly.
			return new Entry (hash, key, value, null);
		}
	}


	private abstract class HashIterator implements Iterator 
	{
		final Entry[] table = ConcurrentHashMap.this.table;
		Entry next;									// next entry to return
		int index;									 // current slot
		Entry current;							 // current entry

		HashIterator () 
		{
			if (size == 0) return;
			Entry[] t = table;
			int i = t.length-1;
			Entry n = t[i];
			while (n == null && i > 0) n = t[--i];
			index = i;
			next = n;
		}


		public final boolean hasNext () 
		{
			return next != null;
		}


		final Entry nextEntry () 
		{
			Entry e = next;
			if (e == null) throw new NoSuchElementException ();

			Entry n = e.next;
			Entry[] t = table;
			int i = index;
			while (n == null && i > 0) n = t[--i];
			index = i;
			next = n;
			return current = e;
		}


		public final void remove () 
		{
			if (current == null) throw new IllegalStateException ();
			Object k = current.key;
			current = null;
			ConcurrentHashMap.this.removeEntryForKey (k, null);
		}
	}


	private final class ValueIterator extends HashIterator 
	{
		public Object next () 
		{
			return nextEntry ().value;
		}
	}


	private final class KeyIterator extends HashIterator 
	{
		public Object next () 
		{
			return nextEntry ().getKey ();
		}
	}


	private final class EntryIterator extends HashIterator 
	{
		public Object next () 
		{
			return nextEntry ();
		}
	}


	// Views

	private transient Set entrySet = null;
	private transient Set keySet = null;
	private transient Collection values = null;

	/**
	 * Returns a set view of the keys contained in this map.	The set is
	 * backed by the map, so changes to the map are reflected in the set, and
	 * vice-versa.	The set supports element removal, which removes the
	 * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
	 * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
	 * <tt>clear</tt> operations.	It does not support the <tt>add</tt> or
	 * <tt>addAll</tt> operations.
	 *
	 * @return a set view of the keys contained in this map.
	 */
	public final Set keySet () 
	{
		Set ks = keySet;
		return (ks != null ? ks : (keySet = new KeySet ()));
	}


	private final class KeySet extends AbstractSet 
	{
		public Iterator iterator () 
		{
			return new KeyIterator ();
		}


		public int size () 
		{
			return size;
		}


		public boolean contains (Object o) 
		{
			return containsKey (o);
		}


		public boolean remove (Object o) 
		{
			return ConcurrentHashMap.this.removeEntryForKey (o, null) != null;
		}


		public void clear () 
		{
			ConcurrentHashMap.this.clear ();
		}
	}


	/**
	 * Returns a collection view of the values contained in this map.	The
	 * collection is backed by the map, so changes to the map are reflected in
	 * the collection, and vice-versa.	The collection supports element
	 * removal, which removes the corresponding mapping from this map, via the
	 * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
	 * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a collection view of the values contained in this map.
	 */
	public final Collection values () 
	{
		Collection vs = values;
		return (vs != null ? vs : (values = new Values ()));
	}


	private final class Values extends AbstractCollection 
	{
		public Iterator iterator () 
		{
			return new ValueIterator ();
		}
		public int size () 
		{
			return size;
		}
		public boolean contains (Object o) 
		{
			return containsValue (o);
		}
		public void clear () 
		{
			ConcurrentHashMap.this.clear ();
		}
	}


	/**
	 * Returns a collection view of the mappings contained in this map.	Each
	 * element in the returned collection is a <tt>Map.Entry</tt>.	The
	 * collection is backed by the map, so changes to the map are reflected in
	 * the collection, and vice-versa.	The collection supports element
	 * removal, which removes the corresponding mapping from the map, via the
	 * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
	 * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a collection view of the mappings contained in this map.
	 * @see Map.Entry
	 */
	public final Set entrySet () 
	{
		Set es = entrySet;
		return (es != null ? es : (entrySet = new EntrySet ()));
	}


	private final class EntrySet extends AbstractSet 
	{
		public Iterator iterator () 
		{
			return new EntryIterator ();
		}
		public boolean contains (Object o) 
		{
			if (! (o instanceof Map.Entry)) return false;
			Map.Entry e = (Map.Entry) o;
			Entry candidate = getEntry (e.getKey ());
			return candidate != null && candidate.equals (e);
		}
		public boolean remove (Object o) 
		{
			return removeMapping (o) != null;
		}
		public int size () 
		{
			return size;
		}
		public void clear () 
		{
			ConcurrentHashMap.this.clear ();
		}
	}


	/**
	 * Save the state of the <tt>ConcurrentHashMap</tt> instance to a stream
	 * (i.e., serialize it).
	 *
	 * @serialData The <i>capacity</i> of the ConcurrentHashMap (the length of
	 * the bucket array) is emitted (int), followed by the <i>size</i> of the
	 * ConcurrentHashMap (the number of key-value mappings), followed by the key
	 * (Object) and value (Object) for each key-value mapping represented by the
	 * ConcurrentHashMap The key-value mappings are emitted in the order that
	 * they are returned by <tt>entrySet ().iterator ()</tt>.
	 *
	 */
	private void writeObject (ObjectOutputStream s)
		throws IOException
	
	{
		// Write out the threshold, loadfactor, and any hidden stuff
		s.defaultWriteObject ();

		// Write out number of buckets
		s.writeInt (table.length);

		// Write out size (number of Mappings)
		s.writeInt (size);

		// Write out keys and values (alternating)
		for (Iterator i = entrySet ().iterator (); i.hasNext (); ) 
		{
			Map.Entry e = (Map.Entry) i.next ();
			s.writeObject (e.getKey ());
			s.writeObject (e.getValue ());
		}
	}


	private static final long serialVersionUID = -6452706556724125778L;

	/**
	 * Reconstitute the <tt>ConcurrentHashMap</tt> instance from a stream (i.e.,
	 * deserialize it).
	 */
	private void readObject (ObjectInputStream s)
		throws IOException, ClassNotFoundException
	
	{
		// Read in the threshold, loadfactor, and any hidden stuff
		s.defaultReadObject ();

		// Read in number of buckets and allocate the bucket array;
		int numBuckets = s.readInt ();
		table = new Entry[numBuckets];

		// Read in size (number of Mappings)
		int size = s.readInt ();

		// Read the keys and values, and put the mappings in the 
		// ConcurrentHashMap
		for (int i=0; i<size; i++) 
		{
			Object key = s.readObject ();
			Object value = s.readObject ();
			put (key, value);
		}
	}


}
