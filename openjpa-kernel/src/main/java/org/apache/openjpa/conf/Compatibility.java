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
    private boolean _flushBeforeDetach = false; 
    private boolean _cascadeWithDetach = false;
    private boolean _useJPA2DefaultOrderColumnName = true;
    private boolean _copyOnDetach = false;
    private boolean _privatePersistentProperties = false;
    private boolean _autoOff = true;
    private boolean _superclassDiscriminatorStrategyByDefault = true;
    private boolean _isAbstractMappingUniDirectional = false;
    private boolean _isNonDefaultMappingAllowed = false;
    private boolean _reorderMetaDataResolution = true;
    private boolean _reloadOnDetach = false;
    private boolean _ignoreDetachedStateFieldForProxySerialization = false;
    private boolean _parseAnnotationsForQueryMode = true;
    
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
     * Whether to turn collection/map tracing off in case of more number of modifications.
     * Defaults to true.
     */
    public boolean getAutoOff() {
        return _autoOff;
    }

    /**
     * Whether to turn collection/map tracing off in case of more number of modifications.
     * Defaults to true.
     */
    public void setAutoOff(boolean autoOff) {
        _autoOff = autoOff;
    }
    
    /**
     * Whether to add class criteria for super class discreminator strategy.
     * Defaults to false.
     */
    public boolean getSuperclassDiscriminatorStrategyByDefault() {
        return _superclassDiscriminatorStrategyByDefault;
    }

    /**
     * Whether to add class criteria for super class discreminator strategy.
     * Defaults to false.
     */
    public void setSuperclassDiscriminatorStrategyByDefault(boolean superclassDiscriminatorStrategyByDefault) {
        _superclassDiscriminatorStrategyByDefault = superclassDiscriminatorStrategyByDefault;
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
     * Whether OpenJPA should ignore the DetachedStateField value when
     * determining if our Proxy classes should be removed during serialization.
     * <P>Starting with version 2.0.0, when the DetachedStateFiled==true, the
     * build time $proxy classes will not be removed.
     * <P>Prior to version 2.0.0, the DetachedStateFiled was not used and
     * the $proxy classes were not being removed during serialization after
     * the Persistence context was cleared.
     * 
     * @param ignoreDSF if true the old Proxy serialization behavior will be used.
     * 
     * @since 2.0.0
     */
    public void setIgnoreDetachedStateFieldForProxySerialization(boolean ignoreDSF) {
        _ignoreDetachedStateFieldForProxySerialization = ignoreDSF;
    }
    
    /**
     * Whether OpenJPA should ignore the DetachedStateField value when
     * determining if our Proxy classes should be removed during serialization.
     * <P>Starting with version 2.0.0, when the DetachedStateFiled==true, the
     * build time $proxy classes will not be removed.
     * <P>Prior to version 2.0.0, the DetachedStateFiled was not used and
     * the $proxy classes were not being removed during serialization after
     * the Persistence context was cleared.
     * 
     * @since 2.0.0
     * @return true if the old Proxy serialization will be used, otherwise false.
     */
    public boolean getIgnoreDetachedStateFieldForProxySerialization() {
        return _ignoreDetachedStateFieldForProxySerialization;
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
     * Affirms if detached entities are copy of the managed instances.
     * Before this option is introduced, detached entities were by default 
     * copies of the managed entities unless the entire cache is detached, only 
     * then the detachment was in-place.
     * This option changes the default behavior such that detachment is now
     * in-place by default. To emulate the previous copy-on-detach behavior
     * set this option to true.  
     * 
     * If the entire cache is being detached (when the persistence context is
     * closed, for example), the detachement 
     * 
     * @since 2.0.0
     */
    public boolean getCopyOnDetach() {
        return _copyOnDetach;
    }
    
    /**
     * Sets if detached entities are copy of the managed instances.
     * Before this option is introduced, detached entities were by default 
     * copies of the managed entities unless the entire cache is detached, only 
     * then the detachment was in-place.
     * This option changes the default behavior such that detachment is now
     * in-place by default. To emulate the previous copy-on-detach behavior
     * set this option to true.   
     * 
     * @since 2.0.0
     */
    public void setCopyOnDetach(boolean copyOnDetach) {
        _copyOnDetach = copyOnDetach;
    }

    /**
     * Whether openjpa will always cascade on detach, regardless of the
     * cascade setting.
     * 
     * @return true if cascade will always occur, false if cascade will only
     * occur if it is specified in metadata
     * 
     * @since 2.0.0
     */
    public boolean getCascadeWithDetach() {
        return _cascadeWithDetach;
    }

    /**
     * Whether openjpa should always cascade on detach, regardless of the
     * cascade setting.
     * 
     * @param cascadeWithDetach true if cascade should always occur, false if
     * it should only occur if specified in metadata
     * 
     * @since 2.0.0
     */
    public void setCascadeWithDetach(boolean cascadeWithDetach) {
        _cascadeWithDetach = cascadeWithDetach;
    }
    
    /**
     * Whether OpenJPA should use the new default order column name defined
     * by JPA 2.0: name; "_"; "ORDER" or the pre-JPA 2.0 default name "ordr".
     * 
     * @since 2.0.0
     * @return true if the JPA2 default name should be used
     */
    public boolean getUseJPA2DefaultOrderColumnName() {
        return _useJPA2DefaultOrderColumnName;
    }

    /**
     * Whether OpenJPA should use the new default order column name defined
     * by JPA 2.0: name; "_"; "ORDER" or the pre-JPA 2.0 default name "ordr".
     * 
     * @param useJPA2 true if the JPA 2.0 default name should be used.  false if
     * the 1.x name should be used.
     * 
     * @since 2.0.0
     */
    public void setUseJPA2DefaultOrderColumnName(boolean useJPA2Name) {
        _useJPA2DefaultOrderColumnName = useJPA2Name;
    }
    

    /**
     * Whether OpenJPA allows private, non-transient properties to be 
     * persistent.  Prior to OpenJPA 2.0, if property access was used,
     * private properties were considered persistent. This is contrary to the
     * JPA specification, which states that persistent properties must be
     * public or protected.  The default value is false.
     * 
     * @since 2.0.0
     * @return true if non-transient private properties should be persistent 
     */
    public boolean getPrivatePersistentProperties() {
        return _privatePersistentProperties;
    }

    /**
     * Whether OpenJPA allows private, non-transient properties to be 
     * persistent.  Prior to OpenJPA 2.0, if property access was used,
     * private properties were considered persistent. This is contrary to the
     * JPA specification, which states that persistent properties must be
     * public or protected.
     * 
     * @param privateProps true if non-transient private properties 
     *        should be persistent
     * @since 2.0.0
     */
    public void setPrivatePersistentProperties(boolean privateProps) {
        _privatePersistentProperties = privateProps;
    }
    
    /**
     * Whether OpenJPA allows bi-directional relationship in the MappedSuperclass.
     * Prior to OpenJPA 2.0, the bi-directional relationship in the MappedSuperclass,
     * is not blocked. This is contrary to the JPA specification, which states that 
     * persistent relationships defined by a mapped superclass must be
     * unidirectional.
     * 
     * @param isAbstractMappingUniDirectional true if relationship defined in the 
     *        MappedSuperclass must be uni-directional
     * @since 2.0.0
     */
    public void setAbstractMappingUniDirectional(boolean isAbstractMappingUniDirectional) {
        _isAbstractMappingUniDirectional = isAbstractMappingUniDirectional;
    }

    
    /**
     * Whether OpenJPA allows bi-directional relationship in the MappedSuperclass.
     * Prior to OpenJPA 2.0, the bi-directional relationship in the MappedSuperclass,
     * is not blocked. This is contrary to the JPA specification, which states that 
     * persistent relationships defined by a mapped superclass must be
     * unidirectional. The default value is false.
     * 
     * @since 2.0.0
     */
    public boolean isAbstractMappingUniDirectional() {
        return _isAbstractMappingUniDirectional;
    }
    
    /**
     * Whether OpenJPA allows non-default entity relationship mapping. 
     * Prior to OpenJPA 2.0, the non-default entity relationship mapping
     * is not allowed. JPA 2.0 spec relaxes this restriction. The
     * default value is false.
     * @since 2.0.0
     */
    public void setNonDefaultMappingAllowed(boolean isNonDefaultMappingAllowed) {
        _isNonDefaultMappingAllowed = isNonDefaultMappingAllowed;
    }

    /**
     * Whether OpenJPA allows non-default entity relationship mapping. 
     * Prior to OpenJPA 2.0, the non-default entity relationship mapping
     * is not allowed. JPA 2.0 spec relaxes this restriction. The
     * default value is false.
     * @since 2.0.0
     */
    public boolean isNonDefaultMappingAllowed() {
        return _isNonDefaultMappingAllowed;
    }
    
    /**
     * Whether OpenJPA should reorder entities in MetaDataRepository.processBuffer() to ensure that the metadata for 
     * entities with foreign keys in their identity are processed after the entities it depends on.
     * 
     * @return true if the reordering should be performed, false if not.
     */
    public boolean getReorderMetaDataResolution() {
        return _reorderMetaDataResolution;
    }
    
    /**
     * Whether OpenJPA should reorder entities in MetaDataRepository.processBuffer() to ensure that the metadata for 
     * entities with foreign keys in their identity are processed after the entities it depends on.
     * 
     * @param reorderProcessBuffer true if the reordering should be performed, false if not.
     */
    public void setReorderMetaDataResolution(boolean reorderProcessBuffer) {
        _reorderMetaDataResolution = reorderProcessBuffer;
    }

    /**
     * Whether OpenJPA should attempt to load fields when the DetachState
     * option is set to loaded. This also determines whether a
     * redundant copy of the version field is made. Beginning in 2.0
     * it defaults to false.
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
     * redundant copy of the version field is made. Beginning in 2.0
     * it defaults to false.
     * 
     * @param reloadOnDetach the _reloadOnDetach to set
     * 
     * @since 1.2.2
     */
    public void setReloadOnDetach(boolean reloadOnDetach) {
        _reloadOnDetach = reloadOnDetach;
    }       

    /**
     * Whether OpenJPA will scan every persistent class in an XML mapping file for annotations prior to executing a 
     * query. In practice this scan is rarely needed, but the option to enable it is present for compatibility with 
     * prior releases.
     * @since 2.0.2
     * @return true if the annotations should be re-parsed when resolving MetaData in MODE_QUERY.  
     */
    public boolean getParseAnnotationsForQueryMode() {
        return _parseAnnotationsForQueryMode;
    }

    /**
     * Whether OpenJPA will scan every persistent class in an XML mapping file for annotations prior to executing a 
     * query. In practice this scan is rarely needed, but the option to enable it is present for compatibility with 
     * prior releases.
     * @since 2.0.2
     */
    public void setParseAnnotationsForQueryMode(boolean parseAnnotationsForQueryMode) {
        _parseAnnotationsForQueryMode = parseAnnotationsForQueryMode;
    }
}
