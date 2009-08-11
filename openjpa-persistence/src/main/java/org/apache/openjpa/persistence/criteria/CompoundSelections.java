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
package org.apache.openjpa.persistence.criteria;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.Selection;

import org.apache.openjpa.kernel.FillStrategy;
import org.apache.openjpa.kernel.ResultShape;
import org.apache.openjpa.persistence.TupleFactory;
import org.apache.openjpa.persistence.TupleImpl;

/**
 * Implements slection terms that are composed of other selection terms.
 *  
 * @author Pinaki Poddar
 * 
 * @since 2.0.0
 *
 */
public class CompoundSelections {
    /**
     * Gets the strategy to fill a given compound selection.
     * 
     */
    public static <X> FillStrategy<X> getFillStrategy(Selection<X> s) {
        if (s instanceof CompoundSelectionImpl) {
            return ((CompoundSelectionImpl<X>)s).getFillStrategy();
        } else {
            return new FillStrategy.Assign<X>();
        }
    }
    
    /**
     * Abstract implementation of a selection term composed of multiple selection terms.
     *
     */
    private abstract static class CompoundSelectionImpl<X> extends SelectionImpl<X> implements CompoundSelection<X> {
        private final List<Selection<?>> _args;
        
        public CompoundSelectionImpl(Class<X> cls, Selection<?>...args) {
            super(cls);
//            assertNoCompoundSelection(args);
            _args = args == null ? (List<Selection<?>>)Collections.EMPTY_LIST : Arrays.asList(args);
        }
        
        public final boolean isCompoundSelection() {
            return true;
        }
        
        /**
         * Return selection items composing a compound selection
         * @return list of selection items
         * @throws IllegalStateException if selection is not a compound
         *           selection
         */
        public final List<Selection<?>> getCompoundSelectionItems() {
            return Collections.unmodifiableList(_args);
        }
        
        void assertNoCompoundSelection(Selection<?>...args) {
            if (args == null)
                return;
            for (Selection<?> s : args) {
                if (s.isCompoundSelection() && !(s.getClass() == NewInstance.class)) {
                    throw new IllegalArgumentException("compound selection " + s + " can not be nested in " + this);
                }
            }
        }

        public abstract FillStrategy<X> getFillStrategy();
    }
    
    /**
     * A compound selection which is an array of its component terms.
     *
     * @param <X> type must be an array
     */
    public static class Array<X> extends CompoundSelectionImpl<X> {
        public Array(Class<X> cls, Selection<?>... terms) {
            super(cls, terms);
            if (!cls.isArray()) {
                throw new IllegalArgumentException(cls + " is not an array. " + this + " needs an array");
            }
        }
        
        public FillStrategy<X> getFillStrategy() {
            return new FillStrategy.Array<X>(getJavaType());
        }
    }
    
    /**
     * A compound selection which is an instance constructed of its component terms.
     *
     * @param <X> type of the constructed instance
     */
    public static class NewInstance<X> extends CompoundSelectionImpl<X> {
        public NewInstance(Class<X> cls, Selection<?>... selections) {
            super(cls, selections);
        }
        
        public FillStrategy<X> getFillStrategy() {
            return new FillStrategy.NewInstance<X>(getJavaType());
        }
    }
    
    /**
     * A compound selection which is a Tuple composed of its component terms.
     *
     */
    public static class Tuple extends CompoundSelectionImpl<javax.persistence.Tuple> {
        public Tuple(final Selection<?>[] selections) {
            super(javax.persistence.Tuple.class, selections);
        }
        
        public FillStrategy<javax.persistence.Tuple> getFillStrategy() {
            List<Selection<?>> terms = getCompoundSelectionItems();
            TupleFactory factory = new TupleFactory(terms.toArray(new TupleElement[terms.size()]));
            return new FillStrategy.Factory<javax.persistence.Tuple>(factory, TupleImpl.PUT);
        }
    }

    public static class MultiSelection<T> extends CompoundSelectionImpl<T> {
        public MultiSelection(Class<T> result, final Selection<?>[] selections) {
            super(result, selections);
        }
        public FillStrategy<T> getFillStrategy() {
            Class<?> resultClass = getJavaType();
            List<Selection<?>> terms = getCompoundSelectionItems();
            FillStrategy<?> strategy = null;
            if (javax.persistence.Tuple.class.isAssignableFrom(resultClass)) {
                TupleFactory factory = new TupleFactory(terms.toArray(new TupleElement[terms.size()]));
                strategy = new FillStrategy.Factory<javax.persistence.Tuple>(factory,  TupleImpl.PUT);
           } else if (resultClass == Object.class) {
               if (terms.size() > 1) { 
                   resultClass = Object[].class;
                   strategy = new FillStrategy.Array<Object[]>(Object[].class);
               } else {
                   strategy = new FillStrategy.Assign();
               }
           } else {
               strategy = resultClass.isArray() 
                        ? new FillStrategy.Array(resultClass) 
                        : new FillStrategy.NewInstance(resultClass);
           } 
            return (FillStrategy<T>)strategy;
        }
    }
}
