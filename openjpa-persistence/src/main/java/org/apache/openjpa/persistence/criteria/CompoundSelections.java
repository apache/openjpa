package org.apache.openjpa.persistence.criteria;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.Selection;

public class CompoundSelections {
    private abstract static class CompoundSelectionImpl<X> extends SelectionImpl<X> implements CompoundSelection<X> {
        private final List<Selection<?>> _args;
        public CompoundSelectionImpl(Class<X> cls, Selection<?>...args) {
            super(cls);
            assertNoCompoundSelection(args);
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

    }
    
    public static class Array<X> extends CompoundSelectionImpl<X> {
        public Array(Class<X> cls, Selection<?>... terms) {
            super(cls, terms);
            if (!cls.isArray()) {
                throw new IllegalArgumentException(cls + " is not an array. " + this + " needs an array");
            }
        }
    }
    
    public static class NewInstance<X> extends CompoundSelectionImpl<X> {
        public NewInstance(Class<X> cls, Selection<?>... selections) {
            super(cls, selections);
        }
    }
    
    public static class Tuple<T extends javax.persistence.Tuple> extends CompoundSelectionImpl<T> {
        public Tuple(final Class<T> cls, final Selection<?>[] selections) {
            super(cls, selections);
        }
    }

}
