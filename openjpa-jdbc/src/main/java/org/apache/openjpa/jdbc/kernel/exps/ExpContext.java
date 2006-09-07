package org.apache.openjpa.jdbc.kernel.exps;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;

/**
 * Expression tree context.
 * 
 * @author Abe White
 * @nojavadoc
 */
public class ExpContext {

    /**
     * Store.
     */
    public JDBCStore store;

    /**
     * Parameters to query.
     */
    public Object[] params;

    /**
     * Fetch configuration.
     */
    public JDBCFetchConfiguration fetch; 

    public ExpContext() {
    }

    public ExpContext(JDBCStore store, Object[] params, 
        JDBCFetchConfiguration fetch) {
        this.store = store;
        this.params = params;
        this.fetch = fetch;
    }
}
