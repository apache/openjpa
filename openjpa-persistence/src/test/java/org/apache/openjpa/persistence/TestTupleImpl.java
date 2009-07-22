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

package org.apache.openjpa.persistence;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import junit.framework.TestCase;

/**
 * Test the following methods in TupleImpl
 * <ul>
 * <li>Tuple</li>
 * <ul>
 * <li>get(int)</li>
 * <li>get(int, Class<X>)</li>
 * <li>get(String)</li>
 * <li>get(String, Class<X>)</li>
 * <li>get(TupleElement<X>)</li>
 * <li>getElements()</li>
 * <li>toArray()</li>
 * </ul>
 * <li>TupleImpl</li>
 * <ul>
 * <li>get(Object)</li>
 * <li>getValues()</li>
 * <li>put(Object, Object)</li>
 * </ul>
 * </ul>
 */
public class TestTupleImpl extends TestCase {
    protected Order _order = new Order();
    protected Product _product = new Product();
    protected Item _item = new Item();
    protected Store _store = new Store();
    protected UrgentOrder _urgentOrder = new UrgentOrder();
    protected Tuple tuple = getTuple();

    public void setUp() {
    }

    /**
     * Default tuple with some arbitrary pseudo entities
     * 
     * @return
     */
    protected TupleImpl getTuple() {
        TupleImpl tuple = new TupleImpl();
        tuple.put("alias1", _order);
        tuple.put("alias2", _product);
        tuple.put("alias3", _item);
        tuple.put("alias4", _store);
        tuple.put("alias5", _urgentOrder);
        return tuple;
    }

    public void testGetInt() {
        assertEquals(_order, tuple.get(0));
        assertEquals(_product, tuple.get(1));
        assertEquals(_item, tuple.get(2));
        assertEquals(_store, tuple.get(3));
        assertEquals(_urgentOrder, tuple.get(4));
        // TODO MDD more tests
    }

    public void testGetIntNegativeValueThrowsException() {
        try {
            Object o = tuple.get(-1);
            fail("tuple.get(-1) should throw IllegalArgumentException");
            o.toString(); // clean up meaningless compiler warning
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testGetIntOutOfRangeThrowsException() {
        try {
            Object o = tuple.get(10);
            fail("tuple.get(i) where i > size of TupleElements should throw IllegalArgumentException");
            o.toString(); // clean up meaningless compiler warning
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            Object o = tuple.get(Integer.MAX_VALUE);
            fail("tuple.get(i) where i > size of TupleElements should throw IllegalArgumentException");
            o.toString(); // clean up meaningless compiler warning
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testGetIntClass() {
        assertEquals(_order, tuple.get(0, Order.class));
        assertEquals(_product, tuple.get(1, Product.class));
        assertEquals(_item, tuple.get(2, Item.class));
        assertEquals(_store, tuple.get(3, Store.class));
        assertEquals(_urgentOrder, tuple.get(4, UrgentOrder.class));

        assertEquals(_urgentOrder, tuple.get(4, Order.class));

    }

    public void testGetIntClassExceptions() {
        // duplicate code, but could be useful later if impl changes.
        try {
            Object o = tuple.get(-1, Order.class);
            fail("tuple.get(-1) should throw IllegalArgumentException");
            o.toString(); // clean up meaningless compiler warning
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            Object o = tuple.get(200, Product.class);
            fail("tuple.get(i) where i > size of TupleElements should throw IllegalArgumentException");
            o.toString(); // clean up meaningless compiler warning
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            Product product = (Product) tuple.get(0, Product.class);
            fail("Expecting IllegalArgumentException when the wrong type is specified on Tuple.get(int, Class)");
            product.toString(); // remove compiler warning for unused variable <sigh>
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            tuple.get(0, UrgentOrder.class);
            fail("Should not be able to upcast Order to UrgentOrder");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testGetString() {
        assertEquals(_order, tuple.get("alias1"));
        assertEquals(_product, tuple.get("alias2"));
        assertEquals(_item, tuple.get("alias3"));
        assertEquals(_store, tuple.get("alias4"));
        assertEquals(_urgentOrder, tuple.get("alias5"));

        try {
            tuple.get("NotAnAlias");
            fail("Expected an IllegalArgumentException for an alias that wasn't found");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            tuple.get((String) null);
            fail("Expected an IllegalArgumentException for null alias");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            tuple.get("");
            fail("Expected an IllegalArgumentException for null alias");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testGetStringClass() {
        // TODO MDD convert to equals tests
        assertTrue(tuple.get("alias1", Order.class) instanceof Order);
        assertTrue(tuple.get("alias2", Product.class) instanceof Product);
        assertTrue(tuple.get("alias3", Item.class) instanceof Item);
        assertTrue(tuple.get("alias4", Store.class) instanceof Store);

        try {
            tuple.get("NotAnAlias", Product.class);
            fail("Expected an IllegalArgumentException for an alias that wasn't found");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            tuple.get((String) null, Item.class);
            fail("Expected an IllegalArgumentException for null alias");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            tuple.get("", Store.class);
            fail("Expected an IllegalArgumentException for null alias");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testGetTupleElement() {
        for (TupleElement<?> element : tuple.getElements()) {
            assertEquals(((TupleElementImpl) element).getValue(), tuple.get(element));
        }
    }

    public void testToArray() {
        Object[] objects = tuple.toArray();
        assertEquals(5, objects.length);
        assertEquals(_order, objects[0]);
        assertEquals(_product, objects[1]);
        assertEquals(_item, objects[2]);
        assertEquals(_store, objects[3]);
        assertEquals(_urgentOrder, objects[4]);
    }

    @SuppressWarnings("unchecked")
    public void testGetElements() {
        List<TupleElement<?>> elements = tuple.getElements();
        assertEquals(5, elements.size());

        TupleElement<Order> orderElement = (TupleElement<Order>) elements.get(0);
        TupleElement<Product> productElement = (TupleElement<Product>) elements.get(1);
        TupleElement<Item> itemElement = (TupleElement<Item>) elements.get(2);
        TupleElement<Store> storeElement = (TupleElement<Store>) elements.get(3);
        TupleElement<UrgentOrder> urgentOrderElement = (TupleElement<UrgentOrder>) elements.get(4);

        assertEquals("alias1", orderElement.getAlias());
        assertEquals(Order.class, orderElement.getJavaType());
        assertEquals("alias2", productElement.getAlias());
        assertEquals(Product.class, productElement.getJavaType());
        assertEquals("alias3", itemElement.getAlias());
        assertEquals(Item.class, itemElement.getJavaType());
        assertEquals("alias4", storeElement.getAlias());
        assertEquals(Store.class, storeElement.getJavaType());
        assertEquals("alias5", urgentOrderElement.getAlias());
        assertEquals(UrgentOrder.class, urgentOrderElement.getJavaType());
    }
    
    

    // Begin fake entities.
    class Order {
        // public Order() {
        // new Exception().printStackTrace();
        // }
    }

    class UrgentOrder extends Order {
    }

    class Item {
    }

    class Product {
    }

    class Store {
    }
}
