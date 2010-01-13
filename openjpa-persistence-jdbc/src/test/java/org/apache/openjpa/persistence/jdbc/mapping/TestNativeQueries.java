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
package org.apache.openjpa.persistence.jdbc.mapping;


import javax.persistence.*;
import java.util.*;

import org.apache.openjpa.persistence.jdbc.common.apps.mappingApp.*;
import org.apache.openjpa.persistence.common.utils.*;
import junit.framework.*;


public class TestNativeQueries	extends AbstractTestCase
{
	
	public TestNativeQueries(String name)
	{
		super(name, "jdbccactusapp");
	}

    public void setUp ()
	{
		deleteAll (Entity1.class);
	}

	public void testSimple ()
	{
		deleteAll (Entity1.class);

		// test create
		{
			EntityManager em = currentEntityManager( );
			startTx(em);
			em.persist (new Entity1 (0, "testSimple", 12));
			endTx(em);
			endEm(em);
		}

		// test Query
		{
/*			JDBCConfiguration conf = (JDBCConfiguration)getConfiguration ();
			DBDictionary dict = conf.getDBDictionaryInstance ();*/

/*			String tableName = dict.getFullName (conf.getMappingRepository ().
				getMapping (Entity1.class, getClass ().getClassLoader (), true).
				getTable (), false);*/

			EntityManager em = currentEntityManager( );
			startTx(em);
			String tableName = "entity_1";
			assertSize (1, em.createNativeQuery
				("SELECT * FROM " + tableName, Entity1.class).
				getResultList ());
			assertSize (1, em.createNativeQuery ("SELECT * FROM " + tableName
				+ " WHERE INTFIELD = 12", Entity1.class).
				getResultList ());

			assertSize (1, em.createNativeQuery ("SELECT * FROM " + tableName
				+ " WHERE INTFIELD = ?1", Entity1.class).
				setParameter (1, 12).
				getResultList ());

			// make sure that out-of-order parameters work
			assertSize (1, em.createNativeQuery ("SELECT * FROM " + tableName
				+ " WHERE INTFIELD = ?2 AND STRINGFIELD = ?1", Entity1.class).
				setParameter (2, 12).
				setParameter (1, "testSimple").
				getResultList ());

			// make sure duplicate parameters work
			assertSize (1, em.createNativeQuery ("SELECT * FROM " + tableName
				+ " WHERE INTFIELD = ?1 AND INTFIELD = ?1", Entity1.class).
				setParameter (1, 12).
				getResultList ());

			assertSize (1, em.createNativeQuery ("SELECT * FROM " + tableName
				+ " WHERE INTFIELD = ?1 OR INTFIELD = ?2", Entity1.class).
				setParameter (1, 12).
				setParameter (2, 13).
				getResultList ());

			// make sure that quoted parameters are ignored as expected
			assertSize (1, em.createNativeQuery ("SELECT * FROM " + tableName
				+ " WHERE INTFIELD = ?1 OR STRINGFIELD = '?5'", Entity1.class).
				setParameter (1, 12).
				getResultList ());

			// test without spaces
			assertSize (1, em.createNativeQuery ("SELECT * FROM " + tableName
				+ " WHERE INTFIELD=?1 OR STRINGFIELD='?5'", Entity1.class).
				setParameter (1, 12).
				getResultList ());

/*			assertSize (1, ((QueryImpl)em.createNativeQuery
				("SELECT * FROM " + tableName
					+ " WHERE INTFIELD = ?1 OR INTFIELD = ?2", Entity1.class)).
				setParameters (12, 1).
				getResultList ());

			assertSize (0, ((QueryImpl)em.createNativeQuery
				("SELECT * FROM " + tableName
					+ " WHERE INTFIELD = ?1 AND INTFIELD = ?2", Entity1.class)).
				setParameters (12, 1).
				getResultList ());
*/
			assertSize (0, em.createNativeQuery ("SELECT * FROM " + tableName
				+ " WHERE INTFIELD = ?1 AND INTFIELD = ?2", Entity1.class).
				setParameter (1, 12).
				setParameter (2, 13).
				getResultList ());

			try
			{
				em.createNativeQuery ("SELECT * FROM " + tableName
					+ " WHERE INTFIELD = ?1", Entity1.class).
					setParameter (0, 12).
					getResultList ();
				fail ("Should not have been able to use param index 0");
			}
			catch (Exception e)
			{
				// as expected
			}


			/*
			 * Named parameters are not supported according to 19 June 3.5.2:
			 *
			 * The use of named parameters is not defined for
			 * native queries. Only positional parameter binding
			 * for SQL queries may be used by portable applications.
			 *
			assertSize (1, em.createNativeQuery ("SELECT * FROM " + tableName
				+ " WHERE INTFIELD = :p",  Entity1.class).
				setParameter ("p", 12).
				getResultList ());
			assertSize (1, em.createNativeQuery ("SELECT * FROM " + tableName
				+ " WHERE INTFIELD = :p OR INTFIELD = :p", Entity1.class).
				setParameter ("p", 12).
				getResultList ());
			*/

			endTx(em);
			endEm(em);
		}
	}
	
	public boolean assertSize(int num, List l)
	{
		return(num == l.size());
	}
}

