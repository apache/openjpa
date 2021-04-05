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
package org.apache.openjpa.lib.rop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.apache.openjpa.lib.test.AbstractTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link ResultList} implementations.
 *
 * @author Abe White
 */
public abstract class ResultListTest extends AbstractTestCase {

    private ResultList[] _lists = null;

    protected boolean subListSupported = false;


    public ResultListTest() {
        this(false);
    }

    public ResultListTest(boolean supportSubList) {
        subListSupported = supportSubList;
    }

    /**
     * Return a result list to use with the given provider.
     */
    protected abstract ResultList getResultList(ResultObjectProvider provider);

    /**
     * Override to customize the result object provider being used. You
     * can return multiple providers to test with each.
     */
    protected ResultObjectProvider[] getResultObjectProviders(List list) {
        return new ResultObjectProvider[]{
            new ListResultObjectProvider(list)
        };
    }

    @Before
    public void setUp() {
        List results = new ArrayList(100);
        for (int i = 0; i < 100; i++)
            results.add(String.valueOf(i));
        ResultObjectProvider[] rops = getResultObjectProviders(results);
        _lists = new ResultList[rops.length];
        for (int i = 0; i < _lists.length; i++)
            _lists[i] = getResultList(rops[i]);
    }

    @Test
    public void testIterator() {
        for (ResultList list : _lists) {
            Iterator itr = list.iterator();
            int count = 0;
            for (; itr.hasNext(); count++)
                assertEquals(String.valueOf(count), itr.next());
            assertEquals(100, count);
            try {
                itr.next();
                fail("After last.");
            }
            catch (IndexOutOfBoundsException | NoSuchElementException ioob) {
            }
        }
    }

    @Test
    public void testIteratorModification() {
        for (ResultList list : _lists) {
            try {
                Iterator itr = list.iterator();
                itr.next();
                itr.remove();
                fail("Allowed modification.");
            }
            catch (Exception e) {
            }
        }
    }

    @Test
    public void testListIteratorForward() {
        for (ResultList list : _lists) {
            ListIterator itr = list.listIterator();
            int count = 0;
            for (; itr.hasNext(); count++) {
                assertEquals(count, itr.nextIndex());
                assertEquals(String.valueOf(count), itr.next());
            }
            assertEquals(100, count);
            try {
                itr.next();
                fail("After last.");
            }
            catch (IndexOutOfBoundsException | NoSuchElementException ioob) {
            }
        }
    }

    @Test
    public void testListIteratorIndex() {
        for (ResultList list : _lists) {
            ListIterator itr = list.listIterator(50);
            int count = 50;
            for (; itr.hasNext(); count++) {
                assertEquals(count, itr.nextIndex());
                assertEquals(String.valueOf(count), itr.next());
            }
            assertEquals(100, count);
            try {
                itr.next();
                fail("After last.");
            }
            catch (IndexOutOfBoundsException | NoSuchElementException ioob) {
            }
        }
    }

    @Test
    public void testListIteratorReverse() {
        for (ResultList list : _lists) {
            ListIterator itr = list.listIterator(100);
            int count = 99;
            for (; itr.hasPrevious(); count--) {
                assertEquals(count, itr.previousIndex());
                assertEquals(String.valueOf(count), itr.previous());
            }
            assertEquals(-1, count);
            try {
                itr.previous();
                fail("Before first.");
            }
            catch (IndexOutOfBoundsException | NoSuchElementException ioob) {
            }
        }
    }

    @Test
    public void testListIteratorModification() {
        for (ResultList list : _lists) {
            try {
                ListIterator itr = list.listIterator();
                itr.next();
                itr.set("foo");
                fail("Allowed modification.");
            }
            catch (Exception e) {
            }
        }
    }

    @Test
    public void testMultipleIterations() {
        testListIteratorIndex();
        testListIteratorForward();
        testListIteratorReverse();
    }

    @Test
    public void testContains() {
        for (ResultList list : _lists) {
            assertTrue(list.contains("0"));
            assertTrue(list.contains("50"));
            assertTrue(list.contains("99"));
            assertFalse(list.contains("-1"));
            assertFalse(list.contains("100"));
            assertFalse(list.contains(null));
            assertTrue(list.containsAll(Arrays.asList(new String[]
                    {"0", "50", "99"})));
            assertFalse(list.containsAll(Arrays.asList(new String[]
                    {"0", "-1", "99"})));
        }
    }

    @Test
    public void testModification() {
        for (ResultList list : _lists) {
            try {
                list.add("foo");
                fail("Allowed modification.");
            }
            catch (UnsupportedOperationException uoe) {
            }
            try {
                list.remove("1");
                fail("Allowed modification.");
            }
            catch (UnsupportedOperationException uoe) {
            }
            try {
                list.set(0, "foo");
                fail("Allowed modification.");
            }
            catch (UnsupportedOperationException uoe) {
            }
        }
    }

    @Test
    public void testGetBegin() {
        for (ResultList list : _lists) {
            for (int j = 0; j < 10; j++)
                assertEquals(String.valueOf(j), list.get(j));
            try {
                list.get(-1);
                fail("Before begin.");
            }
            catch (IndexOutOfBoundsException | NoSuchElementException ioob) {
            }
        }
    }

    @Test
    public void testGetMiddle() {
        for (ResultList list : _lists)
            for (int j = 50; j < 60; j++)
                assertEquals(String.valueOf(j), list.get(j));
    }

    @Test
    public void testGetEnd() {
        for (ResultList list : _lists) {
            for (int j = 90; j < 100; j++)
                assertEquals(String.valueOf(j), list.get(j));
            try {
                list.get(100);
                fail("Past end.");
            }
            catch (IndexOutOfBoundsException | NoSuchElementException ioob) {
            }
        }
    }

    @Test
    public void testGetReverse() {
        for (ResultList list : _lists)
            for (int j = 99; j > -1; j--)
                assertEquals(String.valueOf(j), list.get(j));
    }

    @Test
    public void testMultipleGet() {
        testGetMiddle();
        testGetBegin();
        testGetEnd();

        // take list size and traverse list to cache values if not already
        for (ResultList list : _lists) list.size();
        testListIteratorForward();

        testGetMiddle();
        testGetBegin();
        testGetEnd();
    }

    @Test
    public void testSize() {
        for (ResultList list : _lists)
            assertTrue(list.size() == 100
                    || list.size() == Integer.MAX_VALUE);
    }

    @Test
    public void testEmpty() {
        ResultObjectProvider[] rops = getResultObjectProviders
            (Collections.EMPTY_LIST);
        for (ResultObjectProvider rop : rops) {
            ResultList list = getResultList(rop);
            assertEquals(0, list.size());
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testSubList() {
        ResultObjectProvider[] rops = getResultObjectProviders
            (Collections.EMPTY_LIST);
        for (ResultObjectProvider rop : rops) {
            ResultList list = getResultList(rop);
            try {
                List subList = list.subList(0, 0);
                if (!subListSupported)
                    fail("Should not support subList.");
            }
            catch (UnsupportedOperationException e) {
                if (subListSupported)
                    fail("Should support subList.");
            }
        }
    }
}
