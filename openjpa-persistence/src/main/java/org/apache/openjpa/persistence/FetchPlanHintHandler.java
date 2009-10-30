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
package org.apache.openjpa.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.LockModeType;

import org.apache.openjpa.kernel.FetchConfigurationImpl;
import org.apache.openjpa.kernel.AbstractHintHandler;
import org.apache.openjpa.kernel.MixedLockLevels;
import org.apache.openjpa.lib.conf.ProductDerivations;
import org.apache.openjpa.lib.util.Localizer;

/**
 * Fetch plan hint handler. Handles openjpa.FetchPlan.*, 
 * javax.persistence.lock.* and javax.persistence.query.* hints.
 *
 * @since 2.0.0
 * @nojavadoc
 */
@SuppressWarnings("serial")
public class FetchPlanHintHandler extends AbstractHintHandler {

    private static final Localizer _loc = Localizer.forPackage(FetchPlanHintHandler.class);

    protected static final String PREFIX_FETCHPLAN = PREFIX_OPENJPA + "FetchPlan.";

    // Valid defined product derivation prefixes
    protected static final Set<String> validProductPrefixes =  new HashSet<String>();
    // JPA Specification 2.0 keys are mapped to equivalent FetchPlan keys
    protected static final Map<String, String> javaxHintsMap   = new HashMap<String, String>();
    // hints precedent definitions
    protected static final Map<String, String[]> precedenceMap = new HashMap<String, String[]>();

    static {
        // Initialize valid product prefixes from available product derivations.
        for (String prefix : ProductDerivations.getConfigurationPrefixes()) {
            validProductPrefixes.add(prefix);
        }
        // Initialize javax.persistence to openjpa.FetchPlan hint mapping.
        javaxHintsMap.put(JPAProperties.LOCK_TIMEOUT,  PREFIX_FETCHPLAN + "LockTimeout");
        javaxHintsMap.put(JPAProperties.LOCK_SCOPE,    PREFIX_FETCHPLAN + "LockScope");
        javaxHintsMap.put(JPAProperties.QUERY_TIMEOUT, PREFIX_FETCHPLAN + "QueryTimeout");
        // Initialize hint precedence order mapping from list.
        String[][] precedenceMapList = {
            { JPAProperties.LOCK_TIMEOUT,
              PREFIX_FETCHPLAN  + "LockTimeout",
              PREFIX_OPENJPA    + "LockTimeout" },

            { JPAProperties.LOCK_SCOPE,
              PREFIX_FETCHPLAN  + "LockScope",
              PREFIX_OPENJPA    + "LockScope" },

            { JPAProperties.QUERY_TIMEOUT,
              PREFIX_FETCHPLAN  + "QueryTimeout",
              PREFIX_OPENJPA    + "QueryTimeout" },

            { PREFIX_FETCHPLAN  + "Isolation",
              PREFIX_JDBC       + "TransactionIsolation" },

            { PREFIX_FETCHPLAN  + "EagerFetchMode",
              PREFIX_JDBC       + "EagerFetchMode" },

            { PREFIX_FETCHPLAN  + "FetchDirection",
              PREFIX_JDBC       + "FetchDirection" },

            { PREFIX_FETCHPLAN  + "JoinSyntax",
              PREFIX_JDBC       + "JoinSyntax" },

            { PREFIX_FETCHPLAN  + "LRSSizeAlgorithm",
              PREFIX_FETCHPLAN  + "LRSSize",
              PREFIX_JDBC       + "LRSSize" },

            { PREFIX_FETCHPLAN  + "ResultSetType",
              PREFIX_JDBC       + "ResultSetType" },

            { PREFIX_FETCHPLAN  + "SubclassFetchMode",
              PREFIX_JDBC       + "SubclassFetchMode" },

            { PREFIX_FETCHPLAN  + "ReadLockMode",
              PREFIX_OPENJPA    + "ReadLockLevel" },

            { PREFIX_FETCHPLAN  + "WriteLockMode",
              PREFIX_OPENJPA    + "WriteLockLevel" },

            { PREFIX_FETCHPLAN  + "FetchBatchSize",
              PREFIX_OPENJPA    + "FetchBatchSize" },

            { PREFIX_FETCHPLAN  + "MaxFetchDepth",
              PREFIX_OPENJPA    + "MaxFetchDepth" }
        };
        for (String[] list : precedenceMapList) {
            for (String hint : list)
                precedenceMap.put(hint, list);
        }
    }

    protected FetchPlanImpl _fPlan;

    /**
     * Constructor; supply delegate.
     */
    public FetchPlanHintHandler(FetchPlanImpl fetchPlan) {
        super((FetchConfigurationImpl) fetchPlan.getDelegate());
        _fPlan = fetchPlan;
    }

    public boolean setHint(String hintName, Object value, boolean validateThrowException) {
        if (!JPAProperties.isValidKey(hintName)
            && !validProductPrefixes.contains(getPrefixOf(hintName)))
            return false;
        return super.setHint(hintName, value, validateThrowException);
    }

    protected boolean setHintInternal(String hintName, Object value, boolean validateThrowException) {
        boolean valueSet = false;
        if (hintName.startsWith(PREFIX_FETCHPLAN)) {
            if (hintName.endsWith("LockMode") && !_fConfig.getContext().isActive()) {
                _fConfig.setHint(hintName + ".Defer", toLockLevel(value));
                valueSet = true;
            } else {
                valueSet = hintToSetter(_fPlan, hintName, value);
            }
        } else {
            _fConfig.setHint(hintName, value, validateThrowException);
        }
        return valueSet;
    }

    protected String hintToKey(String key) {
        // transform product derived prefix to openjpa prefix
        if (!key.startsWith(PREFIX_OPENJPA)
            && validProductPrefixes.contains(getPrefixOf(key)))
            key = PREFIX_OPENJPA + key.substring(key.indexOf('.') + 1);

        // transform javax.persistence.* hints to fetch plan hints.
        if (javaxHintsMap.containsKey(key))
            key = javaxHintsMap.get(key);
        return key;
    }

    protected boolean hasPrecedent(String key) {
        boolean hasPrecedent = true;
        String[] list = precedenceMap.get(key);
        if (list != null) {
            for (String hint : list) {
                if (hint.equals(key))
                    break;
                // stop if a higher precedence hint has already defined 
                if (_fConfig.getHint(hint) != null) {
                    hasPrecedent = false;
                    break;
                }
            }
        }
        return hasPrecedent;
    }

    protected void handleException(RuntimeException e) {
        throw PersistenceExceptions.toPersistenceException(e);
    }

    private Integer toLockLevel(Object value) {
        Object origValue = value;
        if (value instanceof String) {
            // to accomodate alias name input in relationship with enum values
            //  e.g. "optimistic-force-increment" == 
            //          LockModeType.OPTIMISTIC_FORCE_INCREMENT
            String strValue = ((String) value).toUpperCase().replace('-', '_');
            value = Enum.valueOf(LockModeType.class, strValue);
        }
        if (value instanceof LockModeType)
            value = MixedLockLevelsHelper.toLockLevel((LockModeType) value);

        Integer intValue = null;
        if (value instanceof Integer)
            intValue = (Integer) value;
        if (intValue == null
            || (intValue != MixedLockLevels.LOCK_NONE
                && intValue != MixedLockLevels.LOCK_READ
                && intValue != MixedLockLevels.LOCK_OPTIMISTIC
                && intValue != MixedLockLevels.LOCK_WRITE
                && intValue != MixedLockLevels.LOCK_OPTIMISTIC_FORCE_INCREMENT
                && intValue != MixedLockLevels.LOCK_PESSIMISTIC_READ
                && intValue != MixedLockLevels.LOCK_PESSIMISTIC_WRITE
                && intValue != MixedLockLevels.LOCK_PESSIMISTIC_FORCE_INCREMENT)
                )
            throw new IllegalArgumentException(_loc.get("bad-lock-level",
                origValue).getMessage());
        return intValue;
    }
}
