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
package org.apache.openjpa.conf;

/**
 * Struct encompassing backwards-compatibility options.
 */
public class Compatibility {

    /**
     * If a JPQL statement is not compliant with the JPA specification,
     * fail to parse it.
     *
     * @since 1.1.0
     */
    public static final int JPQL_STRICT = 0;

    /**
     * If a JPQL statement is not compliant with the JPA specification,
     * warn the first time that statement is parsed.
     *
     * @since 1.1.0
     */
    public static final int JPQL_WARN = 1;

    /**
     * Allow non-compliant extensions of JPQL.
     * 
     * @since 1.1.0
     */
    public static final int JPQL_EXTENDED = 2;

    private boolean _strictIdValues = false;
    private boolean _hollowLookups = true;
    private boolean _checkStore = false;
    private boolean _copyIds = false;
    private boolean _closeOnCommit = true;
    private boolean _quotedNumbers = false;
    private boolean _nonOptimisticVersionCheck = false;
    private int _jpql = JPQL_WARN;
    private boolean _storeMapCollectionInEntityAsBlob = false;
    private boolean _flushBeforeDetach = true;
    private boolean _reorderMetaDataResolution = false;
    private boolean _reloadOnDetach = true;
    private boolean _overrideContextClassloader = false;
    private boolean _resetFlushFlagForCascadePersist = false;//OPENJPA-2051

    /**
     * Whether to require exact identity value types when creating object
     * ids from a class and value. Defaults to false.
     */
    public boolean getStrictIdentityValues() {
        return _strictIdValues;
    }

    /**
     * Whether to require exact identity value types when creating object
     * ids from a class and value. Defaults to false.
     */
    public void setStrictIdentityValues(boolean strictVals) {
        _strictIdValues = strictVals;
    }

    /**
     * Whether to interpret quoted numbers in query strings as numbers.
     * OpenJPA versions 0.3.1 and prior treated them as numbers; more recent
     * versions treat them as strings.
     */
    public boolean getQuotedNumbersInQueries() {
        return _quotedNumbers;
    }

    /**
     * Whether to interpret quoted numbers in query strings as numbers.
     * OpenJPA versions 0.3.1 and prior treated them as numbers; more recent
     * versions treat them as strings.
     */
    public void setQuotedNumbersInQueries(boolean quotedNumbers) {
        _quotedNumbers = quotedNumbers;
    }

    /**
     * Whether to return hollow instances to broker lookups with a
     * <code>validate</code> parameter of false. OpenJPA versions prior to
     * 0.4.0 did not return hollow instances without special configuration
     * (the <code>ObjectLookupMode</code>). Beginning with 0.4.0, hollow
     * objects are the default.
     */
    public boolean getValidateFalseReturnsHollow() {
        return _hollowLookups;
    }

    /**
     * Whether to return hollow instances to broker lookups with a
     * <code>validate</code> parameter of false. OpenJPA versions prior to
     * 0.4.0 did not return hollow instances without special configuration
     * (the <code>ObjectLookupMode</code>). Beginning with 0.4.0, hollow
     * objects are the default.
     */
    public void setValidateFalseReturnsHollow(boolean hollow) {
        _hollowLookups = hollow;
    }

    /**
     * Whether to check the datastore for the existence of a nontransactional
     * cached object in broker lookups with a <code>validate</code> parameter
     * of true. OpenJPA versions prior to 0.4.0 checked the datastore.
     */
    public boolean getValidateTrueChecksStore() {
        return _checkStore;
    }

    /**
     * Whether to check the datastore for the existence of a nontransactional
     * cached object in broker lookups with a <code>validate</code> parameter
     * of true. OpenJPA versions prior to 0.4.0 checked the datastore.
     */
    public void setValidateTrueChecksStore(boolean check) {
        _checkStore = check;
    }

    /**
     * Whether to copy identity objects before returning them to client code.
     * Versions of OpenJPA prior to 0.3.0 always copied identity objects. Also,
     * you should configure OpenJPA to copy identity objects if you mutate them
     * after use.
     */
    public boolean getCopyObjectIds() {
        return _copyIds;
    }

    /**
     * Whether to copy identity objects before returning them to client code.
     * Versions of OpenJPA prior to 0.3.0 always copied identity objects. Also,
     * you should configure OpenJPA to copy identity objects if you mutate them
     * after use.
     */
    public void setCopyObjectIds(boolean copy) {
        _copyIds = copy;
    }

    /**
     * Whether to close the broker when the managed transaction commits.
     * Versions of OpenJPA prior to 0.3.0 did not close the broker.
     */
    public boolean getCloseOnManagedCommit() {
        return _closeOnCommit;
    }

    /**
     * Whether to close the broker when the managed transaction commits.
     * Versions of OpenJPA prior to 0.3.0 did not close the broker.
     */
    public void setCloseOnManagedCommit(boolean close) {
        _closeOnCommit = close;
	}	

    /** 
     * Whether or not to perform a version check on instances being updated
     * in a datastore transaction. Version of OpenJPA prior to 0.4.1 always
     * forced a version check.
     */
    public void setNonOptimisticVersionCheck(boolean nonOptimisticVersionCheck){
        _nonOptimisticVersionCheck = nonOptimisticVersionCheck;
    }

    /** 
     * Whether or not to perform a version check on instances being updated
     * in a datastore transaction. Version of OpenJPA prior to 0.4.1 always
     * forced a version check.
     */
    public boolean getNonOptimisticVersionCheck() {
        return _nonOptimisticVersionCheck;
    }

    /**
     * Whether or not JPQL extensions are allowed. Defaults to
     * {@link #JPQL_STRICT}.
     *
     * @since 1.1.0
     * @see #JPQL_WARN
     * @see #JPQL_STRICT
     * @see #JPQL_EXTENDED
     */
    public int getJPQL() {
        return _jpql;
    }

    /**
     * Whether or not JPQL extensions are allowed. Possible values: "warn",
     * "strict", "extended".
     *
     * @since 1.1.0
     * @see #JPQL_WARN
     * @see #JPQL_STRICT
     * @see #JPQL_EXTENDED
     */
    public void setJPQL(String jpql) {
        if ("warn".equals(jpql))
            _jpql = JPQL_WARN;
        else if ("strict".equals(jpql))
            _jpql = JPQL_STRICT;
        else if ("extended".equals(jpql))
            _jpql = JPQL_EXTENDED;
        else
            throw new IllegalArgumentException(jpql);
    }

    /**
     * Whether if map and collection in entity are stored as blob.
     * Defaults to <code>false</code>.
     *
     * @since 1.1.0 
     */

    public boolean getStoreMapCollectionInEntityAsBlob() {
        return _storeMapCollectionInEntityAsBlob;
    }

    /**
     * Whether if map and collection in entity are stored as blob.
     * Defaults to <code>false</code>.
     *
     * @since 1.1.0 
     */
    public void setStoreMapCollectionInEntityAsBlob(boolean storeAsBlob) {
        _storeMapCollectionInEntityAsBlob = storeAsBlob;
    }
    
    /**
     * Whether OpenJPA should flush changes before detaching or serializing an
     * entity. In JPA this is usually false, but other persistence frameworks
     * (ie JDO) may expect it to be true.
     * <P>Prior to version 1.0.3 and 1.2.0 changes were always flushed.
     * 
     * @since 1.0.3
     * @since 1.2.0
     * @return true if changes should be flushed, otherwise false.
     */
    public boolean getFlushBeforeDetach() {
        return _flushBeforeDetach;
    }

    /**
     * Whether OpenJPA should flush changes before detaching or serializing an
     * entity. In JPA this is usually false, but other persistence frameworks
     * (ie JDO) may expect it to be true.
     * <P>Prior to version 1.0.3 and 1.2.0 changes were always flushed.
     * 
     * @param beforeDetach if true changes will be flushed before detaching or 
     * serializing an entity.
     * 
     * @since 1.0.3
     * @since 1.2.0
     */
    public void setFlushBeforeDetach(boolean beforeDetach) {
        _flushBeforeDetach = beforeDetach;
    }
    
    
    /**
     * Whether OpenJPA should reorder entities in MetaDataRepository.processBuffer() to ensure that the MetaData for 
     * entities with foreign keys in their identity are processed after the entities they depend on.
     * 
     * @return true if the reordering should be performed, false if not.
     */
    public boolean getReorderMetaDataResolution() {
    	return _reorderMetaDataResolution;
    }
    
    /**
     * Whether OpenJPA should reorder entities in MetaDataRepository.processBuffer() to ensure that the MetaData for 
     * entities with foreign keys in their identity are processed after the entities they depend on.
     * 
     * @param reorderProcessBuffer true if the reordering should be performed, false if not.
     */
    public void setReorderMetaDataResolution(boolean reorderProcessBuffer) {
        _reorderMetaDataResolution = reorderProcessBuffer;
    }

    /**
     * Whether OpenJPA should attempt to load fields when the DetachState
     * option is set to loaded. This also determines whether a
     * redundant copy of the version field is made. Defaults to true.
     * 
     * @return the _reloadOnDetach
     * 
     * @since 1.2.2
     */
    public boolean getReloadOnDetach() {
        return _reloadOnDetach;
    }

    /**
     * Whether OpenJPA should attempt to load fields when the DetachState
     * option is set to loaded. This also determines whether a
     * redundant copy of the version field is made. Defaults to true.
     * 
     * @param reloadOnDetach the _reloadOnDetach to set
     * 
     * @since 1.2.2
     */
    public void setReloadOnDetach(boolean reloadOnDetach) {
        _reloadOnDetach = reloadOnDetach;
    }   

    /**
     * Whether to temporally override the thread's Context Classloader when processing
     * ORM XML documents to avoid deadlock potential with certain Classloader hierarchy
     * configurations.  Defaults to false.
     */
    public boolean getOverrideContextClassloader() {
        return _overrideContextClassloader;
    }

    /**
     * Whether to temporally override the thread's Context Classloader when processing
     * ORM XML documents to avoid deadlock potential with certain Classloader hierarchy
     * configurations.  Defaults to false.
     */
    public void setOverrideContextClassloader(boolean overrideContextClassloader) {
        _overrideContextClassloader = overrideContextClassloader;
    }
    
    /**
     * Whether OpenJPA should reset the internal state (flush flag) when cascading a persist to another 
     * Entity. That is, when a flush is performed, OpenJPA keep state to indicate the flush has been 
     * performed.  In certain cascade persist scenarios the fact that a flush has been performed prior to 
     * a cascade persist can cause certain entities to not be written to the database given the prior 
     * flush.  This property, when set, will cause the flush flag to be reset in cascade scenarios. For more 
     * details see JIRA OPENJPA-2051
     *    
     * @since 2.0.x
     */
    public boolean getResetFlushFlagForCascadePersist(){
        return _resetFlushFlagForCascadePersist;
    }
    
    /**
     * Whether OpenJPA should reset the internal state (flush flag) when cascading a persist to another 
     * Entity. That is, when a flush is performed, OpenJPA keep state to indicate the flush has been 
     * performed.  In certain cascade persist scenarios the fact that a flush has been performed prior to 
     * a cascade persist can cause certain entities to not be written to the database given the prior 
     * flush.  This property, when set, will cause the flush flag to be reset in cascade scenarios. For more 
     * details see JIRA OPENJPA-2051
     *    
     * @since 2.0.x
     */
    public void setResetFlushFlagForCascadePersist(boolean b){
        _resetFlushFlagForCascadePersist = b;
    }

}
