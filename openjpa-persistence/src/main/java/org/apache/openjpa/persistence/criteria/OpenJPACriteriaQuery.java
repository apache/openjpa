package org.apache.openjpa.persistence.criteria;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

/**
 * OpenJPA-specific extension to JPA 2.0 Criteria Query API.
 * 
 * @param <T> type of result returned by this query
 * 
 * @author Pinaki Poddar
 * @since 2.0.0
 */
public interface OpenJPACriteriaQuery<T> extends CriteriaQuery<T> {
    /**
     * Convert the query to a JPQL-like string.
     * The conversion of Criteria Query may not be an exact JPQL string.
     *  
     * @return a JPQL-like string.
     */
    public String toCQL();
    
    /**
     * Compile the query.
     * 
     * @return the same instance compiled.
     */
    public OpenJPACriteriaQuery<T> compile();
}
