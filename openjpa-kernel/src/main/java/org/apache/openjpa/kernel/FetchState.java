/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.kernel;

import java.io.Serializable;
import java.util.List;

import org.apache.openjpa.meta.FieldMetaData;

/**
 * Defines the decision to include fields for selection or loading during
 * a fetch operation.
 * A state is resulted by traversal of a relationship except the <em>root</em>
 * state. 
 *
 * @author <A HREF="mailto:pinaki.poddar@gmail.com>Pinaki Poddar</A>
 * @since 4.1
 */
public interface FetchState 
	extends Serializable, Cloneable {
    
	public static final int INFINITE_DEPTH = -1;

    /**
     * Returns the immutable fetch configuration this receiver is based on.
     */
    public FetchConfiguration getFetchConfiguration();

    /**
     * Affirms if the given field requires to be fetched in the context
     * of current fetch operation.
     *
     * @param fm          field metadata. must not be null.
     */
    public boolean requiresFetch(FieldMetaData fm);

    /**
     * Affirms if the given field of the given instance requires to be loaded
     * in the context of current fetch operation.
     *
     * @param sm state manager being populated
     * @param fm field metadata
     */
    public boolean requiresLoad(OpenJPAStateManager sm, FieldMetaData fm);
    
    /**
     * Traverse the given field to generate (possibly) a new state.
     * 
     * @param fm
     * @return a new state resulting out of traversal. If the given field is
     * not a relation then return itself.
     */
    public FetchState traverse (FieldMetaData fm);

    /**
     * Gets the available depth i.e. number of relations that can be traveresed
     * from this receiver. 
     * 
     * @return a positive integer with positive infinity designated as 
     * <code>-1</code>.
     */
    public int getAvailableFetchDepth ();
    
    /**
     * Gets the root state where this receiver is derived from.
     * @return itself if the state is not derived from another state.
     * 
     */
    public FetchState getRoot ();
    
    /**
     * Affirms if this receiver is the root state i.e. not derived as a result
     * of traversing a relationship.
     *  
     */
    public boolean isRoot ();
    
    /**
     * Gets the parent state.
     * @return can be null for the root state.
     */
    public FetchState getParent ();
    
    
    /** 
     * Gets an ordered list of states from this receiver to its root.
     * 
     * @return the order starts from this receiver and ends in the 
     * root. An empty list if this receiver is the root. 
     */ 
    public List getPath ();
    
    /** 
     * Gets an ordered list of relation fields from this receiver to its root.
     * These relations denote the path traversals from the root that resulted 
     * in the current state.
     * 
     * @return the list starts from relation traversal of which resulted in this
     * receiver and ends in the relation traversed from the root. 
     * An empty list, if this receiver itself is the root. 
     */ 
    public List getRelationPath ();
    
    /**
     * Gets the number of times the given field is traversed to arrive
     * at this state.
     * 
     * @param fm
     * @return
     */
    public int getCurrentRecursionDepth (FieldMetaData fm);
    
    /**
     * Gets the recursion depth of the given field. 
     * 
     * @param fm a relation field
     * @return If the field has multiple fetch groups in the current 
     * configuration, then the recursion depth is maximum of all the recursion 
     * depths.
     * The default recursion depth, if none is explictly specified, is 1.
     * The infinite i.e. unlimited recursion depth is designated as 
     * <code>-1</code> 
     */
    public int getRecursionDepth (FieldMetaData fm);
}
