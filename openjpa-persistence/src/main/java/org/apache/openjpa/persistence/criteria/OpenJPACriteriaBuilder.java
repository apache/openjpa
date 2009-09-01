package org.apache.openjpa.persistence.criteria;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.QueryBuilder;
import javax.persistence.metamodel.Attribute;

/**
 * OpenJPA-specific extension to JPA 2.0 Criteria Query Builder API.
 * 
 * 
 * @author Pinaki Poddar
 * @since 2.0.0
 */
public interface OpenJPACriteriaBuilder extends QueryBuilder {
    /**
     * Create a predicate based upon the attribute values of a given
     * "example" entity instance. The predicate is the conjunction 
     * or disjunction of predicates for subset of attribute of the entity.
     * <br>
     * All the singular entity attributes (the basic, embedded
     * and uni-cardinality relations) that have a non-null or non-default
     * value for the example instance and are not an identity or version
     * attribute are included. The comparable attributes can be further
     * pruned by specifying variable list of attributes for exclusion.
     * 
     * @param example a non-null instance of a persistent entity.
     * 
     * @param style specifies various aspects of comparison such as whether
     * non-null attribute values be included, how string-valued attribute be 
     * compared, whether the individual attribute based predicates are ANDed
     * or ORed etc. Can be null to designate default comparison style.
     * 
     * @param excludes list of attributes that are excluded from comparison.
     * Can be null.
     *  
     * @return a predicate 
     */
    public <T> Predicate qbe(From<?, T> from, T example, ComparisonStyle style, Attribute<?,?>... excludes);
    
    /**
     * Overloaded with no extra attribute to exclude.
     */
    public <T> Predicate qbe(From<?, T> from, T example, ComparisonStyle style);
    
    /**
     * Overloaded with default comparison style.
     */
    public <T> Predicate qbe(From<?, T> from, T example, Attribute<?,?>... excludes);
    
    /**
     * Overloaded with default comparison style and no extra attribute to exclude.
     */
    public <T> Predicate qbe(From<?, T> from, T example);
    
    /**
     * Create a mutable style to apply on query-by-example.
     */
    public ComparisonStyle qbeStyle();

}
