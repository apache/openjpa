package org.apache.openjpa.persistence.criteria;

import javax.persistence.criteria.Selection;

public class TupleSelection<Tuple> extends NewInstanceSelection<Tuple> {

    public TupleSelection(final Class<Tuple> cls, final Selection<?>[] selections) {
        super(cls, selections);
    }
}
