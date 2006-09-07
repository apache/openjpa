package org.apache.openjpa.jdbc.kernel.exps;

import org.apache.openjpa.jdbc.sql.Joins;

/**
 * Expression tree state.
 * 
 * @author Abe White
 * @nojavadoc
 */
public class ExpState {

    /**
     * State with no joins.
     */
    public static final ExpState NULL = new ExpState();

    public Joins joins;

    public ExpState() {
    }

    public ExpState(Joins joins) {
        this.joins = joins;
    }
}
