package org.apache.openjpa.jdbc.kernel.exps;

import org.apache.openjpa.jdbc.sql.Joins;

/**
 * Expression tree state for a binary operator.
 * 
 * @author Abe White
 */
class BinaryOpExpState 
    extends ExpState {

    /**
     * State for first expression/value.
     */
    public ExpState state1;

    /**
     * State for second expression/value.
     */
    public ExpState state2;

    public BinaryOpExpState() {
    }

    public BinaryOpExpState(Joins joins, ExpState state1, ExpState state2) {
        super(joins);
        this.state1 = state1;
        this.state2 = state2;
    }
}
